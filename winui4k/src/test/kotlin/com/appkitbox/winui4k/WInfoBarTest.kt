package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/** Tests for WInfoBar's content (title / message / severity / icon), open state and Closing / Closed listeners. */
@Tags("E2E")
class WInfoBarTest : FunSpec() {
    init {
        test("title and message return exactly the strings that were set") {
            val (title, message) = onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.title = "Saved"
                infoBar.message = "Your changes have been written to the file."
                infoBar.title to infoBar.message
            }
            title shouldBe "Saved"
            message shouldBe "Your changes have been written to the file."
        }

        test("isOpen starts out false, and setting it to true opens the bar") {
            val (initial, open) = onUiThreadGet {
                val infoBar = WInfoBar()
                val initial = infoBar.isOpen
                infoBar.isOpen = true
                initial to infoBar.isOpen
            }
            initial.shouldBeFalse()
            open.shouldBeTrue()
        }

        test("severity returns exactly the value that was set") {
            onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.severity = InfoBarSeverity.ERROR
                infoBar.severity
            } shouldBe InfoBarSeverity.ERROR
        }

        test("isClosable and isIconVisible return exactly the values that were set") {
            val (closable, iconVisible) = onUiThreadGet {
                val infoBar = WInfoBar()
                infoBar.isClosable = false
                infoBar.isIconVisible = false
                infoBar.isClosable to infoBar.isIconVisible
            }
            closable.shouldBeFalse()
            iconVisible.shouldBeFalse()
        }

        test("actionButton can be retrieved after being set, and cleared with null") {
            val (set, cleared, button) = onUiThreadGet {
                val infoBar = WInfoBar()
                val button = WButton("Retry")
                infoBar.actionButton = button
                val set = infoBar.actionButton
                infoBar.actionButton = null
                Triple(set, infoBar.actionButton, button)
            }
            set shouldBeSameInstanceAs button
            cleared shouldBe null
        }

        test("icon is null by default, setting it attaches the native IconSource, and null removes it") {
            onUiThreadGet {
                val infoBar = WInfoBar()
                val initial = listOf(infoBar.icon, infoBar.hasNativeIcon())
                infoBar.icon = Symbol.SAVE
                val set = listOf(infoBar.icon, infoBar.hasNativeIcon())
                infoBar.icon = null
                initial + set + listOf(infoBar.icon, infoBar.hasNativeIcon())
            } shouldContainExactly listOf(null, false, Symbol.SAVE, true, null, false)
        }

        test("setting isOpen to false calls Closing and then Closed with PROGRAMMATIC") {
            onUiThreadGet {
                val events = ArrayList<String>()
                val infoBar = WInfoBar()
                infoBar.isOpen = true
                infoBar.addClosingListener { e ->
                    events += "closing:${e.reason}:${e.isCancel}:${e.infoBar === infoBar}"
                }
                infoBar.addCloseListener { events += "closed:$it" }
                infoBar.isOpen = false
                events + "isOpen=${infoBar.isOpen}"
            } shouldContainExactly listOf("closing:PROGRAMMATIC:false:true", "closed:PROGRAMMATIC", "isOpen=false")
        }

        test("setting isCancel to true in Closing keeps it open and Closed is not called. Later listeners receive the previous value") {
            onUiThreadGet {
                val events = ArrayList<String>()
                val infoBar = WInfoBar()
                infoBar.isOpen = true
                infoBar.addClosingListener { e -> e.isCancel = true }
                infoBar.addClosingListener { e -> events += "second:${e.isCancel}" }
                infoBar.addCloseListener { events += "closed:$it" }
                infoBar.isOpen = false
                events + "isOpen=${infoBar.isOpen}"
            } shouldContainExactly listOf("second:true", "isOpen=true")
        }

        test("removed listeners are not called") {
            onUiThreadGet {
                val events = ArrayList<String>()
                val infoBar = WInfoBar()
                val closing: (WInfoBarClosingEvent) -> Unit = { events += "closing" }
                val closed: (InfoBarCloseReason) -> Unit = { events += "closed" }
                infoBar.addClosingListener(closing)
                infoBar.addCloseListener(closed)
                infoBar.removeClosingListener(closing)
                infoBar.removeCloseListener(closed)
                infoBar.isOpen = true
                infoBar.isOpen = false
                events
            }.shouldBeEmpty()
        }

        context("Defaults") {
            test("it is closed, shows the close button and the icon, the severity is INFORMATIONAL, texts are empty, and actionButton, content and icon are null") {
                onUiThreadGet {
                    val infoBar = WInfoBar()
                    listOf(
                        infoBar.isOpen,
                        infoBar.isClosable,
                        infoBar.isIconVisible,
                        infoBar.severity,
                        infoBar.title,
                        infoBar.message,
                        infoBar.actionButton,
                        infoBar.content,
                        infoBar.icon,
                    )
                } shouldContainExactly listOf(false, true, true, InfoBarSeverity.INFORMATIONAL, "", "", null, null, null)
            }
        }

        context("Reading and writing") {
            test("a component set to content reads back and null removes it") {
                onUiThreadGet {
                    val infoBar = WInfoBar()
                    val label = WLabel("Details")
                    infoBar.content = label
                    val set = infoBar.content === label
                    infoBar.content = null
                    listOf(set, infoBar.content)
                } shouldContainExactly listOf(true, null)
            }
        }

        context("Events") {
            test("pressing the close button (×) calls the button listener, Closing and Closed in that order with CLOSE_BUTTON and closes it. Removed listeners are not called") {
                val events = Collections.synchronizedList(mutableListOf<String>())
                val infoBar = onUiThreadGet {
                    WInfoBar().also { infoBar ->
                        infoBar.title = "Saved"
                        infoBar.isOpen = true
                        val removed: () -> Unit = { events += "removed" }
                        infoBar.addCloseButtonListener(removed)
                        infoBar.addCloseButtonListener { events += "button" }
                        infoBar.removeCloseButtonListener(removed)
                        infoBar.addClosingListener { e -> events += "closing:${e.reason}" }
                        infoBar.addCloseListener { events += "closed:$it" }
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(infoBar)
                try {
                    onUiThread {
                        val close = checkNotNull(Xaml.templatePartOrNull(infoBar.inspectable, "CloseButton")) { "No CloseButton" }
                        try {
                            Xaml.invoke(close)
                        } finally {
                            close.release()
                        }
                    }
                    eventually(5.seconds) {
                        events.toList() shouldContainExactly listOf("button", "closing:CLOSE_BUTTON", "closed:CLOSE_BUTTON")
                    }
                    onUiThreadGet { infoBar.isOpen }.shouldBeFalse()
                } finally {
                    UiTestHarness.detach(infoBar)
                }
            }
        }
    }

    /** Whether the native InfoBar.IconSource is set. */
    private fun WInfoBar.hasNativeIcon(): Boolean =
        inspectable.getPtrOrNull(XamlInterop.IInfoBar_get_IconSource)?.also { it.release() } != null
}
