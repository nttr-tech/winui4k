package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WFlyout's content, opening position (placement) and showAt / hide / isOpen, by actually opening it in the shared window.
 * Opening as a menu (PopupMenuListener, invoker) is checked by WFlyoutPopupMenuTest.
 */
class WFlyoutTest : FunSpec() {
    private suspend fun withButton(block: suspend (button: WButton) -> Unit) {
        val button = onUiThreadGet { WButton("Open") }
        UiTestHarness.attachAndAwaitLoaded(button)
        try {
            block(button)
        } finally {
            UiTestHarness.detach(button)
        }
    }

    private suspend fun awaitOpen(flyout: WFlyout, open: Boolean) {
        eventually(5.seconds) { onUiThreadGet { flyout.isOpen } shouldBe open }
    }

    /** Whether the flyout's native Content is the same object as [expected] (if null, whether Content is empty). Call on the UI thread. */
    private fun nativeContentIs(flyout: WFlyout, expected: WComponent?): Boolean {
        val content = flyout.inspectable.getPtrOrNull(XamlInterop.IFlyout_get_Content) ?: return expected == null
        return try {
            expected != null && Xaml.sameObject(content, expected.uiElement)
        } finally {
            content.release()
        }
    }

    init {
        test("placement defaults to WinUI's default (TOP), and all values can be set and read back") {
            // The default is FlyoutBase::g_defaultPlacementMode (FlyoutPlacementMode_Top) of microsoft-ui-xaml
            onUiThreadGet {
                val flyout = WFlyout()
                listOf(flyout.placement) + FlyoutPlacement.entries.map { value ->
                    flyout.placement = value
                    flyout.placement
                }
            } shouldContainExactly listOf(FlyoutPlacement.TOP) + FlyoutPlacement.entries
        }

        test("content is attached by the constructor or by assignment, and removed by setting null") {
            val label = onUiThreadGet { WLabel("Content") }
            val other = onUiThreadGet { WLabel("Other content") }
            onUiThreadGet {
                val flyout = WFlyout(label)
                val results = mutableListOf(flyout.content === label && nativeContentIs(flyout, label))
                flyout.content = other
                results += flyout.content === other && nativeContentIs(flyout, other)
                flyout.content = null
                results += flyout.content == null && nativeContentIs(flyout, null)
                results += WFlyout().content == null && nativeContentIs(WFlyout(), null)
                results
            } shouldContainExactly listOf(true, true, true, true)
        }

        test("showAt opens it, making isOpen true and showing the content, and hide closes it and removes the content") {
            withButton { button ->
                val label = onUiThreadGet { WLabel("Content") }
                val flyout = onUiThreadGet { WFlyout(label) }
                onUiThreadGet { flyout.isOpen }.shouldBeFalse()
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                eventually(5.seconds) {
                    onUiThreadGet { label.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
                }
                onUiThread { flyout.hide() }
                awaitOpen(flyout, false)
                eventually(5.seconds) {
                    onUiThreadGet { label.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeFalse()
                }
            }
        }

        test("replacing content after closing and opening it again shows the new content") {
            withButton { button ->
                val first = onUiThreadGet { WLabel("First") }
                val second = onUiThreadGet { WLabel("Next") }
                val flyout = onUiThreadGet { WFlyout(first) }
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                onUiThread { flyout.hide() }
                awaitOpen(flyout, false)
                onUiThread { flyout.content = second }
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                eventually(5.seconds) {
                    onUiThreadGet { second.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
                }
                onUiThread { flyout.hide() }
                awaitOpen(flyout, false)
            }
        }

        test("calling hide twice while isOpen does not throw and it stays closed") {
            withButton { button ->
                val flyout = onUiThreadGet { WFlyout(WLabel("Content")) }
                onUiThread { flyout.showAt(button) }
                awaitOpen(flyout, true)
                onUiThread { flyout.hide() }
                onUiThread { flyout.hide() }
                awaitOpen(flyout, false)
            }
        }
    }
}
