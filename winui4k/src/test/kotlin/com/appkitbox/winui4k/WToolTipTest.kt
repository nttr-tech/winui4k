package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for JComponent.setToolTipText / getToolTipText and the JToolTip-like [WToolTip] (tipText, component, isOpen).
 * Values are read back from ToolTipService.GetToolTip and ToolTip.Content.
 */
@Tags("E2E")
class WToolTipTest : FunSpec() {
    init {
        test("the string set with setToolTipText can be read, and null removes it (null by default)") {
            onUiThreadGet {
                val button = WButton("Save")
                val initial = button.toolTipText
                button.toolTipText = "Save to a file"
                val set = listOf(button.toolTipText, button.toolTip)
                button.toolTipText = null
                listOf(initial) + set + listOf(button.toolTipText, button.toolTip)
            } shouldContainExactly listOf(null, "Save to a file", "Save to a file", null, null)
        }

        test("an empty string can also be read back as a string hint") {
            onUiThreadGet {
                val label = WLabel("Name")
                label.toolTipText = ""
                label.toolTipText
            } shouldBe ""
        }

        test("the string of a WToolTip attached with setToolTip can be read with toolTipText, including strings changed later") {
            onUiThreadGet {
                val button = WButton("Open")
                val tip = WToolTip()
                tip.text = "First"
                button.setToolTip(tip)
                val first = button.toolTipText
                tip.text = "Afterwards"
                listOf(first, button.toolTipText)
            } shouldContainExactly listOf("First", "Afterwards")
        }

        test("toolTipText is null if the WToolTip content is not a string") {
            onUiThreadGet {
                val button = WButton("Open")
                val tip = WToolTip()
                tip.content = WLabel("Label tooltip")
                button.setToolTip(tip)
                button.toolTipText
            }.shouldBeNull()
        }

        test("WToolTip.component is the attached component, and goes back to null when another hint is set") {
            onUiThread {
                val button = WButton("Open")
                val tip = WToolTip()
                tip.component.shouldBeNull()
                button.setToolTip(tip)
                tip.component shouldBeSameInstanceAs button
                button.toolTipText = "String tooltip"
                tip.component.shouldBeNull()
                button.toolTipText shouldBe "String tooltip"
            }
        }

        test("attaching the same WToolTip to another component removes it from the previous component") {
            onUiThread {
                val first = WButton("1")
                val second = WButton("2")
                val tip = WToolTip()
                tip.text = "Shared"
                first.setToolTip(tip)
                second.setToolTip(tip)
                tip.component shouldBeSameInstanceAs second
                first.toolTipText.shouldBeNull()
                second.toolTipText shouldBe "Shared"
                // Attaching it again to the same component does not detach it
                second.setToolTip(tip)
                tip.component shouldBeSameInstanceAs second
                second.toolTipText shouldBe "Shared"
            }
        }

        test("tipText is the string content, null if not a string or empty. Setting null empties the content") {
            onUiThreadGet {
                val tip = WToolTip()
                val initial = listOf(tip.tipText, tip.text)
                tip.tipText = "Description"
                val set = listOf(tip.tipText, tip.text)
                tip.content = WLabel("Component")
                val component = listOf(tip.tipText, tip.text)
                tip.text = "Back to a string"
                val back = tip.content
                tip.tipText = null
                initial + set + component + listOf(back?.toString(), tip.tipText, tip.text) + listOf(tip.content?.toString())
            } shouldContainExactly listOf(null, "", "Description", "Description", null, "", null, null, "", null)
        }

        test("isOpen is false by default, and if the attached component is in a window, setting true opens it and false closes it") {
            val button = onUiThreadGet { WButton("Tooltip target") }
            val tip = onUiThreadGet { WToolTip().also { it.text = "Open tooltip" } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThreadGet {
                    button.setToolTip(tip)
                    val initial = tip.isOpen
                    tip.isOpen = true
                    val opened = tip.isOpen
                    tip.isOpen = false
                    listOf(initial, opened, tip.isOpen)
                } shouldContainExactly listOf(false, true, false)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        context("Defaults") {
            test("placement defaults to the WinUI default (TOP), the text is empty, and content and component are null") {
                onUiThreadGet {
                    val tip = WToolTip()
                    listOf(tip.placement, tip.text, tip.content, tip.component)
                } shouldContainExactly listOf(ToolTipPlacement.TOP, "", null, null)
            }
        }

        context("Reading and writing") {
            test("placement writes every value to the native ToolTip.Placement and reads it back") {
                onUiThreadGet {
                    val tip = WToolTip()
                    ToolTipPlacement.entries.map { placement ->
                        tip.placement = placement
                        tip.placement
                    }
                } shouldContainExactly ToolTipPlacement.entries
            }

            test("a component set to content reads back from content, and setting text returns content to null") {
                onUiThreadGet {
                    val tip = WToolTip()
                    val label = WLabel("Component tip")
                    tip.content = label
                    val set = tip.content === label
                    tip.text = "Text tip"
                    listOf(set, tip.content, tip.text)
                } shouldContainExactly listOf(true, null, "Text tip")
            }
        }
    }
}
