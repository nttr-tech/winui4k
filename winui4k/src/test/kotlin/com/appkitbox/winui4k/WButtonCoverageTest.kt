package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests for reading and writing the public button members that had no tests (WButtonGroup.buttons, WinUI properties of WAppBarButton). */
@Tags("E2E")
class WButtonCoverageTest : FunSpec() {
    init {
        context("Defaults") {
            test("WAppBarButton's labelPosition is DEFAULT, keyboardAcceleratorText is empty and isCompact is false") {
                onUiThreadGet {
                    val button = WAppBarButton("Save")
                    Triple(button.labelPosition, button.keyboardAcceleratorText, button.isCompact)
                } shouldBe Triple(CommandBarLabelPosition.DEFAULT, "", false)
            }

            test("buttons of an empty WButtonGroup is an empty list") {
                onUiThreadGet { WButtonGroup().buttons } shouldBe emptyList()
            }
        }

        context("Reading and writing") {
            test("WAppBarButton's labelPosition, keyboardAcceleratorText and isCompact read back the set values") {
                onUiThreadGet {
                    val button = WAppBarButton("Save")
                    button.labelPosition = CommandBarLabelPosition.COLLAPSED
                    button.keyboardAcceleratorText = "Ctrl+S"
                    button.isCompact = true
                    Triple(button.labelPosition, button.keyboardAcceleratorText, button.isCompact)
                } shouldBe Triple(CommandBarLabelPosition.COLLAPSED, "Ctrl+S", true)
            }

            test("WButtonGroup's buttons is a copy in the order added, removed buttons disappear, and changing the returned list does not change the group") {
                val (texts, afterRemove, count) = onUiThreadGet {
                    val group = WButtonGroup()
                    val a = WRadioButton("a")
                    val b = WRadioButton("b")
                    val c = WCheckBox("c")
                    group.add(b)
                    group.add(a)
                    group.add(c)
                    val texts = group.buttons.map { it.text }
                    group.remove(a)
                    val copy = group.buttons.toMutableList()
                    copy.clear()
                    Triple(texts, group.buttons.map { it.text }, group.buttonCount)
                }
                texts shouldContainExactly listOf("b", "a", "c")
                afterRemove shouldContainExactly listOf("b", "c")
                count shouldBe 2
            }
        }
    }
}
