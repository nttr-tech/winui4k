package com.appkitbox.winui4k.ink

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests the stroke operations and change notifications of DefaultInkStrokeModel / AbstractInkStrokeModel (no UI needed). */
class DefaultInkStrokeModelTest : FunSpec() {
    /** A one-point stroke distinguishable only by its x coordinate. */
    private fun stroke(x: Double) = InkStroke(listOf(InkPoint(x, 0.0)))

    /** Attaches a listener that records the received events as (type, firstIndex, lastIndex). */
    private fun InkStrokeModel.recordEvents(): MutableList<List<Int>> {
        val events = mutableListOf<List<Int>>()
        addInkStrokeModelListener { e -> events += listOf(e.type, e.firstIndex, e.lastIndex) }
        return events
    }

    private fun InkStrokeModel.xs(): List<Double> = getStrokes().map { it.points[0].x }

    init {
        test("a model given initial contents returns the strokes in that order") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0), stroke(2.0)))
            model.getStrokeCount() shouldBe 2
            model.xs() shouldBe listOf(1.0, 2.0)
        }

        test("addStroke appends at the end and notifies an INSERT at that position") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0)))
            val events = model.recordEvents()
            model.addStroke(stroke(2.0))
            model.xs() shouldBe listOf(1.0, 2.0)
            events shouldBe listOf(listOf(InkStrokeModelEvent.INSERT, 1, 1))
        }

        test("insertStroke inserts at the given position and notifies an INSERT, and an out-of-range position throws") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0), stroke(3.0)))
            val events = model.recordEvents()
            model.insertStroke(1, stroke(2.0))
            model.insertStroke(3, stroke(4.0))
            model.xs() shouldBe listOf(1.0, 2.0, 3.0, 4.0)
            events shouldBe listOf(listOf(InkStrokeModelEvent.INSERT, 1, 1), listOf(InkStrokeModelEvent.INSERT, 3, 3))
            shouldThrow<IndexOutOfBoundsException> { model.insertStroke(5, stroke(9.0)) }
            shouldThrow<IndexOutOfBoundsException> { model.insertStroke(-1, stroke(9.0)) }
        }

        test("setStroke replaces and notifies an UPDATE, and removeStroke removes and notifies a DELETE") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0), stroke(2.0), stroke(3.0)))
            val events = model.recordEvents()
            model.setStroke(1, stroke(20.0))
            model.removeStroke(0)
            model.xs() shouldBe listOf(20.0, 3.0)
            events shouldBe listOf(listOf(InkStrokeModelEvent.UPDATE, 1, 1), listOf(InkStrokeModelEvent.DELETE, 0, 0))
        }

        test("addStrokes notifies the range with a single INSERT, and nothing for an empty list") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0)))
            val events = model.recordEvents()
            model.addStrokes(listOf(stroke(2.0), stroke(3.0)))
            model.addStrokes(emptyList())
            model.xs() shouldBe listOf(1.0, 2.0, 3.0)
            events shouldBe listOf(listOf(InkStrokeModelEvent.INSERT, 1, 2))
        }

        test("removeStrokes notifies the range with a single DELETE, and an invalid range throws") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0), stroke(2.0), stroke(3.0), stroke(4.0)))
            val events = model.recordEvents()
            model.removeStrokes(1, 2)
            model.xs() shouldBe listOf(1.0, 4.0)
            events shouldBe listOf(listOf(InkStrokeModelEvent.DELETE, 1, 2))
            shouldThrow<IndexOutOfBoundsException> { model.removeStrokes(1, 2) }
            shouldThrow<IndexOutOfBoundsException> { model.removeStrokes(1, 0) }
        }

        test("setStrokes replaces everything and notifies a change to everything") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0)))
            var received: InkStrokeModelEvent? = null
            model.addInkStrokeModelListener { received = it }
            model.setStrokes(listOf(stroke(5.0), stroke(6.0)))
            model.xs() shouldBe listOf(5.0, 6.0)
            received!!.isDataChanged shouldBe true
            received!!.source shouldBe model
        }

        test("clear notifies a DELETE of the whole range, and nothing for an empty model") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0), stroke(2.0)))
            val events = model.recordEvents()
            model.clear()
            model.clear()
            model.getStrokeCount() shouldBe 0
            events shouldBe listOf(listOf(InkStrokeModelEvent.DELETE, 0, 1))
        }

        test("getStrokes is a snapshot that does not reflect later changes to the model") {
            val model = DefaultInkStrokeModel(listOf(stroke(1.0)))
            val snapshot = model.getStrokes()
            model.addStroke(stroke(2.0))
            snapshot.size shouldBe 1
        }

        test("listeners are called starting from the most recently registered one, and removed ones are not called") {
            val model = DefaultInkStrokeModel()
            val calls = mutableListOf<String>()
            val first = InkStrokeModelListener { calls += "first" }
            val second = InkStrokeModelListener { calls += "second" }
            model.addInkStrokeModelListener(first)
            model.addInkStrokeModelListener(second)
            model.addStroke(stroke(1.0))
            model.removeInkStrokeModelListener(second)
            model.addStroke(stroke(2.0))
            calls shouldBe listOf("second", "first", "first")
            model.getInkStrokeModelListeners() shouldBe listOf(first)
        }
    }
}
