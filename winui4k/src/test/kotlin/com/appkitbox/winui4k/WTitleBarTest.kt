package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for reading back the properties of [WTitleBar] (WinUI's TitleBar control) and adding and removing listeners.
 * The back / pane toggle buttons need a window to actually press them, so they are not pressed here. The shape seen from Java is tested in WTitleBarForJavaTest.
 */
class WTitleBarTest : FunSpec() {
    init {
        test("title and subtitle read back the written strings, and are empty when new") {
            onUiThread {
                val bar = WTitleBar()
                bar.title shouldBe ""
                bar.subtitle shouldBe ""
                bar.title = "Document 1"
                bar.subtitle = "Draft"
                bar.title shouldBe "Document 1"
                bar.subtitle shouldBe "Draft"
                bar.title = ""
                bar.title shouldBe ""
            }
        }

        test("the back button and pane toggle button are not shown by default, and read back the written values") {
            onUiThread {
                val bar = WTitleBar()
                bar.isBackButtonVisible.shouldBeFalse()
                bar.isPaneToggleButtonVisible.shouldBeFalse()
                bar.isBackButtonVisible = true
                bar.isPaneToggleButtonVisible = true
                bar.isBackButtonVisible.shouldBeTrue()
                bar.isPaneToggleButtonVisible.shouldBeTrue()
                val enabled = bar.isBackButtonEnabled
                bar.isBackButtonEnabled = !enabled
                bar.isBackButtonEnabled shouldBe !enabled
                bar.isBackButtonEnabled = enabled
                bar.isBackButtonEnabled shouldBe enabled
            }
        }

        test("any component can be placed in leftHeader / content / rightHeader, and null removes it") {
            onUiThread {
                val bar = WTitleBar()
                val left = WButton("Left")
                val center = WTextField()
                val right = WButton("Right")
                bar.leftHeader = left
                bar.content = center
                bar.rightHeader = right
                bar.leftHeader shouldBeSameInstanceAs left
                bar.content shouldBeSameInstanceAs center
                bar.rightHeader shouldBeSameInstanceAs right
                bar.leftHeader = null
                bar.content = null
                bar.rightHeader = null
                bar.leftHeader.shouldBeNull()
                bar.content.shouldBeNull()
                bar.rightHeader.shouldBeNull()
            }
        }

        test("setting and clearing iconGlyph and iconUri go through without exceptions, and the written values are remembered") {
            onUiThread {
                val bar = WTitleBar()
                bar.iconGlyph = "\uE700"
                bar.iconGlyph shouldBe "\uE700"
                bar.iconGlyph = null
                bar.iconGlyph.shouldBeNull()
                bar.iconUri = "file:///C:/Windows/System32/imageres.dll"
                bar.iconUri shouldBe "file:///C:/Windows/System32/imageres.dll"
                bar.iconUri = null
                bar.iconUri.shouldBeNull()
            }
        }

        test("adding and removing addBackRequestedListener / addPaneToggleRequestedListener go through without exceptions") {
            onUiThread {
                val bar = WTitleBar()
                val back: () -> Unit = {}
                val toggle: () -> Unit = {}
                bar.addBackRequestedListener(back)
                bar.addPaneToggleRequestedListener(toggle)
                bar.removeBackRequestedListener(back)
                bar.removePaneToggleRequestedListener(toggle)
                bar.removeBackRequestedListener(back) // Removing an unregistered one does nothing
            }
        }

        test("setIsDragRegion accepts true / false / null (back to automatic) without exceptions") {
            onUiThread {
                val label = WLabel("Drag region")
                WTitleBar.setIsDragRegion(label, true)
                WTitleBar.setIsDragRegion(label, false)
                WTitleBar.setIsDragRegion(label, null)
            }
        }
    }
}
