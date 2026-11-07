package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WFrame.focusOwner] and [WFrame.mostRecentFocusOwner], like Window.getFocusOwner / getMostRecentFocusOwner,
 * and for the initial focus when the window becomes active (the same as when a Swing Window gains focus).
 * The expected values are decided from Swing's rules (only the active window returns a focus owner; when inactive the owner is null and
 * the one that last got the focus is remembered). Windows are switched by bringing the shared window to the front.
 */
@Tags("Integration")
class WFrameFocusOwnerTest : FunSpec() {
    private var frame: WFrame? = null

    private class Fields(val first: WTextField, val second: WTextField, val spinner: WSpinner, val nested: WTextField)

    /**
     * Shows a window with two text fields, a WSpinner and a text field in a panel in a scroll pane, and waits until it becomes active.
     * [afterShow] is called right after showing (within the same UI thread task).
     */
    private suspend fun showFrame(afterShow: (Fields) -> Unit = {}): Pair<WFrame, Fields> {
        val (frame, fields) = onUiThreadGet {
            val frame = WFrame(TITLE).also { frame = it }
            val nested = WTextField()
            val fields = Fields(WTextField(), WTextField(), WSpinner(), nested)
            frame.add(fields.first)
            frame.add(fields.second)
            frame.add(fields.spinner)
            frame.add(WScrollPane(WPanel().also { it.add(nested) }))
            frame.isVisible = true
            afterShow(fields)
            frame to fields
        }
        eventually(5.seconds) { onUiThreadGet { frame.isActive && fields.nested.actualHeight > 0.0 }.shouldBeTrue() }
        return frame to fields
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        afterTest {
            frame?.let { onUiThread { it.dispose() } }
            frame = null
        }

        context("Defaults") {
            test("focusOwner before being shown is null (not active)") {
                onUiThread {
                    val frame = WFrame(TITLE).also { frame = it }
                    frame.add(WTextField())
                    frame.focusOwner.shouldBeNull()
                }
            }

            test("after being shown and becoming active, the first component in the content that can take focus has focus (as in Swing)") {
                val (frame, fields) = showFrame()
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.first }
            }
        }

        context("Reading and writing") {
            test("a component chosen with requestFocusInWindow right after showing returns true and has focus after loading") {
                var accepted = false
                val (frame, fields) = showFrame { accepted = it.second.requestFocusInWindow() }
                accepted.shouldBeTrue()
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
            }

            test("when coming back from inactive, the component that last had focus has focus") {
                val (frame, fields) = showFrame()
                onUiThread { fields.second.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
                onUiThread { UiTestHarness.sharedFrame().toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.isActive }.shouldBeFalse() }
                onUiThread { frame.toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
            }

            test("focusOwner and mostRecentFocusOwner of an active window are the component with the focus") {
                val (frame, fields) = showFrame()
                onUiThread { fields.second.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) {
                    onUiThread {
                        frame.focusOwner shouldBeSameInstanceAs fields.second
                        frame.mostRecentFocusOwner shouldBeSameInstanceAs fields.second
                    }
                }
                onUiThread { fields.first.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.first }
            }

            test("when a template part (the input field of a WSpinner) has the focus, and inside nested panels, the component is returned") {
                val (frame, fields) = showFrame()
                onUiThread { fields.spinner.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.spinner }
                onUiThread { fields.nested.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.nested }
            }

            test("when it becomes inactive focusOwner is null and mostRecentFocusOwner is the component that last had the focus") {
                val (frame, fields) = showFrame()
                onUiThread { fields.second.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
                onUiThread { UiTestHarness.sharedFrame().toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.isActive }.shouldBeFalse() }
                onUiThread {
                    frame.focusOwner.shouldBeNull()
                    frame.mostRecentFocusOwner shouldBeSameInstanceAs fields.second
                }
            }
        }

        context("Boundaries and exceptions") {
            test("the first component with isFocusable false is skipped and the next component has focus") {
                val (frame, fields) = onUiThreadGet {
                    val frame = WFrame(TITLE).also { frame = it }
                    val skipped = WTextField().also { it.isFocusable = false }
                    val next = WTextField()
                    frame.add(skipped)
                    frame.add(next)
                    frame.isVisible = true
                    frame to (skipped to next)
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
                onUiThread { fields.first.hasFocus().shouldBeFalse() }
            }

            test("a window without components that can take focus keeps focusOwner null after being shown") {
                val (frame, label) = onUiThreadGet {
                    val frame = WFrame(TITLE).also { frame = it }
                    val label = WLabel("Label only")
                    frame.add(label)
                    frame.isVisible = true
                    frame to label
                }
                eventually(5.seconds) { onUiThreadGet { frame.isActive && label.actualHeight > 0.0 }.shouldBeTrue() }
                onUiThread { frame.focusOwner.shouldBeNull() }
            }

            test("requestFocusInWindow before showing is false, and after showing the first component has focus") {
                val (frame, fields) = onUiThreadGet {
                    val frame = WFrame(TITLE).also { frame = it }
                    val first = WTextField()
                    val second = WTextField()
                    frame.add(first)
                    frame.add(second)
                    second.requestFocusInWindow().shouldBeFalse()
                    frame.isVisible = true
                    frame to (first to second)
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.first }
            }

            test("after removing the component that last had the focus, mostRecentFocusOwner does not return it") {
                val (frame, fields) = showFrame()
                onUiThread { fields.second.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.second }
                onUiThread { UiTestHarness.sharedFrame().toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.isActive }.shouldBeFalse() }
                onUiThread {
                    frame.remove(fields.second)
                    frame.mostRecentFocusOwner shouldNotBeSameInstanceAs fields.second
                }
            }

            test("focusOwner and mostRecentFocusOwner after closing are null") {
                val (frame, fields) = showFrame()
                onUiThread { fields.first.requestFocusInWindow().shouldBeTrue() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs fields.first }
                onUiThread {
                    frame.dispose()
                    frame.focusOwner.shouldBeNull()
                    frame.mostRecentFocusOwner.shouldBeNull()
                }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame focus owner test"
    }
}
