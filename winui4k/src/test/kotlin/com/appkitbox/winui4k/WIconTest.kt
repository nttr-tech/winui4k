package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies that WIcon (Symbol / WImageIcon / WFontIcon) is correctly converted to a WinUI IconElement
 * (its behavior as a value and its Java-facing shape are covered by WIconForJavaTest).
 */
class WIconTest : FunSpec() {
    /** Creates an IconElement from [icon] and returns its contents in the form of [IconElements.describe]. */
    private fun describeCreated(icon: WIcon): String = onUiThreadGet {
        val element = icon.createIconElement()
        try {
            IconElements.describe(element)
        } finally {
            element.release()
        }
    }

    init {
        test("a Symbol becomes a SymbolIcon with the same Symbol value") {
            describeCreated(Symbol.SAVE) shouldBe "Symbol:SAVE"
            describeCreated(Symbol.XBOX_ONE_CONSOLE) shouldBe "Symbol:XBOX_ONE_CONSOLE"
        }

        test("WImageIcon becomes an ImageIcon that loads the file URI of the file's absolute path") {
            describeCreated(WImageIcon("C:/icons/save.png")) shouldBe "Image:file:///C:/icons/save.png"
        }

        test("ms-appx and https URIs passed to WImageIcon become the ImageIcon's image URI as is") {
            describeCreated(WImageIcon("ms-appx:///Assets/save.png")) shouldBe "Image:ms-appx:///Assets/save.png"
            describeCreated(WImageIcon("https://example.com/icons/save.png")) shouldBe "Image:https://example.com/icons/save.png"
        }

        test("WFontIcon becomes a FontIcon with the glyph and font, and omitting the font gives Segoe Fluent Icons") {
            describeCreated(WFontIcon("\uE74E")) shouldBe "Font:\uE74E/Segoe Fluent Icons"
            describeCreated(WFontIcon("★", "Segoe UI Symbol")) shouldBe "Font:★/Segoe UI Symbol"
        }

        test("each creation from the same WIcon produces a separate IconElement (it can be reused across buttons)") {
            onUiThreadGet {
                val first = Symbol.ACCEPT.createIconElement()
                val second = Symbol.ACCEPT.createIconElement()
                try {
                    Xaml.sameObject(first, second)
                } finally {
                    first.release()
                    second.release()
                }
            } shouldBe false
        }
    }
}
