package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/** Tests for [WTeachingTip]'s properties (read back from native) and the close reason of hide(). */
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
    }

    private companion object {
        /** The time to wait for the opening animation (scaling up) to finish. */
        const val OPEN_ANIMATION_MILLIS = 1_000L
    }
}
