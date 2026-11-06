package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for reading back [WAppWindow]'s title, resize / move, clientSize and hide / show, the state transitions and
 * properties of [WOverlappedPresenter], the colors, height and theme of [WAppWindowTitleBar], and [WDisplayArea.nearest].
 * The native state is also checked with Win32 ([WindowNatives]). Each test creates its own WFrame and disposes it at the end,
 * and the shared "winui4k tests" window is not closed. The shape seen from Java is tested in WAppWindowForJavaTest.
 */
@Tags("Integration")
class WAppWindowTest : FunSpec() {
    private val frames = mutableListOf<WFrame>()

    private fun newFrame(): WFrame = onUiThreadGet { WFrame(TITLE).also { frames += it } }

    /** Shows it and waits until the content is on the XamlRoot. */
    private suspend fun show(frame: WFrame) {
        onUiThread { frame.isVisible = true }
        eventually(5.seconds) { onUiThreadGet { frame.contentPane.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)?.release() != null } shouldBe true }
    }

    /** Temporarily wraps the current OverlappedPresenter of [frame] and passes it to [block] (call on the UI thread). */
    private fun <T> withPresenter(frame: WFrame, block: (WOverlappedPresenter) -> T): T {
        val presenter = frame.appWindow.appWindow.getPtr(WindowingInterop.IAppWindow_get_Presenter)
        val overlapped = try {
            presenter.queryInterface(WindowingInterop.IID_IOverlappedPresenter)
        } finally {
            presenter.release()
        }
        val wrapper = WOverlappedPresenter(overlapped)
        return try {
            block(wrapper)
        } finally {
            wrapper.release()
        }
    }

    init {
        beforeSpec { UiTestHarness.sharedFrame() } // So that closing its own window does not exit the app
        afterTest {
            val toClose = frames.toList()
            frames.clear()
            onUiThread { toClose.forEach { it.dispose() } }
        }

        test("title reads back the written string as is, and handles an empty string too") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.appWindow.title shouldBe TITLE
                frame.appWindow.title = "Another title"
                frame.appWindow.title shouldBe "Another title"
                frame.appWindow.title = ""
                frame.appWindow.title shouldBe ""
            }
        }

        test("resize and assigning size read back the pixel values if they are larger than the OS minimum") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.appWindow.resize(700, 520)
                frame.appWindow.size shouldBe WDimension(700, 520)
                frame.appWindow.size = WDimension(810, 600)
                frame.appWindow.size shouldBe WDimension(810, 600)
            }
        }

        test("move and assigning position read back the same coordinates if inside the work area") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                val area = WDisplayArea.primary().workArea
                frame.appWindow.resize(500, 400)
                frame.appWindow.move(area.x + 60, area.y + 40)
                frame.appWindow.position shouldBe WPoint(area.x + 60, area.y + 40)
                frame.appWindow.position = WPoint(area.x + 90, area.y + 70)
                frame.appWindow.position shouldBe WPoint(area.x + 90, area.y + 70)
            }
        }

        test("clientSize is smaller than size by the title bar and border, and reads back the value given to resizeClient") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.appWindow.resize(700, 520)
                val client = frame.appWindow.clientSize
                client.height shouldBeLessThan 520 // The title bar height is always subtracted
                client.width shouldBeGreaterThan 0
                frame.appWindow.resizeClient(640, 400)
                frame.appWindow.clientSize shouldBe WDimension(640, 400)
            }
        }

        test("hide makes isVisible false, and show(false) and show() make it true again") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.appWindow.isVisible.shouldBeTrue()
                frame.appWindow.hide()
                frame.appWindow.isVisible.shouldBeFalse()
                frame.appWindow.show(false)
                frame.appWindow.isVisible.shouldBeTrue()
                frame.appWindow.hide()
                frame.appWindow.isVisible.shouldBeFalse()
                frame.appWindow.show()
                frame.appWindow.isVisible.shouldBeTrue()
            }
        }

        test("WOverlappedPresenter's state becomes MAXIMIZED / MINIMIZED / RESTORED with maximize, minimize and restore") {
            val frame = newFrame()
            show(frame)
            onUiThread { withPresenter(frame) { it.state shouldBe OverlappedPresenterState.RESTORED } }
            onUiThread { withPresenter(frame) { it.maximize() } }
            eventually(5.seconds) { onUiThreadGet { withPresenter(frame) { it.state } } shouldBe OverlappedPresenterState.MAXIMIZED }
            onUiThread { withPresenter(frame) { it.restore() } }
            eventually(5.seconds) { onUiThreadGet { withPresenter(frame) { it.state } } shouldBe OverlappedPresenterState.RESTORED }
            onUiThread { withPresenter(frame) { it.minimize() } }
            eventually(5.seconds) { onUiThreadGet { withPresenter(frame) { it.state } } shouldBe OverlappedPresenterState.MINIMIZED }
            onUiThread { withPresenter(frame) { it.restore() } }
            eventually(5.seconds) { onUiThreadGet { withPresenter(frame) { it.state } } shouldBe OverlappedPresenterState.RESTORED }
        }

        test("when maximized, size is not smaller than the work area, and restoring returns to the size before maximizing") {
            val frame = newFrame()
            show(frame)
            val before = onUiThreadGet {
                frame.appWindow.resize(700, 520)
                frame.appWindow.size
            }
            onUiThread { withPresenter(frame) { it.maximize() } }
            eventually(5.seconds) {
                onUiThreadGet { frame.appWindow.size.width } shouldBeGreaterThan before.width
            }
            onUiThread { withPresenter(frame) { it.restore() } }
            eventually(5.seconds) { onUiThreadGet { frame.appWindow.size } shouldBe before }
        }

        test("isMaximizable = false removes the maximize button (WS_MAXIMIZEBOX), and true restores it") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                withPresenter(frame) {
                    it.isMaximizable.shouldBeTrue()
                    it.isMaximizable = false
                    it.isMaximizable.shouldBeFalse()
                }
            }
            (WindowNatives.style(frame) and WindowNatives.WS_MAXIMIZEBOX) shouldBeExactly 0L
            onUiThread { withPresenter(frame) { it.isMaximizable = true } }
            (WindowNatives.style(frame) and WindowNatives.WS_MAXIMIZEBOX) shouldBeExactly WindowNatives.WS_MAXIMIZEBOX
        }

        test("isMinimizable and isResizable read back the written values") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                withPresenter(frame) {
                    it.isMinimizable.shouldBeTrue()
                    it.isMinimizable = false
                    it.isMinimizable.shouldBeFalse()
                    it.isMinimizable = true
                    it.isMinimizable.shouldBeTrue()
                    it.isResizable.shouldBeTrue()
                    it.isResizable = false
                    it.isResizable.shouldBeFalse()
                    it.isResizable = true
                    it.isResizable.shouldBeTrue()
                }
            }
        }

        test("isAlwaysOnTop is tied to topmost (WS_EX_TOPMOST)") {
            val frame = newFrame()
            show(frame)
            (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBeExactly 0L
            onUiThread { withPresenter(frame) { it.isAlwaysOnTop = true } }
            onUiThread { withPresenter(frame) { it.isAlwaysOnTop.shouldBeTrue() } }
            (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBeExactly WindowNatives.WS_EX_TOPMOST
            onUiThread { withPresenter(frame) { it.isAlwaysOnTop = false } }
            (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBeExactly 0L
        }

        test("setBorderAndTitleBar(false, false) removes the border and title bar (WS_CAPTION), and (true, true) restores them") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                withPresenter(frame) {
                    it.hasBorder.shouldBeTrue()
                    it.hasTitleBar.shouldBeTrue()
                    it.setBorderAndTitleBar(false, false)
                    it.hasBorder.shouldBeFalse()
                    it.hasTitleBar.shouldBeFalse()
                }
            }
            // The title bar appears only when both WS_BORDER and WS_DLGFRAME (WS_CAPTION) are present. One may remain, so check that the two are not both present
            (WindowNatives.style(frame) and WindowNatives.WS_CAPTION) shouldNotBe WindowNatives.WS_CAPTION
            onUiThread {
                withPresenter(frame) {
                    it.setBorderAndTitleBar(true, true)
                    it.hasBorder.shouldBeTrue()
                    it.hasTitleBar.shouldBeTrue()
                }
            }
            (WindowNatives.style(frame) and WindowNatives.WS_CAPTION) shouldBeExactly WindowNatives.WS_CAPTION
        }

        test("preferredMinimum / Maximum width and height read back the written values, and null resets them to unset") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                withPresenter(frame) {
                    it.preferredMinimumWidth = 480
                    it.preferredMinimumHeight = 360
                    it.preferredMaximumWidth = 1600
                    it.preferredMaximumHeight = 1200
                    it.preferredMinimumWidth shouldBe 480
                    it.preferredMinimumHeight shouldBe 360
                    it.preferredMaximumWidth shouldBe 1600
                    it.preferredMaximumHeight shouldBe 1200
                    it.preferredMinimumWidth = null
                    it.preferredMinimumHeight = null
                    it.preferredMaximumWidth = null
                    it.preferredMaximumHeight = null
                    it.preferredMinimumWidth.shouldBeNull()
                    it.preferredMinimumHeight.shouldBeNull()
                    it.preferredMaximumWidth.shouldBeNull()
                    it.preferredMaximumHeight.shouldBeNull()
                }
            }
        }

        test("WAppWindowTitleBar colors read back the written WColor (including alpha), and null resets them to unset") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                val titleBar = frame.appWindow.titleBar
                val color = WColor(200, 30, 60, 255)
                val translucent = WColor(10, 120, 240, 128)
                val setters: List<Pair<(WColor?) -> Unit, () -> WColor?>> = listOf(
                    ({ c: WColor? -> titleBar.backgroundColor = c }) to { titleBar.backgroundColor },
                    ({ c: WColor? -> titleBar.foregroundColor = c }) to { titleBar.foregroundColor },
                    ({ c: WColor? -> titleBar.inactiveBackgroundColor = c }) to { titleBar.inactiveBackgroundColor },
                    ({ c: WColor? -> titleBar.inactiveForegroundColor = c }) to { titleBar.inactiveForegroundColor },
                    ({ c: WColor? -> titleBar.buttonBackgroundColor = c }) to { titleBar.buttonBackgroundColor },
                    ({ c: WColor? -> titleBar.buttonForegroundColor = c }) to { titleBar.buttonForegroundColor },
                    ({ c: WColor? -> titleBar.buttonHoverBackgroundColor = c }) to { titleBar.buttonHoverBackgroundColor },
                    ({ c: WColor? -> titleBar.buttonHoverForegroundColor = c }) to { titleBar.buttonHoverForegroundColor },
                    ({ c: WColor? -> titleBar.buttonPressedBackgroundColor = c }) to { titleBar.buttonPressedBackgroundColor },
                    ({ c: WColor? -> titleBar.buttonPressedForegroundColor = c }) to { titleBar.buttonPressedForegroundColor },
                    ({ c: WColor? -> titleBar.buttonInactiveBackgroundColor = c }) to { titleBar.buttonInactiveBackgroundColor },
                    ({ c: WColor? -> titleBar.buttonInactiveForegroundColor = c }) to { titleBar.buttonInactiveForegroundColor },
                )
                setters.forEach { (set, get) ->
                    get().shouldBeNull()
                    set(color)
                    get().toArgbList() shouldBe color.toArgbList()
                    set(translucent)
                    get().toArgbList() shouldBe translucent.toArgbList()
                    set(null)
                    get().shouldBeNull()
                }
            }
        }

        test("resetToDefault resets the set colors to unset") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                val titleBar = frame.appWindow.titleBar
                titleBar.backgroundColor = WColor(1, 2, 3)
                titleBar.buttonHoverForegroundColor = WColor(4, 5, 6)
                titleBar.resetToDefault()
                titleBar.backgroundColor.shouldBeNull()
                titleBar.buttonHoverForegroundColor.shouldBeNull()
            }
        }

        test("preferredHeightOption and preferredTheme read back the written values, and height is taller with TALL than with STANDARD") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                val titleBar = frame.appWindow.titleBar
                titleBar.extendsContentIntoTitleBar = true // TALL / COLLAPSED can be chosen only while the content extends into the title bar (ERROR_INVALID_STATE)
                titleBar.preferredHeightOption = TitleBarHeightOption.STANDARD
                titleBar.preferredHeightOption shouldBe TitleBarHeightOption.STANDARD
                val standard = titleBar.height
                standard shouldBeGreaterThan 0
                titleBar.preferredHeightOption = TitleBarHeightOption.TALL
                titleBar.preferredHeightOption shouldBe TitleBarHeightOption.TALL
                titleBar.height shouldBeGreaterThan standard
                titleBar.preferredHeightOption = TitleBarHeightOption.STANDARD
                for (theme in listOf(TitleBarTheme.LIGHT, TitleBarTheme.DARK, TitleBarTheme.USE_DEFAULT_APP_MODE)) {
                    titleBar.preferredTheme = theme
                    titleBar.preferredTheme shouldBe theme
                }
            }
        }

        test("changes of WFrame.extendsContentIntoTitleBar are reflected in AppWindowTitleBar.extendsContentIntoTitleBar") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                withClue("Initial value") { frame.appWindow.titleBar.extendsContentIntoTitleBar.shouldBeFalse() }
                frame.extendsContentIntoTitleBar = true
                withClue("true from WFrame") { frame.appWindow.titleBar.extendsContentIntoTitleBar.shouldBeTrue() }
                frame.extendsContentIntoTitleBar = false
                withClue("false from WFrame") { frame.appWindow.titleBar.extendsContentIntoTitleBar.shouldBeFalse() }
            }
        }

        test("WDisplayArea.nearest returns the display containing that window's position") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                val area = WDisplayArea.primary().workArea
                frame.appWindow.resize(500, 400)
                frame.appWindow.move(area.x + 50, area.y + 50)
                val position = frame.appWindow.position
                val nearest = WDisplayArea.nearest(frame.appWindow).bounds
                (position.x >= nearest.x && position.x < nearest.x + nearest.width).shouldBeTrue()
                (position.y >= nearest.y && position.y < nearest.y + nearest.height).shouldBeTrue()
                nearest shouldBe WDisplayArea.all().first { a ->
                    val b = a.bounds
                    position.x >= b.x && position.x < b.x + b.width && position.y >= b.y && position.y < b.y + b.height
                }.bounds
            }
        }
    }

    private fun WColor?.toArgbList() = this?.let { listOf(it.alpha, it.red, it.green, it.blue) }

    private companion object {
        const val TITLE = "winui4k app window test"
    }
}
