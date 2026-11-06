package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldNotBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.io.File
import java.nio.file.Files
import java.util.Collections
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the state and decorations of the JFrame-like [WFrame] (extendedState, isResizable, isAlwaysOnTop, isUndecorated, toFront / toBack,
 * setIconImage, jMenuBar). The native state is checked by reading it directly with Win32 ([WindowNatives]).
 * Each test creates its own WFrame and disposes it at the end, and the shared "winui4k tests" window is not closed.
 */
@Tags("Integration")
class WFrameStateTest : FunSpec() {
    private val frames = mutableListOf<WFrame>()
    private val states: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private lateinit var workDir: File

    private fun newFrame(title: String = TITLE): WFrame = onUiThreadGet {
        WFrame(title).also {
            frames += it
            it.addWindowStateListener { e -> states += "${e.oldState}->${e.newState}" }
        }
    }

    /** Shows it and waits until the content is on the XamlRoot. */
    private suspend fun show(frame: WFrame) {
        onUiThread { frame.isVisible = true }
        eventually(5.seconds) { onUiThreadGet { frame.contentPane.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)?.release() != null } shouldBe true }
    }

    private fun scaleOf(component: WComponent): Double = onUiThreadGet {
        val root = component.uiElement.getPtr(XamlInterop.IUIElement_get_XamlRoot)
        try {
            root.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale)
        } finally {
            root.release()
        }
    }

    private fun pngFile(name: String): File = File(workDir, name).also { it.writeBytes(WindowNatives.solidPng(32, 0xFF2080E0.toInt())) }

    /** The kind of Window.SystemBackdrop of [frame] ("none" / "mica:<Kind>" / "acrylic" / "other"). */
    private fun backdropOf(frame: WFrame): String = onUiThreadGet {
        val window2 = frame.window.queryInterface(XamlInterop.IID_IWindow2)
        try {
            val native = window2.getPtrOrNull(XamlInterop.IWindow2_get_SystemBackdrop)
            if (native == null) {
                "none"
            } else {
                try {
                    val mica = native.queryInterfaceOrNull(XamlInterop.IID_IMicaBackdrop)
                    val acrylic = native.queryInterfaceOrNull(XamlInterop.IID_IDesktopAcrylicBackdrop)
                    try {
                        when {
                            mica != null -> "mica:" + mica.getInt(XamlInterop.IMicaBackdrop_get_Kind)
                            acrylic != null -> "acrylic"
                            else -> "other"
                        }
                    } finally {
                        mica?.release()
                        acrylic?.release()
                    }
                } finally {
                    native.release()
                }
            }
        } finally {
            window2.release()
        }
    }

    init {
        beforeSpec {
            UiTestHarness.sharedFrame() // So that closing its own window does not exit the app
            workDir = Files.createTempDirectory("winui4k-frame-state").toFile()
        }
        afterSpec { workDir.deleteRecursively() }
        beforeTest { states.clear() }
        afterTest {
            val toClose = frames.toList()
            frames.clear()
            onUiThread { toClose.forEach { it.dispose() } }
        }

        test("extendedState maximizes, minimizes and restores to normal, and windowStateChanged comes too. HORIZ / VERT are BOTH, and values including ICONIFIED minimize") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.extendedState shouldBe WFrame.NORMAL
                frame.extendedState = WFrame.MAXIMIZED_BOTH
                frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
                frame.extendedState = WFrame.NORMAL
                frame.extendedState shouldBe WFrame.NORMAL
                frame.extendedState = WFrame.MAXIMIZED_HORIZ
                frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
                frame.extendedState = WFrame.NORMAL
                frame.extendedState = WFrame.MAXIMIZED_VERT
                frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
                frame.extendedState = WFrame.ICONIFIED or WFrame.MAXIMIZED_BOTH
                frame.extendedState shouldBe WFrame.ICONIFIED
                frame.extendedState = WFrame.NORMAL
                frame.extendedState shouldBe WFrame.NORMAL
            }
            eventually(5.seconds) { states.toList() shouldBe listOf("0->6", "6->0", "0->6", "6->0", "0->6", "6->1", "1->0") }
        }

        test("extendedState set before it is shown is remembered without showing it, and maximizes it when shown") {
            val frame = newFrame()
            onUiThread {
                frame.extendedState = WFrame.MAXIMIZED_BOTH
                frame.isVisible.shouldBeFalse()
                frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
            }
            show(frame)
            onUiThread {
                frame.isVisible.shouldBeTrue()
                frame.appWindow.position // Readable
                frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
            }
        }

        test("setting ICONIFIED before it is shown minimizes it when shown") {
            val frame = newFrame()
            onUiThread {
                frame.extendedState = WFrame.ICONIFIED
                frame.isVisible.shouldBeFalse()
                frame.isVisible = true
            }
            eventually(5.seconds) { onUiThreadGet { frame.extendedState } shouldBe WFrame.ICONIFIED }
            onUiThread { frame.isVisible.shouldBeTrue() }
        }

        test("isResizable = false removes border resizing and the maximize button, and true restores them") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.isResizable.shouldBeTrue()
                (WindowNatives.style(frame) and WindowNatives.WS_THICKFRAME) shouldNotBe 0L
                frame.isResizable = false
                frame.isResizable.shouldBeFalse()
                (WindowNatives.style(frame) and WindowNatives.WS_THICKFRAME) shouldBe 0L
                (WindowNatives.style(frame) and WindowNatives.WS_MAXIMIZEBOX) shouldBe 0L
                frame.setSize(420, 320) // Programmatic size changes work
                frame.size shouldBe WDimension(420, 320)
                frame.isResizable = true
                (WindowNatives.style(frame) and WindowNatives.WS_THICKFRAME) shouldNotBe 0L
                (WindowNatives.style(frame) and WindowNatives.WS_MAXIMIZEBOX) shouldNotBe 0L
            }
        }

        test("isAlwaysOnTop makes it topmost (WS_EX_TOPMOST). The default is false") {
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.isAlwaysOnTop.shouldBeFalse()
                frame.isAlwaysOnTop = true
                frame.isAlwaysOnTop.shouldBeTrue()
                (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldNotBe 0L
                frame.isAlwaysOnTop = false
                (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBe 0L
            }
        }

        test("even after opening and closing a WComboBox drop-down in the shared window, isAlwaysOnTop = true makes it topmost (WS_EX_TOPMOST)") {
            // After a drop-down (a windowed Popup) had been opened, the first OverlappedPresenter.IsAlwaysOnTop = true
            // read back as true but WS_EX_TOPMOST was not set (doc/troubleshooting.md)
            val combo = onUiThreadGet { WComboBox().also { c -> listOf("Small", "Large").forEach { c.addItem(it) } } }
            UiTestHarness.attachAndAwaitLoaded(combo)
            try {
                // Wait a little after each so that the opening and closing animations progress
                onUiThread { combo.isDropDownOpen = true }
                Thread.sleep(POPUP_SETTLE_MILLIS)
                onUiThread { combo.isDropDownOpen = false }
                Thread.sleep(POPUP_SETTLE_MILLIS)
            } finally {
                UiTestHarness.detach(combo)
            }
            val frame = newFrame()
            show(frame)
            onUiThread {
                frame.isAlwaysOnTop = true
                (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBe WindowNatives.WS_EX_TOPMOST
                frame.isAlwaysOnTop = false
                (WindowNatives.exStyle(frame) and WindowNatives.WS_EX_TOPMOST) shouldBe 0L
            }
        }

        test("isUndecorated = true removes the title bar and false restores it. It can be changed before or after it is shown") {
            val frame = newFrame()
            onUiThread {
                frame.isUndecorated.shouldBeFalse()
                frame.isUndecorated = true
                frame.isUndecorated.shouldBeTrue()
            }
            show(frame)
            onUiThread {
                // The title bar appears only when both WS_BORDER and WS_DLGFRAME (WS_CAPTION) are present. SetBorderAndTitleBar(false, false)
                // may leave WS_DLGFRAME, so check that the two are not both present
                (WindowNatives.style(frame) and WindowNatives.WS_CAPTION) shouldNotBe WindowNatives.WS_CAPTION
                frame.isUndecorated = false
                frame.isUndecorated.shouldBeFalse()
                (WindowNatives.style(frame) and WindowNatives.WS_CAPTION) shouldBe WindowNatives.WS_CAPTION
            }
        }

        test("toFront brings it in front of other windows and activates it, and toBack sends it behind. Nothing is done to a hidden window") {
            val first = newFrame()
            show(first)
            val second = newFrame("$TITLE 2")
            show(second)
            eventually(5.seconds) { onUiThreadGet { WindowNatives.isAbove(second, first) }.shouldBeTrue() }
            onUiThread { first.toFront() }
            eventually(5.seconds) {
                onUiThreadGet { WindowNatives.isAbove(first, second) }.shouldBeTrue()
                onUiThreadGet { first.isActive }.shouldBeTrue()
            }
            onUiThread {
                first.toBack()
                WindowNatives.isAbove(second, first).shouldBeTrue()
                second.isVisible = false
                second.toFront()
                second.isVisible.shouldBeFalse()
            }
        }

        test("setIconImage makes both .png and .ico the title bar and taskbar icon") {
            val frame = newFrame()
            show(frame)
            val png = pngFile("icon.png")
            val ico = File(workDir, "icon.ico").also { it.writeBytes(IcoFiles.icoFromPng(WindowNatives.solidPng(48, 0xFFE04020.toInt()))) }
            onUiThread {
                frame.iconImage.shouldBeNull()
                val before = WindowNatives.icons(frame)
                frame.setIconImage(png.path)
                frame.iconImage shouldBe png.path
                val withPng = WindowNatives.icons(frame)
                withPng.forEach { it shouldNotBeExactly 0L }
                withPng shouldNotBe before
                frame.setIconImage(ico.path)
                frame.iconImage shouldBe ico.path
                WindowNatives.icons(frame).forEach { it shouldNotBeExactly 0L }
                WindowNatives.icons(frame) shouldNotBe withPng
                frame.setIconImage(WImageIcon(png.path))
                frame.iconImage shouldBe png.path
            }
        }

        test("setIconImage throws IllegalArgumentException for a missing file, anything other than .ico and .png, non-PNG content and non-file URIs") {
            val frame = newFrame()
            val broken = File(workDir, "broken.png").also { it.writeText("Not a PNG") }
            val bmp = File(workDir, "icon.bmp").also { it.writeBytes(ByteArray(16)) }
            onUiThread {
                shouldThrow<IllegalArgumentException> { frame.setIconImage(File(workDir, "none.png").path) }
                shouldThrow<IllegalArgumentException> { frame.setIconImage(bmp.path) }
                shouldThrow<IllegalArgumentException> { frame.setIconImage(broken.path) }
                shouldThrow<IllegalArgumentException> { frame.setIconImage(WImageIcon("https://example.com/icon.png")) }
                frame.iconImage.shouldBeNull()
            }
        }

        test("setIconImage(WImageIcon) can use an image whose path contains spaces, and a file URI with unescaped spaces throws IllegalArgumentException") {
            val frame = newFrame()
            show(frame)
            val dir = File(workDir, "with space").also { it.mkdirs() }
            val png = File(dir, "icon image.png").also { it.writeBytes(WindowNatives.solidPng(32, 0xFF20C040.toInt())) }
            onUiThread {
                frame.setIconImage(WImageIcon(png.path)) // uri becomes a file URI escaped with %20
                frame.iconImage shouldBe png.path
                WindowNatives.icons(frame).forEach { it shouldNotBeExactly 0L }
                val raw = "file:///" + png.absolutePath.replace(File.separatorChar, '/') // Spaces as is (not a valid java.net.URI)
                shouldThrow<IllegalArgumentException> { frame.setIconImage(WImageIcon(raw)) }
                frame.iconImage shouldBe png.path // A failed setting does not change the previous value
            }
        }

        test("jMenuBar is placed below the title bar and above the content, and null removes it. Content set later with setContentPane also goes below the menu") {
            val frame = newFrame()
            val button = onUiThreadGet { WButton("Content") }
            val menuBar = onUiThreadGet {
                frame.add(button)
                WMenuBar().also {
                    it.add(WMenuBarItem("File"))
                    frame.jMenuBar = it
                }
            }
            show(frame)
            val scale = scaleOf(menuBar)
            eventually(5.seconds) {
                onUiThread {
                    frame.jMenuBar shouldBeSameInstanceAs menuBar
                    menuBar.actualHeight shouldBeGreaterThan 0.0
                    val menuBottom = menuBar.locationOnScreen.y + menuBar.actualHeight * scale
                    button.locationOnScreen.y.toDouble() shouldBeGreaterThan menuBottom - 1
                }
            }
            val replaced = onUiThreadGet { WButton("Replaced content") }
            onUiThread { frame.setContentPane(replaced) }
            eventually(5.seconds) {
                onUiThread {
                    replaced.actualHeight shouldBeGreaterThan 0.0
                    replaced.locationOnScreen.y.toDouble() shouldBeGreaterThan menuBar.locationOnScreen.y + menuBar.actualHeight * scale - 1
                }
            }
            onUiThread {
                frame.jMenuBar = null
                frame.jMenuBar.shouldBeNull()
                menuBar.parent.shouldBeNull()
            }
        }

        test("pack with a jMenuBar also includes the menu's height") {
            val frame = newFrame()
            val menuBar = onUiThreadGet {
                frame.add(
                    WPanel().also {
                        it.width = 200.0
                        it.height = 100.0
                    },
                )
                WMenuBar().also {
                    it.add(WMenuBarItem("File"))
                    frame.jMenuBar = it
                }
            }
            show(frame)
            val scale = scaleOf(menuBar)
            eventually(5.seconds) { onUiThreadGet { menuBar.actualHeight } shouldBeGreaterThan 0.0 }
            onUiThread {
                frame.pack()
                val expected = ceil((menuBar.actualHeight + 148) * scale).toInt()
                frame.appWindow.clientSize.height shouldBeGreaterThanOrEqual expected - 1
                frame.appWindow.clientSize.height.toDouble() shouldBe (expected.toDouble() plusOrMinus 1.0)
            }
        }

        context("Reading and writing") {
            test("systemBackdrop defaults to MICA (MicaBackdrop with Kind Base). MICA_ALT has Kind BaseAlt, ACRYLIC is DesktopAcrylicBackdrop, NONE removes it, and MICA restores it") {
                val frame = newFrame()
                val seen = mutableListOf(onUiThreadGet { frame.systemBackdrop.name } to backdropOf(frame))
                for (type in listOf(SystemBackdropType.MICA_ALT, SystemBackdropType.ACRYLIC, SystemBackdropType.NONE, SystemBackdropType.MICA)) {
                    onUiThread { frame.systemBackdrop = type }
                    seen += onUiThreadGet { frame.systemBackdrop.name } to backdropOf(frame)
                }
                seen shouldBe listOf("MICA" to "mica:0", "MICA_ALT" to "mica:1", "ACRYLIC" to "acrylic", "NONE" to "none", "MICA" to "mica:0")
            }
        }

        context("Swing compatibility") {
            test("state is the same as Frame.getState / setState: ICONIFIED minimizes, and NORMAL only undoes the minimization. Maximized reads as NORMAL and setting NORMAL does not restore it") {
                val frame = newFrame()
                show(frame)
                onUiThread {
                    frame.state shouldBe WFrame.NORMAL
                    frame.extendedState = WFrame.MAXIMIZED_BOTH
                    frame.state shouldBe WFrame.NORMAL
                    frame.state = WFrame.NORMAL
                    frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
                    frame.state = WFrame.ICONIFIED
                    frame.state shouldBe WFrame.ICONIFIED
                    frame.extendedState shouldBe WFrame.ICONIFIED
                    frame.state = WFrame.NORMAL // Swing removes only the ICONIFIED bit from extendedState, so it goes back to maximized
                    frame.extendedState shouldBe WFrame.MAXIMIZED_BOTH
                    frame.extendedState = WFrame.NORMAL
                    frame.state = WFrame.MAXIMIZED_BOTH // Values other than NORMAL and ICONIFIED do nothing (as in Swing)
                    frame.extendedState shouldBe WFrame.NORMAL
                }
            }

            test("state = ICONIFIED before it is shown minimizes it when shown (the same as extendedState)") {
                val frame = newFrame()
                onUiThread {
                    frame.state = WFrame.ICONIFIED
                    frame.state shouldBe WFrame.ICONIFIED
                    frame.isVisible.shouldBeFalse()
                    frame.isVisible = true
                }
                eventually(5.seconds) { onUiThreadGet { frame.extendedState } shouldBe WFrame.ICONIFIED }
            }

            test("isAlwaysOnTopSupported is true") {
                val frame = newFrame()
                onUiThreadGet { frame.isAlwaysOnTopSupported }.shouldBeTrue()
            }

            test("isAutoRequestFocus defaults to true. A window with false does not become active when shown, and toFront only brings it to the front") {
                val first = newFrame()
                show(first)
                eventually(5.seconds) { onUiThreadGet { first.isActive }.shouldBeTrue() }
                val second = newFrame("$TITLE 2")
                onUiThread {
                    second.isAutoRequestFocus.shouldBeTrue()
                    second.isAutoRequestFocus = false
                }
                show(second)
                continually(1.seconds) {
                    onUiThreadGet { second.isVisible }.shouldBeTrue()
                    onUiThreadGet { second.isActive }.shouldBeFalse()
                    onUiThreadGet { first.isActive }.shouldBeTrue()
                }
                onUiThread {
                    first.toFront()
                    second.toFront()
                }
                eventually(5.seconds) { onUiThreadGet { WindowNatives.isAbove(second, first) }.shouldBeTrue() }
                continually(1.seconds) { onUiThreadGet { second.isActive }.shouldBeFalse() }
            }
        }
    }

    private companion object {
        const val TITLE = "winui4k frame state test"
        const val POPUP_SETTLE_MILLIS = 500L
    }
}
