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
 * Tests for [WFrame.layout], like JFrame.setLayout / getLayout, and [WFrame.add] with constraints. The expected values are decided from the result of JFrame's
 * `frame.setLayout(new BorderLayout()); frame.add(c, BorderLayout.NORTH)`
 * (only the default content not being a BorderLayout differs from Swing). Each test creates its own WFrame and disposes it at the end.
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
            test("layout is null by default (the content is the contentPane WPanel that stacks vertically and has no layout manager)") {
                val frame = newFrame()
                onUiThread {
                    frame.layout.shouldBeNull()
                    frame.content shouldBeSameInstanceAs frame.contentPane
                }
            }
        }

        context("Reading and writing") {
            test("setLayout replaces the content with a WLayoutPanel with that manager and moves the contentPane children in the same order") {
                val frame = newFrame()
                onUiThread {
                    val first = WButton("One")
                    val second = WButton("Two")
                    frame.add(first)
                    frame.add(second)
                    val layout = WFlowLayout()
                    frame.layout = layout
                    frame.layout shouldBeSameInstanceAs layout
                    val panel = frame.content.shouldBeInstanceOf<WLayoutPanel>()
                    panel.layout shouldBeSameInstanceAs layout
                    panel.components.toList() shouldBe listOf(first, second)
                    first.parent shouldBeSameInstanceAs panel
                    frame.contentPane.componentCount shouldBe 0
                }
            }

            test("after setLayout(WBorderLayout()), add(c, NORTH) and add without a constraint go to the north and the center, as in Swing") {
                val frame = newFrame()
                onUiThread {
                    frame.layout = WBorderLayout()
                    val north = WButton("North")
                    val center = WButton("Center")
                    frame.add(north, WBorderLayout.NORTH)
                    frame.add(center)
                    val layout = frame.layout as WBorderLayout
                    layout.getLayoutComponent(WBorderLayout.NORTH) shouldBeSameInstanceAs north
                    layout.getLayoutComponent(WBorderLayout.CENTER) shouldBeSameInstanceAs center
                }
            }

            test("if the content is already a WLayoutPanel, setLayout replaces that panel's manager without replacing the content") {
                val frame = newFrame()
                onUiThread {
                    val root = WLayoutPanel(WFlowLayout())
                    frame.setContentPane(root)
                    val layout = WBorderLayout()
                    frame.layout = layout
                    frame.content shouldBeSameInstanceAs root
                    root.layout shouldBeSameInstanceAs layout
                    frame.layout shouldBeSameInstanceAs layout
                }
            }

            test("a child put in the center with setLayout(WBorderLayout()) fills the window content below the menu bar when shown") {
                val frame = newFrame()
                val center = onUiThreadGet {
                    frame.jMenuBar = WMenuBar().also { it.add(WMenuBarItem("File")) }
                    frame.layout = WBorderLayout()
                    frame.setSize(400, 300)
                    WButton("Center").also { frame.add(it, WBorderLayout.CENTER) }
                }
                onUiThread { frame.isVisible = true }
                // It first appears at its natural size and expands with the layout manager's arrangement (invokeLater)
                eventually(5.seconds) {
                    onUiThread {
                        val panel = frame.content as WLayoutPanel
                        panel.actualWidth shouldBeGreaterThan 300.0
                        center.actualWidth shouldBe (panel.actualWidth plusOrMinus 0.5)
                        center.actualHeight shouldBe (panel.actualHeight plusOrMinus 0.5)
                        center.parent shouldBeSameInstanceAs panel
                    }
                }
            }
        }

        context("Boundaries and exceptions") {
            test("adding to the default content (WPanel) with a non-null constraint throws IllegalArgumentException and the child is not added. With a null constraint it is added") {
                val frame = newFrame()
                onUiThread {
                    val north = WButton("North")
                    shouldThrow<IllegalArgumentException> { frame.add(north, WBorderLayout.NORTH) }.message shouldContain "setLayout"
                    shouldThrow<IllegalArgumentException> { frame.add(north, WBorderLayout.NORTH, 0) }
                    north.parent.shouldBeNull()
                    frame.contentPane.componentCount shouldBe 0
                    frame.add(north, null)
                    north.parent shouldBeSameInstanceAs frame.contentPane
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
                    frame.layout.shouldBeNull()
                }
            }

            test("setLayout(null) replaces the default content with a WLayoutPanel without a manager (Swing's null layout)") {
                val frame = newFrame()
                onUiThread {
                    val child = WButton("Child")
                    frame.add(child)
                    frame.layout = null
                    val panel = frame.content.shouldBeInstanceOf<WLayoutPanel>()
                    panel.layout.shouldBeNull()
                    child.parent shouldBeSameInstanceAs panel
                }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame layout test"
    }
}
