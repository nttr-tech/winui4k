package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests that WDefaultComboBoxModel handles the selection and fires the same notifications as Swing's DefaultComboBoxModel (no window is used).
 * The shape seen from Java is tested in WDefaultComboBoxModelForJavaTest.
 */
class WDefaultComboBoxModelTest : FunSpec() {
    /** A listener that records received notifications as "type:index0:index1". */
    private class Recorder : WListDataListener {
        val events = mutableListOf<String>()

        override fun intervalAdded(e: WListDataEvent) {
            events += "added:${e.index0}:${e.index1}"
        }

        override fun intervalRemoved(e: WListDataEvent) {
            events += "removed:${e.index0}:${e.index1}"
        }

        override fun contentsChanged(e: WListDataEvent) {
            events += "changed:${e.index0}:${e.index1}"
        }
    }

    private fun <E> WDefaultComboBoxModel<E>.recorded(): Recorder = Recorder().also { addListDataListener(it) }

    private fun WDefaultComboBoxModel<String>.contents(): List<String> = (0 until getSize()).map { getElementAt(it) }

    init {
        test("passing an array or List selects the first item, and no arguments or an empty array leave it unselected") {
            WDefaultComboBoxModel(arrayOf("Red", "Green")).selectedItem shouldBe "Red"
            WDefaultComboBoxModel(listOf("Blue")).selectedItem shouldBe "Blue"
            WDefaultComboBoxModel<String>().selectedItem.shouldBeNull()
            WDefaultComboBoxModel(emptyArray<String>()).selectedItem.shouldBeNull()
        }

        test("adding the first item to an empty model selects it, firing the selection's CONTENTS_CHANGED (-1, -1) after INTERVAL_ADDED") {
            val model = WDefaultComboBoxModel<String>()
            val recorder = model.recorded()
            model.addElement("Red")
            model.addElement("Green")
            model.selectedItem shouldBe "Red"
            recorder.events shouldContainExactly listOf("added:0:0", "changed:-1:-1", "added:1:1")
        }

        test("selectedItem does not notify when an equal value is set, and can select a value not in the list") {
            val model = WDefaultComboBoxModel(arrayOf("Red", "Green"))
            val recorder = model.recorded()
            model.selectedItem = "Red"
            model.selectedItem = "Purple"
            model.selectedItem = null
            model.selectedItem = null
            recorder.events shouldContainExactly listOf("changed:-1:-1", "changed:-1:-1")
        }

        test("removing the selected first item selects the next, removing another selects the previous, and removing the last one selects null (INTERVAL_REMOVED after the selection notification)") {
            val model = WDefaultComboBoxModel(arrayOf("Red", "Green", "Blue"))
            val recorder = model.recorded()
            model.removeElementAt(0)
            val afterFirst = model.selectedItem
            model.selectedItem = "Blue"
            recorder.events.clear()
            model.removeElement("Blue")
            val afterLast = model.selectedItem
            model.removeElementAt(0)
            Triple(afterFirst, afterLast, model.selectedItem) shouldBe Triple("Green", "Green", null)
            recorder.events shouldContainExactly listOf("changed:-1:-1", "removed:1:1", "changed:-1:-1", "removed:0:0")
        }

        test("removing an unselected item does not change the selection, and removeElement of a value not in the list does nothing") {
            val model = WDefaultComboBoxModel(arrayOf("Red", "Green"))
            val recorder = model.recorded()
            model.removeElement("Missing")
            model.removeElementAt(1)
            model.selectedItem shouldBe "Red"
            recorder.events shouldContainExactly listOf("removed:1:1")
        }

        test("insertElementAt does not change the selection and does not select even in an empty model (only addElement selects, as in Swing)") {
            val model = WDefaultComboBoxModel<String>()
            model.insertElementAt("Green", 0)
            model.insertElementAt("Red", 0)
            model.contents() shouldContainExactly listOf("Red", "Green")
            model.selectedItem.shouldBeNull()
        }

        test("removeAllElements deselects and fires INTERVAL_REMOVED only once (nothing if empty)") {
            val model = WDefaultComboBoxModel(arrayOf("Red", "Green", "Blue"))
            val recorder = model.recorded()
            model.removeAllElements()
            model.removeAllElements()
            model.getSize() shouldBe 0
            model.selectedItem.shouldBeNull()
            recorder.events shouldContainExactly listOf("removed:0:2")
        }

        test("getElementAt returns null when out of range, and getIndexOf returns the first equal index or -1") {
            val model = WDefaultComboBoxModel(arrayOf("Red", "Green", "Red"))
            listOf(model.getElementAt(-1), model.getElementAt(3)).forEach { it.shouldBeNull() }
            model.getIndexOf("Red") shouldBe 0
            model.getIndexOf("Missing") shouldBe -1
        }

        test("addAll notifies once without changing the selection, throws ArrayIndexOutOfBoundsException for an out-of-range position, and does nothing if empty") {
            val model = WDefaultComboBoxModel<String>()
            val recorder = model.recorded()
            model.addAll(listOf("Red", "Blue"))
            model.addAll(1, listOf("Green", "Yellow"))
            model.addAll(emptyList())
            shouldThrow<ArrayIndexOutOfBoundsException> { model.addAll(5, listOf("White")) }
            model.contents() shouldContainExactly listOf("Red", "Green", "Yellow", "Blue")
            model.selectedItem.shouldBeNull()
            recorder.events shouldContainExactly listOf("added:0:1", "added:1:2")
        }

        test("listeners receive notifications starting from the last one added (same as Swing's EventListenerList)") {
            val model = WDefaultComboBoxModel<String>()
            val calls = mutableListOf<String>()
            for (name in listOf("first", "second")) {
                model.addListDataListener(
                    object : WListDataListener {
                        override fun intervalAdded(e: WListDataEvent) {
                            calls += name
                        }

                        override fun intervalRemoved(e: WListDataEvent) = Unit

                        override fun contentsChanged(e: WListDataEvent) = Unit
                    },
                )
            }
            model.insertElementAt("Red", 0)
            calls shouldContainExactly listOf("second", "first")
            model.getListDataListeners().size shouldBe 2
        }
    }
}
