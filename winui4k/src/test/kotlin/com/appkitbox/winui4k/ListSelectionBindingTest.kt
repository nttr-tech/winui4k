package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** An interface to handle WList and WListBox in the same tests (both have the JList-like selection API). */
private interface SelectableList {
    val component: WComponent
    var selectionModel: WListSelectionModel
    var selectedIndices: IntArray
    var selectedIndex: Int
    val selectedItem: String?
    val selectedValue: Any?
    val selectedValuesList: List<Any?>
    val isSelectionEmpty: Boolean
    val minSelectionIndex: Int
    val maxSelectionIndex: Int
    val anchorSelectionIndex: Int
    val leadSelectionIndex: Int
    var model: WListModel<*>

    /** The native selection (SelectedItems converted to indices by identity). */
    val nativeSelection: List<Int>

    /** Whether the native mode is single selection. */
    val isNativeSingle: Boolean

    fun setSelectionMode(selectionMode: Int)

    fun setSelectionInterval(anchor: Int, lead: Int)

    fun addSelectionInterval(anchor: Int, lead: Int)

    fun removeSelectionInterval(index0: Int, index1: Int)

    fun clearSelection()

    fun isSelectedIndex(index: Int): Boolean

    fun setSelectedValue(anObject: Any?, shouldScroll: Boolean)

    fun addListSelectionListener(listener: WListSelectionListener)

    fun removeListSelectionListener(listener: WListSelectionListener)

    fun addRunnableListener(listener: () -> Unit)

    /** Sets the native selection directly to [indices] (instead of a display operation). */
    fun selectNatively(indices: List<Int>)

    /** Sets the WinUI enumeration mode to Ctrl / Shift multiple selection (EXTENDED) (without starting the Swing-style sync). */
    fun useNativeExtended()
}

private class ListViewSelection(val list: WList) : SelectableList {
    override val component: WComponent get() = list
    override var selectionModel: WListSelectionModel by list::selectionModel
    override var selectedIndices: IntArray by list::selectedIndices
    override var selectedIndex: Int by list::selectedIndex
    override val selectedItem: String? get() = list.selectedItem
    override val selectedValue: Any? get() = list.selectedValue
    override val selectedValuesList: List<Any?> get() = list.selectedValuesList
    override val isSelectionEmpty: Boolean get() = list.isSelectionEmpty
    override val minSelectionIndex: Int get() = list.minSelectionIndex
    override val maxSelectionIndex: Int get() = list.maxSelectionIndex
    override val anchorSelectionIndex: Int get() = list.anchorSelectionIndex
    override val leadSelectionIndex: Int get() = list.leadSelectionIndex
    override var model: WListModel<*> by list::model
    override val nativeSelection: List<Int> get() = list.listItems.selectedIndices().toList()
    override val isNativeSingle: Boolean get() = list.selectionMode == ListViewSelectionMode.SINGLE

    override fun setSelectionMode(selectionMode: Int) = list.setSelectionMode(selectionMode)

    override fun setSelectionInterval(anchor: Int, lead: Int) = list.setSelectionInterval(anchor, lead)

    override fun addSelectionInterval(anchor: Int, lead: Int) = list.addSelectionInterval(anchor, lead)

    override fun removeSelectionInterval(index0: Int, index1: Int) = list.removeSelectionInterval(index0, index1)

    override fun clearSelection() = list.clearSelection()

    override fun isSelectedIndex(index: Int): Boolean = list.isSelectedIndex(index)

    override fun setSelectedValue(anObject: Any?, shouldScroll: Boolean) = list.setSelectedValue(anObject, shouldScroll)

    override fun addListSelectionListener(listener: WListSelectionListener) = list.addListSelectionListener(listener)

    override fun removeListSelectionListener(listener: WListSelectionListener) = list.removeListSelectionListener(listener)

    override fun addRunnableListener(listener: () -> Unit) = list.addListSelectionListener(listener)

    override fun selectNatively(indices: List<Int>) = list.listItems.applySelection(indices)

    override fun useNativeExtended() {
        list.selectionMode = ListViewSelectionMode.EXTENDED
    }
}

private class ListBoxSelection(val list: WListBox) : SelectableList {
    override val component: WComponent get() = list
    override var selectionModel: WListSelectionModel by list::selectionModel
    override var selectedIndices: IntArray by list::selectedIndices
    override var selectedIndex: Int by list::selectedIndex
    override val selectedItem: String? get() = list.selectedItem
    override val selectedValue: Any? get() = list.selectedValue
    override val selectedValuesList: List<Any?> get() = list.selectedValuesList
    override val isSelectionEmpty: Boolean get() = list.isSelectionEmpty
    override val minSelectionIndex: Int get() = list.minSelectionIndex
    override val maxSelectionIndex: Int get() = list.maxSelectionIndex
    override val anchorSelectionIndex: Int get() = list.anchorSelectionIndex
    override val leadSelectionIndex: Int get() = list.leadSelectionIndex
    override var model: WListModel<*> by list::model
    override val nativeSelection: List<Int> get() = list.listItems.selectedIndices().toList()
    override val isNativeSingle: Boolean get() = list.selectionMode == SelectionMode.SINGLE

    override fun setSelectionMode(selectionMode: Int) = list.setSelectionMode(selectionMode)

    override fun setSelectionInterval(anchor: Int, lead: Int) = list.setSelectionInterval(anchor, lead)

    override fun addSelectionInterval(anchor: Int, lead: Int) = list.addSelectionInterval(anchor, lead)

    override fun removeSelectionInterval(index0: Int, index1: Int) = list.removeSelectionInterval(index0, index1)

    override fun clearSelection() = list.clearSelection()

    override fun isSelectedIndex(index: Int): Boolean = list.isSelectedIndex(index)

    override fun setSelectedValue(anObject: Any?, shouldScroll: Boolean) = list.setSelectedValue(anObject, shouldScroll)

    override fun addListSelectionListener(listener: WListSelectionListener) = list.addListSelectionListener(listener)

    override fun removeListSelectionListener(listener: WListSelectionListener) = list.removeListSelectionListener(listener)

    override fun addRunnableListener(listener: () -> Unit) = list.addListSelectionListener(listener)

    override fun selectNatively(indices: List<Int>) = list.listItems.applySelection(indices)

    override fun useNativeExtended() {
        list.selectionMode = SelectionMode.EXTENDED
    }
}

/** A record of events (whether the source is the list, the changed range, whether adjusting). */
private data class Recorded(val sourceIsList: Boolean, val first: Int, val last: Int, val adjusting: Boolean)

/**
 * Tests for the Swing-style selection API of WList / WListBox (two-way sync with selectionModel, setSelectionMode(int), interval selection,
 * WListSelectionListener). Expected values come from the behavior of Swing's JList and DefaultListSelectionModel.
 */
abstract class ListSelectionBindingSpec(private val create: (WListModel<*>) -> Any) : FunSpec() {
    private fun listOf5(): SelectableList = wrap(create(WDefaultListModel<Any?>().also { m -> "abcde".forEach { m.addElement(it.toString()) } }))

    private fun wrap(component: Any): SelectableList = when (component) {
        is WList -> ListViewSelection(component)
        is WListBox -> ListBoxSelection(component)
        else -> error("unexpected component: $component")
    }

    private fun SelectableList.record(): MutableList<Recorded> {
        val events = ArrayList<Recorded>()
        addListSelectionListener { e -> events += Recorded(e.source === component, e.firstIndex, e.lastIndex, e.valueIsAdjusting) }
        return events
    }

    init {
        test("the default is multiple interval selection, the same as JList: the selectionMode of selectionModel is MULTIPLE_INTERVAL_SELECTION, the display is multiple selection with Ctrl / Shift, and nothing is selected") {
            onUiThreadGet {
                val list = listOf5()
                listOf(list.selectionModel.getSelectionMode(), list.minSelectionIndex, list.maxSelectionIndex, list.selectedIndices.size) to
                    (list.isSelectionEmpty to list.isNativeSingle)
            } shouldBe (listOf(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION, -1, -1, 0) to (true to false))
        }

        test("adding intervals with the default allows disjoint selections, which are also reflected in the display (the same as JList's default)") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionInterval(0, 1)
                list.addSelectionInterval(3, 3)
                Triple(list.selectedIndices.toList(), list.nativeSelection, list.selectedValuesList)
            } shouldBe Triple(listOf(0, 1, 3), listOf(0, 1, 3), listOf("a", "b", "d"))
        }

        test("selecting on the display side is copied to selectionModel, and an event with the list as source is delivered") {
            onUiThreadGet {
                val list = listOf5()
                val events = list.record()
                list.selectedIndex = 2
                list.selectedIndex = 0
                Triple(events.toList(), list.selectionModel.isSelectedIndex(0), list.anchorSelectionIndex to list.leadSelectionIndex)
            } shouldBe Triple(
                listOf(Recorded(true, 2, 2, false), Recorded(true, 0, 2, false)),
                true,
                0 to 0,
            )
        }

        test("in single selection, setSelectionInterval(1, 3) selects only the lead 3 (as in Swing)") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
                list.setSelectionInterval(1, 3)
                Triple(list.selectedIndices.toList(), list.selectedIndex, list.nativeSelection)
            } shouldBe Triple(listOf(3), 3, listOf(3))
        }

        test("in MULTIPLE_INTERVAL_SELECTION, selecting, adding and removing intervals are reflected in the display, and anchor and lead are the ends of the last operation") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                list.setSelectionInterval(1, 2)
                val afterSet = list.nativeSelection
                list.addSelectionInterval(4, 4)
                val afterAdd = list.nativeSelection
                list.removeSelectionInterval(2, 2)
                listOf(afterSet, afterAdd, list.nativeSelection, list.selectedValuesList, listOf(list.anchorSelectionIndex, list.leadSelectionIndex)) to
                    list.isNativeSingle
            } shouldBe (listOf(listOf(1, 2), listOf(1, 2, 4), listOf(1, 4), listOf("b", "e"), listOf(2, 2)) to false)
        }

        test("changing selectionModel directly is also reflected in the display") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                list.selectionModel.setSelectionInterval(0, 1)
                list.selectionModel.addSelectionInterval(3, 3)
                list.nativeSelection
            } shouldContainExactly listOf(0, 1, 3)
        }

        test("in SINGLE_INTERVAL_SELECTION, when the display side adds a disjoint item, the selection becomes only the added range") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
                list.setSelectionInterval(0, 1)
                list.selectNatively(listOf(0, 1, 3))
                Triple(list.selectedIndices.toList(), list.nativeSelection, list.selectionModel.getSelectionMode())
            } shouldBe Triple(listOf(3), listOf(3), WListSelectionModel.SINGLE_INTERVAL_SELECTION)
        }

        test("switching from multiple selection to SINGLE_SELECTION keeps only the frontmost selection (as in Swing since JDK 9)") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                list.selectedIndices = intArrayOf(1, 2, 4)
                list.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
                Triple(list.selectedIndices.toList(), list.nativeSelection, list.isNativeSingle)
            } shouldBe Triple(listOf(1), listOf(1), true)
        }

        test("setting selectedIndices ignores out-of-range indices, and Runnable listeners are called only once") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                var runs = 0
                list.addRunnableListener { runs++ }
                list.selectedIndices = intArrayOf(0, 2, 9)
                Triple(list.selectedIndices.toList(), list.nativeSelection, runs)
            } shouldBe Triple(listOf(0, 2), listOf(0, 2), 1)
        }

        test("even after syncing starts, Runnable listeners are called once per selection change on the display side") {
            onUiThreadGet {
                val list = listOf5()
                list.selectionModel
                var runs = 0
                list.addRunnableListener { runs++ }
                list.selectedIndex = 0
                list.selectedIndex = 2
                runs
            } shouldBe 2
        }

        test("Kotlin lambdas split into the Runnable form and the WListSelectionListener form depending on whether they take an argument") {
            onUiThreadGet {
                val list = WList(listOf("a", "b"))
                var plain = 0
                var withEvent = 0
                list.addListSelectionListener { plain++ }
                list.addListSelectionListener { _: WListSelectionEvent -> withEvent++ }
                list.addListSelectionListener { e -> if (!e.valueIsAdjusting) withEvent++ }
                list.selectedIndex = 1
                plain to withEvent
            } shouldBe (1 to 2)
        }

        test("replacing selectionModel reflects the new model's selection and mode in the display, and changes of the old model are not delivered") {
            onUiThreadGet {
                val list = listOf5()
                val old = list.selectionModel
                val replacement = WDefaultListSelectionModel()
                replacement.setSelectionInterval(0, 1)
                list.selectionModel = replacement
                old.setSelectionInterval(4, 4)
                Triple(list.nativeSelection, list.isNativeSingle, list.selectionModel === replacement)
            } shouldBe Triple(listOf(0, 1), false, true)
        }

        test("a listener removed with removeListSelectionListener is not called") {
            onUiThreadGet {
                val list = listOf5()
                var calls = 0
                val listener = WListSelectionListener { calls++ }
                list.addListSelectionListener(listener)
                list.setSelectionInterval(1, 1)
                list.removeListSelectionListener(listener)
                list.setSelectionInterval(2, 2)
                calls
            } shouldBe 1
        }

        test("setSelectedValue selects the first element equal by equals, does not change if not found, and deselects if null (as in Swing)") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectedValue("c", true)
                val found = list.selectedIndices.toList()
                list.setSelectedValue("z", false)
                val notFound = list.selectedIndices.toList()
                list.setSelectedValue(null, false)
                listOf(found, notFound, list.selectedIndices.toList(), list.nativeSelection)
            } shouldBe listOf(listOf(2), listOf(2), emptyList(), emptyList())
        }

        test("in multiple selection, adding elements in front shifts the selection, and inserting at a selected position also selects the inserted elements (same as Swing's insertIndexInterval)") {
            onUiThreadGet {
                val model = WDefaultListModel<Any?>().also { m -> "abcd".forEach { m.addElement(it.toString()) } }
                val list = wrap(create(model))
                list.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                list.setSelectionInterval(1, 2)
                model.add(0, "x")
                val shifted = list.nativeSelection to list.selectedIndices.toList()
                model.add(2, "y")
                val inserted = list.nativeSelection to list.selectedValuesList
                model.removeElementAt(2)
                Triple(shifted, inserted, list.selectedValuesList)
            } shouldBe Triple(listOf(2, 3) to listOf(2, 3), listOf(2, 3, 4) to listOf("y", "b", "c"), listOf("b", "c"))
        }

        test("in single selection, inserting at the selected position shifts the selection back with the original element") {
            onUiThreadGet {
                val model = WDefaultListModel<Any?>().also { m -> "abc".forEach { m.addElement(it.toString()) } }
                val list = wrap(create(model))
                list.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
                list.setSelectionInterval(1, 1)
                model.add(1, "x")
                list.nativeSelection to list.selectedIndices.toList()
            } shouldBe (listOf(2) to listOf(2))
        }

        test("inserting at a selected position with the default multiple interval selection also selects the inserted element (the same as Swing's DefaultListSelectionModel)") {
            onUiThreadGet {
                val model = WDefaultListModel<Any?>().also { m -> "abc".forEach { m.addElement(it.toString()) } }
                val list = wrap(create(model))
                list.setSelectionInterval(1, 1)
                model.add(1, "x")
                list.nativeSelection to list.selectedValuesList
            } shouldBe (listOf(1, 2) to listOf("x", "b"))
        }

        test("replacing model also clears the selectionModel's selection (same as JList.setModel)") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionInterval(1, 1)
                list.model = WDefaultListModel<Any?>().also { it.addElement("x") }
                list.selectionModel.isSelectionEmpty() to list.nativeSelection
            } shouldBe (true to emptyList())
        }

        test("clearSelection and isSelectedIndex") {
            onUiThreadGet {
                val list = listOf5()
                list.setSelectionInterval(3, 3)
                val before = list.isSelectedIndex(3)
                list.clearSelection()
                Triple(before, list.isSelectedIndex(3), list.nativeSelection)
            } shouldBe Triple(true, false, emptyList())
        }

        test("selectedIndex is the smallest selection both before and after the Swing-style sync starts, and selectedItem and selectedValue are the element at that position (as in JList)") {
            onUiThreadGet {
                val list = listOf5()
                list.useNativeExtended()
                list.selectedIndex = 3 // Select d first, then add b (the native SelectedIndex stays d)
                list.selectNatively(listOf(1, 3))
                val before = listOf(list.selectedIndex, list.selectedItem, list.selectedValue)
                list.isSelectedIndex(1) // Starts the sync
                before to listOf(list.selectedIndex, list.selectedItem, list.selectedValue)
            } shouldBe (listOf(1, "b", "b") to listOf(1, "b", "b"))
        }

        test("passing a non-constant to setSelectionMode throws IllegalArgumentException") {
            onUiThreadGet {
                val list = listOf5()
                shouldThrow<IllegalArgumentException> { list.setSelectionMode(3) }
                shouldThrow<IllegalArgumentException> { list.setSelectionMode(-1) }
                list.isNativeSingle to list.selectionModel.getSelectionMode()
            } shouldBe (false to WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        }
    }
}

/** The Swing-style selection API of [WList] (ListView). */
@Tags("E2E")
class WListSelectionTest : ListSelectionBindingSpec({ WList(it) }) {
    init {
        test("switching to a mode that does not allow selection (NONE) also clears the selectionModel's selection, and switching back to single selection does not restore the previous selection") {
            onUiThreadGet {
                val list = WList(listOf("a", "b", "c"))
                list.setSelectionInterval(1, 1)
                list.selectionMode = ListViewSelectionMode.NONE
                val none = list.selectedIndices.toList() to list.selectionModel.isSelectionEmpty()
                list.selectionMode = ListViewSelectionMode.SINGLE
                listOf(none, list.selectedIndices.toList() to list.listItems.selectedIndices().toList())
            } shouldBe listOf(emptyList<Int>() to true, emptyList<Int>() to emptyList<Int>())
        }
    }
}

/** The Swing-style selection API of [WListBox] (ListBox). */
@Tags("E2E")
class WListBoxSelectionTest : ListSelectionBindingSpec({ WListBox(it) })
