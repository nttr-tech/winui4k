package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
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

    /** Control.BorderThickness (left, top, right, bottom). */
    private fun borderThicknessOf(button: WButton): List<Double> {
        val control = button.inspectable.queryInterface(XamlInterop.IID_IControl)
        return try {
            XamlStructs.getThickness(control, XamlInterop.IControl_get_BorderThickness).toList()
        } finally {
            control.release()
        }
    }

    /** The BorderThickness of the template's ContentPresenter (the thickness of the border actually drawn). */
    private fun presenterBorderThicknessOf(button: WButton): List<Double> {
        val root = checkNotNull(Xaml.childAt(button.inspectable, 0)) { "the template has not been applied" }
        val presenter = root.queryInterface(XamlInterop.IID_IContentPresenter)
        root.release()
        return try {
            XamlStructs.getThickness(presenter, XamlInterop.IContentPresenter_get_BorderThickness).toList()
        } finally {
            presenter.release()
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

        test("isContentAreaFilled is true by default; false applies SubtleButtonStyle, and setting it back to true clears the Style") {
            val (initial, subtle, restored) = onUiThreadGet {
                val button = WButton("Close")
                val initial = button.isContentAreaFilled to styleKeyOf(button)
                button.isContentAreaFilled = false
                val subtle = button.isContentAreaFilled to styleKeyOf(button)
                button.isContentAreaFilled = true
                Triple(initial, subtle, button.isContentAreaFilled to styleKeyOf(button))
            }
            initial shouldBe (true to null)
            subtle shouldBe (false to "SubtleButtonStyle")
            restored shouldBe (true to null)
        }

        test("isAccent takes precedence over isContentAreaFilled = false, and clearing isAccent returns to no fill") {
            val styles = onUiThreadGet {
                val button = WButton("Close")
                button.isContentAreaFilled = false
                button.isAccent = true
                val both = styleKeyOf(button)
                button.isAccent = false
                val subtleAgain = styleKeyOf(button)
                button.isAccent = true
                button.isContentAreaFilled = true
                button.isAccent = false
                listOf(both, subtleAgain, styleKeyOf(button))
            }
            styles shouldBe listOf("AccentButtonStyle", "SubtleButtonStyle", null)
        }

        test("isBorderPainted = false makes the border thickness 0, and true returns it to the theme default (1)") {
            val button = onUiThreadGet { WButton("Border") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val (initial, painted) = onUiThreadGet { borderThicknessOf(button) to button.isBorderPainted }
                initial shouldBe listOf(1.0, 1.0, 1.0, 1.0)
                painted.shouldBeTrue()

                val (hidden, presenterHidden, notPainted) = onUiThreadGet {
                    button.isBorderPainted = false
                    button.uiElement.call(XamlInterop.IUIElement_UpdateLayout)
                    Triple(borderThicknessOf(button), presenterBorderThicknessOf(button), button.isBorderPainted)
                }
                hidden shouldBe listOf(0.0, 0.0, 0.0, 0.0)
                presenterHidden shouldBe listOf(0.0, 0.0, 0.0, 0.0)
                notPainted.shouldBeFalse()

                val restored = onUiThreadGet {
                    button.isBorderPainted = true
                    borderThicknessOf(button)
                }
                restored shouldBe listOf(1.0, 1.0, 1.0, 1.0)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("isBorderPainted = false is kept when switching built-in styles") {
            val button = onUiThreadGet { WButton("Border") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                val thickness = onUiThreadGet {
                    button.isBorderPainted = false
                    button.isContentAreaFilled = false
                    val subtle = borderThicknessOf(button)
                    button.isAccent = true
                    subtle to borderThicknessOf(button)
                }
                thickness shouldBe (listOf(0.0, 0.0, 0.0, 0.0) to listOf(0.0, 0.0, 0.0, 0.0))
            } finally {
                onUiThread { button.isAccent = false }
                UiTestHarness.detach(button)
            }
        }

        test("Java sees isContentAreaFilled / setContentAreaFilled and isBorderPainted / setBorderPainted") {
            WButton::class.java.getMethod("isContentAreaFilled").returnType shouldBe Boolean::class.javaPrimitiveType
            WButton::class.java.getMethod("setContentAreaFilled", Boolean::class.javaPrimitiveType).returnType shouldBe Void.TYPE
            WButton::class.java.getMethod("isBorderPainted").returnType shouldBe Boolean::class.javaPrimitiveType
            WButton::class.java.getMethod("setBorderPainted", Boolean::class.javaPrimitiveType).returnType shouldBe Void.TYPE
        }
    }

    private companion object {
        val BUILT_IN_STYLES = listOf("AccentButtonStyle", "SubtleButtonStyle", "DefaultButtonStyle")
    }
}
