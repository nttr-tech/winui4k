package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WLabel.isOpaque] / [WLabel.background] / [WLabel.border], [WPanel.border] and [WLayoutPanel.border], which correspond to setOpaque, setBackground and setBorder of JLabel / JPanel.
 * The expected values come from Swing's rules (JLabel is transparent by default and paints the background color only with setOpaque(true);
 * the preferred size grows by the border's width, a panel's children are laid out inside the border, and JPanel's getInsets is the border's width).
 */
@Tags("E2E")
class WPanelBorderTest : FunSpec() {
    /** The color of Panel.Background of the root Grid of [label] (null if there is no brush). */
    private fun nativeBackground(label: WLabel): WColor? {
        val panel = label.inspectable.queryInterface(XamlInterop.IID_IPanel)
        try {
            val brush = panel.getPtrOrNull(XamlInterop.IPanel_get_Background) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        } finally {
            panel.release()
        }
    }

    /** The number of native Children of [container] (including decoration elements without child wrappers). */
    private fun nativeChildCount(container: WContainer): Int {
        val panel = container.inspectable.queryInterface(XamlInterop.IID_IPanel)
        val children: ComPtr = panel.getPtr(XamlInterop.IPanel_get_Children)
        panel.release()
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(4)
                children.call(FoundationInterop.IVector_get_Size, out)
                Ffi.backend.memory.getInt(out, 0)
            }
        } finally {
            children.release()
        }
    }

    /** Puts [component] aligned to the top left, and runs [block] on the UI thread after it is laid out. */
    private fun <T> shown(component: WComponent, block: () -> T): T {
        val host = onUiThreadGet {
            WPanel().also {
                it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                it.verticalAlignmentInParent = VerticalAlignment.TOP
                it.add(component)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(host)
        try {
            repeat(2) { onUiThread { host.validate() } }
            onUiThread { host.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(host)
        }
    }

    init {
        context("WLabel background") {
            test("by default isOpaque is false and background is null, and the background is transparent") {
                onUiThreadGet {
                    val label = WLabel("Heading")
                    listOf(label.isOpaque, label.background, nativeBackground(label)?.alpha)
                } shouldBe listOf(false, null, 0)
            }

            test("while isOpaque is false, setting background does not paint it (as with JLabel)") {
                onUiThreadGet {
                    val label = WLabel("Heading")
                    label.background = WColor(255, 255, 0)
                    label.background to nativeBackground(label)?.alpha
                } shouldBe (WColor(255, 255, 0) to 0)
            }

            test("setting isOpaque to true paints with the background color, and setting it back to false makes it transparent again") {
                onUiThreadGet {
                    val label = WLabel("Heading")
                    label.background = WColor(255, 255, 0)
                    label.isOpaque = true
                    val painted = nativeBackground(label)
                    label.isOpaque = false
                    painted to nativeBackground(label)?.alpha
                } shouldBe (WColor(255, 255, 0) to 0)
            }
        }

        context("WLabel and WPanel borders") {
            test("an empty border (top 5, left 6, bottom 7, right 8) on a WLabel makes the preferred size 14 wider and 12 taller") {
                val label = onUiThreadGet { WLabel("Heading") }
                shown(label) {
                    val before = label.preferredSize()
                    label.border = WBorderFactory.createEmptyBorder(5, 6, 7, 8)
                    label.updateLayout()
                    val after = label.preferredSize()
                    WSize(after.width - before.width, after.height - before.height)
                } shouldBe WSize(14.0, 12.0)
            }

            test("a line border of thickness 2 on a WLabel makes the preferred size 4 larger in both width and height, and null restores it") {
                val label = onUiThreadGet { WLabel("Heading") }
                shown(label) {
                    val before = label.preferredSize()
                    label.border = WBorderFactory.createLineBorder(WColor(255, 0, 0), 2)
                    label.updateLayout()
                    val lined = label.preferredSize()
                    label.border = null
                    label.updateLayout()
                    listOf(lined.width - before.width, lined.height - before.height, label.preferredSize() == before)
                } shouldBe listOf(4.0, 4.0, true)
            }

            test("a compound of a line and an empty border on a WPanel places the child inside the border (line 1 + padding 10)") {
                val (panel, child) = onUiThreadGet {
                    val child = WButton("Center")
                    WPanel().also { it.add(child) } to child
                }
                shown(panel) {
                    panel.border = WBorderFactory.createCompoundBorder(
                        WBorderFactory.createLineBorder(WColor(0, 0, 255)),
                        WBorderFactory.createEmptyBorder(10, 10, 10, 10),
                    )
                    panel.updateLayout()
                    child.location
                } shouldBe WPoint(11, 11)
            }

            test("a compound with two line borders throws IllegalArgumentException, and the border does not change") {
                onUiThreadGet {
                    val label = WLabel("Heading")
                    val panel = WPanel()
                    val twoLines = WBorderFactory.createCompoundBorder(
                        WBorderFactory.createLineBorder(WColor(0, 0, 0)),
                        WBorderFactory.createLineBorder(WColor(255, 0, 0)),
                    )
                    shouldThrow<IllegalArgumentException> { label.border = twoLines }
                    shouldThrow<IllegalArgumentException> { panel.border = twoLines }
                    label.border to panel.border
                } shouldBe (null to null)
            }
        }

        context("WLayoutPanel border") {
            test("an empty border makes insets the border's width, and the children are placed inside it (as with JPanel's getInsets)") {
                val (panel, child) = onUiThreadGet {
                    val child = WButton("Center")
                    WLayoutPanel(WBorderLayout()).also {
                        it.width = 200.0
                        it.height = 100.0
                        it.add(child, WBorderLayout.CENTER)
                    } to child
                }
                shown(panel) {
                    panel.border = WBorderFactory.createEmptyBorder(5, 6, 7, 8)
                    panel.validate()
                    panel.updateLayout()
                    listOf(panel.insets, child.bounds)
                } shouldBe listOf(WInsets(top = 5.0, left = 6.0, bottom = 7.0, right = 8.0), WRectangle(6, 5, 186, 88))
            }

            test("a line border places a line element behind the children (not in the child list), which stays after removeAll and disappears with null") {
                val panel = onUiThreadGet {
                    WLayoutPanel(WBorderLayout()).also {
                        it.width = 120.0
                        it.height = 60.0
                        it.add(WButton("Center"), WBorderLayout.CENTER)
                    }
                }
                shown(panel) {
                    panel.border = WBorderFactory.createLineBorder(WColor(255, 0, 0), 3)
                    val withLine = listOf(nativeChildCount(panel), panel.componentCount, panel.insets == WInsets(3.0))
                    panel.removeAll()
                    val afterRemoveAll = nativeChildCount(panel)
                    panel.border = null
                    withLine + listOf(afterRemoveAll, nativeChildCount(panel), panel.insets == WInsets())
                } shouldBe listOf(2, 1, true, 1, 0, true)
            }
        }
    }
}
