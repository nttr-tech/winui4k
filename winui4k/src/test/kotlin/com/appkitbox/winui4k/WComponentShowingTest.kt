package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WComponent.isDisplayable] / [WComponent.isShowing] / [WComponent.topLevelAncestor], which correspond to
 * Component.isDisplayable / isShowing and JComponent.getTopLevelAncestor.
 * Expected values come from Swing's rules: isDisplayable is whether it is connected to a displayable window (true even inside a hidden window,
 * false after disposal), isShowing is whether it and all its parents are visible and the window is shown too, and getTopLevelAncestor is
 * the window it is placed in.
 */
@Tags("Integration")
class WComponentShowingTest : FunSpec() {
    /** Places a label inside a panel in the shared window and removes it after [check]. */
    private suspend fun withLabelInPanel(check: suspend (WPanel, WLabel) -> Unit) {
        val (panel, label) = onUiThreadGet {
            val label = WLabel("Label")
            WPanel().also { it.add(label) } to label
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            check(panel, label)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        context("Defaults") {
            test("a label not placed in a window is not displayable (isDisplayable), not showing (isShowing), and has no top-level ancestor") {
                onUiThreadGet {
                    val label = WLabel("Label")
                    Triple(label.isDisplayable, label.isShowing, label.topLevelAncestor)
                } shouldBe Triple(false, false, null)
            }
        }

        context("Reading and writing") {
            test("a label inside a panel placed in the shared window is displayable and showing, and its top-level ancestor is that window") {
                val frame = UiTestHarness.sharedFrame()
                withLabelInPanel { _, label ->
                    onUiThreadGet { Triple(label.isDisplayable, label.isShowing, label.topLevelAncestor === frame) } shouldBe
                        Triple(true, true, true)
                }
            }

            test("hiding the label itself makes isShowing false while isDisplayable stays true") {
                withLabelInPanel { _, label ->
                    onUiThreadGet {
                        label.isVisible = false
                        label.isShowing to label.isDisplayable
                    } shouldBe (false to true)
                }
            }

            test("hiding the parent panel makes the child label's isShowing false, and showing the parent again makes it true") {
                withLabelInPanel { panel, label ->
                    onUiThreadGet {
                        panel.isVisible = false
                        val hidden = label.isShowing to label.isVisible
                        panel.isVisible = true
                        hidden to label.isShowing
                    } shouldBe ((false to true) to true)
                }
            }

            test("a label removed from the window becomes not displayable and not showing after Unloaded, and loses its top-level ancestor") {
                val (panel, label) = onUiThreadGet {
                    val label = WLabel("Label")
                    WPanel().also { it.add(label) } to label
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                UiTestHarness.detach(panel)
                eventually(10.seconds) {
                    onUiThreadGet { Triple(label.isDisplayable, label.isShowing, label.topLevelAncestor) } shouldBe Triple(false, false, null)
                }
            }
        }

        context("Swing compatibility") {
            test("a label inside a hidden (isVisible = false) WFrame has isShowing false, isDisplayable true, and that WFrame as its ancestor") {
                val loaded = CountDownLatch(1)
                val (frame, label) = onUiThreadGet {
                    val frame = WFrame("Hidden window")
                    frame.isAutoRequestFocus = false
                    val label = WLabel("Label")
                    label.addLoadedListener { loaded.countDown() }
                    frame.add(label)
                    frame.isVisible = true
                    frame to label
                }
                try {
                    loaded.await(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true
                    onUiThreadGet {
                        val shown = label.isShowing
                        frame.isVisible = false
                        listOf(shown, label.isShowing, label.isDisplayable, label.topLevelAncestor === frame)
                    } shouldBe listOf(true, false, true, true)
                    onUiThreadGet {
                        frame.isVisible = true
                        label.isShowing
                    } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
                eventually(10.seconds) {
                    onUiThreadGet { label.isDisplayable to label.topLevelAncestor } shouldBe (false to null)
                }
            }
        }
    }
}
