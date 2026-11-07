package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.TreePath
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the look and place of the initial focus WinUI4K puts when a window becomes active ([FrameFocusTracker])
 * (display issue 3 in doc/swing_to_winui4k_issues.md).
 *
 * - Look: Swing shows no prominent rectangle for the initial focus. WinUI's Programmatic focus draws the keyboard focus rectangle when the window's last input
 *   was the keyboard (CoerceFocusState in microsoft-ui-xaml's focusmgr.cpp), so the tests check that even then
 *   FocusState does not become Keyboard
 * - Place: as in Swing, the selected button for a radio button group, the selected row for a tree, and the content buttons for a toolbar
 */
@Tags("E2E")
class WFrameInitialFocusTest : FunSpec() {
    private var frame: WFrame? = null

    /** Shows a window whose content is made with [build] and waits until it becomes active and the content is loaded. */
    private suspend fun show(build: (WFrame) -> WComponent): Pair<WFrame, WComponent> {
        val (shown, probe) = onUiThreadGet {
            val frame = WFrame(TITLE).also { frame = it }
            frame.setSize(480, 360)
            val probe = build(frame)
            frame.isVisible = true
            frame to probe
        }
        eventually(5.seconds) { onUiThreadGet { shown.isActive && probe.actualHeight > 0.0 }.shouldBeTrue() }
        return shown to probe
    }

    /** Calls [block] with the element that has focus in [frame] (passes null if none). Call on the UI thread. */
    private fun <T> withFocused(frame: WFrame, block: (ComPtr?) -> T): T {
        val root = checkNotNull(frame.contentPane.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) { "not attached" }
        val focused = try {
            Xaml.focusedElement(root)
        } finally {
            root.release()
        }
        return try {
            block(focused)
        } finally {
            focused?.release()
        }
    }

    /** The FocusState of the element that has focus in [frame] (Unfocused=0, Pointer=1, Keyboard=2, Programmatic=3). */
    private fun focusStateIn(frame: WFrame): Int = onUiThreadGet {
        withFocused(frame) { focused ->
            val element = checkNotNull(focused) { "no element has focus" }.queryInterface(XamlInterop.IID_IUIElement)
            try {
                element.getInt(XamlInterop.IUIElement_get_FocusState)
            } finally {
                element.release()
            }
        }
    }

    /** Whether the element that has focus in [frame] is the element of [component] itself. */
    private fun isFocusedElement(frame: WFrame, component: WComponent): Boolean = onUiThreadGet {
        withFocused(frame) { focused -> focused != null && Xaml.identityOf(focused) == Xaml.identityOf(component.inspectable) }
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        afterTest {
            frame?.let { onUiThread { it.dispose() } }
            frame = null
        }

        context("look") {
            test("the initial focus right after showing does not show the keyboard focus rectangle (FocusState is Pointer)") {
                val button = onUiThreadGet { WButton("OK") }
                val (frame, _) = show {
                    it.add(button)
                    button
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs button }
                focusStateIn(frame) shouldBe XamlInterop.FocusState_Pointer
            }

            test("the keyboard focus rectangle is not shown either when a window operated with the keyboard becomes active again and gets the initial focus") {
                // A window with no focusable component at first. Send a key to make the window's last input the keyboard
                val (frame, _) = show { frame -> WLabel("Label only").also { frame.add(it, WBorderLayout.NORTH) } }
                TestKeyboard.pressIn(TITLE, VK_F13, SCAN_F13)
                val button = onUiThreadGet { WButton("OK").also { frame.add(it) } }
                eventually(5.seconds) { onUiThreadGet { button.actualHeight > 0.0 }.shouldBeTrue() }
                onUiThread { frame.focusOwner shouldBe null }
                // Bringing another window to the front and back activates the window without focus, which puts the initial focus
                onUiThread { UiTestHarness.sharedFrame().toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.isActive }.shouldBeFalse() }
                onUiThread { frame.toFront() }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs button }
                focusStateIn(frame) shouldBe XamlInterop.FocusState_Pointer
            }
        }

        context("place") {
            test("radio buttons in a button group put the initial focus on the selected button, not the first one (as in Swing)") {
                val buttons = onUiThreadGet { listOf(WRadioButton("Small"), WRadioButton("Medium", true), WRadioButton("Large")) }
                val (frame, _) = show { frame ->
                    frame.layout = WFlowLayout()
                    val group = WButtonGroup()
                    buttons.forEach {
                        group.add(it)
                        frame.add(it)
                    }
                    buttons[2]
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs buttons[1] }
                focusStateIn(frame) shouldBe XamlInterop.FocusState_Pointer
            }

            test("if nothing is selected in a button group, the initial focus goes to the first button") {
                val buttons = onUiThreadGet { listOf(WRadioButton("Small"), WRadioButton("Medium")) }
                val (frame, _) = show { frame ->
                    frame.layout = WFlowLayout()
                    val group = WButtonGroup()
                    buttons.forEach {
                        group.add(it)
                        frame.add(it)
                    }
                    buttons[1]
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs buttons[0] }
            }

            test("if the selected radio button cannot take focus (isFocusable = false), the initial focus goes to the first button") {
                val buttons = onUiThreadGet {
                    listOf(WRadioButton("Small"), WRadioButton("Medium", true).also { it.isFocusable = false })
                }
                val (frame, _) = show { frame ->
                    frame.layout = WFlowLayout()
                    val group = WButtonGroup()
                    buttons.forEach {
                        group.add(it)
                        frame.add(it)
                    }
                    buttons[1]
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs buttons[0] }
            }

            test("a tree puts the initial focus on the selected row, not the first row") {
                val root = DefaultMutableTreeNode("Documents")
                val work = DefaultMutableTreeNode("Work")
                val spec = DefaultMutableTreeNode("Spec.docx")
                work.add(spec)
                work.add(DefaultMutableTreeNode("Schedule.xlsx"))
                root.add(work)
                val tree = onUiThreadGet {
                    WTree(root).also {
                        it.width = 240.0
                        it.height = 200.0
                    }
                }
                val (frame, _) = show { frame ->
                    frame.add(tree)
                    tree.selectionPath = TreePath(spec.getPath())
                    tree
                }
                eventually(5.seconds) { onUiThreadGet { frame.focusOwner } shouldBeSameInstanceAs tree }
                eventually(5.seconds) {
                    onUiThread {
                        val bounds = tree.getPathBounds(TreePath(spec.getPath())).shouldNotBeNull()
                        // The position of the row with focus (TreeViewItem) is the same as that of the selected row
                        val position = withFocused(frame) { focused ->
                            val origin = Xaml.positionInRoot(tree.uiElement)
                            val at = Xaml.positionInRoot(checkNotNull(focused))
                            WPoint((at[0] - origin[0]).roundToInt(), (at[1] - origin[1]).roundToInt())
                        }
                        position shouldBe WPoint(bounds.x, bounds.y)
                    }
                }
                focusStateIn(frame) shouldBe XamlInterop.FocusState_Pointer
            }

            test("a toolbar (WCommandBar) puts the initial focus on the first button, not the \"...\" button at the right end, and the buttons' AllowFocusOnInteraction stays false") {
                val buttons = onUiThreadGet { listOf(WAppBarButton("New", Symbol.DOCUMENT), WAppBarButton("Open", Symbol.OPEN_FILE)) }
                val (frame, _) = show { frame ->
                    val bar = WCommandBar()
                    buttons.forEach { bar.add(it) }
                    frame.add(bar)
                    bar
                }
                eventually(5.seconds) { isFocusedElement(frame, buttons[0]).shouldBeTrue() }
                focusStateIn(frame) shouldBe XamlInterop.FocusState_Pointer
                // AllowFocusOnInteraction, set to true only temporarily to accept Pointer focus, is back to AppBarButton's default of false
                onUiThreadGet {
                    buttons[0].frameworkElement.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
                }.shouldBeFalse()
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k initial focus test"

        /** A key that does nothing when pressed (F13). */
        const val VK_F13 = 0x7C
        const val SCAN_F13 = 0x64
    }
}
