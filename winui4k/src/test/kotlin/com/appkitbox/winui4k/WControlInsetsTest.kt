package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WControl.insets], corresponding to JComponent.getInsets, the inner padding ([WControl.padding] / [WControl.setPadding]) and
 * the content alignment ([WControl.horizontalContentAlignment] / [WControl.verticalContentAlignment]).
 *
 * Swing's getInsets is the width inside the border (Border), and JButton's margin is included in the border. In WinUI it corresponds to the sum of
 * the border thickness (Control.BorderThickness) and the inner padding (Control.Padding). The expected button defaults are taken from WinUI's
 * Button_themeresources.xaml (ButtonPadding = 11,5,11,6, ButtonBorderThemeThickness = 1).
 */
@Tags("E2E")
class WControlInsetsTest : FunSpec() {
    /** Reads [read] on the control made by [create] put in a window, and removes it. */
    private fun <T : WControl, R> attached(create: () -> T, read: (T) -> R): R {
        val control = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(control)
        return try {
            onUiThreadGet { read(control) }
        } finally {
            UiTestHarness.detach(control)
        }
    }

    init {
        context("Defaults") {
            test("the insets of a WButton put in a window are the sum of the theme's border 1 and padding (11, 5, 11, 6)") {
                attached({ WButton("Press") }) { it.insets } shouldBe WInsets(top = 6.0, left = 12.0, bottom = 7.0, right = 12.0)
            }

            test("the default content alignment of WButton is centered both horizontally and vertically") {
                attached({ WButton("Press") }) { it.horizontalContentAlignment to it.verticalContentAlignment } shouldBe
                    (HorizontalAlignment.CENTER to VerticalAlignment.CENTER)
            }
        }

        context("Reading and writing") {
            test("the sides of setPadding go into Control.Padding, and insets become their sum with the border thickness") {
                attached({ WButton("Press") }) { button ->
                    button.setPadding(1.0, 2.0, 3.0, 4.0)
                    XamlStructs.getThickness(button.control, XamlInterop.IControl_get_Padding).toList() to button.insets
                } shouldBe (listOf(1.0, 2.0, 3.0, 4.0) to WInsets(top = 3.0, left = 2.0, bottom = 5.0, right = 4.0))
            }

            test("padding gives the same padding on all sides") {
                attached({ WButton("Press") }) { button ->
                    button.padding = 8.0
                    button.padding to XamlStructs.getThickness(button.control, XamlInterop.IControl_get_Padding).toList()
                } shouldBe (8.0 to listOf(8.0, 8.0, 8.0, 8.0))
            }

            test("the insets of a border combining a line border and an empty border are the sum of both, as with Swing's CompoundBorder") {
                attached({ WButton("Press") }) { button ->
                    button.border = WBorderFactory.createCompoundBorder(
                        WBorderFactory.createLineBorder(WColor.BLACK, 2),
                        WBorderFactory.createEmptyBorder(1, 2, 3, 4),
                    )
                    button.insets
                } shouldBe WInsets(top = 3.0, left = 4.0, bottom = 5.0, right = 6.0)
            }

            test("horizontalContentAlignment and verticalContentAlignment read back the set values from native") {
                attached({ WButton("Press") }) { button ->
                    button.horizontalContentAlignment = HorizontalAlignment.LEFT
                    button.verticalContentAlignment = VerticalAlignment.BOTTOM
                    button.horizontalContentAlignment to button.verticalContentAlignment
                } shouldBe (HorizontalAlignment.LEFT to VerticalAlignment.BOTTOM)
            }
        }

        context("Boundaries and exceptions") {
            test("setting the border back to null returns insets to the theme default (the value when put in the window)") {
                attached({ WButton("Press") }) { button ->
                    button.border = WBorderFactory.createEmptyBorder(9, 9, 9, 9)
                    button.border = null
                    button.insets
                } shouldBe WInsets(top = 6.0, left = 12.0, bottom = 7.0, right = 12.0)
            }
        }
    }
}
