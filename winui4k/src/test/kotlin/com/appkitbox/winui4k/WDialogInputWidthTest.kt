package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify the width of text fields in dialogs. In Swing, the JTextField of a JOptionPane input dialog and in the contentPane of a JDialog (the center of a BorderLayout)
 * spans the dialog's width. The tests check with the native positions and widths of the open dialog that in WinUI4K too the field's right edge reaches the right edge of the content area
 * (Content of the ContentDialog template) and its left edge is at the left edge of the message (to the right of the icon if any).
 */
@Tags("E2E")
class WDialogInputWidthTest : FunSpec() {
    /** Puts an owner button on the shared window, runs [block] and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    /** Waits until the dialog opens and the text field's layout settles, checks it with [check], and closes it with the close button. */
    private suspend fun inspectAndClose(owner: WComponent, check: (OpenContentDialogs.InputExtent) -> Unit) {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
        try {
            eventually(3.seconds) { check(OpenContentDialogs.inputExtent(owner).shouldNotBeNull()) }
        } finally {
            OpenContentDialogs.press(owner, "CloseButton")
            eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
        }
    }

    /**
     * Opens, without blocking, a dialog with a header in the north and [field] added with [WContentDialog.add] using [layout] (in the center of the default WBorderLayout if null).
     */
    private fun showDialogWith(owner: WComponent, field: WComponent, layout: WLayoutManager? = null) = onUiThread {
        val dialog = WContentDialog("Rename")
        if (layout != null) dialog.layout = layout
        dialog.add(WLabel("New name:"), if (layout == null) WBorderLayout.NORTH else null)
        dialog.add(field)
        dialog.closeButtonText = "Cancel"
        dialog.show(owner) { }
    }

    init {
        test("the text field of showInputDialog spans from the right of the icon to the dialog's right edge (it does not shrink to the width of the initial text)") {
            withOwner { owner ->
                WinUiUtilities.invokeLater { WOptionPane.showInputDialog(owner, "Your name:", "Alice") }
                inspectAndClose(owner) { extent ->
                    extent.rightInset shouldBe (0.0 plusOrMinus TOLERANCE)
                    extent.leftInset shouldBeGreaterThan 0.0 // For the question mark icon
                    extent.width shouldBeGreaterThan MIN_FILLED_WIDTH
                }
            }
        }

        test("the text field of showInputDialog without an icon (PLAIN_MESSAGE) spans from the left edge to the right edge of the content area") {
            withOwner { owner ->
                WinUiUtilities.invokeLater {
                    WOptionPane.showInputDialog(owner, "Your name:", "Name", WOptionPane.PLAIN_MESSAGE)
                }
                inspectAndClose(owner) { extent ->
                    extent.leftInset shouldBe (0.0 plusOrMinus TOLERANCE)
                    extent.rightInset shouldBe (0.0 plusOrMinus TOLERANCE)
                }
            }
        }

        test("a WTextField with columns added to the center of WContentDialog's default content (WBorderLayout) spans the dialog's width, as in JDialog") {
            withOwner { owner ->
                val field = onUiThreadGet { WTextField(16).also { it.text = "report.txt" } }
                showDialogWith(owner, field)
                inspectAndClose(owner) { extent ->
                    extent.leftInset shouldBe (0.0 plusOrMinus TOLERANCE)
                    extent.rightInset shouldBe (0.0 plusOrMinus TOLERANCE)
                }
                // The preferred width stays the column width (usable as the preference for a layout manager)
                onUiThreadGet { field.columns to field.width.isNaN() } shouldBe (16 to true)
            }
        }

        test("a WTextField with an explicit width added to a WContentDialog with WFlowLayout keeps that width") {
            withOwner { owner ->
                val field = onUiThreadGet { WTextField(16).also { it.width = EXPLICIT_WIDTH } }
                showDialogWith(owner, field, WFlowLayout())
                inspectAndClose(owner) { extent -> extent.width shouldBe (EXPLICIT_WIDTH plusOrMinus TOLERANCE) }
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0

        /** Wider than this even after subtracting the margins and the icon from the dialog's minimum width (ContentDialogMinWidth = 320). The width of "Alice" is about 100. */
        const val MIN_FILLED_WIDTH = 180.0

        const val EXPLICIT_WIDTH = 120.0
    }
}
