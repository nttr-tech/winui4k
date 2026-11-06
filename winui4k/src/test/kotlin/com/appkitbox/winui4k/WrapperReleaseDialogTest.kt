package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that the handlers of dialogs ([WContentDialog], [WOptionPane]) do not capture the wrapper, and that the wrapper and the native object
 * are collected once the dialog is closed and its references are dropped ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseDialogTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private fun dialog(): WContentDialog = WContentDialog("Confirm", WLabel("Continue?")).apply {
        primaryButtonText = "Yes"
        closeButtonText = "Cancel"
    }

    /** Adds open/close and button listeners. The listeners do not reference the dialog (they only record here). */
    private fun addListeners(dialog: WContentDialog) {
        val events = events
        dialog.addDialogListener(
            object : WContentDialogAdapter() {
                override fun dialogClosed(e: WContentDialogEvent) {
                    events += "closed:${e.result}"
                }
            },
        )
        dialog.addButtonClickListener { e -> events += "button:${e.button}" }
    }

    /** Places an owner button in the shared window, runs [block], and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    private suspend fun awaitOpen(owner: WComponent) {
        eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeTrue() }
        Thread.sleep(OPEN_SETTLE_MILLIS)
    }

    init {
        beforeTest { events.clear() }

        test("a dialog that was never opened, with listeners that do not reference it, is collected and its native object destroyed once all listeners are removed") {
            val (reference, probe) = onUiThreadGet {
                val dialog = dialog()
                val listener = object : WContentDialogAdapter() {}
                val button: (WContentDialogButtonEvent) -> Unit = { }
                dialog.addDialogListener(listener)
                dialog.addButtonClickListener(button)
                dialog.removeDialogListener(listener)
                dialog.removeButtonClickListener(button)
                WeakReference(dialog) to NativeProbe(dialog.inspectable)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a dialog with listeners that do not reference it is collected and its native object destroyed when its references are dropped after opening and closing it") {
            withOwner { owner ->
                val reference = onUiThreadGet {
                    val dialog = dialog()
                    addListeners(dialog)
                    dialog.show(owner) { events += "callback:$it" }
                    WeakReference(dialog)
                }
                awaitOpen(owner)
                val probe = OpenContentDialogs.probe(owner).shouldNotBeNull()
                OpenContentDialogs.press(owner, "CloseButton")
                eventually(5.seconds) { events() shouldBe listOf("button:CLOSE", "callback:NONE", "closed:NONE") }
                eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
                onUiThread { } // Wait for the Unloaded handler to finish running
                Reclamation.awaitCollected(reference).shouldBeTrue()
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }
        }

        test("a dialog that stops the close button with DO_NOTHING_ON_CLOSE and builds its content with contentPane and setLayout is also collected after dispose and dropping the reference") {
            withOwner { owner ->
                val reference = onUiThreadGet {
                    val dialog = dialog()
                    addListeners(dialog)
                    dialog.defaultCloseOperation = WContentDialog.DO_NOTHING_ON_CLOSE
                    dialog.content = null // Back to the default contentPane
                    dialog.add(WLabel("Name"))
                    dialog.layout = WBorderLayout()
                    dialog.add(WTextField(), WBorderLayout.SOUTH)
                    dialog.show(owner)
                    WeakReference(dialog)
                }
                awaitOpen(owner)
                val probe = OpenContentDialogs.probe(owner).shouldNotBeNull()
                OpenContentDialogs.press(owner, "CloseButton")
                eventually(5.seconds) { events() shouldBe listOf("button:CLOSE") }
                Thread.sleep(OPEN_SETTLE_MILLIS)
                OpenContentDialogs.isOpen(owner).shouldBeTrue()
                onUiThread { reference.get()?.dispose() }
                eventually(5.seconds) { events() shouldBe listOf("button:CLOSE", "closed:NONE") }
                eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
                onUiThread { }
                Reclamation.awaitCollected(reference).shouldBeTrue()
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }
        }

        test("a dialog opened by WOptionPane has its native object destroyed too after it is closed (the Esc and button handlers do not capture the dialog)") {
            withOwner { owner ->
                val results = events
                onUiThread {
                    WOptionPane.showConfirmDialog(owner, "Do you want to save?", "Confirm", WOptionPane.YES_NO_OPTION, WOptionPane.QUESTION_MESSAGE, null) {
                        results += "result:$it"
                    }
                }
                awaitOpen(owner)
                val probe = OpenContentDialogs.probe(owner).shouldNotBeNull()
                OpenContentDialogs.press(owner, "SecondaryButton")
                eventually(5.seconds) { events() shouldBe listOf("result:${WOptionPane.NO_OPTION}") }
                eventually(5.seconds) { OpenContentDialogs.isOpen(owner).shouldBeFalse() }
                onUiThread { }
                Reclamation.awaitDestroyed(probe).shouldBeTrue()
            }
        }
    }

    private companion object {
        const val OPEN_SETTLE_MILLIS = 600L
    }
}
