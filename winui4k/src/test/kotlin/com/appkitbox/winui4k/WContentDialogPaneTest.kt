package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the [WContentDialog] APIs shaped like JDialog's content and closing: [WContentDialog.contentPane] and add / remove / setLayout,
 * pack / setLocationRelativeTo that do nothing, [WContentDialog.defaultCloseOperation] (DO_NOTHING_ON_CLOSE stops Esc and the close button),
 * and passing a [WFrame] as the owner. The expected values are decided from the results of the same calls on JDialog.
 * Keys are sent to the shared window with [TestKeyboard].
 */
@Tags("Integration")
class WContentDialogPaneTest : FunSpec() {
    private val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun events(): List<String> = synchronized(events) { events.toList() }

    private suspend fun awaitEvents(vararg expected: String) {
        eventually(5.seconds) { events() shouldBe expected.toList() }
    }

    /** Places the owner button in the shared window, runs [block], and removes it at the end. */
    private suspend fun withOwner(block: suspend (WButton) -> Unit) {
        val owner = onUiThreadGet { WButton("Owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        try {
            block(owner)
        } finally {
            UiTestHarness.detach(owner)
        }
    }

    /** A dialog with a close button. Records opening and closing as "closing:result:canceled" and "closed:result". */
    private fun dialog(): WContentDialog = WContentDialog("Confirm").apply {
        primaryButtonText = "OK"
        closeButtonText = "Close"
        val events = events
        addDialogListener(
            object : WContentDialogListener {
                override fun dialogOpened(e: WContentDialogEvent) = Unit

                override fun dialogClosing(e: WContentDialogEvent) {
                    events += "closing:${e.result}:${e.isCancel}"
                }

                override fun dialogClosed(e: WContentDialogEvent) {
                    events += "closed:${e.result}"
                }
            },
        )
    }

    private suspend fun awaitOpen(dialog: WContentDialog) {
        eventually(5.seconds) { onUiThreadGet { OpenContentDialogs.isLoadedOnUi(dialog) }.shouldBeTrue() }
        Thread.sleep(OPEN_SETTLE_MILLIS) // The focus moves inside during the opening animation
    }

    private suspend fun awaitClosed(dialog: WContentDialog) {
        eventually(5.seconds) { onUiThreadGet { OpenContentDialogs.isLoadedOnUi(dialog) }.shouldBeFalse() }
    }

    private fun pressEscape() = TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)

    /** Whether the native ContentControl.Content is [component] (call on the UI thread). */
    private fun nativeContentIs(dialog: WContentDialog, component: WComponent): Boolean {
        val control = dialog.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        return try {
            val content = control.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return false
            try {
                Xaml.sameObject(content, component.inspectable)
            } finally {
                content.release()
            }
        } finally {
            control.release()
        }
    }

    init {
        beforeTest { events.clear() }

        context("Defaults") {
            test("while content is null and contentPane is empty there is no native content, and adding to contentPane makes it the content. contentPane is WBorderLayout as in JDialog, and defaultCloseOperation is HIDE_ON_CLOSE") {
                onUiThread {
                    val dialog = WContentDialog()
                    dialog.content.shouldBeNull()
                    val pane = dialog.contentPane.shouldBeInstanceOf<WLayoutPanel>()
                    pane.layout.shouldBeInstanceOf<WBorderLayout>()
                    dialog.layout shouldBeSameInstanceAs pane.layout
                    nativeContentIs(dialog, pane).shouldBeFalse()
                    dialog.add(WLabel("Body"))
                    nativeContentIs(dialog, pane).shouldBeTrue()
                    dialog.content.shouldBeNull()
                    dialog.defaultCloseOperation shouldBe WContentDialog.HIDE_ON_CLOSE
                    val label = WLabel("Content")
                    WContentDialog("Heading", label).let { nativeContentIs(it, label) }.shouldBeTrue()
                }
            }

            test("add(c, SOUTH) and add without a constraint, without calling setLayout, go to the south and the center, as in JDialog") {
                onUiThread {
                    val dialog = WContentDialog()
                    val center = WLabel("Center")
                    val south = WButton("South")
                    dialog.add(center)
                    dialog.add(south, WBorderLayout.SOUTH)
                    val layout = dialog.layout as WBorderLayout
                    layout.getLayoutComponent(WBorderLayout.CENTER) shouldBeSameInstanceAs center
                    layout.getLayoutComponent(WBorderLayout.SOUTH) shouldBeSameInstanceAs south
                    nativeContentIs(dialog, dialog.contentPane).shouldBeTrue()
                }
            }
        }

        context("Reading and writing") {
            test("add / remove go to contentPane while content is null, and to the container after one is put into content (getContentPane is that too)") {
                onUiThread {
                    val dialog = WContentDialog()
                    val pane = dialog.contentPane
                    val first = WLabel("One")
                    val second = WLabel("Two")
                    dialog.add(first, WBorderLayout.NORTH)
                    dialog.add(second, 0)
                    pane.components.toList() shouldBe listOf(second, first)
                    dialog.remove(second)
                    second.parent.shouldBeNull()
                    val grid = WGrid()
                    dialog.content = grid
                    nativeContentIs(dialog, grid).shouldBeTrue()
                    dialog.contentPane shouldBeSameInstanceAs grid
                    dialog.add(second)
                    second.parent shouldBeSameInstanceAs grid
                    dialog.content = null
                    dialog.contentPane shouldBeSameInstanceAs pane
                    nativeContentIs(dialog, pane).shouldBeTrue()
                }
            }

            test("setLayout replaces the manager of contentPane without replacing the content (getContentPane after setLayout is also the shown content)") {
                onUiThread {
                    val dialog = WContentDialog()
                    val pane = dialog.contentPane as WLayoutPanel
                    val first = WLabel("One")
                    dialog.add(first)
                    val flow = WFlowLayout()
                    dialog.layout = flow
                    dialog.layout shouldBeSameInstanceAs flow
                    pane.layout shouldBeSameInstanceAs flow
                    dialog.content.shouldBeNull()
                    dialog.contentPane shouldBeSameInstanceAs pane
                    nativeContentIs(dialog, pane).shouldBeTrue()
                    val second = WLabel("Two")
                    dialog.contentPane.add(second)
                    pane.components.toList() shouldBe listOf(first, second)
                }
            }

            test("defaultCloseOperation reads back DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE and DISPOSE_ON_CLOSE") {
                onUiThread {
                    val dialog = WContentDialog()
                    listOf(WContentDialog.DO_NOTHING_ON_CLOSE, WContentDialog.DISPOSE_ON_CLOSE, WContentDialog.HIDE_ON_CLOSE).map {
                        dialog.defaultCloseOperation = it
                        dialog.defaultCloseOperation
                    } shouldBe listOf(0, 2, 1)
                }
            }
        }

        context("Events") {
            test("with DO_NOTHING_ON_CLOSE neither Esc nor the close button closes it, dialogClosing arrives with isCancel = true, and dispose closes it") {
                withOwner { owner ->
                    val dialog = onUiThreadGet { dialog().also { it.defaultCloseOperation = WContentDialog.DO_NOTHING_ON_CLOSE } }
                    onUiThread { dialog.show(owner) }
                    awaitOpen(dialog)
                    pressEscape()
                    awaitEvents("closing:NONE:true")
                    OpenContentDialogs.press(owner, "CloseButton")
                    awaitEvents("closing:NONE:true", "closing:NONE:true")
                    Thread.sleep(OPEN_SETTLE_MILLIS)
                    onUiThreadGet { dialog.isVisible }.shouldBeTrue()
                    onUiThread { dialog.dispose() }
                    awaitEvents("closing:NONE:true", "closing:NONE:true", "closing:NONE:false", "closed:NONE")
                    awaitClosed(dialog)
                }
            }

            test("even with DO_NOTHING_ON_CLOSE, calling dispose in dialogClosing closes it (the form of disposing in Swing's windowClosing)") {
                withOwner { owner ->
                    val dialog = onUiThreadGet {
                        dialog().also {
                            it.defaultCloseOperation = WContentDialog.DO_NOTHING_ON_CLOSE
                            it.addDialogListener(
                                object : WContentDialogAdapter() {
                                    override fun dialogClosing(e: WContentDialogEvent) {
                                        e.dialog.dispose()
                                    }
                                },
                            )
                        }
                    }
                    onUiThread { dialog.show(owner) }
                    awaitOpen(dialog)
                    pressEscape()
                    awaitEvents("closing:NONE:true", "closed:NONE")
                    awaitClosed(dialog)
                }
            }

            test("even with DO_NOTHING_ON_CLOSE the primary button closes it. HIDE_ON_CLOSE and DISPOSE_ON_CLOSE close it with Esc") {
                withOwner { owner ->
                    val dialog = onUiThreadGet { dialog().also { it.defaultCloseOperation = WContentDialog.DO_NOTHING_ON_CLOSE } }
                    onUiThread { dialog.show(owner) }
                    awaitOpen(dialog)
                    OpenContentDialogs.press(owner, "PrimaryButton")
                    awaitEvents("closing:PRIMARY:false", "closed:PRIMARY")
                    awaitClosed(dialog)
                    for (operation in listOf(WContentDialog.HIDE_ON_CLOSE, WContentDialog.DISPOSE_ON_CLOSE)) {
                        events.clear()
                        onUiThread {
                            dialog.defaultCloseOperation = operation
                            dialog.show(owner)
                        }
                        awaitOpen(dialog)
                        pressEscape()
                        awaitEvents("closing:NONE:false", "closed:NONE")
                        awaitClosed(dialog)
                    }
                }
            }
        }

        context("Boundaries and exceptions") {
            test("putting EXIT_ON_CLOSE or another value in defaultCloseOperation throws IllegalArgumentException and the value does not change (the same as JDialog)") {
                onUiThread {
                    val dialog = WContentDialog()
                    dialog.defaultCloseOperation = WContentDialog.DO_NOTHING_ON_CLOSE
                    for (value in listOf(WContentDialog.EXIT_ON_CLOSE, -1, 4)) {
                        shouldThrow<IllegalArgumentException> { dialog.defaultCloseOperation = value }
                    }
                    dialog.defaultCloseOperation shouldBe WContentDialog.DO_NOTHING_ON_CLOSE
                }
            }

            test("if content is not a container, add, setLayout and getContentPane throw IllegalStateException. add with constraints to content without a manager throws IllegalArgumentException") {
                onUiThread {
                    val dialog = WContentDialog()
                    dialog.layout = null
                    shouldThrow<IllegalArgumentException> { dialog.add(WButton("x"), WBorderLayout.NORTH) }
                    dialog.contentPane.componentCount shouldBe 0
                    val stack = WPanel()
                    dialog.content = stack
                    shouldThrow<IllegalArgumentException> { dialog.add(WButton("x"), WBorderLayout.NORTH) }
                    stack.componentCount shouldBe 0
                    dialog.content = WLabel("Text only")
                    shouldThrow<IllegalStateException> { dialog.add(WButton("x")) }
                    shouldThrow<IllegalStateException> { dialog.remove(WButton("x")) }
                    shouldThrow<IllegalStateException> { dialog.layout = WBorderLayout() }
                    shouldThrow<IllegalStateException> { dialog.contentPane }
                }
            }
        }

        context("Swing compatibility") {
            test("constants with the same values as Swing can be read from WContentDialog") {
                listOf(
                    WContentDialog.DO_NOTHING_ON_CLOSE,
                    WContentDialog.HIDE_ON_CLOSE,
                    WContentDialog.DISPOSE_ON_CLOSE,
                    WContentDialog.EXIT_ON_CLOSE,
                ) shouldBe listOf(0, 1, 2, 3)
            }

            test("opening after getContentPane().add makes contentPane the content. pack and setLocationRelativeTo (null, a WFrame, a component) do nothing") {
                withOwner { owner ->
                    val dialog = onUiThreadGet { dialog() }
                    onUiThread {
                        dialog.contentPane.add(WLabel("Body put directly in contentPane"))
                        dialog.pack()
                        dialog.setLocationRelativeTo(null)
                        dialog.show(owner)
                    }
                    awaitOpen(dialog)
                    onUiThread {
                        nativeContentIs(dialog, dialog.contentPane).shouldBeTrue() // contentPane becomes the content on opening
                        dialog.pack()
                        dialog.setLocationRelativeTo(UiTestHarness.sharedFrame())
                        dialog.setLocationRelativeTo(owner)
                        dialog.isVisible.shouldBeTrue()
                    }
                    events() shouldBe emptyList()
                    onUiThread { dialog.hide() }
                    awaitClosed(dialog)
                }
            }

            test("a dialog with the default content (WBorderLayout) expands to the children's preferred size as JDialog's pack does, and the center child takes all the rest") {
                withOwner { owner ->
                    val dialog = onUiThreadGet { dialog() }
                    val (north, center) = onUiThreadGet {
                        val north = WLabel("Header row")
                        val center = WPanel().also {
                            it.width = WIDE_WIDTH
                            it.height = WIDE_HEIGHT
                        }
                        dialog.add(north, WBorderLayout.NORTH)
                        dialog.add(center)
                        dialog.show(owner)
                        north to center
                    }
                    awaitOpen(dialog)
                    eventually(5.seconds) {
                        onUiThread {
                            val pane = dialog.contentPane
                            // Even a child wider than the dialog's minimum width (320) is not shrunk; the dialog expands
                            pane.actualWidth shouldBe (WIDE_WIDTH plusOrMinus 1.0)
                            north.actualWidth shouldBe (WIDE_WIDTH plusOrMinus 1.0)
                            center.actualWidth shouldBe (WIDE_WIDTH plusOrMinus 1.0)
                            center.actualHeight shouldBe (WIDE_HEIGHT plusOrMinus 1.0)
                            pane.actualHeight shouldBe (north.actualHeight + WIDE_HEIGHT plusOrMinus 1.0)
                        }
                    }
                    onUiThread { dialog.hide() }
                    awaitClosed(dialog)
                }
            }

            test("a WFrame can be passed as the owner (the JDialog(frame, title, modal) form). showAndWait opens in that window and returns the pressed button") {
                withOwner { owner ->
                    val frame = UiTestHarness.sharedFrame()
                    val dialog = onUiThreadGet {
                        WContentDialog(frame, "Heading", true).also {
                            it.primaryButtonText = "OK"
                            it.add(WLabel("Body"))
                        }
                    }
                    onUiThreadGet { dialog.owner } shouldBeSameInstanceAs frame
                    WinUiUtilities.invokeLater { events += "returned:${dialog.showAndWait()}" }
                    awaitOpen(dialog)
                    OpenContentDialogs.isOpen(owner).shouldBeTrue() // Open in the shared window
                    OpenContentDialogs.press(owner, "PrimaryButton")
                    awaitEvents("returned:PRIMARY")
                    awaitClosed(dialog)
                }
            }
        }
    }

    private companion object {
        /** The width of a child wider than the dialog's minimum width (ContentDialogMinWidth = 320) and narrower than the maximum width (548) minus the margins. */
        const val WIDE_WIDTH = 420.0
        const val WIDE_HEIGHT = 150.0

        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val OPEN_SETTLE_MILLIS = 600L
    }
}
