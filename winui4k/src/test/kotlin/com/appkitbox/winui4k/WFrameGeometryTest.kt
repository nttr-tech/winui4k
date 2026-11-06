package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the position and size of the JFrame-like [WFrame] (setSize / setLocation / setBounds / setMinimumSize / pack / setLocationRelativeTo).
 * As with Swing since Java 9, the unit is DIP, and it checks that the physical pixels of the native AppWindow are the WinUI scale factor (XamlRoot.RasterizationScale)
 * times that. Each test creates its own WFrame and disposes it at the end, and the shared "winui4k tests" window is not closed.
 */
class WFrameGeometryTest : FunSpec() {
    private val frames = mutableListOf<WFrame>()

    private fun newFrame(title: String = TITLE): WFrame = onUiThreadGet { WFrame(title).also { frames += it } }

    /** Shows it and waits until the content is on the XamlRoot. */
    private suspend fun show(frame: WFrame) {
        onUiThread { frame.isVisible = true }
        eventually(5.seconds) { onUiThreadGet { frame.contentPane.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)?.release() != null } shouldBe true }
    }

    /** The scale factor WinUI uses for this window's content (physical pixels per DIP). Read after it is shown. */
    private fun scaleOf(frame: WFrame): Double = onUiThreadGet {
        val root = frame.contentPane.uiElement.getPtr(XamlInterop.IUIElement_get_XamlRoot)
        try {
            root.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale)
        } finally {
            root.release()
        }
    }

    private fun px(dip: Int, scale: Double) = (dip * scale).roundToInt()

    /** An empty panel [width] wide and [height] high (DIP). */
    private fun box(width: Double, height: Double) = WPanel().also {
        it.width = width
        it.height = height
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() } // So that closing its own window does not exit the app
        afterTest {
            val toClose = frames.toList()
            frames.clear()
            onUiThread { toClose.forEach { it.dispose() } }
        }

        test("setSize is in DIP, and the AppWindow's physical pixels are the scale factor times that. size, width and height read back the same values") {
            val frame = newFrame()
            show(frame)
            val scale = scaleOf(frame)
            onUiThread {
                frame.setSize(400, 300)
                frame.appWindow.size shouldBe WDimension(px(400, scale), px(300, scale))
                frame.size shouldBe WDimension(400, 300)
                frame.width shouldBe 400
                frame.height shouldBe 300
                frame.size = WDimension(420, 310)
                frame.appWindow.size shouldBe WDimension(px(420, scale), px(310, scale))
            }
        }

        test("setLocation, location, x, y and setBounds, bounds are in DIP too") {
            val frame = newFrame()
            show(frame)
            val scale = scaleOf(frame)
            onUiThread {
                frame.setLocation(100, 80)
                frame.appWindow.position shouldBe WPoint(px(100, scale), px(80, scale))
                frame.location shouldBe WPoint(100, 80)
                frame.x shouldBe 100
                frame.y shouldBe 80
                frame.setBounds(120, 90, 500, 350)
                frame.bounds shouldBe WRectangle(120, 90, 500, 350)
                frame.appWindow.position shouldBe WPoint(px(120, scale), px(90, scale))
                frame.appWindow.size shouldBe WDimension(px(500, scale), px(350, scale))
                frame.location = WPoint(130, 95)
                frame.bounds shouldBe WRectangle(130, 95, 500, 350)
            }
        }

        test("values set with setSize and setLocation before it is shown stay the same after it is shown") {
            val frame = newFrame()
            onUiThread {
                frame.setBounds(110, 70, 430, 330)
                frame.bounds shouldBe WRectangle(110, 70, 430, 330)
            }
            show(frame)
            onUiThread { frame.bounds shouldBe WRectangle(110, 70, 430, 330) }
        }

        test("setSize smaller than minimumSize enlarges to minimumSize. Shrinking by the user (native) also stops. null removes it") {
            val frame = newFrame()
            show(frame)
            val scale = scaleOf(frame)
            onUiThread {
                frame.minimumSize.shouldBeNull()
                frame.setSize(500, 400)
                frame.minimumSize = WDimension(450, 350)
                frame.minimumSize shouldBe WDimension(450, 350)
                frame.size shouldBe WDimension(500, 400) // Already larger, so it does not change
                frame.setSize(100, 100)
                frame.size shouldBe WDimension(450, 350)
                frame.setSize(480, 200)
                frame.size shouldBe WDimension(480, 350)
                frame.appWindow.resize(10, 10) // Stops at the OS minimum size, the same as dragging the border
                frame.appWindow.size.width shouldBeGreaterThanOrEqual px(450, scale)
                frame.appWindow.size.height shouldBeGreaterThanOrEqual px(350, scale)
                frame.minimumSize = null
                frame.setSize(300, 200)
                frame.size shouldBe WDimension(300, 200)
            }
        }

        test("a minimumSize larger than the current size enlarges the window. Negative values throw IllegalArgumentException and the value does not change") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.setSize(400, 300)
                frame.minimumSize = WDimension(500, 250)
                frame.size shouldBe WDimension(500, 300)
                shouldThrow<IllegalArgumentException> { frame.minimumSize = WDimension(-1, 10) }
                shouldThrow<IllegalArgumentException> { frame.minimumSize = WDimension(10, -1) }
                frame.minimumSize shouldBe WDimension(500, 250)
            }
        }

        test("pack before it is shown fits the client area to the content's preferred size (including padding)") {
            val frame = newFrame()
            onUiThread {
                frame.add(box(200.0, 100.0))
                frame.pack()
            }
            show(frame)
            val scale = scaleOf(frame)
            onUiThread {
                // contentPane is a StackPanel with padding 24, so the content 200 x 100 plus 24 on every side
                frame.appWindow.clientSize shouldBe WDimension(ceil(248 * scale).toInt(), ceil(148 * scale).toInt())
            }
            // After it is shown, layout lines up the content in the client area (ActualWidth / ActualHeight do not include the padding)
            eventually(5.seconds) {
                onUiThread {
                    frame.contentPane.actualWidth shouldBe (200.0 plusOrMinus 1.0)
                    frame.contentPane.actualHeight shouldBe (100.0 plusOrMinus 1.0)
                }
            }
        }

        test("pack after it is shown enlarges it by the added children. Content such as buttons gets the same size as pack before it is shown") {
            val frame = newFrame()
            val before = onUiThreadGet {
                frame.add(WButton("OK"))
                frame.add(WTextField("Text"))
                frame.pack()
                frame.appWindow.clientSize
            }
            show(frame)
            val scale = scaleOf(frame)
            onUiThread {
                frame.pack()
                frame.appWindow.clientSize.width shouldBeInRange before.width - 1..before.width + 1
                frame.appWindow.clientSize.height shouldBeInRange before.height - 1..before.height + 1
            }
            val other = newFrame("$TITLE 2")
            show(other)
            onUiThread {
                other.add(box(200.0, 100.0))
                other.pack()
                other.add(box(300.0, 50.0))
                other.pack()
                // The spacing between children is 12: height is 24 + 100 + 12 + 50 + 24, width is the wider 300 + 48
                other.appWindow.clientSize shouldBe WDimension(ceil(348 * scale).toInt(), ceil(210 * scale).toInt())
            }
        }

        test("pack does not make it smaller than minimumSize") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.add(box(50.0, 20.0))
                frame.minimumSize = WDimension(400, 300)
                frame.pack()
                frame.width shouldBeGreaterThanOrEqual 400
                frame.height shouldBeGreaterThanOrEqual 300
            }
        }

        test("setLocationRelativeTo(null) puts it at the center of the primary display's work area") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.setSize(400, 300)
                frame.setLocationRelativeTo(null)
                val area = WDisplayArea.primary().workArea
                val size = frame.appWindow.size
                frame.appWindow.position shouldBe WPoint(area.x + (area.width - size.width) / 2, area.y + (area.height - size.height) / 2)
            }
        }

        test("setLocationRelativeTo(component) aligns it with the component's center, shifting it by any amount that extends outside the work area") {
            val owner = newFrame()
            val button = onUiThreadGet { WButton("Anchor") }
            onUiThread {
                owner.add(button)
                owner.setSize(600, 400)
                owner.setLocationRelativeTo(null)
            }
            show(owner)
            val frame = newFrame("$TITLE 2")
            show(frame)
            val scale = scaleOf(owner)
            onUiThread {
                frame.setSize(300, 200)
                frame.setLocationRelativeTo(button)
                val origin = button.locationOnScreen
                val buttonCenterX = origin.x + button.actualWidth * scale / 2
                val buttonCenterY = origin.y + button.actualHeight * scale / 2
                val position = frame.appWindow.position
                val size = frame.appWindow.size
                (position.x + size.width / 2.0) shouldBe (buttonCenterX plusOrMinus 1.0)
                (position.y + size.height / 2.0) shouldBe (buttonCenterY plusOrMinus 1.0)

                // With the reference window moved to the top-left of the work area, the centered position extends past the top-left, so it stops at the work area's edge
                val area = WDisplayArea.primary().workArea
                owner.appWindow.move(area.x, area.y)
                frame.setSize(800, 600)
                frame.setLocationRelativeTo(button)
                frame.appWindow.position shouldBe WPoint(area.x, area.y)
            }
        }

        test("setLocationRelativeTo with a component not in a window centers it on the screen, the same as null") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.setSize(400, 300)
                frame.setLocationRelativeTo(WButton("Not loaded"))
                val area = WDisplayArea.primary().workArea
                val size = frame.appWindow.size
                frame.appWindow.position shouldBe WPoint(area.x + (area.width - size.width) / 2, area.y + (area.height - size.height) / 2)
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame geometry test"
    }
}
