package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/** Tests for [WTeachingTip]'s properties (read back from native), the close reason with the buttons and hide(), and the action button listeners. */
@Tags("E2E")
class WTeachingTipTest : FunSpec() {
    /** Whether the native TeachingTip.IconSource is set. */
    private fun WTeachingTip.hasNativeIcon(): Boolean =
        inspectable.getPtrOrNull(XamlInterop.ITeachingTip_get_IconSource)?.also { it.release() } != null

    init {
        test("title, subtitle and button texts read back the set values, and button texts default to empty") {
            onUiThreadGet {
                val tip = WTeachingTip("New feature", "You can share from here")
                val defaults = listOf(tip.actionButtonText, tip.closeButtonText)
                tip.actionButtonText = "Try it"
                tip.closeButtonText = "Later"
                defaults + listOf(tip.title, tip.subtitle, tip.actionButtonText, tip.closeButtonText)
            } shouldContainExactly listOf("", "", "New feature", "You can share from here", "Try it", "Later")
        }

        test("icon is null by default, setting it attaches the native IconSource, and null removes it") {
            onUiThreadGet {
                val tip = WTeachingTip()
                val initial = listOf(tip.icon, tip.hasNativeIcon())
                tip.icon = Symbol.SHARE
                val set = listOf(tip.icon, tip.hasNativeIcon())
                tip.icon = WFontIcon("\uE8A5")
                val font = listOf(tip.icon is WFontIcon, tip.hasNativeIcon())
                tip.icon = null
                initial + set + font + listOf(tip.icon, tip.hasNativeIcon())
            } shouldContainExactly listOf(null, false, Symbol.SHARE, true, true, true, null, false)
        }

        test("closing an open callout with hide() gives the close reason PROGRAMMATIC") {
            val reasons = Collections.synchronizedList(ArrayList<TeachingTipCloseReason>())
            val panel = onUiThreadGet { WPanel() }
            val tip = onUiThreadGet {
                WTeachingTip("Title").also { tip ->
                    panel.add(tip)
                    tip.addCloseListener { reasons += it }
                }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread { tip.show() }
                eventually(5.seconds) { onUiThreadGet { tip.isOpen } shouldBe true }
                // hide() during the opening animation is ignored because TeachingTip sets IsOpen back to true (OnIsOpenChanged in TeachingTip.cpp)
                Thread.sleep(OPEN_ANIMATION_MILLIS)
                onUiThread { tip.hide() }
                eventually(5.seconds) { reasons.toList() shouldContainExactly listOf(TeachingTipCloseReason.PROGRAMMATIC) }
                onUiThreadGet { tip.isOpen } shouldBe false
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        context("Defaults") {
            test("target is null, preferredPlacement is AUTO, light dismiss is disabled, and it is closed") {
                onUiThreadGet {
                    val tip = WTeachingTip()
                    listOf(tip.target, tip.preferredPlacement, tip.isLightDismissEnabled, tip.isOpen)
                } shouldContainExactly listOf(null, TeachingTipPlacement.AUTO, false, false)
            }
        }

        context("Reading and writing") {
            test("preferredPlacement reads back every value, and isLightDismissEnabled and target read back the set values") {
                onUiThread {
                    val tip = WTeachingTip()
                    TeachingTipPlacement.entries.map { placement ->
                        tip.preferredPlacement = placement
                        tip.preferredPlacement
                    } shouldContainExactly TeachingTipPlacement.entries
                    tip.isLightDismissEnabled = true
                    tip.isLightDismissEnabled shouldBe true
                    val button = WButton("Target")
                    tip.target = button
                    (tip.target === button) shouldBe true
                    tip.target = null
                    tip.target shouldBe null
                }
            }
        }

        context("Events") {
            test("pressing the action button calls the ActionListener and keeps it open, and pressing the close button closes it with CLOSE_BUTTON. Removed listeners are not called") {
                val events = Collections.synchronizedList(ArrayList<String>())
                val target = onUiThreadGet { WButton("Target") }
                val panel = onUiThreadGet { WPanel().also { it.add(target) } }
                val tip = onUiThreadGet {
                    WTeachingTip("New feature", "You can share from here").also { tip ->
                        panel.add(tip)
                        tip.target = target
                        tip.actionButtonText = "Try it"
                        tip.closeButtonText = "Later"
                        val removedAction: () -> Unit = { events += "removed action" }
                        val removedClose: (TeachingTipCloseReason) -> Unit = { events += "removed close" }
                        tip.addActionListener(removedAction)
                        tip.addCloseListener(removedClose)
                        tip.addActionListener { events += "action" }
                        tip.addCloseListener { events += "closed:$it" }
                        tip.removeActionListener(removedAction)
                        tip.removeCloseListener(removedClose)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    onUiThread { tip.show() }
                    eventually(5.seconds) { onUiThreadGet { tip.isOpen } shouldBe true }
                    Thread.sleep(OPEN_ANIMATION_MILLIS)
                    pressInOpenPopup(panel, "ActionButton")
                    eventually(5.seconds) { events.toList() shouldContainExactly listOf("action") }
                    onUiThreadGet { tip.isOpen } shouldBe true
                    pressInOpenPopup(panel, "CloseButton")
                    eventually(5.seconds) { events.toList() shouldContainExactly listOf("action", "closed:CLOSE_BUTTON") }
                    onUiThreadGet { tip.isOpen } shouldBe false
                } finally {
                    UiTestHarness.detach(panel)
                }
            }
        }
    }

    /** Finds and presses the element named [name] in the Popups open in [owner]'s window (the teaching tip's parts are placed in a Popup). */
    private fun pressInOpenPopup(owner: WComponent, name: String) = onUiThread {
        val root = checkNotNull(owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) { "not placed in a window" }
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        try {
            val found = (0 until popups.getInt(FoundationInterop.IVectorView_get_Size)).firstNotNullOfOrNull { index ->
                val popup = popups.getPtr(FoundationInterop.IVectorView_GetAt, index)
                val child = try {
                    popup.getPtrOrNull(XamlInterop.IPopup_get_Child)
                } finally {
                    popup.release()
                }
                child?.let { TemplateParts.findByName(it, name) } // Ownership of child passes to findByName
            } ?: error("No $name in the open Popups")
            try {
                Xaml.invoke(found)
            } finally {
                found.release()
            }
        } finally {
            popups.release()
        }
    }

    private companion object {
        /** The time to wait for the opening animation (scaling up) to finish. */
        const val OPEN_ANIMATION_MILLIS = 1_000L
    }
}
