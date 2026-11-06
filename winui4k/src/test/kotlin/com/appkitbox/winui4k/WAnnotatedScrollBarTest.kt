package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.attachAndAwaitLoaded
import com.appkitbox.winui4k.UiTestHarness.detach
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WAnnotatedScrollBar's connectTo, addLabel / clearLabels, and adding and removing DetailLabelRequested listeners.
 * DetailLabelRequested fires only when the mouse actually hovers over the rail, so this only checks that adding and removing go through without exceptions.
 */
@Tags("E2E")
class WAnnotatedScrollBarTest : FunSpec() {
    /** The number of elements in the native AnnotatedScrollBar.Labels (call on the UI thread). */
    private fun labelCount(bar: WAnnotatedScrollBar): Int {
        val labels = bar.inspectable.getPtr(XamlInterop.IAnnotatedScrollBar_get_Labels)
        try {
            return labels.getInt(FoundationInterop.IVector_get_Size)
        } finally {
            labels.release()
        }
    }

    init {
        test("addLabel adds to Labels once per call, clearLabels resets it to 0, and labels can be added again afterwards") {
            onUiThreadGet {
                val bar = WAnnotatedScrollBar()
                val initial = labelCount(bar)
                bar.addLabel("A", 0.0)
                bar.addLabel("B", 300.5)
                bar.addLabel("", 900.0) // An empty label can be added too
                val added = labelCount(bar)
                bar.clearLabels()
                val cleared = labelCount(bar)
                bar.addLabel("C", 10.0)
                listOf(initial, added, cleared, labelCount(bar))
            } shouldBe listOf(0, 3, 0, 1)
        }

        test("clearLabels does not throw even without labels") {
            onUiThreadGet {
                val bar = WAnnotatedScrollBar()
                bar.clearLabels()
                labelCount(bar)
            } shouldBe 0
        }

        test("connectTo returns false while the ScrollView is not shown yet (no ScrollPresenter)") {
            onUiThreadGet { WAnnotatedScrollBar().connectTo(WScrollView(WLabel("Content"))) } shouldBe false
        }

        test("connectTo returns true after the ScrollView's Loaded, and adding labels afterwards does not throw") {
            val scrollView = onUiThreadGet { WScrollView(WLabel("Content")) }
            val bar = onUiThreadGet { WAnnotatedScrollBar() }
            attachAndAwaitLoaded(scrollView)
            try {
                onUiThreadGet {
                    val connected = bar.connectTo(scrollView)
                    bar.addLabel("First", 0.0)
                    connected to labelCount(bar)
                } shouldBe (true to 1)
            } finally {
                detach(scrollView)
            }
        }

        test("addDetailLabelRequestedListener can be removed with a Kotlin lambda too, and another listener can be removed while keeping one not removed") {
            onUiThreadGet {
                val bar = WAnnotatedScrollBar()
                val first: (Double) -> String = { "One $it" }
                val second: (Double) -> String = { "Two $it" }
                bar.addDetailLabelRequestedListener(first)
                bar.addDetailLabelRequestedListener(second)
                bar.removeDetailLabelRequestedListener(first)
                bar.removeDetailLabelRequestedListener(first) // Removing twice does nothing
                bar.removeDetailLabelRequestedListener(second)
                true
            } shouldBe true
        }
    }
}
