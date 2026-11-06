package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.NoSuchElementException

/**
 * Tests WDefaultListModel / WAbstractListModel / WListDataEvent (DefaultListModel / AbstractListModel / ListDataEvent-style)
 * (no window is used). The expected values are the type and range of the ListDataEvents that Swing's DefaultListModel sends for the same operations.
 * The shape seen from Java is in WDefaultListModelForJavaTest.
 */
class WDefaultListModelTest : FunSpec() {
    /** A listener that records each event as a string like "A0-0" (A = added, R = removed, C = replaced, followed by index0-index1). */
    private class Recorder : WListDataListener {
        val events = mutableListOf<String>()

        override fun intervalAdded(e: WListDataEvent) {
            events += "A${e.index0}-${e.index1}"
        }

        override fun intervalRemoved(e: WListDataEvent) {
            events += "R${e.index0}-${e.index1}"
        }

        override fun contentsChanged(e: WListDataEvent) {
            events += "C${e.index0}-${e.index1}"
        }
    }

    private fun modelOf(vararg items: String): Pair<WDefaultListModel<String>, Recorder> {
        val model = WDefaultListModel<String>()
        items.forEach { model.addElement(it) }
        val recorder = Recorder()
        model.addListDataListener(recorder)
        return model to recorder
    }

    private fun WDefaultListModel<String>.contents(): List<String> = (0 until getSize()).map { getElementAt(it) }

    init {
        test("addElement notifies an addition at the end as one event (INTERVAL_ADDED, that index)") {
            val model = WDefaultListModel<String>()
            val recorder = Recorder()
            model.addListDataListener(recorder)
            model.addElement("a")
            model.addElement("b")
            recorder.events shouldContainExactly listOf("A0-0", "A1-1")
            model.contents() shouldContainExactly listOf("a", "b")
        }

        test("add(index, e) and insertElementAt(e, index) notify one addition at the insertion position") {
            val (model, recorder) = modelOf("a", "b")
            model.add(1, "x")
            model.insertElementAt("y", 0)
            recorder.events shouldContainExactly listOf("A1-1", "A0-0")
            model.contents() shouldContainExactly listOf("y", "a", "x", "b")
        }

        test("inserting out of range throws ArrayIndexOutOfBoundsException and notifies nothing") {
            val (model, recorder) = modelOf("a", "b", "c")
            shouldThrow<ArrayIndexOutOfBoundsException> { model.add(5, "x") }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.add(-1, "x") }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.insertElementAt("x", 4) }
            recorder.events.shouldBeEmpty()
            model.getSize() shouldBe 3
        }

        test("addAll(collection) notifies the added range as one event (adding 2 to the end of 3 elements gives 3..4)") {
            val (model, recorder) = modelOf("a", "b", "c")
            model.addAll(listOf("d", "e"))
            recorder.events shouldContainExactly listOf("A3-4")
            model.contents() shouldContainExactly listOf("a", "b", "c", "d", "e")
        }

        test("addAll(index, collection) notifies the range added from index as one event (inserting 3 at 1 gives 1..3)") {
            val (model, recorder) = modelOf("a", "b", "c")
            model.addAll(1, listOf("p", "q", "r"))
            recorder.events shouldContainExactly listOf("A1-3")
            model.contents() shouldContainExactly listOf("a", "p", "q", "r", "b", "c")
        }

        test("an empty addAll notifies nothing, and addAll with an out-of-range index throws ArrayIndexOutOfBoundsException") {
            val (model, recorder) = modelOf("a")
            model.addAll(emptyList())
            model.addAll(0, emptyList())
            shouldThrow<ArrayIndexOutOfBoundsException> { model.addAll(2, listOf("x")) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.addAll(-1, listOf("x")) }
            recorder.events.shouldBeEmpty()
            model.contents() shouldContainExactly listOf("a")
        }

        test("set returns the element before the replacement and notifies CONTENTS_CHANGED with that index") {
            val (model, recorder) = modelOf("a", "b")
            model.set(1, "z") shouldBe "b"
            model.setElementAt("y", 0)
            recorder.events shouldContainExactly listOf("C1-1", "C0-0")
            model.contents() shouldContainExactly listOf("y", "z")
        }

        test("remove(index) returns the removed element, and like removeElementAt notifies INTERVAL_REMOVED with that index") {
            val (model, recorder) = modelOf("a", "b", "c")
            model.remove(0) shouldBe "a"
            model.removeElementAt(1)
            recorder.events shouldContainExactly listOf("R0-0", "R1-1")
            model.contents() shouldContainExactly listOf("b")
        }

        test("removeElement removes the first match and returns true, or returns false and notifies nothing if absent") {
            val (model, recorder) = modelOf("a", "b", "a")
            model.removeElement("b") shouldBe true
            model.removeElement("nope") shouldBe false
            model.removeElement("a") shouldBe true
            recorder.events shouldContainExactly listOf("R1-1", "R0-0")
            model.contents() shouldContainExactly listOf("a")
        }

        test("removeRange(1, 3) notifies the removal of 1..3 as one event and leaves 0 and 4") {
            val (model, recorder) = modelOf("0", "1", "2", "3", "4")
            model.removeRange(1, 3)
            recorder.events shouldContainExactly listOf("R1-3")
            model.contents() shouldContainExactly listOf("0", "4")
        }

        test("removeRange throws IllegalArgumentException for from > to and ArrayIndexOutOfBoundsException out of range (removing nothing)") {
            val (model, recorder) = modelOf("0", "1", "2")
            shouldThrow<IllegalArgumentException> { model.removeRange(2, 1) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.removeRange(1, 3) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.removeRange(-1, 1) }
            recorder.events.shouldBeEmpty()
            model.contents() shouldContainExactly listOf("0", "1", "2")
        }

        test("clear and removeAllElements notify the removal of everything as one event (0..2 for 3 elements), and nothing on an empty model") {
            val (model, recorder) = modelOf("a", "b", "c")
            model.clear()
            model.clear()
            model.removeAllElements()
            recorder.events shouldContainExactly listOf("R0-2")
            model.isEmpty() shouldBe true
        }

        test("reading or writing an out-of-range index throws ArrayIndexOutOfBoundsException") {
            val (model, recorder) = modelOf("a", "b", "c")
            shouldThrow<ArrayIndexOutOfBoundsException> { model.getElementAt(3) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model[-1] }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.elementAt(3) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.set(3, "x") }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.remove(3) }
            shouldThrow<ArrayIndexOutOfBoundsException> { model.removeElementAt(3) }
            recorder.events.shouldBeEmpty()
        }

        test("firstElement and lastElement throw NoSuchElementException on an empty model and otherwise return the elements at both ends") {
            val empty = WDefaultListModel<String>()
            shouldThrow<NoSuchElementException> { empty.firstElement() }
            shouldThrow<NoSuchElementException> { empty.lastElement() }
            val (model, _) = modelOf("a", "b", "c")
            model.firstElement() shouldBe "a"
            model.lastElement() shouldBe "c"
        }

        test("search: contains / indexOf / lastIndexOf (with a start position) give the same results as Vector") {
            val (model, _) = modelOf("a", "b", "a", "b")
            model.contains("b") shouldBe true
            model.contains("z") shouldBe false
            model.indexOf("a") shouldBe 0
            model.indexOf("a", 1) shouldBe 2
            model.indexOf("a", 3) shouldBe -1
            model.lastIndexOf("b") shouldBe 3
            model.lastIndexOf("a", 1) shouldBe 0
            model.lastIndexOf("z") shouldBe -1
        }

        test("size and getSize are the same value, toArray and elements return all elements in order, and toString is the same as Vector") {
            val (model, _) = modelOf("a", "b")
            model.size shouldBe 2
            model.getSize() shouldBe 2
            model.toArray().toList() shouldContainExactly listOf("a", "b")
            Collections.list(model.elements()) shouldContainExactly listOf("a", "b")
            model.toString() shouldBe "[a, b]"
        }

        test("null can be an element too") {
            val model = WDefaultListModel<String?>()
            model.addElement(null)
            model.getElementAt(0) shouldBe null
            model.indexOf(null) shouldBe 0
        }

        test("the event source is the model itself and type uses the same constants as Swing") {
            val model = WDefaultListModel<String>()
            val sources = mutableListOf<Any>()
            val types = mutableListOf<Int>()
            model.addListDataListener(object : WListDataListener {
                override fun intervalAdded(e: WListDataEvent) {
                    sources += e.source
                    types += e.type
                }

                override fun intervalRemoved(e: WListDataEvent) {
                    sources += e.source
                    types += e.type
                }

                override fun contentsChanged(e: WListDataEvent) {
                    sources += e.source
                    types += e.type
                }
            })
            model.addElement("a")
            model.set(0, "b")
            model.remove(0)
            sources.all { it === model } shouldBe true
            types shouldContainExactly listOf(1, 0, 2)
            listOf(WListDataEvent.CONTENTS_CHANGED, WListDataEvent.INTERVAL_ADDED, WListDataEvent.INTERVAL_REMOVED) shouldContainExactly
                listOf(0, 1, 2)
        }

        test("WListDataEvent normalizes index0 > index1 to index0 <= index1, and toString has the same format as Swing") {
            val event = WListDataEvent("src", WListDataEvent.INTERVAL_ADDED, 5, 2)
            event.index0 shouldBe 2
            event.index1 shouldBe 5
            event.type shouldBe 1
            event.toString() shouldBe "com.appkitbox.winui4k.WListDataEvent[type=1,index0=2,index1=5]"
        }

        test("listeners are called starting from the most recently added one, and getListDataListeners is in the order they were added") {
            val model = WDefaultListModel<String>()
            val calls = mutableListOf<String>()
            val first = listenerNamed("first", calls)
            val second = listenerNamed("second", calls)
            model.addListDataListener(first)
            model.addListDataListener(second)
            model.addElement("a")
            calls shouldContainExactly listOf("second", "first")
            model.getListDataListeners().toList() shouldContainExactly listOf(first, second)
        }

        test("a listener added twice is called twice, and remove removes only one") {
            val model = WDefaultListModel<String>()
            val calls = mutableListOf<String>()
            val listener = listenerNamed("l", calls)
            model.addListDataListener(listener)
            model.addListDataListener(listener)
            model.addElement("a")
            calls.size shouldBe 2
            model.removeListDataListener(listener)
            model.addElement("b")
            calls.size shouldBe 3
        }

        test("a listener removed during a notification still receives it, and a listener added during it is not called for it") {
            val model = WDefaultListModel<String>()
            val calls = mutableListOf<String>()
            val removed = listenerNamed("removed", calls)
            model.addListDataListener(removed)
            model.addListDataListener(object : WListDataListener {
                override fun intervalAdded(e: WListDataEvent) {
                    model.removeListDataListener(removed)
                    model.addListDataListener(listenerNamed("late", calls))
                }

                override fun intervalRemoved(e: WListDataEvent) = Unit

                override fun contentsChanged(e: WListDataEvent) = Unit
            })
            model.addElement("a")
            calls shouldContainExactly listOf("removed")
        }

        test("a read-only model extending WAbstractListModel notifies the same ranges as Swing with the fire methods") {
            val model = object : WAbstractListModel<Int>() {
                override fun getSize(): Int = 3

                override fun getElementAt(index: Int): Int = index * 10

                fun fireAll() {
                    fireIntervalAdded(this, 0, 2)
                    fireIntervalRemoved(this, 4, 1)
                    fireContentsChanged(this, 1, 1)
                }
            }
            val recorder = Recorder()
            model.addListDataListener(recorder)
            model.fireAll()
            recorder.events shouldContainExactly listOf("A0-2", "R1-4", "C1-1")
            model.getElementAt(2) shouldBe 20
        }
    }

    private fun listenerNamed(name: String, calls: MutableList<String>) = object : WListDataListener {
        override fun intervalAdded(e: WListDataEvent) {
            calls += name
        }

        override fun intervalRemoved(e: WListDataEvent) {
            calls += name
        }

        override fun contentsChanged(e: WListDataEvent) {
            calls += name
        }
    }
}
