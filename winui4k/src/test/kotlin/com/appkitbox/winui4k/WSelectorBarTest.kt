package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests verifying WSelectorBar's item management, selection, and SelectionChanged, plus WSelectorBarItem's text. */
@Tags("E2E")
class WSelectorBarTest : FunSpec() {
    private fun barOf(vararg texts: String): WSelectorBar = WSelectorBar().also { bar -> texts.forEach { bar.addItem(WSelectorBarItem(it)) } }

    init {
        test("adding items via addItem is reflected in itemCount") {
            onUiThreadGet {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                selectorBar.itemCount
            } shouldBe 2
        }

        test("text returns exactly the value that was set") {
            onUiThreadGet {
                val item = WSelectorBarItem("old")
                item.text = "new"
                item.text
            } shouldBe "new"
        }

        test("selectedIndex starts out -1 (unselected) and returns exactly the value that was set") {
            val (before, after) = onUiThreadGet {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                val before = selectorBar.selectedIndex
                selectorBar.selectedIndex = 1
                before to selectorBar.selectedIndex
            }
            before shouldBe -1
            after shouldBe 1
        }

        test("the SelectionChanged listener fires with the selected index on every change to selectedIndex") {
            val received = mutableListOf<Int>()
            onUiThread {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                selectorBar.addSelectionListener { index -> received += index }
                selectorBar.selectedIndex = 0
                selectorBar.selectedIndex = 1
            }
            received shouldBe listOf(0, 1)
        }

        test("selectedIndex = -1 clears the selection, and -1 is passed to the notification") {
            val received = mutableListOf<Int>()
            val after = onUiThreadGet {
                val selectorBar = barOf("A", "B")
                selectorBar.selectedIndex = 1
                selectorBar.addSelectionListener { index -> received += index }
                selectorBar.selectedIndex = -1
                selectorBar.selectedIndex
            }
            after shouldBe -1
            received shouldBe listOf(-1)
        }

        test("an out-of-range selectedIndex (-2 and itemCount) throws IndexOutOfBoundsException, and the selection does not change") {
            val selected = onUiThreadGet {
                val selectorBar = barOf("A", "B")
                selectorBar.selectedIndex = 0
                shouldThrow<IndexOutOfBoundsException> { selectorBar.selectedIndex = -2 }
                shouldThrow<IndexOutOfBoundsException> { selectorBar.selectedIndex = 2 }
                selectorBar.selectedIndex
            }
            selected shouldBe 0
        }

        test("getItem returns the items in the order added, and out of range (-1 and itemCount) throws IndexOutOfBoundsException") {
            onUiThread {
                val a = WSelectorBarItem("A")
                val b = WSelectorBarItem("B")
                val selectorBar = WSelectorBar()
                selectorBar.addItem(a)
                selectorBar.addItem(b)
                selectorBar.getItem(0) shouldBeSameInstanceAs a
                selectorBar.getItem(1) shouldBeSameInstanceAs b
                shouldThrow<IndexOutOfBoundsException> { selectorBar.getItem(-1) }
                shouldThrow<IndexOutOfBoundsException> { selectorBar.getItem(2) }
            }
        }

        test("insertItem inserts at the given position, also allows the end (itemCount), and throws IndexOutOfBoundsException when out of range") {
            val texts = onUiThreadGet {
                val selectorBar = barOf("A", "C")
                selectorBar.insertItem(WSelectorBarItem("B"), 1)
                selectorBar.insertItem(WSelectorBarItem("D"), 3)
                shouldThrow<IndexOutOfBoundsException> { selectorBar.insertItem(WSelectorBarItem("X"), 5) }
                shouldThrow<IndexOutOfBoundsException> { selectorBar.insertItem(WSelectorBarItem("X"), -1) }
                (0 until selectorBar.itemCount).map { selectorBar.getItem(it).text }
            }
            texts shouldBe listOf("A", "B", "C", "D")
        }

        test("even after inserting in front with insertItem, selectedIndex returns the new position of the selected item") {
            onUiThreadGet {
                val selectorBar = barOf("A", "B")
                selectorBar.selectedIndex = 1
                selectorBar.insertItem(WSelectorBarItem("First"), 0)
                selectorBar.selectedIndex
            } shouldBe 2
        }

        test("removeItem(index) and removeItem(item) reduce the items, and out of range throws IndexOutOfBoundsException") {
            val texts = onUiThreadGet {
                val selectorBar = barOf("A", "B", "C", "D")
                selectorBar.removeItem(1)
                selectorBar.removeItem(selectorBar.getItem(1)) // C
                selectorBar.removeItem(WSelectorBarItem("An item of another SelectorBar")) // Nothing happens
                shouldThrow<IndexOutOfBoundsException> { selectorBar.removeItem(2) }
                shouldThrow<IndexOutOfBoundsException> { selectorBar.removeItem(-1) }
                (0 until selectorBar.itemCount).map { selectorBar.getItem(it).text }
            }
            texts shouldBe listOf("A", "D")
        }

        test("after removeItem of the selected item, selectedIndex returns -1") {
            onUiThreadGet {
                val selectorBar = barOf("A", "B")
                selectorBar.selectedIndex = 1
                selectorBar.removeItem(1)
                selectorBar.selectedIndex
            } shouldBe -1
        }

        test("icon returns the icon as set, and null removes it") {
            onUiThread {
                val item = WSelectorBarItem("A")
                item.icon shouldBe null
                item.icon = Symbol.HOME
                item.icon shouldBe Symbol.HOME
                item.icon = null
                item.icon shouldBe null
            }
        }

        test("in a loaded SelectorBar, the items' isSelected follows changes of selectedIndex") {
            val bar = onUiThreadGet { barOf("A", "B") }
            UiTestHarness.attachAndAwaitLoaded(bar)
            try {
                val states = onUiThreadGet {
                    bar.selectedIndex = 1
                    val first = listOf(bar.getItem(0).isSelected, bar.getItem(1).isSelected)
                    bar.selectedIndex = -1
                    first + listOf(bar.getItem(0).isSelected, bar.getItem(1).isSelected)
                }
                states shouldBe listOf(false, true, false, false)
            } finally {
                UiTestHarness.detach(bar)
            }
        }

        test("ChangeListeners receive events whose source is this SelectorBar on each selection change, and stop receiving them when removed") {
            val sources = mutableListOf<Any>()
            val (bar, afterRemove) = onUiThreadGet {
                val selectorBar = barOf("A", "B")
                val listener = WChangeListener { e -> sources += e.source }
                selectorBar.addChangeListener(listener)
                selectorBar.selectedIndex = 0
                selectorBar.selectedIndex = 1
                val registered = selectorBar.getChangeListeners().size
                selectorBar.removeChangeListener(listener)
                selectorBar.selectedIndex = 0
                selectorBar to registered
            }
            afterRemove shouldBe 1
            sources.size shouldBe 2
            sources.forEach { it shouldBeSameInstanceAs bar }
        }

        test("SelectionChanged listeners are also called for UI Automation selection (SelectionItem.Select), the same as a user operation") {
            val received = mutableListOf<Int>()
            val bar = onUiThreadGet {
                barOf("A", "B").also { it.addSelectionListener { index -> received += index } }
            }
            UiTestHarness.attachAndAwaitLoaded(bar)
            try {
                onUiThread { Xaml.invoke(bar.getItem(1).inspectable) }
                Reclamation.awaitWithGc { onUiThreadGet { received.isNotEmpty() } }
                received shouldBe listOf(1)
            } finally {
                UiTestHarness.detach(bar)
            }
        }
    }
}
