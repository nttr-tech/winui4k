package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.win32.Win32
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for the MouseListener / MouseMotionListener / MouseWheelListener-style listeners ([WComponent.addMouseListener]
 * and so on). WinUI pointer input cannot be produced with window messages, so [TestMouse] builds stand-ins for the values
 * read from native events and passes them in. The expected values are derived from the contract of Swing's (Windows AWT's)
 * MouseEvent and Windows' system settings (double-click time and area, drag area, wheel lines).
 */
class WMouseEventTest : FunSpec() {
    /** Two 200 x 100 panels (an outer one and an inner one inside it) and a record of what was received. */
    private class Board {
        val outer = WPanel().also {
            it.name = "outer"
            it.width = 300.0
            it.height = 200.0
        }
        val inner = WPanel().also {
            it.name = "inner"
            it.width = 200.0
            it.height = 100.0
            outer.add(it)
        }
        val events = mutableListOf<String>()
        val received = mutableListOf<WMouseEvent>()

        /** Adds to [component] listeners of every kind that record received events as "name:kind". */
        fun recordAll(component: WComponent) {
            val recorder = Recorder(component.name)
            component.addMouseListener(recorder)
            component.addMouseMotionListener(recorder)
            component.addMouseWheelListener(recorder)
        }

        inner class Recorder(private val name: String) : WMouseAdapter() {
            private fun record(kind: String, e: WMouseEvent) {
                events += "$name:$kind"
                received += e
            }

            override fun mouseClicked(e: WMouseEvent) = record("clicked", e)

            override fun mousePressed(e: WMouseEvent) = record("pressed", e)

            override fun mouseReleased(e: WMouseEvent) = record("released", e)

            override fun mouseEntered(e: WMouseEvent) = record("entered", e)

            override fun mouseExited(e: WMouseEvent) = record("exited", e)

            override fun mouseDragged(e: WMouseEvent) = record("dragged", e)

            override fun mouseMoved(e: WMouseEvent) = record("moved", e)

            override fun mouseWheelMoved(e: WMouseWheelEvent) = record("wheel", e)
        }
    }

    /** Places the panel in the window, completes layout, runs [block] on the UI thread, and removes it. */
    private fun withBoard(block: (Board) -> Unit) {
        val board = onUiThreadGet { Board() }
        UiTestHarness.attachAndAwaitLoaded(board.outer)
        try {
            onUiThread {
                board.outer.updateLayout()
                block(board)
            }
        } finally {
            UiTestHarness.detach(board.outer)
        }
    }

    init {
        test("pressing and releasing inside calls pressed, released and clicked in order, with the position as floored DIP integers, the button BUTTON1 and the count 1") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.click(10.7, 20.2, WMouseEvent.BUTTON1, board.inner)
                board.events shouldContainExactly listOf("inner:pressed", "inner:released", "inner:clicked")
                val (pressed, released, clicked) = board.received
                pressed.source shouldBeSameInstanceAs board.inner
                pressed.component shouldBeSameInstanceAs board.inner
                listOf(pressed.id, released.id, clicked.id) shouldBe
                    listOf(WMouseEvent.MOUSE_PRESSED, WMouseEvent.MOUSE_RELEASED, WMouseEvent.MOUSE_CLICKED)
                listOf(pressed.x, pressed.y, pressed.point) shouldBe listOf(10, 20, WPoint(10, 20))
                listOf(pressed.preciseX, pressed.preciseY) shouldBe listOf(10.7, 20.2)
                board.received.map { it.button } shouldBe List(3) { WMouseEvent.BUTTON1 }
                board.received.map { it.clickCount } shouldBe listOf(1, 1, 1)
                board.received.map { it.isPopupTrigger } shouldBe listOf(false, false, false)
                // While pressed, the left button is held; after release, it is not (same as AWT's getModifiersEx)
                (pressed.modifiersEx and WInputEvent.BUTTON1_DOWN_MASK != 0).shouldBeTrue()
                released.modifiersEx shouldBe 0
                // The old-format getModifiers contains the BUTTON1_MASK of the button whose state changed
                listOf(pressed.modifiers, released.modifiers, clicked.modifiers) shouldBe List(3) { WInputEvent.BUTTON1_MASK }
            }
        }

        test("the pointer is captured on press and released when the last button is released, and is not captured for a press a control has handled") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.click(5.0, 5.0, WMouseEvent.BUTTON1, board.inner)
                mouse.captures shouldContainExactly listOf("capture:inner", "release:inner")
                mouse.captures.clear()
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner), handled = true)
                mouse.release(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner), handled = true)
                mouse.captures.shouldBeEmpty()
                board.events shouldContainExactly listOf(
                    "inner:pressed",
                    "inner:released",
                    "inner:clicked",
                    "inner:pressed",
                    "inner:released",
                    "inner:clicked",
                )
            }
        }

        test("consecutive presses within Windows' double-click time and area increase clickCount to 2 and 3, and it returns to 1 when outside them") {
            withBoard { board ->
                board.recordAll(board.inner)
                val doubleClickMillis = Win32.doubleClickTimeMillis().toLong()
                val halfWidth = Win32.systemMetric(Win32.SM_CXDOUBLECLK) / 2.0
                val mouse = TestMouse()
                val counts = mutableListOf<Int>()
                fun clickAt(x: Double, button: Int = WMouseEvent.BUTTON1) {
                    board.received.clear()
                    mouse.click(x, 50.0, button, board.inner)
                    counts += board.received.single { it.id == WMouseEvent.MOUSE_CLICKED }.clickCount
                }
                clickAt(50.0)
                mouse.advance(doubleClickMillis)
                clickAt(50.0) // exactly at the time limit
                mouse.advance(1)
                clickAt(50.0 + halfWidth) // at the edge of the area
                mouse.advance(doubleClickMillis + 1)
                clickAt(50.0) // timed out
                mouse.advance(1)
                clickAt(50.0 + halfWidth + 1) // outside the area
                mouse.advance(1)
                clickAt(50.0 + halfWidth + 1, WMouseEvent.BUTTON3) // a different button
                counts shouldContainExactly listOf(1, 2, 3, 1, 1, 1)
            }
        }

        test("moving while holding the pressed button delivers mouseDragged, and moving beyond the drag area means releasing is not a click") {
            withBoard { board ->
                board.recordAll(board.inner)
                val dragWidth = Win32.systemMetric(Win32.SM_CXDRAG).toDouble()
                val mouse = TestMouse()
                mouse.press(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.move(10.0 + dragWidth, 10.0, listOf(board.inner)) // inside the area
                mouse.release(10.0 + dragWidth, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.press(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.move(10.0 + dragWidth + 1, 10.0, listOf(board.inner)) // outside the area
                mouse.move(10.0, 10.0, listOf(board.inner)) // moving back still does not make a click
                mouse.release(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                board.events shouldContainExactly listOf(
                    "inner:pressed",
                    "inner:dragged",
                    "inner:released",
                    "inner:clicked",
                    "inner:pressed",
                    "inner:dragged",
                    "inner:dragged",
                    "inner:released",
                )
                val dragged = board.received.first { it.id == WMouseEvent.MOUSE_DRAGGED }
                listOf(dragged.button, dragged.clickCount) shouldBe listOf(WMouseEvent.NOBUTTON, 0)
                (dragged.modifiersEx and WInputEvent.BUTTON1_DOWN_MASK != 0).shouldBeTrue()
                dragged.modifiers shouldBe WInputEvent.BUTTON1_MASK
            }
        }

        test("the drag area is compared in physical pixels (2/3 when converted to DIPs at 150% display scale)") {
            withBoard { board ->
                board.recordAll(board.inner)
                val dragWidth = Win32.systemMetric(Win32.SM_CXDRAG).toDouble()
                val mouse = TestMouse(pixelsPerDip = 1.5)
                mouse.press(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.move(10.0 + dragWidth / 1.5 + 0.5, 10.0, listOf(board.inner))
                mouse.release(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                board.events shouldContainExactly listOf("inner:pressed", "inner:dragged", "inner:released")
            }
        }

        test("releasing outside the component delivers mouseReleased but not mouseClicked") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.press(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.release(10.0, 100.0, WMouseEvent.BUTTON1, listOf(board.inner)) // the bottom edge of a height of 100 is outside
                mouse.press(10.0, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.release(-0.5, 10.0, WMouseEvent.BUTTON1, listOf(board.inner))
                board.events shouldContainExactly listOf("inner:pressed", "inner:released", "inner:pressed", "inner:released")
                board.received[1].y shouldBe 100
                board.received[3].x shouldBe -1
            }
        }

        test("releasing the right button sets isPopupTrigger to true, and the old-format modifiers are BUTTON3_MASK (= META_MASK)") {
            withBoard { board ->
                board.recordAll(board.inner)
                TestMouse().click(5.0, 5.0, WMouseEvent.BUTTON3, board.inner)
                board.received.map { it.button } shouldBe List(3) { WMouseEvent.BUTTON3 }
                board.received.map { it.isPopupTrigger } shouldBe listOf(false, true, false)
                board.received[0].modifiers shouldBe WInputEvent.BUTTON3_MASK
                WInputEvent.BUTTON3_MASK shouldBe WInputEvent.META_MASK
                (board.received[0].modifiersEx and WInputEvent.BUTTON3_DOWN_MASK != 0).shouldBeTrue()
            }
        }

        test("moving without pressing a button delivers mouseMoved, and moves while holding a button pressed on another element are not delivered") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.move(30.0, 40.0, listOf(board.inner))
                mouse.press(1.0, 1.0, WMouseEvent.BUTTON1, emptyList()) // pressed on another element
                mouse.move(31.0, 40.0, listOf(board.inner))
                board.events shouldContainExactly listOf("inner:moved")
                board.received.single().id shouldBe WMouseEvent.MOUSE_MOVED
            }
        }

        test("mouseEntered / mouseExited are delivered once each time the boundary is crossed (once even if WinUI sends the same direction repeatedly)") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.enter(0.0, 5.0, board.inner)
                mouse.enter(1.0, 5.0, board.inner)
                mouse.exit(-1.0, 5.0, board.inner)
                mouse.exit(-2.0, 5.0, board.inner)
                mouse.enter(0.0, 5.0, board.inner)
                board.events shouldContainExactly listOf("inner:entered", "inner:exited", "inner:entered")
                board.received.map { it.id } shouldBe listOf(WMouseEvent.MOUSE_ENTERED, WMouseEvent.MOUSE_EXITED, WMouseEvent.MOUSE_ENTERED)
                board.received.map { it.clickCount to it.button } shouldBe List(3) { 0 to WMouseEvent.NOBUTTON }
            }
        }

        test("when both the child and the parent have listeners, presses, releases, moves and wheels that bubble to the parent are received only by the child") {
            withBoard { board ->
                board.recordAll(board.inner)
                board.recordAll(board.outer)
                val mouse = TestMouse()
                val both = listOf(board.inner, board.outer)
                mouse.move(5.0, 5.0, both)
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, both)
                mouse.release(5.0, 5.0, WMouseEvent.BUTTON1, both)
                mouse.wheel(5.0, 5.0, -120, both)
                board.events shouldContainExactly listOf("inner:moved", "inner:pressed", "inner:released", "inner:clicked", "inner:wheel")
                mouse.captures shouldContainExactly listOf("capture:inner", "release:inner")
            }
        }

        test("when the child has no listener the parent receives the events, and a press a child control has handled does not reach the parent") {
            withBoard { board ->
                board.recordAll(board.outer)
                val mouse = TestMouse()
                val both = listOf(board.inner, board.outer)
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, both)
                mouse.release(5.0, 5.0, WMouseEvent.BUTTON1, both)
                // When a button inside the panel is pressed: ButtonBase marks it handled and captures the pointer itself
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, both, handled = true, handledByContent = true)
                mouse.release(5.0, 5.0, WMouseEvent.BUTTON1, both, handled = true)
                board.events shouldContainExactly listOf("outer:pressed", "outer:released", "outer:clicked")
            }
        }

        test("wheel rotation is positive toward the user as in Swing, 120 per notch, wheelRotation 0 for less than one notch, and a horizontal wheel comes with Shift") {
            withBoard { board ->
                val wheels = mutableListOf<WMouseWheelEvent>()
                board.inner.addMouseWheelListener { e -> wheels += e }
                val mouse = TestMouse()
                mouse.wheel(5.0, 5.0, 120, listOf(board.inner)) // 1 notch away from the user
                mouse.wheel(5.0, 5.0, -240, listOf(board.inner)) // 2 notches toward the user
                mouse.wheel(5.0, 5.0, 60, listOf(board.inner)) // half a notch on a high-resolution wheel
                mouse.wheel(5.0, 5.0, 120, listOf(board.inner), horizontal = true) // 1 notch to the right
                wheels.map { it.wheelRotation } shouldBe listOf(-1, 2, 0, 1)
                wheels.map { it.preciseWheelRotation } shouldBe listOf(-1.0, 2.0, -0.5, 1.0)
                wheels.map { it.isShiftDown } shouldBe listOf(false, false, false, true)
                wheels.map { it.isHorizontal } shouldBe listOf(false, false, false, true)
                wheels.map { it.id }.distinct() shouldBe listOf(WMouseEvent.MOUSE_WHEEL)
                val lines = Win32.wheelScrollLines()
                if (lines == Win32.WHEEL_PAGESCROLL) {
                    wheels[1].scrollType shouldBe WMouseWheelEvent.WHEEL_BLOCK_SCROLL
                    wheels[1].scrollAmount shouldBe 1
                } else {
                    wheels[1].scrollType shouldBe WMouseWheelEvent.WHEEL_UNIT_SCROLL
                    wheels[1].scrollAmount shouldBe lines
                    wheels[1].unitsToScroll shouldBe lines * 2
                }
            }
        }

        test("calling consume marks the native event as handled and makes isConsumed true") {
            withBoard { board ->
                val consumed = mutableListOf<Boolean>()
                board.inner.addMouseListener(object : WMouseAdapter() {
                    override fun mousePressed(e: WMouseEvent) {
                        e.consume()
                        consumed += e.isConsumed
                    }

                    override fun mouseReleased(e: WMouseEvent) {
                        consumed += e.isConsumed
                    }
                })
                board.inner.addMouseWheelListener { e -> e.consume() }
                val mouse = TestMouse()
                mouse.click(5.0, 5.0, WMouseEvent.BUTTON1, board.inner)
                mouse.wheel(5.0, 5.0, 120, listOf(board.inner))
                consumed shouldContainExactly listOf(true, false)
                mouse.captures shouldContainExactly listOf("capture:inner", "handled:inner", "release:inner", "handled:inner")
            }
        }

        test("if the capture is lost while pressed, the pressed state is discarded and nothing is delivered on a later release") {
            withBoard { board ->
                board.recordAll(board.inner)
                val mouse = TestMouse()
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.loseCapture(board.inner)
                mouse.move(6.0, 5.0, listOf(board.inner))
                mouse.release(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner))
                board.events shouldContainExactly listOf("inner:pressed")
            }
        }

        test("addMouseListener alone does not cause mouseDragged / mouseMoved / mouseWheelMoved to be called (as in Swing)") {
            withBoard { board ->
                board.inner.addMouseListener(board.Recorder("inner"))
                val mouse = TestMouse()
                mouse.move(5.0, 5.0, listOf(board.inner))
                mouse.press(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.move(50.0, 5.0, listOf(board.inner))
                mouse.release(50.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner))
                mouse.wheel(5.0, 5.0, 120, listOf(board.inner))
                board.events shouldContainExactly listOf("inner:pressed", "inner:released")
            }
        }

        test("modifier keys can be read with isShiftDown and the like, and the Windows key becomes Meta") {
            withBoard { board ->
                board.recordAll(board.inner)
                val modifiers = WInputEvent.fromVirtualKeyModifiers(Xaml.MODIFIER_CONTROL or Xaml.MODIFIER_SHIFT)
                TestMouse().press(5.0, 5.0, WMouseEvent.BUTTON1, listOf(board.inner), keyModifiersEx = modifiers)
                val pressed = board.received.single()
                listOf(pressed.isShiftDown, pressed.isControlDown, pressed.isAltDown, pressed.isMetaDown) shouldBe
                    listOf(true, true, false, false)
                pressed.modifiers shouldBe (WInputEvent.SHIFT_MASK or WInputEvent.CTRL_MASK or WInputEvent.BUTTON1_MASK)
                WInputEvent.fromVirtualKeyModifiers(Xaml.MODIFIER_MENU or Xaml.MODIFIER_WINDOWS) shouldBe
                    (WInputEvent.ALT_DOWN_MASK or WInputEvent.META_DOWN_MASK)
            }
        }

        test("WinUI's PointerUpdateKind maps to AWT button numbers (left 1, middle 2, right 3, extra 4 / 5)") {
            listOf(
                WindowingInterop.PointerUpdateKind_LeftButtonPressed,
                WindowingInterop.PointerUpdateKind_LeftButtonReleased,
                WindowingInterop.PointerUpdateKind_MiddleButtonPressed,
                WindowingInterop.PointerUpdateKind_RightButtonReleased,
                WindowingInterop.PointerUpdateKind_XButton1Pressed,
                WindowingInterop.PointerUpdateKind_XButton2Released,
                0, // Other (moves and so on)
            ).map { buttonOfUpdateKind(it) } shouldBe listOf(1, 1, 2, 3, 4, 5, 0)
            (1..5).map { WInputEvent.getMaskForButton(it) } shouldBe listOf(1024, 2048, 4096, 16384, 32768)
        }

        test("classification of the source of a handled press (handledByContent): a control in the content and outside the tree are handled by others; itself, its parts and text in the content are its own") {
            // Native pointer events cannot be created, so only the check MouseInput.handledByContent applies to OriginalSource is verified with real elements
            val button = onUiThreadGet { WButton("OK") }
            val label = onUiThreadGet { WLabel("Text") }
            val combo = onUiThreadGet { WComboBox().also { it.addItem("Small") } }
            val panel = onUiThreadGet { WPanel().also { listOf(button, label, combo).forEach(it::add) } }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThreadGet {
                    val buttonPart = checkNotNull(Xaml.childAt(button.inspectable, 0)) { "Button has no template" }
                    val popupElement = checkNotNull(TemplateParts.find(combo, "Popup")) { "ComboBox has no Popup" }
                    val popup = popupElement.queryInterface(XamlInterop.IID_IPopup)
                    popupElement.release()
                    val popupChild = checkNotNull(popup.getPtrOrNull(XamlInterop.IPopup_get_Child)) { "Popup has no Child" }
                    val detached = WLabel("Outside the tree")
                    try {
                        listOf(
                            isHandledInContent(panel, button.inspectable),
                            isHandledInContent(panel, buttonPart),
                            isHandledInContent(panel, label.inspectable),
                            isHandledInContent(panel, panel.inspectable),
                            isHandledInContent(button, buttonPart),
                            isHandledInContent(panel, detached.inspectable),
                            // Inside the drop-down Popup is outside the combo box for the mouse (JComboBox's popup is also a separate component)
                            isHandledInContent(combo, popupChild),
                        )
                    } finally {
                        popupChild.release()
                        popup.release()
                        buttonPart.release()
                    }
                } shouldContainExactly listOf(true, true, false, false, false, true, true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("adding or removing null does nothing, the list is in the order added, and removed listeners receive nothing") {
            withBoard { board ->
                val first = board.Recorder("first")
                val second = board.Recorder("second")
                board.inner.addMouseListener(null)
                board.inner.addMouseListener(first)
                board.inner.addMouseListener(second)
                board.inner.addMouseMotionListener(first)
                board.inner.addMouseWheelListener(second)
                board.inner.mouseListeners.toList() shouldBe listOf(first, second)
                board.inner.mouseMotionListeners.toList() shouldBe listOf(first)
                board.inner.mouseWheelListeners.toList() shouldBe listOf(second)
                board.inner.removeMouseListener(null)
                board.inner.removeMouseListener(first)
                board.inner.removeMouseMotionListener(first)
                TestMouse().click(5.0, 5.0, WMouseEvent.BUTTON1, board.inner)
                board.events shouldContainExactly listOf("second:pressed", "second:released", "second:clicked")
                board.inner.mouseListeners.toList() shouldBe listOf(second)
                board.inner.mouseMotionListeners.toList().shouldBeEmpty()
                WButton().mouseListeners.isEmpty().shouldBeTrue()
            }
        }

        test("the MouseEvent IDs and button constants have the same values as AWT") {
            listOf(
                WMouseEvent.MOUSE_FIRST, WMouseEvent.MOUSE_LAST, WMouseEvent.MOUSE_CLICKED, WMouseEvent.MOUSE_PRESSED,
                WMouseEvent.MOUSE_RELEASED, WMouseEvent.MOUSE_MOVED, WMouseEvent.MOUSE_ENTERED, WMouseEvent.MOUSE_EXITED,
                WMouseEvent.MOUSE_DRAGGED, WMouseEvent.MOUSE_WHEEL,
            ) shouldBe listOf(500, 507, 500, 501, 502, 503, 504, 505, 506, 507)
            listOf(WMouseEvent.NOBUTTON, WMouseEvent.BUTTON1, WMouseEvent.BUTTON2, WMouseEvent.BUTTON3) shouldBe listOf(0, 1, 2, 3)
            listOf(WMouseWheelEvent.WHEEL_UNIT_SCROLL, WMouseWheelEvent.WHEEL_BLOCK_SCROLL) shouldBe listOf(0, 1)
            listOf(
                WInputEvent.SHIFT_DOWN_MASK,
                WInputEvent.CTRL_DOWN_MASK,
                WInputEvent.META_DOWN_MASK,
                WInputEvent.ALT_DOWN_MASK,
                WInputEvent.BUTTON1_DOWN_MASK,
                WInputEvent.BUTTON2_DOWN_MASK,
                WInputEvent.BUTTON3_DOWN_MASK,
                WInputEvent.ALT_GRAPH_DOWN_MASK,
            ) shouldBe listOf(64, 128, 256, 512, 1024, 2048, 4096, 8192)
            listOf(
                WInputEvent.SHIFT_MASK,
                WInputEvent.CTRL_MASK,
                WInputEvent.META_MASK,
                WInputEvent.ALT_MASK,
                WInputEvent.ALT_GRAPH_MASK,
                WInputEvent.BUTTON1_MASK,
                WInputEvent.BUTTON2_MASK,
                WInputEvent.BUTTON3_MASK,
            ) shouldBe listOf(1, 2, 4, 8, 32, 16, 8, 4)
        }

        test("getMaskForButton throws IllegalArgumentException when the button number is out of range") {
            listOf(0, 6).map { runCatching { WInputEvent.getMaskForButton(it) }.exceptionOrNull() is IllegalArgumentException } shouldBe
                listOf(true, true)
        }

        test("even with mouse listeners on a WButton, doClick's click works and no mouse events arrive") {
            val button = onUiThreadGet { WButton("Press") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThreadGet {
                    val events = mutableListOf<String>()
                    button.addMouseListener(object : WMouseAdapter() {
                        override fun mousePressed(e: WMouseEvent) {
                            events += "pressed"
                        }
                    })
                    button.addActionListener { events += "action" }
                    button.doClick()
                    events.toList()
                } shouldContainExactly listOf("action")
                onUiThreadGet { button.isPressed }.shouldBeFalse()
            } finally {
                UiTestHarness.detach(button)
            }
        }
    }
}
