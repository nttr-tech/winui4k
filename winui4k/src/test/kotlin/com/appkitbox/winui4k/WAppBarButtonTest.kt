package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests the icons (overriding [WButtonBase.icon]) and WAction of the toolbar buttons WAppBarButton / WAppBarToggleButton.
 * Reads the native properties to check that the icon goes into AppBarButton.Icon / AppBarToggleButton.Icon rather than
 * Content.
 * The form seen from Java is in WAppBarButtonForJavaTest.
 */
class WAppBarButtonTest : FunSpec() {
    /** Content of WAppBarButton.Icon ([IconElements.describe]). "none" if absent. */
    private fun nativeIcon(button: WAppBarButton): String = describeIcon(button, XamlInterop.IAppBarButton_get_Icon)

    /** Content of WAppBarToggleButton.Icon ([IconElements.describe]). "none" if absent. */
    private fun nativeIcon(button: WAppBarToggleButton): String = describeIcon(button, XamlInterop.IAppBarToggleButton_get_Icon)

    private fun describeIcon(button: WButtonBase, slot: Int): String {
        val icon = button.inspectable.getPtrOrNull(slot) ?: return "none"
        return try {
            IconElements.describe(icon)
        } finally {
            icon.release()
        }
    }

    private class RecordingAction(name: String, icon: WIcon?) : WAbstractAction(name, icon) {
        var performed = 0

        override fun actionPerformed(e: WActionEvent) {
            performed++
        }
    }

    init {
        test("WAppBarButton's icon goes into the native Icon for a Symbol, an image and a font glyph alike, and Content stays empty") {
            onUiThreadGet {
                val button = WAppBarButton("Save", Symbol.SAVE)
                val symbol = nativeIcon(button)
                button.icon = WFontIcon("\uE8E5")
                val font = nativeIcon(button)
                button.icon = WImageIcon("C:/icons/open.png")
                val image = nativeIcon(button)
                listOf(symbol, font, image, button.icon, ButtonContents.contentOf(button))
            } shouldContainExactly listOf(
                "Symbol:SAVE",
                "Font:\uE8E5/Segoe Fluent Icons",
                "Image:file:///C:/icons/open.png",
                WImageIcon("C:/icons/open.png"),
                emptyList<String>(),
            )
        }

        test("setting WAppBarButton's icon to null also removes the native Icon") {
            onUiThreadGet {
                val button = WAppBarButton("Save", WFontIcon("\uE74E"))
                button.icon = null
                button.icon to nativeIcon(button)
            } shouldBe (null to "none")
        }

        test("WAppBarToggleButton's icon also goes into the native Icon and is removed with null") {
            onUiThreadGet {
                val button = WAppBarToggleButton("Bold", Symbol.BOLD)
                val symbol = nativeIcon(button)
                button.icon = WFontIcon("\uE8DD")
                val font = nativeIcon(button)
                button.icon = null
                listOf(symbol, font, nativeIcon(button), ButtonContents.contentOf(button))
            } shouldContainExactly listOf("Symbol:BOLD", "Font:\uE8DD/Segoe Fluent Icons", "none", emptyList<String>())
        }

        test("changing the text position and gap does not create Content for an AppBar button (the values are only kept)") {
            onUiThreadGet {
                val button = WAppBarButton("Save", Symbol.SAVE)
                button.horizontalTextPosition = HorizontalTextPosition.CENTER
                button.verticalTextPosition = VerticalTextPosition.BOTTOM
                button.iconTextGap = 10.0
                listOf(button.horizontalTextPosition, button.verticalTextPosition, button.iconTextGap, ButtonContents.contentOf(button))
            } shouldContainExactly listOf(HorizontalTextPosition.CENTER, VerticalTextPosition.BOTTOM, 10.0, emptyList<String>())
        }

        test("binding an action to WAppBarButton puts SMALL_ICON into the native Icon, and clicking calls the action") {
            onUiThreadGet {
                val action = RecordingAction("Save", Symbol.SAVE)
                val button = WAppBarButton()
                button.action = action
                val before = nativeIcon(button)
                action.putValue(WAction.SMALL_ICON, Symbol.OPEN_FILE)
                button.doClick()
                listOf(before, nativeIcon(button), action.performed)
            } shouldContainExactly listOf("Symbol:SAVE", "Symbol:OPEN_FILE", 1)
        }

        test("WAppBarToggleButton's action is linked to the toggle's selected state via SELECTED_KEY") {
            onUiThreadGet {
                val action = RecordingAction("Bold", Symbol.BOLD)
                action.putValue(WAction.SELECTED_KEY, true)
                val button = WAppBarToggleButton()
                button.action = action
                val selected = button.isSelected
                button.doClick()
                listOf(nativeIcon(button), selected, action.getValue(WAction.SELECTED_KEY))
            } shouldContainExactly listOf("Symbol:BOLD", true, false)
        }
    }
}
