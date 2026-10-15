package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/** Tests a [WContentDialog] shown over the shared test window. */
class WContentDialogTest : FunSpec() {
    /** Lays out a single child at the panel's whole size; the preferred size is the child's. */
    private class FillLayout : WLayoutManager {
        override fun preferredLayoutSize(parent: WLayoutPanel): WSize = parent.layoutChildren.firstOrNull()?.preferredSize() ?: WSize(0.0, 0.0)

        override fun layoutContainer(parent: WLayoutPanel) {
            parent.layoutChildren.forEach { parent.setBounds(it, 0.0, 0.0, parent.layoutSize.width, parent.layoutSize.height) }
        }
    }

    /** A form that wants to be [FORM_WIDTH] wide. */
    private fun wideForm(): WLayoutPanel = onUiThreadGet {
        WLayoutPanel(FillLayout()).also { panel ->
            panel.add(
                WBorder().also {
                    it.width = FORM_WIDTH
                    it.height = FORM_HEIGHT
                },
            )
        }
    }

    /** Shows a dialog with [content] (after [configure]) and returns the content's actual width once it settles. */
    private suspend fun shownWidth(content: WLayoutPanel, configure: (WContentDialog) -> Unit): Double {
        val owner = onUiThreadGet { WLabel("owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        val dialog = onUiThreadGet {
            WContentDialog("Wide form", content).also {
                it.closeButtonText = "Close"
                configure(it)
            }
        }
        try {
            onUiThread { dialog.show(owner) }
            var last = 0.0
            eventually(SETTLE_TIMEOUT) {
                repeat(3) { onUiThread { dialog.updateLayout() } }
                val width = onUiThreadGet { content.actualWidth }
                (width > 0.0 && width == last).also { last = width } shouldBe true
            }
            return last
        } finally {
            onUiThread { dialog.hide() }
            repeat(3) { onUiThread { } }
            UiTestHarness.detach(owner)
        }
    }

    init {
        test("by default the dialog is no wider than the theme's ContentDialogMaxWidth (548)") {
            shownWidth(wideForm()) { } shouldBeLessThan DEFAULT_MAX_WIDTH
        }

        test("dialogMaxWidth lets the dialog grow to fit a wider form") {
            val form = wideForm()
            shownWidth(form) { it.dialogMaxWidth = FORM_WIDTH + 100.0 } shouldBe (FORM_WIDTH plusOrMinus 1.0)
            onUiThreadGet { WContentDialog().dialogMaxWidth.isNaN() } shouldBe true
        }
    }

    private companion object {
        const val DEFAULT_MAX_WIDTH = 548.0
        const val FORM_WIDTH = 700.0
        const val FORM_HEIGHT = 40.0
        val SETTLE_TIMEOUT = 5.seconds
    }
}
