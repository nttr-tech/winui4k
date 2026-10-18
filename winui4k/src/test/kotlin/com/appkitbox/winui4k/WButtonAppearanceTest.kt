package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Verifies WButton's appearance (switching built-in styles such as isAccent). */
class WButtonAppearanceTest : FunSpec() {
    /** The key of the Style set on the button ("other" if it is none of the built-in styles, null if unset). */
    private fun styleKeyOf(button: WButton): String? {
        val style = button.frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_get_Style) ?: return null
        return try {
            BUILT_IN_STYLES.firstOrNull { key ->
                val candidate = WinUiUtilities.lookupApplicationResource(key)
                try {
                    Xaml.sameObject(style, candidate)
                } finally {
                    candidate.release()
                }
            } ?: "other"
        } finally {
            style.release()
        }
    }

    init {
        test("isAccent is false by default and Style is unset (the implicit style)") {
            val (accent, style) = onUiThreadGet {
                val button = WButton("Save")
                button.isAccent to styleKeyOf(button)
            }
            accent.shouldBeFalse()
            style shouldBe null
        }

        test("isAccent = true applies AccentButtonStyle, and false clears the Style") {
            val (accentStyle, accent, defaultStyle) = onUiThreadGet {
                val button = WButton("Save")
                button.isAccent = true
                val accentStyle = styleKeyOf(button)
                val accent = button.isAccent
                button.isAccent = false
                Triple(accentStyle, accent, styleKeyOf(button))
            }
            accentStyle shouldBe "AccentButtonStyle"
            accent.shouldBeTrue()
            defaultStyle shouldBe null
        }

        test("setting the same value to isAccent repeatedly does not change the state") {
            onUiThreadGet {
                val button = WButton("Save")
                button.isAccent = true
                button.isAccent = true
                val twiceTrue = styleKeyOf(button)
                button.isAccent = false
                button.isAccent = false
                twiceTrue to styleKeyOf(button)
            } shouldBe ("AccentButtonStyle" to null)
        }

        test("Java sees isAccent / setAccent(boolean)") {
            WButton::class.java.getMethod("isAccent").returnType shouldBe Boolean::class.javaPrimitiveType
            WButton::class.java.getMethod("setAccent", Boolean::class.javaPrimitiveType).returnType shouldBe Void.TYPE
        }
    }

    private companion object {
        val BUILT_IN_STYLES = listOf("AccentButtonStyle", "SubtleButtonStyle", "DefaultButtonStyle")
    }
}
