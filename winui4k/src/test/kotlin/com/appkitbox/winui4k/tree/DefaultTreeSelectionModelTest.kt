package com.appkitbox.winui4k.tree

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Tests for DefaultTreeSelectionModel and TreeSelectionEvent (no UI needed). Expected values are derived from the rules of Swing's DefaultTreeSelectionModel. */
@Tags("Pure")
class DefaultTreeSelectionModelTest : FunSpec() {
    private val a = TreePath(arrayOf<Any?>("root", "a"))
    private val b = TreePath(arrayOf<Any?>("root", "b"))
    private val c = TreePath(arrayOf<Any?>("root", "c"))

    /** Records received events as a sequence of (path, added) and the lead before and after. */
    private class Recorder : TreeSelectionListener {
        val events = mutableListOf<Triple<List<Pair<TreePath, Boolean>>, TreePath?, TreePath?>>()

        override fun valueChanged(e: TreeSelectionEvent) {
            events += Triple(e.getPaths().map { it to e.isAddedPath(it) }, e.getOldLeadSelectionPath(), e.getNewLeadSelectionPath())
        }
    }

    /** A RowMapper that maps [a], [b], [c] to rows 0, 1, 2 and everything else to invisible (-1). */
    private val rows = RowMapper { paths -> paths?.map { listOf(a, b, c).indexOf(it) }?.toIntArray() }

    init {
        test("the default mode is DISCONTIGUOUS, and setting an out-of-range value also makes it DISCONTIGUOUS") {
            val model = DefaultTreeSelectionModel()
            model.getSelectionMode() shouldBe TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
            model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
            model.setSelectionMode(99)
            model.getSelectionMode() shouldBe TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        }

        test("setSelectionPaths reports only the changed paths in one event (added ones first), and lead is the last path") {
            val model = DefaultTreeSelectionModel()
            val recorder = Recorder().also(model::addTreeSelectionListener)
            model.setSelectionPaths(arrayOf(a, b))
            model.setSelectionPaths(arrayOf(b, c))
            recorder.events shouldBe listOf(
                Triple(listOf(a to true, b to true), null, b),
                Triple(listOf(c to true, a to false), b, c),
            )
            model.getSelectionPath() shouldBe b
            model.getLeadSelectionPath() shouldBe c
            model.getSelectionCount() shouldBe 2
        }

        test("in SINGLE, only the first of several paths is selected, and addSelectionPath replaces the selection") {
            val model = DefaultTreeSelectionModel().also { it.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION) }
            val recorder = Recorder().also(model::addTreeSelectionListener)
            model.setSelectionPaths(arrayOf(a, b))
            model.getSelectionPaths()?.toList() shouldBe listOf(a)
            model.addSelectionPath(b)
            model.getSelectionPaths()?.toList() shouldBe listOf(b)
            recorder.events.last() shouldBe Triple(listOf(b to true, a to false), a, b)
        }

        test("adding only already selected paths does not notify or change lead. On removal, lead is the last remaining path") {
            val model = DefaultTreeSelectionModel()
            model.setSelectionPaths(arrayOf(a, b))
            val recorder = Recorder().also(model::addTreeSelectionListener)
            model.addSelectionPath(a)
            recorder.events shouldBe emptyList()
            model.getLeadSelectionPath() shouldBe b
            model.addSelectionPath(c)
            model.removeSelectionPath(c)
            recorder.events.last() shouldBe Triple(listOf(c to false), c, b)
            model.removeSelectionPath(TreePath("not selected"))
            recorder.events.size shouldBe 2
        }

        test("clearSelection reports all paths as removed and does nothing if empty. An empty getSelectionPaths is an empty array") {
            val model = DefaultTreeSelectionModel()
            model.setSelectionPaths(arrayOf(a, b))
            val recorder = Recorder().also(model::addTreeSelectionListener)
            model.clearSelection()
            model.clearSelection()
            recorder.events shouldBe listOf(Triple(listOf(a to false, b to false), b, null))
            model.getSelectionPaths()?.size shouldBe 0
            model.getSelectionPath().shouldBeNull()
            model.isSelectionEmpty().shouldBeTrue()
        }

        test("listeners are called starting from the last one added (same as Swing's EventListenerList)") {
            val model = DefaultTreeSelectionModel()
            val order = mutableListOf<String>()
            model.addTreeSelectionListener { order += "1" }
            model.addTreeSelectionListener { order += "2" }
            model.setSelectionPath(a)
            order shouldBe listOf("2", "1")
        }

        test("in CONTIGUOUS, if the rows are not contiguous only the first path is selected, and if removing makes it non-contiguous the whole selection is cleared") {
            val model = DefaultTreeSelectionModel().also {
                it.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION)
                it.setRowMapper(rows)
            }
            model.setSelectionPaths(arrayOf(a, c))
            model.getSelectionPaths()?.toList() shouldBe listOf(a)
            model.addSelectionPath(c)
            model.getSelectionPaths()?.toList() shouldBe listOf(c)
            model.addSelectionPath(b)
            model.addSelectionPath(a)
            model.getSelectionPaths()?.toList() shouldBe listOf(c, b, a)
            model.removeSelectionPath(b)
            model.isSelectionEmpty().shouldBeTrue()
        }

        test("the row APIs use the RowMapper and exclude invisible paths. null if none are visible, an empty array without a RowMapper") {
            val model = DefaultTreeSelectionModel()
            model.setSelectionPaths(arrayOf(c, TreePath("hidden"), a))
            model.getSelectionRows()?.toList() shouldBe emptyList()
            model.getLeadSelectionRow() shouldBe -1
            model.setRowMapper(rows)
            model.getSelectionRows()?.toList() shouldBe listOf(2, 0)
            listOf(model.getMinSelectionRow(), model.getMaxSelectionRow(), model.getLeadSelectionRow()) shouldBe listOf(0, 2, 0)
            model.isRowSelected(2).shouldBeTrue()
            model.isRowSelected(1).shouldBeFalse()
            model.setSelectionPath(TreePath("hidden"))
            model.getSelectionRows().shouldBeNull()
            model.getMinSelectionRow() shouldBe -1
        }

        test("TreeSelectionEvent throws IllegalArgumentException for isAddedPath of a path not included and for an out-of-range index, and cloneWithSource replaces only the source") {
            val event = TreeSelectionEvent("origin", arrayOf(a, b), booleanArrayOf(true, false), null, a)
            event.getPath() shouldBe a
            event.isAddedPath().shouldBeTrue()
            event.isAddedPath(1).shouldBeFalse()
            shouldThrow<IllegalArgumentException> { event.isAddedPath(c) }
            shouldThrow<IllegalArgumentException> { event.isAddedPath(2) }
            val clone = event.cloneWithSource("tree") as TreeSelectionEvent
            listOf(clone.source, clone.getPaths().toList(), clone.isAddedPath(b), clone.getNewLeadSelectionPath()) shouldBe
                listOf("tree", listOf(a, b), false, a)
        }
    }
}
