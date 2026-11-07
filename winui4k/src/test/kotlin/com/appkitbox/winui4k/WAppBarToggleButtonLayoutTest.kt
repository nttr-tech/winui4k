package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Tests that check, by actually showing them, that when a WAppBarToggleButton on a toolbar (WCommandBar) is selected, the accent-colored background (AppBarToggleButtonInnerBorder)
 * covers the icon (ContentViewbox) and the icon does not stick out.
 */
@Tags("E2E")
class WAppBarToggleButtonLayoutTest : FunSpec() {
    /** The top and bottom edges of the background and the icon of [button], in the button's coordinates. */
    private fun backgroundAndIcon(button: WAppBarToggleButton): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        val background = checkNotNull(templatePartVerticalEdges(button, "AppBarToggleButtonInnerBorder")) { "background is not shown" }
        val icon = checkNotNull(templatePartVerticalEdges(button, "ContentViewbox")) { "icon is not shown" }
        return background to icon
    }

    /** Shows [bar], runs [block] on the UI thread and then removes it. */
    private fun <T> withShown(bar: WCommandBar, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(bar)
        try {
            return onUiThreadGet {
                bar.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(bar)
        }
    }

    private fun assertIconInside(edges: Pair<Pair<Double, Double>, Pair<Double, Double>>) {
        val (background, icon) = edges
        icon.first shouldBeGreaterThanOrEqual background.first
        icon.second shouldBeLessThanOrEqual background.second
    }

    init {
        test("for a toggle button selected before being put on the toolbar, the background covers the icon and the icon does not stick out of the background") {
            lateinit var bold: WAppBarToggleButton
            val bar = onUiThreadGet {
                WCommandBar().also { bar ->
                    bar.add(WAppBarButton("Cut", Symbol.CUT))
                    bold = WAppBarToggleButton("Bold", Symbol.BOLD)
                    bold.isSelected = true
                    bar.add(bold)
                }
            }
            assertIconInside(withShown(bar) { backgroundAndIcon(bold) })
        }

        test("even on a toolbar in NORTH of a WBorderLayout, the background of a selected toggle button covers the icon and the icon does not stick out") {
            lateinit var bold: WAppBarToggleButton
            lateinit var bar: WCommandBar
            lateinit var status: WLabel
            val panel = onUiThreadGet {
                bar = WCommandBar()
                bar.add(WAppBarButton("Cut", Symbol.CUT))
                bar.addSeparator()
                bold = WAppBarToggleButton("Bold", Symbol.BOLD)
                bold.isSelected = true
                bold.toolTipText = "Bold"
                bar.add(bold)
                WLayoutPanel(WBorderLayout()).also {
                    it.width = 600.0
                    it.height = 200.0
                    it.add(bar, WBorderLayout.NORTH)
                    status = WLabel("Bold: true")
                    it.add(status, WBorderLayout.CENTER)
                }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            val (edges, barHeight, statusTop) = try {
                onUiThreadGet {
                    panel.updateLayout()
                    Triple(backgroundAndIcon(bold), bar.actualHeight, VisualHitTest.transform(status.uiElement, panel.uiElement, 0.0, 0.0).second)
                }
            } finally {
                UiTestHarness.detach(panel)
            }
            assertIconInside(edges)
            // The toolbar keeps its closed height (48), and the CENTER component starts below it
            barHeight shouldBe 48.0
            statusTop shouldBe 48.0
        }

        test("for a toggle button selected after showing too, the background covers the icon and the icon does not stick out of the background") {
            lateinit var italic: WAppBarToggleButton
            val bar = onUiThreadGet {
                WCommandBar().also { bar ->
                    italic = WAppBarToggleButton("Italic", Symbol.ITALIC)
                    bar.add(italic)
                }
            }
            assertIconInside(
                withShown(bar) {
                    italic.isSelected = true
                    bar.updateLayout()
                    backgroundAndIcon(italic)
                },
            )
        }
    }
}
