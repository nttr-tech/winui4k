package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests the Container-style child list ([WContainer.add] with a position, [WContainer.getComponent], [WContainer.remove] with a position,
 * [WContainer.isAncestorOf]), [WComponent.parent], reparenting, and revalidate / validate / invalidate.
 * The expected values follow the behavior of Swing's Container (exceptions for out-of-range positions, reparenting removes from the old parent, removeAll from the end).
 */
@Tags("Integration")
class WContainerTest : FunSpec() {
    /** A layout manager that records the addLayoutComponent / removeLayoutComponent / layoutContainer calls. */
    private class RecordingLayout : WLayoutManager {
        val calls = mutableListOf<String>()
        var layoutCount = 0

        override fun addLayoutComponent(component: WComponent, constraints: Any?) {
            require(constraints != REJECTED) { "rejected constraint" }
            calls += "add:${component.name}:$constraints"
        }

        override fun removeLayoutComponent(component: WComponent) {
            calls += "remove:${component.name}"
        }

        override fun preferredLayoutSize(parent: WLayoutPanel): WSize = WSize(100.0, 100.0)

        override fun layoutContainer(parent: WLayoutPanel) {
            layoutCount++
        }
    }

    private fun label(name: String): WLabel = WLabel(name).also { it.name = name }

    private fun names(container: WContainer): List<String> = container.components.map { it.name }

    init {
        test("add with a position inserts in the middle, and getComponent / getComponents / getComponentCount return that order") {
            onUiThread {
                val panel = WPanel()
                val a = label("a")
                val b = label("b")
                val c = label("c")
                panel.add(a)
                panel.add(b)
                panel.add(c, 0)
                panel.add(label("d"), 2)
                panel.add(label("e"), -1)
                names(panel) shouldContainExactly listOf("c", "a", "d", "b", "e")
                panel.componentCount shouldBe 5
                panel.getComponent(0) shouldBeSameInstanceAs c
                panel.getComponent(3) shouldBeSameInstanceAs b
            }
        }

        test("children inserted at a position are in that order in the native StackPanel too") {
            val (panel, children) = onUiThreadGet {
                val panel = WPanel()
                val first = label("first")
                val second = label("second")
                val third = label("third")
                panel.add(third)
                panel.add(first, 0)
                panel.add(second, 1)
                panel to listOf(first, second, third)
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread {
                    panel.updateLayout()
                    val ys = children.map { it.actualY }
                    (ys[0] < ys[1] && ys[1] < ys[2]).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("add with a position less than -1 or greater than the child count throws IllegalArgumentException and adds no child") {
            onUiThread {
                val panel = WPanel()
                panel.add(label("a"))
                val extra = label("x")
                shouldThrow<IllegalArgumentException> { panel.add(extra, 2) }
                shouldThrow<IllegalArgumentException> { panel.add(extra, -2) }
                panel.componentCount shouldBe 1
                extra.parent.shouldBeNull()
                panel.add(extra, 1) // A position equal to the child count is the end
                names(panel) shouldContainExactly listOf("a", "x")
            }
        }

        test("remove with a position and getComponent throw ArrayIndexOutOfBoundsException when out of range") {
            onUiThread {
                val panel = WPanel()
                panel.add(label("a"))
                panel.add(label("b"))
                shouldThrow<ArrayIndexOutOfBoundsException> { panel.remove(2) }
                shouldThrow<ArrayIndexOutOfBoundsException> { panel.remove(-1) }
                shouldThrow<ArrayIndexOutOfBoundsException> { panel.getComponent(2) }
                panel.remove(0)
                names(panel) shouldContainExactly listOf("b")
            }
        }

        test("parent is the container it was added to and goes back to null on remove and removeAll; remove of a non-child does nothing") {
            onUiThread {
                val panel = WPanel()
                val a = label("a")
                val b = label("b")
                val stranger = label("s")
                a.parent.shouldBeNull()
                panel.add(a)
                panel.add(b)
                a.parent shouldBeSameInstanceAs panel
                panel.remove(stranger)
                panel.componentCount shouldBe 2
                panel.remove(a)
                a.parent.shouldBeNull()
                panel.removeAll()
                b.parent.shouldBeNull()
                panel.components.toList().shouldBeEmpty()
            }
        }

        test("adding a component with a parent to another container removes it from the original container and places it in the new one") {
            val (second, moved, first) = onUiThreadGet {
                val first = WPanel()
                val second = WPanel()
                val moved = label("moved")
                first.add(label("stay"))
                first.add(moved)
                second.add(moved)
                Triple(second, moved, first)
            }
            onUiThread {
                names(first) shouldContainExactly listOf("stay")
                moved.parent shouldBeSameInstanceAs second
            }
            UiTestHarness.attachAndAwaitLoaded(second)
            try {
                onUiThread {
                    // It is also a child of the new parent on the native side (the original parent is not placed in the window)
                    moved.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(second)
            }
        }

        test("adding again within the same container inserts at the position after removal (same as Swing)") {
            onUiThread {
                val panel = WPanel()
                val a = label("a")
                listOf(a, label("b"), label("c")).forEach { panel.add(it) }
                panel.add(a, 2)
                names(panel) shouldContainExactly listOf("b", "c", "a")
                // Removing it leaves 2 children, so 3 is out of range (IllegalArgumentException, same as Swing)
                shouldThrow<IllegalArgumentException> { panel.add(a, 3) }
                names(panel) shouldContainExactly listOf("b", "c")
                a.parent.shouldBeNull()
            }
        }

        test("adding the component itself or an ancestor as a child throws IllegalArgumentException") {
            onUiThread {
                val outer = WPanel()
                val inner = WPanel()
                outer.add(inner)
                shouldThrow<IllegalArgumentException> { outer.add(outer) }
                shouldThrow<IllegalArgumentException> { inner.add(outer) }
                val border = WBorder()
                inner.add(border)
                shouldThrow<IllegalArgumentException> { border.child = outer }
                border.child.shouldBeNull()
                outer.parent.shouldBeNull()
            }
        }

        test("isAncestorOf follows parent and finds ancestors even through single-child hosts") {
            onUiThread {
                val outer = WPanel()
                val inner = WGrid()
                val border = WBorder()
                val leaf = label("leaf")
                outer.add(inner)
                inner.add(border, 0, 0)
                border.child = leaf
                outer.isAncestorOf(leaf).shouldBeTrue()
                inner.isAncestorOf(leaf).shouldBeTrue()
                inner.isAncestorOf(outer).shouldBeFalse()
                outer.isAncestorOf(outer).shouldBeFalse()
                outer.isAncestorOf(null).shouldBeFalse()
                outer.isAncestorOf(label("other")).shouldBeFalse()
            }
        }

        test("putting it into a single-child host makes parent that host, and removing or replacing it sets it back to null") {
            onUiThread {
                val border = WBorder()
                val scrollPane = WScrollPane()
                val scrollView = WScrollView()
                val expander = WExpander()
                val tab = WTabViewItem("tab")
                val split = WSplitView()
                val hosts: List<Pair<WComponent, (WComponent?) -> Unit>> = listOf(
                    border to { c -> border.child = c },
                    scrollPane to { c -> scrollPane.content = c },
                    scrollView to { c -> scrollView.content = c },
                    expander to { c -> expander.content = c },
                    tab to { c -> tab.content = c },
                    split to { c -> split.content = c },
                    split to { c -> split.pane = c },
                )
                for ((host, put) in hosts) {
                    val first = label("first")
                    val second = label("second")
                    put(first)
                    first.parent shouldBeSameInstanceAs host
                    put(second)
                    first.parent.shouldBeNull()
                    second.parent shouldBeSameInstanceAs host
                    put(null)
                    second.parent.shouldBeNull()
                }
            }
        }

        test("putting a host's child into another parent sets the original host's child to null (including moving from WSplitView's pane to content)") {
            onUiThread {
                val border = WBorder()
                val panel = WPanel()
                val moved = label("moved")
                border.child = moved
                panel.add(moved)
                border.child.shouldBeNull()
                moved.parent shouldBeSameInstanceAs panel

                val split = WSplitView()
                split.pane = moved
                panel.componentCount shouldBe 0
                split.content = moved
                split.pane.shouldBeNull()
                split.content shouldBeSameInstanceAs moved
                moved.parent shouldBeSameInstanceAs split

                val expander = WExpander()
                expander.content = moved
                split.content.shouldBeNull()
                moved.parent shouldBeSameInstanceAs expander
            }
        }

        test("removing with Xaml.detach (used by the ribbon and others) also removes it from the container's child list and parent") {
            onUiThread {
                val panel = WPanel()
                val child = label("child")
                panel.add(child)
                Xaml.detach(child)
                panel.componentCount shouldBe 0
                child.parent.shouldBeNull()
            }
        }

        test("WLayoutPanel's layoutChildren is in the same order as getComponents, and add with a position and constraints reaches the manager") {
            onUiThread {
                val manager = RecordingLayout()
                val panel = WLayoutPanel(manager)
                val a = label("a")
                val b = label("b")
                val c = label("c")
                panel.add(a, "north")
                panel.add(b)
                panel.add(c, "west", 0)
                panel.layoutChildren shouldContainExactly listOf(c, a, b)
                names(panel) shouldContainExactly listOf("c", "a", "b")
                panel.add(a, 0) // add with only a position passes null constraints
                panel.remove(1)
                panel.removeAll()
                manager.calls shouldContainExactly listOf(
                    "add:a:north",
                    "add:b:null",
                    "add:c:west",
                    "remove:a",
                    "add:a:null",
                    "remove:c",
                    "remove:b",
                    "remove:a",
                )
            }
        }

        test("if the WLayoutPanel's manager rejects the constraints, an exception is thrown and the child is not added") {
            onUiThread {
                val panel = WLayoutPanel(RecordingLayout())
                val child = label("child")
                shouldThrow<IllegalArgumentException> { panel.add(child, REJECTED) }
                panel.componentCount shouldBe 0
                panel.layoutChildren.shouldBeEmpty()
                child.parent.shouldBeNull()
                panel.add(child) // It is not left on the native side either, so it can be added again
                panel.componentCount shouldBe 1
            }
        }

        test("a child's revalidate schedules a re-layout of every ancestor WLayoutPanel, through hosts, once each") {
            val (outerLayout, innerLayout, leaf) = onUiThreadGet {
                val outerLayout = RecordingLayout()
                val innerLayout = RecordingLayout()
                val outer = WLayoutPanel(outerLayout)
                val inner = WLayoutPanel(innerLayout)
                val border = WBorder()
                val leaf = label("leaf")
                outer.add(border)
                border.child = inner
                inner.add(leaf)
                Triple(outerLayout, innerLayout, leaf)
            }
            onUiThread { } // Finish the re-layouts scheduled while building
            val before = onUiThreadGet { outerLayout.layoutCount to innerLayout.layoutCount }
            onUiThread {
                leaf.naturalSize = WSize(1.0, 1.0)
                repeat(3) { leaf.revalidate() }
                leaf.naturalSize.shouldBeNull()
                outerLayout.layoutCount shouldBe before.first // Only schedules; does not lay out on the spot
            }
            onUiThread {
                outerLayout.layoutCount shouldBe before.first + 1
                innerLayout.layoutCount shouldBe before.second + 1
            }
        }

        test("invalidate discards the natural size of itself and its ancestors but does not schedule a re-layout") {
            val (layout, border, leaf) = onUiThreadGet {
                val layout = RecordingLayout()
                val panel = WLayoutPanel(layout)
                val border = WBorder()
                val leaf = label("leaf")
                panel.add(border)
                border.child = leaf
                Triple(layout, border, leaf)
            }
            onUiThread { }
            val before = onUiThreadGet { layout.layoutCount }
            onUiThread {
                border.naturalSize = WSize(1.0, 1.0)
                leaf.naturalSize = WSize(1.0, 1.0)
                leaf.invalidate()
                leaf.naturalSize.shouldBeNull()
                border.naturalSize.shouldBeNull()
            }
            onUiThread { layout.layoutCount shouldBe before }
        }

        test("validate runs the scheduled re-layout on the spot, and the scheduled run does not happen again") {
            val (layout, panel, outer) = onUiThreadGet {
                val layout = RecordingLayout()
                val panel = WLayoutPanel(layout)
                val outer = WPanel()
                outer.add(panel)
                Triple(layout, panel, outer)
            }
            onUiThread { }
            val before = onUiThreadGet { layout.layoutCount }
            onUiThread {
                panel.revalidate()
                outer.validate() // Follows the child WContainers and finishes the WLayoutPanel's scheduled layout
                layout.layoutCount shouldBe before + 1
            }
            onUiThread { layout.layoutCount shouldBe before + 1 }
        }
    }

    private companion object {
        /** A constraint that [RecordingLayout] rejects. */
        const val REJECTED = "rejected"
    }
}
