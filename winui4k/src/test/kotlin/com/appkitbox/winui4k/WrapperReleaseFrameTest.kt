package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that the [WFrame] handlers (Window.Closed / Activated, AppWindow.Closing / Changed), the default button, the menu bar and children
 * do not capture the WFrame, and that the WFrame and the child wrappers are collected once it is closed and the reference released ([Reclamation]).
 * The shared "winui4k tests" window is kept open (the app exits when the last window closes).
 */
class WrapperReleaseFrameTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private class Built(val frame: WeakReference<WFrame>, val child: WeakReference<WButton>)

    init {
        beforeSpec { UiTestHarness.sharedFrame() }
        beforeTest { events.clear() }

        test("with window listeners, a default button, a menu bar and children added and shown, disposing and releasing the reference collects the WFrame and its children") {
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
                Built(WeakReference(frame), WeakReference(button))
            }
            eventually(5.seconds) { events() shouldContain "opened" }
            onUiThread { built.frame.get()?.dispose() }
            eventually(5.seconds) { events() shouldContain "closed" }
            Reclamation.awaitCollected(built.frame).shouldBeTrue()
            Reclamation.awaitCollected(built.child).shouldBeTrue()
        }

        test("a WFrame never shown is collected once the reference is released") {
            val built = onUiThreadGet {
                val frame = WFrame(TITLE)
                val button = WButton("Inner")
                frame.add(button)
                frame.addWindowListener(object : WWindowAdapter() {})
                Built(WeakReference(frame), WeakReference(button))
            }
            Reclamation.awaitCollected(built.frame).shouldBeTrue()
            Reclamation.awaitCollected(built.child).shouldBeTrue()
        }
    }

    private companion object {
        const val TITLE = "winui4k frame release test"
    }
}
