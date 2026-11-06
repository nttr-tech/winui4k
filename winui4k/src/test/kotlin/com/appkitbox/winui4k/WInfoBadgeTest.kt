package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds

/** Tests for [WInfoBadge]'s value (-1 for a dot, 0, a number) and icon (the native IconSource). */
@Tags("E2E")
class WInfoBadgeTest : FunSpec() {
    /** Whether the native InfoBadge.IconSource is set. */
    private fun WInfoBadge.hasNativeIcon(): Boolean =
        inspectable.getPtrOrNull(XamlInterop.IInfoBadge_get_IconSource)?.also { it.release() } != null

    /**
     * Which one a loaded badge is currently showing: "value:<text>" if ValueTextBlock of the template (InfoBadge_themeresources.xaml) is visible,
     * "icon" if IconPresenter is visible, and "dot" if both are hidden. Call on the UI thread.
     */
    private fun WInfoBadge.shown(): String {
        val text = findNamed(uiElement, "ValueTextBlock")
        val icon = findNamed(uiElement, "IconPresenter")
        try {
            return when {
                text != null && isVisible(text) -> {
                    val block = text.queryInterface(XamlInterop.IID_ITextBlock)
                    try {
                        "value:" + block.getString(XamlInterop.ITextBlock_get_Text)
                    } finally {
                        block.release()
                    }
                }
                icon != null && isVisible(icon) -> "icon"
                else -> "dot"
            }
        } finally {
            text?.release()
            icon?.release()
        }
    }

    private fun isVisible(element: ComPtr): Boolean = element.getInt(XamlInterop.IUIElement_get_Visibility) == 0

    /** The IUIElement of the first element named [name] under [root] (borrowed) (the caller releases it). */
    private fun findNamed(root: ComPtr, name: String): ComPtr? {
        val framework = root.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement)
        val matched = framework?.let {
            try {
                it.getString(XamlInterop.IFrameworkElement_get_Name) == name
            } finally {
                it.release()
            }
        } ?: false
        if (matched) return root.queryInterface(XamlInterop.IID_IUIElement)
        for (index in 0 until Xaml.childCount(root)) {
            val child = Xaml.childAt(root, index) ?: continue
            try {
                findNamed(child, name)?.let { return it }
            } finally {
                child.release()
            }
        }
        return null
    }

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

        test("a value smaller than -1 throws IllegalArgumentException, and the value does not change") {
            onUiThreadGet {
                val badge = WInfoBadge()
                badge.value = 3
                val error = shouldThrow<IllegalArgumentException> { badge.value = -2 }
                listOf(error.message.orEmpty(), badge.value.toString())
            }.let { (message, value) ->
                message shouldContain "-1"
                value shouldBe "3"
            }
        }

        test("when loaded, 0 or greater shows the number, -1 a dot, and -1 with an icon the icon (a number wins over icon when 0 or greater)") {
            // microsoft-ui-xaml's InfoBadge::OnDisplayKindPropertiesChanged: Value >= 0 → Value, IconSource → Icon, otherwise Dot
            val badge = onUiThreadGet { WInfoBadge() }
            UiTestHarness.attachAndAwaitLoaded(badge)
            try {
                suspend fun expect(shown: String) = eventually(5.seconds) { onUiThreadGet { badge.shown() } shouldBe shown }
                expect("dot")
                onUiThread { badge.value = 0 }
                expect("value:0")
                onUiThread { badge.value = 128 }
                expect("value:128")
                onUiThread { badge.icon = Symbol.MAIL }
                expect("value:128")
                onUiThread { badge.value = -1 }
                expect("icon")
                onUiThread { badge.icon = null }
                expect("dot")
            } finally {
                UiTestHarness.detach(badge)
            }
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
