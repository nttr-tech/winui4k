package com.appkitbox.winui4k

/**
 * Mouse input for tests: creates a [PointerSample] in place of WinUI pointer events and passes it to the processing of
 * the component's mouse listeners ([MouseInput.dispatch]). WinUI 3 pointer input does not arrive even if window messages
 * (WM_MOUSEMOVE / WM_LBUTTONDOWN etc.) are sent with PostMessageW / SendMessageW to InputSiteWindowClass or the parent
 * window (doc/verification.md), so reading from native events is not exercised. Use on the UI thread.
 *
 * This object remembers the pressed buttons and the time. [targets] lists the components in the order the native
 * event propagates to parents (deepest element first) (they are delivered as the same event). [captures] records
 * captures, releases and events marked handled.
 */
internal class TestMouse(
    /** Physical pixels per DIP. */
    private val pixelsPerDip: Double = 1.0,
) {
    /** Records of capture ("capture:name"), release ("release:name") and handled ("handled:name"). The name is [WComponent.name]. */
    val captures = mutableListOf<String>()

    /** The time of the next event (milliseconds). Advanced with [advance]. */
    var time = START_MILLIS
        private set

    private var buttonsDown = 0

    /** Advances the time by [millis] milliseconds. */
    fun advance(millis: Long) {
        time += millis
    }

    /** Presses [button]. [handled] is a press the control marked handled, [handledByContent] is when that was a child control. */
    @Suppress("LongParameterList") // corresponds to the fields of PointerSample
    @JvmOverloads
    fun press(
        x: Double,
        y: Double,
        button: Int,
        targets: List<WComponent>,
        handled: Boolean = false,
        handledByContent: Boolean = false,
        keyModifiersEx: Int = 0,
    ) {
        buttonsDown = buttonsDown or WInputEvent.getMaskForButton(button)
        send(PointerKind.PRESSED, x, y, targets, button, handled, handledByContent, keyModifiersEx = keyModifiersEx)
    }

    /** Releases [button]. */
    @JvmOverloads
    fun release(x: Double, y: Double, button: Int, targets: List<WComponent>, handled: Boolean = false) {
        buttonsDown = buttonsDown and WInputEvent.getMaskForButton(button).inv()
        send(PointerKind.RELEASED, x, y, targets, button, handled, false)
    }

    /** Moves to [x], [y] (a drag if a button is pressed). */
    fun move(x: Double, y: Double, targets: List<WComponent>) = send(PointerKind.MOVED, x, y, targets)

    /** Enters the bounds of [targets] (WinUI sends it per element, so deliver them one by one). */
    fun enter(x: Double, y: Double, target: WComponent) = send(PointerKind.ENTERED, x, y, listOf(target))

    /** Exits the bounds of [target]. */
    fun exit(x: Double, y: Double, target: WComponent) = send(PointerKind.EXITED, x, y, listOf(target))

    /** Rotates the wheel ([delta] is WinUI's MouseWheelDelta; positive is away / right, 120 per notch). */
    @JvmOverloads
    fun wheel(x: Double, y: Double, delta: Int, targets: List<WComponent>, horizontal: Boolean = false) =
        send(PointerKind.WHEEL, x, y, targets, wheelDelta = delta, horizontal = horizontal)

    /** The pointer capture is lost (such as when the window becomes inactive). */
    fun loseCapture(target: WComponent) = send(PointerKind.CAPTURE_LOST, 0.0, 0.0, listOf(target))

    /** Presses and releases [button] at [x], [y]. */
    fun click(x: Double, y: Double, button: Int, target: WComponent) {
        press(x, y, button, listOf(target))
        release(x, y, button, listOf(target))
    }

    @Suppress("LongParameterList") // corresponds 1:1 to the fields of PointerSample
    private fun send(
        kind: PointerKind,
        x: Double,
        y: Double,
        targets: List<WComponent>,
        button: Int = WMouseEvent.NOBUTTON,
        handled: Boolean = false,
        handledByContent: Boolean = false,
        wheelDelta: Int = 0,
        horizontal: Boolean = false,
        keyModifiersEx: Int = 0,
    ) {
        val sample = PointerSample(
            kind = kind,
            x = x,
            y = y,
            changedButton = button,
            buttonsDown = buttonsDown,
            keyModifiersEx = keyModifiersEx,
            timestampMillis = time,
            wheelDelta = wheelDelta,
            isHorizontalWheel = horizontal,
            handled = handled,
            handledByContent = { handledByContent },
            pixelsPerDip = pixelsPerDip,
        )
        for (target in targets) target.input.mouse.dispatch(sample, RecordingTarget(target.name))
    }

    private inner class RecordingTarget(private val name: String) : PointerTarget {
        override fun capture(): Boolean {
            captures += "capture:$name"
            return true
        }

        override fun releaseCapture() {
            captures += "release:$name"
        }

        override fun markHandled() {
            captures += "handled:$name"
        }
    }

    private companion object {
        const val START_MILLIS = 1_000_000L
    }
}
