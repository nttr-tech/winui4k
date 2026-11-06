package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Elements that have the same string but should be distinguished as different things (toString is only the name). */
private class Fruit(val name: String) {
    override fun toString(): String = name
}

/** A gateway for handling WList and WListBox in the same tests (both have a JList-style model API). */
private interface ListUnderTest {
    var model: WListModel<*>
    var selectedIndex: Int
    val selectedValue: Any?
    val selectedValuesList: List<Any?>
    val selectedItem: String?
    val itemCount: Int

    fun getItem(index: Int): String

    fun addItem(item: String)

    fun removeItem(index: Int)

    fun removeAllItems()

    fun setListData(listData: Array<out Any?>)

    fun setListData(listData: List<*>)

    /** Switches to a mode allowing multiple selection and selects everything. */
    fun selectAllInMultipleMode()

    /** The strings put into the native Items. */
    fun nativeTexts(): List<String>
}

private class ListViewUnderTest(val list: WList) : ListUnderTest {
    override var model: WListModel<*> by list::model
    override var selectedIndex: Int by list::selectedIndex
    override val selectedValue: Any? get() = list.selectedValue
    override val selectedValuesList: List<Any?> get() = list.selectedValuesList
    override val selectedItem: String? get() = list.selectedItem
    override val itemCount: Int get() = list.itemCount

    override fun getItem(index: Int): String = list.getItem(index)

    override fun addItem(item: String) = list.addItem(item)

    override fun removeItem(index: Int) = list.removeItem(index)

    override fun removeAllItems() = list.removeAllItems()

    override fun setListData(listData: Array<out Any?>) = list.setListData(listData)

    override fun setListData(listData: List<*>) = list.setListData(listData)

    override fun selectAllInMultipleMode() {
        list.selectionMode = ListViewSelectionMode.EXTENDED
        list.selectAll()
    }

    override fun nativeTexts(): List<String> = list.listItems.nativeTexts()
}

private class ListBoxUnderTest(val list: WListBox) : ListUnderTest {
    override var model: WListModel<*> by list::model
    override var selectedIndex: Int by list::selectedIndex
    override val selectedValue: Any? get() = list.selectedValue
    override val selectedValuesList: List<Any?> get() = list.selectedValuesList
    override val selectedItem: String? get() = list.selectedItem
    override val itemCount: Int get() = list.itemCount

    override fun getItem(index: Int): String = list.getItem(index)

    override fun addItem(item: String) = list.addItem(item)

    override fun removeItem(index: Int) = list.removeItem(index)

    override fun removeAllItems() = list.removeAllItems()

    override fun setListData(listData: Array<out Any?>) = list.setListData(listData)

    override fun setListData(listData: List<*>) = list.setListData(listData)

    override fun selectAllInMultipleMode() {
        list.selectionMode = SelectionMode.EXTENDED
        list.selectAll()
    }

    override fun nativeTexts(): List<String> = list.listItems.nativeTexts()
}

/**
 * Tests the data API of WList / WListBox backed by a [WListModel] (JList's getModel / setModel / setListData / getSelectedValue).
 * The expected values are based on the behavior of Swing's JList and DefaultListSelectionModel (model changes shift, clear or keep the selection).
 */
abstract class ListModelBindingSpec(
    private val create: (WListModel<*>) -> Any,
    private val createFromStrings: (List<String>) -> Any,
) : FunSpec() {
    private fun wrap(component: Any): ListUnderTest = when (component) {
        is WList -> ListViewUnderTest(component)
        is WListBox -> ListBoxUnderTest(component)
        else -> error("unexpected component: $component")
    }

    private fun listWith(model: WListModel<*>): ListUnderTest = wrap(create(model))

    private fun modelOf(vararg values: Any?): WDefaultListModel<Any?> = WDefaultListModel<Any?>().also { model ->
        values.forEach { model.addElement(it) }
    }

    init {
        test("model elements are shown as their toString() strings, and null elements become empty strings") {
            onUiThreadGet {
                val list = listWith(modelOf(Fruit("Apple"), 3, null))
                Triple(list.nativeTexts(), list.itemCount, list.getItem(1))
            } shouldBe Triple(listOf("Apple", "3", ""), 3, "3")
        }

        test("adding, inserting, replacing, removing, removing a range and removing everything in the model are reflected in the display immediately") {
            onUiThreadGet {
                val model = modelOf("a", "b")
                val list = listWith(model)
                val steps = mutableListOf<List<String>>()
                model.addElement("c")
                steps += list.nativeTexts()
                model.add(0, "First")
                steps += list.nativeTexts()
                model.addAll(2, listOf("x", "y"))
                steps += list.nativeTexts()
                model.set(1, "A")
                steps += list.nativeTexts()
                model.removeElementAt(0)
                steps += list.nativeTexts()
                model.removeRange(1, 2)
                steps += list.nativeTexts()
                model.clear()
                steps += list.nativeTexts()
                steps
            } shouldContainExactly listOf(
                listOf("a", "b", "c"),
                listOf("First", "a", "b", "c"),
                listOf("First", "a", "x", "y", "b", "c"),
                listOf("First", "A", "x", "y", "b", "c"),
                listOf("A", "x", "y", "b", "c"),
                listOf("A", "b", "c"),
                emptyList(),
            )
        }

        test("selectedValue is the model's element itself at the selected position, distinguishing elements with the same string") {
            onUiThreadGet {
                val first = Fruit("Same")
                val second = Fruit("Same")
                val list = listWith(modelOf(first, second))
                list.selectedIndex = 1
                list.selectedValue shouldBeSameInstanceAs second
                list.selectedValuesList.single() shouldBeSameInstanceAs second
                list.selectedItem
            } shouldBe "Same"
        }

        test("when nothing is selected, selectedValue and selectedItem are null and selectedValuesList is empty") {
            onUiThreadGet {
                val list = listWith(modelOf("a"))
                list.selectedValue shouldBe null
                list.selectedItem shouldBe null
                list.selectedValuesList
            }.shouldBeEmpty()
        }

        test("selectedValuesList in multiple selection is in index order, including elements with the same string") {
            onUiThreadGet {
                val values = arrayOf<Any?>(Fruit("x"), Fruit("x"), "y")
                val list = listWith(modelOf(*values))
                list.selectAllInMultipleMode()
                list.selectedValuesList.zip(values).all { (actual, expected) -> actual === expected } to list.selectedValuesList.size
            } shouldBe (true to 3)
        }

        test("inserting or removing before the selected element shifts the selection, and removing the selected element clears it (same as Swing)") {
            onUiThreadGet {
                val model = modelOf("a", "b", "c")
                val list = listWith(model)
                list.selectedIndex = 1
                model.add(0, "Before")
                val afterInsert = list.selectedIndex to list.selectedValue
                model.removeElementAt(0)
                val afterRemoveBefore = list.selectedIndex to list.selectedValue
                model.addElement("After")
                val afterAppend = list.selectedIndex to list.selectedValue
                model.removeElementAt(1)
                listOf(afterInsert, afterRemoveBefore, afterAppend, list.selectedIndex to list.selectedValue)
            } shouldContainExactly listOf(2 to "b", 1 to "b", 1 to "b", -1 to null)
        }

        test("replacing the selected element keeps the selection at that position, and selectedValue becomes the new element (same as Swing)") {
            onUiThreadGet {
                val model = modelOf("a", "b", "c")
                val list = listWith(model)
                list.selectedIndex = 1
                model.set(1, "B")
                Triple(list.selectedIndex, list.selectedValue, list.nativeTexts())
            } shouldBe Triple(1, "B", listOf("a", "B", "c"))
        }

        test("replacing selected elements in multiple selection keeps all the selections") {
            onUiThreadGet {
                val model = modelOf("a", "b", "c")
                val list = listWith(model)
                list.selectAllInMultipleMode()
                model.set(1, "B")
                list.selectedValuesList
            } shouldContainExactly listOf("a", "B", "c")
        }

        test("replacing model switches to the new model's elements and clears the selection, and changes of the old model no longer arrive") {
            onUiThreadGet {
                val old = modelOf("a", "b")
                val list = listWith(old)
                list.selectedIndex = 0
                val replacement = modelOf("x", "y", "z")
                list.model = replacement
                old.addElement("c")
                Triple(list.model === replacement, list.selectedIndex, list.nativeTexts())
            } shouldBe Triple(true, -1, listOf("x", "y", "z"))
        }

        test("setListData replaces the contents with either an array or a list, and addItem can still add afterwards") {
            onUiThreadGet {
                val list = listWith(modelOf("Before"))
                list.setListData(arrayOf<Any?>("a", 1))
                val fromArray = list.nativeTexts()
                list.setListData(listOf("x", "y"))
                list.addItem("z")
                Triple(fromArray, list.nativeTexts(), list.model.getSize())
            } shouldBe Triple(listOf("a", "1"), listOf("x", "y", "z"), 3)
        }

        test("addItem / removeItem / removeAllItems of a list created from a list of strings edit the model (WDefaultListModel)") {
            onUiThreadGet {
                val list = wrap(createFromStrings(listOf("Mon", "Tue")))
                val model = list.model as WDefaultListModel<*>
                list.addItem("Wed")
                val added = model.size
                list.removeItem(0)
                val afterRemove = model.getElementAt(0)
                list.removeAllItems()
                listOf<Any?>(added, afterRemove, model.size, list.itemCount)
            } shouldContainExactly listOf<Any?>(3, "Tue", 0, 0)
        }

        test("with an app-specific read-only model, the display and getItem follow the model, and the addItem family throws UnsupportedOperationException") {
            onUiThreadGet {
                val data = arrayOf("One", "Two", "Three")
                val model = object : WAbstractListModel<String>() {
                    override fun getSize(): Int = data.size

                    override fun getElementAt(index: Int): String = data[index]

                    fun change(index: Int, value: String) {
                        data[index] = value
                        fireContentsChanged(this, index, index)
                    }
                }
                val list = listWith(model)
                model.change(2, "III")
                shouldThrow<UnsupportedOperationException> { list.addItem("Four") }
                shouldThrow<UnsupportedOperationException> { list.removeItem(0) }
                shouldThrow<UnsupportedOperationException> { list.removeAllItems() }
                list.nativeTexts() to list.getItem(2)
            } shouldBe (listOf("One", "Two", "III") to "III")
        }

        test("even with a model that does not report ranges correctly (contentsChanged with index -1), the whole list is reread and the display matches") {
            onUiThreadGet {
                val data = mutableListOf("a", "b")
                val model = object : WAbstractListModel<String>() {
                    override fun getSize(): Int = data.size

                    override fun getElementAt(index: Int): String = data[index]

                    fun reloadAll() = fireContentsChanged(this, -1, -1)
                }
                val list = listWith(model)
                data += "c"
                data[0] = "A"
                model.reloadAll()
                list.nativeTexts()
            } shouldContainExactly listOf("A", "b", "c")
        }

        test("getItem out of range throws ArrayIndexOutOfBoundsException") {
            onUiThreadGet {
                val list = listWith(modelOf("a"))
                runCatching { list.getItem(1) }.exceptionOrNull()?.javaClass
            } shouldBe ArrayIndexOutOfBoundsException::class.java
        }
    }
}

/** The model API of [WList] (ListView). */
class WListModelTest : ListModelBindingSpec({ WList(it) }, { WList(it) })

/** The model API of [WListBox] (ListBox). */
class WListBoxModelTest : ListModelBindingSpec({ WListBox(it) }, { WListBox(it) })
