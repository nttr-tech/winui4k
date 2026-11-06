package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that the [WFrame] handlers (Window.Closed / Activated, AppWindow.Closing / Changed), the default button, the menu bar and children
 * do not capture the WFrame, and that the WFrame, the child wrappers and the native Window are collected once it is closed and the reference released ([Reclamation]).
 * Also checks the DisplayArea reference of [WDisplayArea].
 * The shared "winui4k tests" window is kept open (the app exits when the last window closes).
 */
@Tags("Memory")
class WrapperReleaseFrameTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private class Built(val frame: WeakReference<WFrame>, val child: WeakReference<WButton>, val window: NativeProbe)

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        beforeTest { events.clear() }

        test("with window listeners, a default button, a menu bar and children added and shown, disposing and releasing the reference collects the WFrame and its children, and destroys the Window") {
            val events = events
            val built = onUiThreadGet {
                val frame = WFrame(TITLE)
                frame.addWindowListener(
                    object : WWindowAdapter() {
                        override fun windowOpened(e: WWindowEvent) {
                            events += "opened"
                        }

                        override fun windowClosed(e: WWindowEvent) {
                            events += "closed"
                        }
                    },
                )
                frame.addWindowFocusListener(object : WWindowFocusListener {
                    override fun windowGainedFocus(e: WWindowEvent) = Unit

                    override fun windowLostFocus(e: WWindowEvent) = Unit
                })
                frame.addWindowStateListener { }
                val button = WButton("Default")
                frame.add(button)
                frame.add(WTextField())
                frame.defaultButton = button
                frame.jMenuBar = WMenuBar().also { it.add(WMenuBarItem("File")) }
                frame.isVisible = true
                Built(WeakReference(frame), WeakReference(button), NativeProbe(frame.window))
            }
            eventually(5.seconds) { events() shouldContain "opened" }
            onUiThread { built.frame.get()?.dispose() }
            eventually(5.seconds) { events() shouldContain "closed" }
            Reclamation.awaitCollected(built.frame).shouldBeTrue()
            Reclamation.awaitCollected(built.child).shouldBeTrue()
            Reclamation.awaitDestroyed(built.window).shouldBeTrue()
        }

        test("a WFrame never shown is collected and its Window destroyed once the reference is released") {
            val built = onUiThreadGet {
                val frame = WFrame(TITLE)
                val button = WButton("Inner")
                frame.add(button)
                frame.addWindowListener(object : WWindowAdapter() {})
                Built(WeakReference(frame), WeakReference(button), NativeProbe(frame.window))
            }
            Reclamation.awaitCollected(built.frame).shouldBeTrue()
            Reclamation.awaitCollected(built.child).shouldBeTrue()
            Reclamation.awaitDestroyed(built.window).shouldBeTrue()
        }

        context("Lifetime") {
            test("a WDisplayArea received from primary, all or nearest is collected once the reference is released, and releases its DisplayArea reference too") {
                // DisplayArea does not support weak references (NativeProbe gets E_NOINTERFACE), so check with the reference count minus the one the test holds
                val built = onUiThreadGet {
                    val areas = listOf(WDisplayArea.primary(), WDisplayArea.nearest(UiTestHarness.sharedFrame().appWindow)) +
                        WDisplayArea.all()
                    areas.forEach { it.workArea } // Readable
                    areas.map { area ->
                        val native = area.displayArea.also { it.addRef() } // The reference the test holds
                        Triple(WeakReference(area), native, refCount(native))
                    }
                }
                built.forEach { (area, native, before) ->
                    Reclamation.awaitCollected(area).shouldBeTrue()
                    Reclamation.awaitWithGc { onUiThreadGet { refCount(native) } == before - 1 }.shouldBeTrue()
                    onUiThread { native.release() }
                }
            }
        }
    }

    /** The current reference count of [pointer] (read from the return values of AddRef and Release). */
    private fun refCount(pointer: ComPtr): Int {
        pointer.rawCall(1, ComPtr.UNKNOWN_DESC)
        return pointer.rawCall(2, ComPtr.UNKNOWN_DESC)
    }

    private companion object {
        const val TITLE = "winui4k frame release test"
    }
}
