package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

/** Tests for [WInfoBadge]'s value (-1 for a dot, 0, a number) and icon (the native IconSource). */
class WInfoBadgeTest : FunSpec() {
    /** Whether the native InfoBadge.IconSource is set. */
    private fun WInfoBadge.hasNativeIcon(): Boolean =
        inspectable.getPtrOrNull(XamlInterop.IInfoBadge_get_IconSource)?.also { it.release() } != null

    init {
        test("value defaults to -1 (a dot badge), and 0 and positive numbers can be read back") {
            onUiThreadGet {
                val badge = WInfoBadge()
                val initial = badge.value
                badge.value = 0
                val zero = badge.value
                badge.value = 42
                val many = badge.value
                badge.value = -1
                listOf(initial, zero, many, badge.value)
            } shouldContainExactly listOf(-1, 0, 42, -1)
        }

        test("icon is null by default, setting it attaches the native IconSource, and null removes it") {
            onUiThreadGet {
                val badge = WInfoBadge()
                val initial = listOf(badge.icon, badge.hasNativeIcon())
                badge.icon = Symbol.SAVE
                val set = listOf(badge.icon, badge.hasNativeIcon())
                badge.icon = null
                initial + set + listOf(badge.icon, badge.hasNativeIcon())
            } shouldContainExactly listOf(null, false, Symbol.SAVE, true, null, false)
        }

        test("an icon set with setSymbolIcon can also be read with icon") {
            onUiThreadGet {
                val badge = WInfoBadge()
                badge.setSymbolIcon(Symbol.MAIL)
                listOf(badge.icon, badge.hasNativeIcon())
            } shouldContainExactly listOf(Symbol.MAIL, true)
        }
    }
}
