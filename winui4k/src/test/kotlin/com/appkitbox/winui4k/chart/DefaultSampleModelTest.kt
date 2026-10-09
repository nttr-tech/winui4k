package com.appkitbox.winui4k.chart

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests the data operations and change notifications (SampleModelEvent) of DefaultSampleModel / AbstractSampleModel (no UI needed). */
class DefaultSampleModelTest : FunSpec() {
    /** Attaches a listener that records the received events as (type, index0, index1). */
    private fun SampleModel<*>.recordEvents(): MutableList<List<Int>> {
        val events = mutableListOf<List<Int>>()
        addSampleModelListener { e -> events += listOf(e.type, e.index0, e.index1) }
        return events
    }

    init {
        test("a model created with of returns its size and each element") {
            val model = DefaultSampleModel.of(10.0, 20.0, 30.0)
            listOf(model.getSize(), model.getElementAt(0), model[2], model.isEmpty()) shouldBe
                listOf(3, 10.0, 30.0, false)
        }

        test("a model created from a collection is not affected by changes to the original collection") {
            val source = mutableListOf("a", "b")
            val model = DefaultSampleModel(source)
            source += "c"
            model.toList() shouldBe listOf("a", "b")
        }

        test("addElement / add notify INTERVAL_ADDED at the position after the addition") {
            val model = DefaultSampleModel.of("Jan", "Feb")
            val events = model.recordEvents()
            model.addElement("Mar")
            model.add(0, "Dec")
            model.toList() shouldBe listOf("Dec", "Jan", "Feb", "Mar")
            events shouldBe listOf(
                listOf(SampleModelEvent.INTERVAL_ADDED, 2, 2),
                listOf(SampleModelEvent.INTERVAL_ADDED, 0, 0),
            )
        }

        test("addAll notifies a single INTERVAL_ADDED for all elements, and nothing for an empty collection") {
            val model = DefaultSampleModel.of(1, 2)
            val events = model.recordEvents()
            model.addAll(listOf(3, 4, 5))
            model.addAll(1, listOf(9, 8))
            model.addAll(emptyList())
            model.toList() shouldBe listOf(1, 9, 8, 2, 3, 4, 5)
            events shouldBe listOf(
                listOf(SampleModelEvent.INTERVAL_ADDED, 2, 4),
                listOf(SampleModelEvent.INTERVAL_ADDED, 1, 2),
            )
        }

        test("set returns the element before the replacement and notifies CONTENTS_CHANGED") {
            val model = DefaultSampleModel.of(1.0, 2.0, 3.0)
            val events = model.recordEvents()
            val old = model.set(1, 5.0)
            listOf(old, model[1]) shouldBe listOf(2.0, 5.0)
            events shouldBe listOf(listOf(SampleModelEvent.CONTENTS_CHANGED, 1, 1))
        }

        test("remove / removeElement / removeRange notify INTERVAL_REMOVED at the positions before the removal") {
            val model = DefaultSampleModel.of("a", "b", "c", "d", "e", "f")
            val events = model.recordEvents()
            val removed = model.remove(0)
            val found = model.removeElement("d")
            val missing = model.removeElement("z")
            model.removeRange(1, 2)
            listOf(removed, found, missing) shouldBe listOf("a", true, false)
            model.toList() shouldBe listOf("b", "f")
            events shouldBe listOf(
                listOf(SampleModelEvent.INTERVAL_REMOVED, 0, 0),
                listOf(SampleModelEvent.INTERVAL_REMOVED, 2, 2),
                listOf(SampleModelEvent.INTERVAL_REMOVED, 1, 2),
            )
        }

        test("removeRange rejects fromIndex > toIndex and leaves the model unchanged") {
            val model = DefaultSampleModel.of(1, 2, 3)
            shouldThrow<IllegalArgumentException> { model.removeRange(2, 1) }
            model.toList() shouldBe listOf(1, 2, 3)
        }

        test("operations at out-of-range positions throw IndexOutOfBoundsException and do not notify") {
            val model = DefaultSampleModel.of(1, 2)
            val events = model.recordEvents()
            shouldThrow<IndexOutOfBoundsException> { model.set(2, 9) }
            shouldThrow<IndexOutOfBoundsException> { model.add(3, 9) }
            shouldThrow<IndexOutOfBoundsException> { model.remove(-1) }
            events shouldBe emptyList()
        }

        test("removeAllElements notifies INTERVAL_REMOVED for the whole range, and nothing for an empty model") {
            val model = DefaultSampleModel.of(1, 2, 3)
            val events = model.recordEvents()
            model.removeAllElements()
            model.removeAllElements()
            model.isEmpty() shouldBe true
            events shouldBe listOf(listOf(SampleModelEvent.INTERVAL_REMOVED, 0, 2))
        }

        test("setAll replaces all elements and notifies only twice, once for the removal and once for the addition") {
            val model = DefaultSampleModel.of(1, 2, 3)
            val events = model.recordEvents()
            model.setAll(listOf(7, 8))
            model.toList() shouldBe listOf(7, 8)
            events shouldBe listOf(
                listOf(SampleModelEvent.INTERVAL_REMOVED, 0, 2),
                listOf(SampleModelEvent.INTERVAL_ADDED, 0, 1),
            )
        }

        test("contains / indexOf look for equal elements") {
            val model = DefaultSampleModel.of("x", "y", "x")
            listOf(model.contains("y"), model.contains("z"), model.indexOf("x"), model.indexOf("z")) shouldBe
                listOf(true, false, 0, -1)
        }

        test("listeners are called starting from the most recently registered one, and removed listeners are not called") {
            val model = DefaultSampleModel.of(1)
            val calls = mutableListOf<String>()
            val first = SampleModelListener { calls += "first" }
            val second = SampleModelListener { calls += "second" }
            model.addSampleModelListener(first)
            model.addSampleModelListener(second)
            model.addElement(2)
            model.removeSampleModelListener(second)
            model.addElement(3)
            calls shouldBe listOf("second", "first", "first")
            model.getSampleModelListeners() shouldBe listOf(first)
        }

        test("the event source is the model that changed") {
            val model = DefaultSampleModel.of(1)
            var source: SampleModel<*>? = null
            model.addSampleModelListener { source = it.getSource() }
            model.addElement(2)
            (source === model) shouldBe true
        }

        test("AbstractSampleModel becomes a read-only model with just getSize and getElementAt") {
            val squares = object : AbstractSampleModel<Int>() {
                override fun getSize(): Int = 4

                override fun getElementAt(index: Int): Int = index * index
            }
            (0 until squares.getSize()).map { squares.getElementAt(it) } shouldBe listOf(0, 1, 4, 9)
        }
    }
}
