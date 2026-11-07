package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the JFrame.getContentPane / setLayout / getLayout-like [WFrame.contentPane] and [WFrame.layout], and [WFrame.add] with constraints.
 * The expected values come from the results of JFrame's `frame.add(c, BorderLayout.NORTH)` (the default content is BorderLayout) and `frame.setLayout(...)`.
 * Each test creates its own WFrame and disposes it at the end.
 */
@Tags("Integration")
class WFrameLayoutTest : FunSpec() {
    private var frame: WFrame? = null

    private fun newFrame(): WFrame = onUiThreadGet { WFrame(TITLE).also { frame = it } }

    init {
        beforeSpec { UiTestHarness.sharedFrame() } // So that closing our own window does not end the app
        afterTest {
            frame?.let { onUiThread { it.dispose() } }
            frame = null
        }

        context("Defaults") {
            test("the content is a WLayoutPanel with WBorderLayout by default (the same as JFrame's default contentPane). getContentPane is that content, and getLayout its manager") {
                val frame = newFrame()
                onUiThread {
                    val pane = frame.contentPane.shouldBeInstanceOf<WLayoutPanel>()
                    frame.content shouldBeSameInstanceAs pane
                    pane.layout.shouldBeInstanceOf<WBorderLayout>()
                    frame.layout shouldBeSameInstanceAs pane.layout
                    pane.insets shouldBe WInsets() // No padding (the same as Swing's contentPane)
                }
            }

            test("add(c, NORTH) and add without a constraint, without calling setLayout, go to the north and the center, as in Swing") {
                val frame = newFrame()
                onUiThread {
                    val north = WButton("North")
                    val center = WButton("Center")
                    frame.add(north, WBorderLayout.NORTH)
                    frame.add(center)
                    val layout = frame.layout as WBorderLayout
                    layout.getLayoutComponent(WBorderLayout.NORTH) shouldBeSameInstanceAs north
                    layout.getLayoutComponent(WBorderLayout.CENTER) shouldBeSameInstanceAs center
                    north.parent shouldBeSameInstanceAs frame.contentPane
                    // Swing's string constraints (BorderLayout.SOUTH = "South") can also be used as is
                    val south = WButton("South")
                    frame.add(south, "South")
                    layout.getLayoutComponent(WBorderLayout.SOUTH) shouldBeSameInstanceAs south
                }
            }

            test("when shown with the default content, the north child is at the top of the window with the full width, and the center child takes all the rest") {
                val frame = newFrame()
                val (north, center) = onUiThreadGet {
                    frame.setSize(400, 300)
                    val north = WButton("North").also { frame.add(it, WBorderLayout.NORTH) }
                    val center = WButton("Center").also { frame.add(it) }
                    north to center
                }
                onUiThread { frame.isVisible = true }
                eventually(5.seconds) {
                    onUiThread {
                        val pane = frame.contentPane
                        pane.actualWidth shouldBeGreaterThan 300.0
                        north.bounds.x shouldBe 0
                        north.bounds.y shouldBe 0
                        north.actualWidth shouldBe (pane.actualWidth plusOrMinus 0.5)
                        north.actualHeight shouldBeGreaterThan 0.0
                        center.bounds.y shouldBe north.bounds.height
                        center.actualWidth shouldBe (pane.actualWidth plusOrMinus 0.5)
                        center.actualHeight shouldBe (pane.actualHeight - north.actualHeight plusOrMinus 1.0)
                    }
                }
            }
        }

        context("Reading and writing") {
            test("setLayout replaces the content's manager without replacing the content. getContentPane stays the shown content (as in Swing)") {
                val frame = newFrame()
                onUiThread {
                    val pane = frame.contentPane
                    val first = WButton("One")
                    frame.add(first)
                    val layout = WFlowLayout()
                    frame.layout = layout
                    frame.layout shouldBeSameInstanceAs layout
                    frame.contentPane shouldBeSameInstanceAs pane
                    frame.content shouldBeSameInstanceAs pane
                    (pane as WLayoutPanel).layout shouldBeSameInstanceAs layout
                    // getContentPane().add after setLayout also goes into the shown content
                    val second = WButton("Two")
                    frame.contentPane.add(second)
                    pane.components.toList() shouldBe listOf(first, second)
                }
            }

            test("after replacing with setContentPane, getContentPane returns that container, and setLayout replaces that panel's manager") {
                val frame = newFrame()
                onUiThread {
                    val root = WLayoutPanel(WFlowLayout())
                    frame.setContentPane(root)
                    frame.contentPane shouldBeSameInstanceAs root
                    val layout = WBorderLayout()
                    frame.layout = layout
                    frame.content shouldBeSameInstanceAs root
                    root.layout shouldBeSameInstanceAs layout
                    frame.layout shouldBeSameInstanceAs layout
                }
            }

            test("a child put in the center of the default content fills the window content below the menu bar when shown") {
                val frame = newFrame()
                val center = onUiThreadGet {
                    frame.jMenuBar = WMenuBar().also { it.add(WMenuBarItem("File")) }
                    frame.setSize(400, 300)
                    WButton("Center").also { frame.add(it, WBorderLayout.CENTER) }
                }
                onUiThread { frame.isVisible = true }
                // It first appears at its natural size and expands with the layout manager's arrangement (invokeLater)
                eventually(5.seconds) {
                    onUiThread {
                        val panel = frame.contentPane
                        panel.actualWidth shouldBeGreaterThan 300.0
                        center.actualWidth shouldBe (panel.actualWidth plusOrMinus 0.5)
                        center.actualHeight shouldBe (panel.actualHeight plusOrMinus 0.5)
                        center.parent shouldBeSameInstanceAs panel
                    }
                }
            }
        }

        context("Boundaries and exceptions") {
            test("adding with a non-null constraint to a container without a manager (WPanel) set with setContentPane throws IllegalArgumentException and the child is not added. With a null constraint it is added") {
                val frame = newFrame()
                onUiThread {
                    val stack = WPanel()
                    frame.setContentPane(stack)
                    val north = WButton("North")
                    shouldThrow<IllegalArgumentException> { frame.add(north, WBorderLayout.NORTH) }
                    shouldThrow<IllegalArgumentException> { frame.add(north, WBorderLayout.NORTH, 0) }
                    north.parent.shouldBeNull()
                    stack.componentCount shouldBe 0
                    frame.add(north, null)
                    north.parent shouldBeSameInstanceAs stack
                }
            }

            test("adding with a constraint to a WLayoutPanel without a layout manager (Swing's null layout) also throws IllegalArgumentException") {
                val frame = newFrame()
                onUiThread {
                    val root = WLayoutPanel(null)
                    frame.setContentPane(root)
                    shouldThrow<IllegalArgumentException> { frame.add(WButton("x"), WBorderLayout.CENTER) }
                    root.componentCount shouldBe 0
                }
            }

            test("when setContentPane has made a container other than a WLayoutPanel the content, setLayout throws IllegalStateException and the content does not change") {
                val frame = newFrame()
                onUiThread {
                    val grid = WGrid()
                    frame.setContentPane(grid)
                    shouldThrow<IllegalStateException> { frame.layout = WBorderLayout() }
                    frame.content shouldBeSameInstanceAs grid
                    frame.contentPane shouldBeSameInstanceAs grid
                    frame.layout.shouldBeNull()
                }
            }

            test("when a component that is not a container is made the content with setContentPane, getContentPane throws IllegalStateException") {
                val frame = newFrame()
                onUiThread {
                    val button = WButton("Content")
                    frame.setContentPane(button)
                    shouldThrow<IllegalStateException> { frame.contentPane }.message shouldContain "not a container"
                    frame.content shouldBeSameInstanceAs button
                }
            }

            test("setLayout(null) makes the default content a WLayoutPanel without a manager (Swing's null layout) without replacing it") {
                val frame = newFrame()
                onUiThread {
                    val pane = frame.contentPane
                    val child = WButton("Child")
                    frame.add(child)
                    frame.layout = null
                    frame.layout.shouldBeNull()
                    frame.content shouldBeSameInstanceAs pane
                    (pane as WLayoutPanel).layout.shouldBeNull()
                    child.parent shouldBeSameInstanceAs pane
                }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame layout test"
    }
}
