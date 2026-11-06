package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests WDefaultListSelectionModel (no UI needed). The expected values were derived by hand from the rules of Swing's DefaultListSelectionModel.
 * Events are compared as (firstIndex, lastIndex, valueIsAdjusting) triples.
 */
@Tags("Pure")
class WDefaultListSelectionModelTest : FunSpec() {
    private fun WDefaultListSelectionModel.record(): MutableList<Triple<Int, Int, Boolean>> {
        val events = mutableListOf<Triple<Int, Int, Boolean>>()
        addListSelectionListener { e -> events += Triple(e.firstIndex, e.lastIndex, e.valueIsAdjusting) }
        return events
    }

    private fun WDefaultListSelectionModel.indices(): List<Int> = getSelectedIndices().toList()

    private fun model(mode: Int = WListSelectionModel.MULTIPLE_INTERVAL_SELECTION) =
        WDefaultListSelectionModel().apply { setSelectionMode(mode) }

    init {
        test("right after creation nothing is selected, anchor / lead / min / max are -1, and the mode is MULTIPLE_INTERVAL") {
            val m = WDefaultListSelectionModel()
            listOf(
                m.isSelectionEmpty(),
                m.getMinSelectionIndex(),
                m.getMaxSelectionIndex(),
                m.getAnchorSelectionIndex(),
                m.getLeadSelectionIndex(),
                m.getSelectionMode(),
                m.getValueIsAdjusting(),
                m.getSelectedItemsCount(),
            ) shouldBe listOf(true, -1, -1, -1, -1, 2, false, 0)
            m.indices() shouldBe emptyList()
        }

        test("the selection mode constants have the same values as Swing") {
            listOf(
                WListSelectionModel.SINGLE_SELECTION,
                WListSelectionModel.SINGLE_INTERVAL_SELECTION,
                WListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
            ) shouldBe listOf(0, 1, 2)
        }

        test("setSelectionMode accepts the 3 constants and throws IllegalArgumentException for anything else") {
            val m = WDefaultListSelectionModel()
            val modes = (0..2).map { mode ->
                m.setSelectionMode(mode)
                m.getSelectionMode()
            }
            modes shouldBe listOf(0, 1, 2)
            shouldThrow<IllegalArgumentException> { m.setSelectionMode(3) }
            shouldThrow<IllegalArgumentException> { m.setSelectionMode(-1) }
            m.getSelectionMode() shouldBe 2
        }

        test("changing to a narrower mode shrinks the selection to the smallest row (SINGLE) or the range continuing from the smallest row (SINGLE_INTERVAL) (as since JDK 9)") {
            val m = model()
            m.setSelectionInterval(1, 2)
            m.addSelectionInterval(5, 6)
            val events = m.record()
            m.setSelectionMode(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            val interval = listOf(m.indices(), m.getAnchorSelectionIndex(), m.getLeadSelectionIndex())
            m.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
            val single = listOf(m.indices(), m.getAnchorSelectionIndex(), m.getLeadSelectionIndex())
            listOf(interval, single, events) shouldBe listOf(
                listOf(listOf(1, 2), 1, 2),
                listOf(listOf(1), 1, 1),
                listOf(Triple(1, 6, false), Triple(1, 2, false)),
            )
        }

        test("changing to a wider mode, or when the selection is empty, does not change the selection or fire events") {
            val m = model(WListSelectionModel.SINGLE_SELECTION)
            m.setSelectionInterval(3, 3)
            val events = m.record()
            m.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            m.addSelectionInterval(5, 5)
            m.clearSelection()
            events.clear()
            m.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
            listOf(m.indices(), events) shouldBe listOf(emptyList<Int>(), emptyList<Triple<Int, Int, Boolean>>())
        }

        test("setSelectionInterval selects the range even with index0 > index1, with anchor=index0 and lead=index1, and fires one event") {
            val m = model()
            val events = m.record()
            m.setSelectionInterval(3, 1)
            m.indices() shouldBe listOf(1, 2, 3)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex(), m.getMinSelectionIndex(), m.getMaxSelectionIndex()) shouldBe
                listOf(3, 1, 1, 3)
            events shouldBe listOf(Triple(1, 3, false))
        }

        test("setSelectionInterval replaces the previous selection, overlapping rows stay selected, and the event spans from the minimum to the maximum changed row") {
            val m = model()
            m.setSelectionInterval(1, 3)
            val events = m.record()
            m.setSelectionInterval(2, 5)
            m.indices() shouldBe listOf(2, 3, 4, 5)
            // Row 1 is deselected, 4 and 5 are selected, the anchor moves from 3 to 2 and the lead from 1 to 5
            events shouldBe listOf(Triple(1, 5, false))
        }

        test("selecting the same range again fires no event") {
            val m = model()
            m.setSelectionInterval(2, 2)
            val events = m.record()
            m.setSelectionInterval(2, 2)
            events shouldBe emptyList()
            m.indices() shouldBe listOf(2)
        }

        test("setSelectionInterval / addSelectionInterval / removeSelectionInterval with -1 for either argument do nothing") {
            val m = model()
            m.setSelectionInterval(1, 2)
            val events = m.record()
            m.setSelectionInterval(-1, 3)
            m.setSelectionInterval(3, -1)
            m.addSelectionInterval(-1, 5)
            m.removeSelectionInterval(1, -1)
            m.indices() shouldBe listOf(1, 2)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(1, 2)
            events shouldBe emptyList()
        }

        test("addSelectionInterval in MULTIPLE_INTERVAL adds a non-contiguous range, and anchor / lead become the ends of the added range") {
            val m = model()
            m.setSelectionInterval(1, 1)
            val events = m.record()
            m.addSelectionInterval(4, 5)
            m.indices() shouldBe listOf(1, 4, 5)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(4, 5)
            // The anchor moves from 1 to 4 and the lead from 1 to 5, so row 1 is in the range too
            events shouldBe listOf(Triple(1, 5, false))
        }

        test("with anchor / lead notification off, the addSelectionInterval event covers only the rows actually selected") {
            val m = model()
            m.isLeadAnchorNotificationEnabled = false
            m.setSelectionInterval(1, 1)
            val events = m.record()
            m.addSelectionInterval(4, 5)
            events shouldBe listOf(Triple(4, 5, false))
            m.getAnchorSelectionIndex() shouldBe 4
        }

        test("in SINGLE_INTERVAL a connecting range is added, and a non-connecting range replaces the selection with only that range") {
            val joined = model(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            joined.setSelectionInterval(2, 3)
            joined.addSelectionInterval(4, 5)
            joined.indices() shouldBe listOf(2, 3, 4, 5)

            val apart = model(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            apart.setSelectionInterval(2, 3)
            apart.addSelectionInterval(5, 6)
            apart.indices() shouldBe listOf(5, 6)

            val before = model(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            before.setSelectionInterval(5, 6)
            before.addSelectionInterval(1, 3)
            before.indices() shouldBe listOf(1, 2, 3)
        }

        test("setSelectionInterval in SINGLE_SELECTION selects only the one row index1, and so does addSelectionInterval") {
            val m = model(WListSelectionModel.SINGLE_SELECTION)
            m.setSelectionInterval(1, 4)
            m.indices() shouldBe listOf(4)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(4, 4)
            m.addSelectionInterval(7, 2)
            m.indices() shouldBe listOf(2)
        }

        test("removeSelectionInterval in MULTIPLE_INTERVAL can remove the middle and leave two ranges") {
            val m = model()
            m.setSelectionInterval(1, 6)
            m.removeSelectionInterval(3, 4)
            m.indices() shouldBe listOf(1, 2, 5, 6)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(3, 4)
        }

        test("removeSelectionInterval in SINGLE_INTERVAL removing the middle removes up to the end of the selection") {
            val middle = model(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            middle.setSelectionInterval(1, 6)
            middle.removeSelectionInterval(3, 4)
            middle.indices() shouldBe listOf(1, 2)

            val head = model(WListSelectionModel.SINGLE_INTERVAL_SELECTION)
            head.setSelectionInterval(1, 6)
            head.removeSelectionInterval(1, 2)
            head.indices() shouldBe listOf(3, 4, 5, 6)
        }

        test("clearSelection deselects everything and resets min / max to -1 but keeps anchor / lead, and fires no event if the selection is empty") {
            val m = model()
            m.setSelectionInterval(2, 4)
            val events = m.record()
            m.clearSelection()
            listOf(m.isSelectionEmpty(), m.getMinSelectionIndex(), m.getMaxSelectionIndex()) shouldBe listOf(true, -1, -1)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(2, 4)
            events shouldBe listOf(Triple(2, 4, false))
            m.clearSelection()
            events.size shouldBe 1
        }

        test("isSelectedIndex is false out of range (negative or too large rows)") {
            val m = model()
            m.setSelectionInterval(2, 3)
            listOf(-1, 0, 1, 2, 3, 4, 100).map { m.isSelectedIndex(it) } shouldBe
                listOf(false, false, false, true, true, false, false)
        }

        test("getSelectedIndices is in ascending order, and getSelectedItemsCount and min / max agree even for a scattered selection") {
            val m = model()
            m.addSelectionInterval(9, 9)
            m.addSelectionInterval(5, 6)
            m.addSelectionInterval(1, 1)
            m.indices() shouldBe listOf(1, 5, 6, 9)
            listOf(m.getSelectedItemsCount(), m.getMinSelectionIndex(), m.getMaxSelectionIndex()) shouldBe listOf(4, 1, 9)
        }

        test("deselecting the minimum row makes the minimum the next selected row, and deselecting the maximum row makes the maximum the previous selected row") {
            val m = model()
            m.addSelectionInterval(1, 1)
            m.addSelectionInterval(4, 4)
            m.addSelectionInterval(8, 8)
            m.removeSelectionInterval(1, 1)
            listOf(m.getMinSelectionIndex(), m.getMaxSelectionIndex()) shouldBe listOf(4, 8)
            m.removeSelectionInterval(8, 8)
            listOf(m.getMinSelectionIndex(), m.getMaxSelectionIndex()) shouldBe listOf(4, 4)
        }

        test("events while setValueIsAdjusting(true) have valueIsAdjusting=true, and setting it back to false fires one event for the whole accumulated range") {
            val m = model()
            val events = m.record()
            m.setValueIsAdjusting(true)
            events shouldBe emptyList()
            m.setSelectionInterval(1, 2)
            m.setSelectionInterval(4, 5)
            m.getValueIsAdjusting() shouldBe true
            events shouldBe listOf(Triple(1, 2, true), Triple(1, 5, true))
            m.setValueIsAdjusting(false)
            events shouldBe listOf(Triple(1, 2, true), Triple(1, 5, true), Triple(1, 5, false))
        }

        test("toggling setValueIsAdjusting without changes fires no event, setting the same value is ignored, and the accumulated range is not carried over to the next operation") {
            val m = model()
            val events = m.record()
            m.setValueIsAdjusting(true)
            m.setValueIsAdjusting(true)
            m.setValueIsAdjusting(false)
            events shouldBe emptyList()
            m.setValueIsAdjusting(true)
            m.setSelectionInterval(7, 8)
            m.setValueIsAdjusting(false)
            m.setSelectionInterval(0, 0)
            events shouldBe listOf(Triple(7, 8, true), Triple(7, 8, false), Triple(0, 8, false))
        }

        test("insertIndexInterval(before=true) shifts the selection at and after the insertion point back, and inserted rows inherit the original row's state (unselected stays unselected)") {
            val m = model()
            m.setSelectionInterval(2, 3)
            val events = m.record()
            m.insertIndexInterval(1, 2, true)
            m.indices() shouldBe listOf(4, 5)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(4, 5)
            events shouldBe listOf(Triple(2, 5, false))
        }

        test("insertIndexInterval(before=false) after a selected row selects the inserted rows too, and anchor / lead do not move") {
            val m = model()
            m.setSelectionInterval(2, 3)
            val events = m.record()
            m.insertIndexInterval(3, 2, false)
            m.indices() shouldBe listOf(2, 3, 4, 5)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(2, 3)
            events shouldBe listOf(Triple(4, 5, false))
        }

        test("insertIndexInterval(before=true) before a selected row shifts that row back and selects the inserted rows too") {
            val m = model()
            m.setSelectionInterval(2, 3)
            m.insertIndexInterval(2, 2, true)
            m.indices() shouldBe listOf(2, 3, 4, 5)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(4, 5)
        }

        test("in SINGLE_SELECTION, rows inserted after the selected row are unselected") {
            val m = model(WListSelectionModel.SINGLE_SELECTION)
            m.setSelectionInterval(2, 2)
            m.insertIndexInterval(2, 1, false)
            m.indices() shouldBe listOf(2)
        }

        test("removeIndexInterval closes up the selection, and anchor / lead shift forward if after the removed range, or become the row before the range if inside it") {
            val m = model()
            m.setSelectionInterval(2, 5)
            val events = m.record()
            m.removeIndexInterval(3, 4)
            m.indices() shouldBe listOf(2, 3)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(2, 3)
            // Rows 4 and 5 are deselected and the lead moves from 5 to 3, so 3..5 (the anchor at 2 does not move)
            events shouldBe listOf(Triple(3, 5, false))

            val inside = model()
            inside.setSelectionInterval(2, 4)
            inside.removeIndexInterval(3, 5)
            listOf(inside.getAnchorSelectionIndex(), inside.getLeadSelectionIndex()) shouldBe listOf(2, 2)
            inside.indices() shouldBe listOf(2)
        }

        test("insertIndexInterval and removeIndexInterval throw IndexOutOfBoundsException for negative rows and lengths, and an insertion of length 0 does nothing (as in Swing)") {
            val m = model()
            m.setSelectionInterval(2, 3)
            val events = m.record()
            shouldThrow<IndexOutOfBoundsException> { m.insertIndexInterval(-1, 1, true) }
            shouldThrow<IndexOutOfBoundsException> { m.insertIndexInterval(1, -1, true) }
            shouldThrow<IndexOutOfBoundsException> { m.removeIndexInterval(-1, 2) }
            shouldThrow<IndexOutOfBoundsException> { m.removeIndexInterval(2, -3) }
            m.insertIndexInterval(1, 0, true)
            listOf(m.indices(), m.getAnchorSelectionIndex(), m.getLeadSelectionIndex(), events) shouldBe
                listOf(listOf(2, 3), 2, 3, emptyList<Triple<Int, Int, Boolean>>())
        }

        test("removeIndexInterval treats index0 > index1 as the same range") {
            val m = model()
            m.setSelectionInterval(2, 5)
            m.removeIndexInterval(4, 3)
            m.indices() shouldBe listOf(2, 3)
        }

        test("removing the first row 0 keeps anchor / lead at 0 if they are 0 (they do not become -1)") {
            val m = model()
            m.setSelectionInterval(0, 0)
            m.removeIndexInterval(0, 0)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(0, 0)
            m.isSelectionEmpty() shouldBe true
        }

        test("setLeadSelectionIndex selects anchor..lead if the anchor is selected, and moving the lead shrinks the range") {
            val m = model()
            m.setSelectionInterval(2, 2)
            m.setLeadSelectionIndex(5)
            m.indices() shouldBe listOf(2, 3, 4, 5)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(2, 5)
            val events = m.record()
            m.setLeadSelectionIndex(3)
            m.indices() shouldBe listOf(2, 3)
            events shouldBe listOf(Triple(3, 5, false))
        }

        test("setLeadSelectionIndex deselects anchor..lead if the anchor is not selected") {
            val m = model()
            m.setSelectionInterval(2, 5)
            m.removeSelectionInterval(3, 3)
            m.setLeadSelectionIndex(5)
            m.indices() shouldBe listOf(2)
        }

        test("setLeadSelectionIndex in SINGLE_SELECTION moves the anchor along to the lead and selects only that one row") {
            val m = model(WListSelectionModel.SINGLE_SELECTION)
            m.setSelectionInterval(2, 2)
            m.setLeadSelectionIndex(6)
            m.indices() shouldBe listOf(6)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(6, 6)
        }

        test("while the anchor is -1, setLeadSelectionIndex(-1) fires no event and a lead other than -1 cannot be set; -1 is ignored when there is an anchor") {
            val m = model()
            val events = m.record()
            m.setLeadSelectionIndex(-1)
            m.setLeadSelectionIndex(3)
            listOf(m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(-1, -1)
            m.setSelectionInterval(1, 2)
            m.setLeadSelectionIndex(-1)
            m.getLeadSelectionIndex() shouldBe 2
            events shouldBe listOf(Triple(1, 2, false))
        }

        test("setAnchorSelectionIndex does not change the selection and fires an event covering the old and new positions of the moved anchor") {
            val m = model()
            m.setSelectionInterval(1, 1)
            val events = m.record()
            m.setAnchorSelectionIndex(4)
            m.indices() shouldBe listOf(1)
            m.getAnchorSelectionIndex() shouldBe 4
            events shouldBe listOf(Triple(1, 4, false))
            m.setAnchorSelectionIndex(4)
            events.size shouldBe 1
        }

        test("listeners are called starting from the most recently added one, a listener added twice is called twice, and removing removes the one added last") {
            val m = model()
            val calls = mutableListOf<String>()
            val a = WListSelectionListener { calls += "a" }
            val b = WListSelectionListener { calls += "b" }
            m.addListSelectionListener(a)
            m.addListSelectionListener(b)
            m.addListSelectionListener(a)
            m.getListSelectionListeners().toList() shouldBe listOf(a, b, a)
            m.setSelectionInterval(0, 0)
            calls shouldBe listOf("a", "b", "a")
            calls.clear()
            m.removeListSelectionListener(a)
            m.getListSelectionListeners().toList() shouldBe listOf(a, b)
            m.setSelectionInterval(1, 1)
            calls shouldBe listOf("b", "a")
        }

        test("even if a listener removes itself during a notification, the other listeners that were to receive it are called") {
            val m = model()
            val calls = mutableListOf<String>()
            lateinit var first: WListSelectionListener
            first = WListSelectionListener {
                calls += "first"
                m.removeListSelectionListener(first)
            }
            m.addListSelectionListener(first)
            m.addListSelectionListener { calls += "last" }
            m.setSelectionInterval(0, 0)
            m.setSelectionInterval(1, 1)
            calls shouldBe listOf("last", "first", "last")
        }

        test("the event source is the selection model itself, and subclasses can notify any range with the protected fireValueChanged") {
            val m = object : WDefaultListSelectionModel() {
                fun fire(first: Int, last: Int) = fireValueChanged(first, last)
            }
            var received: WListSelectionEvent? = null
            m.addListSelectionListener { received = it }
            m.fire(2, 4)
            val e = received!!
            listOf(e.source === m, e.firstIndex, e.lastIndex, e.valueIsAdjusting) shouldBe listOf(true, 2, 4, false)
        }

        context("Swing compatibility") {
            test("moveLeadSelectionIndex moves only the lead without changing the selection, and reports one change covering the old and new lead") {
                val m = model()
                m.setSelectionInterval(2, 4)
                val events = m.record()
                m.moveLeadSelectionIndex(1)
                listOf(m.indices(), m.getAnchorSelectionIndex(), m.getLeadSelectionIndex()) shouldBe listOf(listOf(2, 3, 4), 2, 1)
                events shouldBe listOf(Triple(1, 4, false))
            }

            test("moveLeadSelectionIndex(-1) does nothing if the anchor is not -1, and sets the lead to -1 if the anchor is -1") {
                val m = model()
                m.setSelectionInterval(1, 3)
                val events = m.record()
                m.moveLeadSelectionIndex(-1)
                m.getLeadSelectionIndex() shouldBe 3
                events shouldBe emptyList()
                val fresh = model()
                fresh.moveLeadSelectionIndex(5)
                fresh.getLeadSelectionIndex() shouldBe 5
                fresh.moveLeadSelectionIndex(-1)
                listOf(fresh.getLeadSelectionIndex(), fresh.getAnchorSelectionIndex(), fresh.isSelectionEmpty()) shouldBe listOf(-1, -1, true)
            }

            test("if leadAnchorNotificationEnabled is false, moveLeadSelectionIndex reports nothing") {
                val m = model()
                m.setSelectionInterval(0, 2)
                m.isLeadAnchorNotificationEnabled = false
                val events = m.record()
                m.moveLeadSelectionIndex(0)
                m.getLeadSelectionIndex() shouldBe 0
                events shouldBe emptyList()
            }
        }
    }
}
