package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Collections

/**
 * Tests that check operations requested before being added to the screen (before showing, or right after being added to a window and before the first layout)
 * take effect when shown, as in Swing. The expected values are what Swing shows when called in the same order (the selected row appears selected,
 * the row to show is visible, etc.).
 */
@Tags("E2E")
class BeforeShowRequestsTest : FunSpec() {
    private fun items(count: Int) = (0 until count).map { "Item ${it + 1}" }

    /** Runs layouts and waits until [condition] holds (up to 5 seconds). true if it holds. */
    private fun awaitUi(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(50)
        }
        return false
    }

    /** Waits outside the UI thread until [condition] holds (up to 5 seconds; [condition] may hand work to the UI thread). true if it holds. */
    private fun awaitOffUi(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return false
    }

    /** The number of Popups open in the window of [owner] (VisualTreeHelper.GetOpenPopupsForXamlRoot). 0 if not attached. Call on the UI thread. */
    private fun openPopupCount(owner: WComponent): Int {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return 0
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        return try {
            popups.getInt(FoundationInterop.IVectorView_get_Size)
        } finally {
            popups.release()
        }
    }

    /**
     * The [left, top, content width, content height] of the first Popup open in the window of [owner] (Popup.HorizontalOffset / VerticalOffset and
     * the child's ActualWidth / ActualHeight; DIP). null if none is open. Call on the UI thread.
     */
    private fun firstOpenPopupBounds(owner: WComponent): DoubleArray? {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        try {
            if (popups.getInt(FoundationInterop.IVectorView_get_Size) == 0) return null
            val popup = popups.getPtr(FoundationInterop.IVectorView_GetAt, 0)
            try {
                val child = popup.getPtrOrNull(XamlInterop.IPopup_get_Child) ?: return null
                val element = try {
                    child.queryInterface(XamlInterop.IID_IFrameworkElement)
                } finally {
                    child.release()
                }
                return try {
                    doubleArrayOf(
                        popup.getDouble(XamlInterop.IPopup_get_HorizontalOffset),
                        popup.getDouble(XamlInterop.IPopup_get_VerticalOffset),
                        element.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth),
                        element.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight),
                    )
                } finally {
                    element.release()
                }
            } finally {
                popup.release()
            }
        } finally {
            popups.release()
        }
    }

    init {
        context("WList.ensureIndexIsVisible") {
            test("when called before showing, the element is visible when shown") {
                val list = onUiThreadGet {
                    WList(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(42)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }

            test("even when called right after being added to a shown window (before the first layout), the element is visible") {
                val list = onUiThreadGet { WList(items(100)).also { it.height = 200.0 } }
                val frame = UiTestHarness.sharedFrame()
                onUiThread {
                    frame.add(list)
                    list.ensureIndexIsVisible(42)
                }
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }

            test("requesting an element out of range before showing does nothing (the first one stays visible)") {
                val list = onUiThreadGet {
                    WList(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(100)
                        it.ensureIndexIsVisible(-1)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.lastVisibleIndex > 0 } shouldBe true
                    onUiThreadGet { list.firstVisibleIndex } shouldBe 0
                } finally {
                    UiTestHarness.detach(list)
                }
            }
        }

        context("WListBox.ensureIndexIsVisible") {
            test("when called before showing, the element is visible when shown") {
                val list = onUiThreadGet {
                    WListBox(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(42)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }
        }
        context("WTable.setRowSelectionInterval") {
            /** A table of 3 rows of Name / Department. */
            fun table(): WTable = WTable(
                arrayOf(arrayOf<Any?>("Alice", "Sales"), arrayOf<Any?>("Bob", "Engineering"), arrayOf<Any?>("Carol", "Support")),
                arrayOf<Any?>("Name", "Department"),
            )

            /** The row the table (TableView) selects on screen (ITableView.SelectedIndex). */
            fun nativeSelectedRow(table: WTable): Int = table.inspectable.getInt(TabularInterop.ITableView_get_SelectedIndex)

            test("selecting the second row before showing makes the table select that row when shown, and selectedRow is 1") {
                val table = onUiThreadGet { table().also { it.setRowSelectionInterval(1, 1) } }
                UiTestHarness.attachAndAwaitLoaded(table)
                try {
                    awaitUi { nativeSelectedRow(table) == 1 } shouldBe true
                    onUiThreadGet { table.selectedRow } shouldBe 1
                } finally {
                    UiTestHarness.detach(table)
                }
            }

            test("selecting and then clearing the selection before showing leaves nothing selected when shown") {
                val table = onUiThreadGet {
                    table().also {
                        it.setRowSelectionInterval(1, 1)
                        it.clearSelection()
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(table)
                try {
                    awaitUi { table.actualHeight > 0.0 } shouldBe true
                    Thread.sleep(300)
                    onUiThreadGet { nativeSelectedRow(table) to table.selectedRow } shouldBe (-1 to -1)
                } finally {
                    UiTestHarness.detach(table)
                }
            }
        }

        context("WSplitPane divider position") {
            /**
             * Puts [split] in CENTER of a 400 x 260 window with WBorderLayout and shows it without activating it (the same form as the JSplitPane sample).
             * [afterShow] is called right after showing (within the same UI thread task). Returns after the content is laid out.
             */
            fun showInFrame(split: WSplitPane, afterShow: () -> Unit = {}): WFrame {
                val frame = onUiThreadGet {
                    WFrame("BeforeShowRequestsTest").also {
                        it.layout = WBorderLayout()
                        it.add(split, WBorderLayout.CENTER)
                        it.add(WLabel(" "), WBorderLayout.SOUTH)
                        it.setSize(400, 260)
                        it.isAutoRequestFocus = false
                        it.isVisible = true
                        afterShow()
                    }
                }
                awaitUi { split.actualWidth > 0.0 && split.actualHeight > 0.0 } shouldBe true
                Thread.sleep(500) // Wait for the layout after showing (until it grows to the window's size)
                return frame
            }

            test("calling setDividerLocation(200) right after showing puts the divider at 200, and the last position the listener saw is also 200 (as in Swing)") {
                val seen = mutableListOf<Int>()
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("Left"), WLabel("Right")).also { pane ->
                        pane.addChangeListener { seen += pane.dividerLocation }
                    }
                }
                val frame = showInFrame(split) { split.dividerLocation = 200 }
                try {
                    onUiThreadGet { listOf(split.dividerLocation, seen.last(), split.divider.x) } shouldBe listOf(200, 200, 200)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("calling setDividerLocation(150) before showing keeps the divider at 150 after showing") {
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("Left"), WLabel("Right")).also { it.dividerLocation = 150 }
                }
                val frame = showInFrame(split)
                try {
                    onUiThreadGet { split.dividerLocation to split.divider.x } shouldBe (150 to 150)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("in a vertical split with resizeWeight 0.7 and setDividerLocation(0.7) given before showing, the divider is at 70% of the shown height") {
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WTextPane().also { it.text = "Editor" }, WTextPane().also { it.text = "Console" })
                        .also {
                            it.resizeWeight = 0.7
                            it.setDividerLocation(0.7)
                        }
                }
                val frame = showInFrame(split)
                try {
                    onUiThreadGet {
                        // The same formula as Swing: (int) ((height - divider size) * 0.7)
                        val expected = ((Math.floor(split.actualHeight).toInt() - split.dividerSize) * 0.7).toInt()
                        listOf(split.dividerLocation, split.divider.y) to listOf(expected, expected)
                    }.let { (actual, expected) -> actual shouldBe expected }
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setDividerLocation(0.0) and (1.0) given before showing put the divider at the ends (0 and height - divider)") {
                val top = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WLabel("Top"), WLabel("Bottom")).also { it.setDividerLocation(0.0) }
                }
                val topFrame = showInFrame(top)
                try {
                    onUiThreadGet { top.dividerLocation } shouldBe 0
                } finally {
                    onUiThread { topFrame.dispose() }
                }
                val bottom = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WLabel("Top"), WLabel("Bottom")).also { it.setDividerLocation(1.0) }
                }
                val bottomFrame = showInFrame(bottom)
                try {
                    onUiThreadGet { bottom.dividerLocation to Math.floor(bottom.actualHeight).toInt() - bottom.dividerSize }
                        .let { (actual, expected) -> actual shouldBe expected }
                } finally {
                    onUiThread { bottomFrame.dispose() }
                }
            }
        }

        context("WScrollPane scroll position") {
            /** Shows a WScrollPane containing a WPanel with 50 labels stacked vertically in CENTER of a 400 x 260 window with WBorderLayout. */
            fun showScroll(afterShow: (WScrollPane) -> Unit): Pair<WFrame, WScrollPane> = onUiThreadGet {
                val items = WPanel().also { panel -> (1..50).forEach { panel.add(WLabel("Item $it")) } }
                val scroll = WScrollPane(items)
                val frame = WFrame("BeforeShowRequestsTest").also {
                    it.layout = WBorderLayout()
                    it.add(scroll, WBorderLayout.CENTER)
                    it.setSize(400, 260)
                    it.isAutoRequestFocus = false
                    it.isVisible = true
                }
                afterShow(scroll)
                frame to scroll
            }

            test("setting viewport.viewPosition to (0, 300) right after showing keeps that position visible after layout") {
                val (frame, scroll) = showScroll { it.viewport.viewPosition = WPoint(0, 300) }
                try {
                    awaitUi { scroll.viewport.viewPosition == WPoint(0, 300) } shouldBe true
                    Thread.sleep(500) // It must not be reset by a later layout
                    onUiThreadGet { scroll.viewport.viewPosition } shouldBe WPoint(0, 300)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setting the value of the vertical scroll bar to 200 right after showing keeps that position visible after layout") {
                val (frame, scroll) = showScroll { it.verticalScrollBar.value = 200 }
                try {
                    awaitUi { scroll.viewport.viewPosition.y == 200 } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { scroll.verticalScrollBar.value } shouldBe 200
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("a position requested right after showing is replaced by a position requested next (the last request takes effect)") {
                val (frame, scroll) = showScroll {
                    it.viewport.viewPosition = WPoint(0, 300)
                    it.viewport.viewPosition = WPoint(0, 120)
                }
                try {
                    awaitUi { scroll.viewport.viewPosition == WPoint(0, 120) } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { scroll.viewport.viewPosition } shouldBe WPoint(0, 120)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }

        context("dialogs right after showing") {
            /** Shows a 400 x 260 window with one label without activating it (call on the UI thread). */
            fun shownFrame(): WFrame = WFrame("BeforeShowRequestsTest").also {
                it.add(WLabel("Main"))
                it.setSize(400, 260)
                it.isAutoRequestFocus = false
                it.isVisible = true
            }

            test("WOptionPane.showMessageDialog(frame, ...) right after showing opens without an exception and returns when closed with OK (as in Swing)") {
                val events: MutableList<String> = Collections.synchronizedList(mutableListOf())
                var frame: WFrame? = null
                WinUiUtilities.invokeLater {
                    val shown = shownFrame().also { frame = it }
                    try {
                        WOptionPane.showMessageDialog(shown, "Resume from the last session?")
                        events += "returned"
                    } catch (e: RuntimeException) {
                        events += "error:${e.javaClass.simpleName}"
                    }
                }
                try {
                    awaitOffUi { events.isNotEmpty() || frame?.let { f -> runCatching { OpenContentDialogs.isOpen(f.content) }.getOrDefault(false) } == true } shouldBe true
                    synchronized(events) { events.toList() } shouldBe emptyList()
                    Thread.sleep(300)
                    OpenContentDialogs.press(frame!!.content, "PrimaryButton")
                    awaitUi { events.isNotEmpty() } shouldBe true
                    synchronized(events) { events.toList() } shouldBe listOf("returned")
                } finally {
                    frame?.let { f -> onUiThread { f.dispose() } }
                }
            }

            test("WContentDialog.show(owner) right after showing does not throw and opens after the content loads") {
                val events: MutableList<String> = Collections.synchronizedList(mutableListOf())
                val (frame, dialog) = onUiThreadGet {
                    val frame = shownFrame()
                    val dialog = WContentDialog("Welcome").also {
                        it.closeButtonText = "Close"
                        it.addDialogListener(
                            object : WContentDialogAdapter() {
                                override fun dialogOpened(e: WContentDialogEvent) {
                                    events += "opened"
                                }
                            },
                        )
                    }
                    dialog.show(frame.content) { events += "closed:$it" }
                    frame to dialog
                }
                try {
                    awaitUi { events.isNotEmpty() } shouldBe true
                    synchronized(events) { events.toList() } shouldBe listOf("opened")
                    onUiThread { dialog.hide() }
                    awaitUi { events.size >= 2 } shouldBe true
                    synchronized(events) { events.toList() } shouldBe listOf("opened", "closed:NONE")
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("an owner not on any window still throws IllegalArgumentException") {
                onUiThreadGet {
                    runCatching { WContentDialog("x").show(WLabel("not shown")) }.exceptionOrNull()?.javaClass
                } shouldBe IllegalArgumentException::class.java
            }
        }

        context("menus and popups right after showing") {
            test("setting WMenuBarItem.isPopupMenuVisible to true right after showing opens the menu after the content loads") {
                val (frame, menu) = onUiThreadGet {
                    val menu = WMenuBarItem("File").also {
                        it.add("Open")
                        it.add("Exit")
                    }
                    val frame = WFrame("BeforeShowRequestsTest").also {
                        it.jMenuBar = WMenuBar().also { bar -> bar.add(menu) }
                        it.add(WLabel("Main"))
                        it.setSize(400, 260)
                        it.isAutoRequestFocus = false
                        it.isVisible = true
                    }
                    menu.isPopupMenuVisible = true
                    frame to menu
                }
                try {
                    awaitUi { menu.isPopupMenuVisible } shouldBe true
                    onUiThread { menu.isPopupMenuVisible = false }
                    awaitUi { !menu.isPopupMenuVisible } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("calling WMenuFlyout.show(invoker, x, y) right after showing opens the popup after the content loads") {
                val (frame, popup) = onUiThreadGet {
                    val target = WLabel("Right-click this label")
                    val popup = WMenuFlyout().also {
                        it.add("Rename")
                        it.add("Delete")
                    }
                    val frame = WFrame("BeforeShowRequestsTest").also {
                        it.add(target)
                        it.setSize(400, 260)
                        it.isAutoRequestFocus = false
                        it.isVisible = true
                    }
                    popup.show(target, 60.0, 10.0)
                    frame to popup
                }
                try {
                    awaitUi { popup.isOpen } shouldBe true
                    onUiThread { popup.hide() }
                    awaitUi { !popup.isOpen } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }

        context("flyouts, hints and the toolbar's \"...\" right after showing") {
            /** Lays out [components] with WFlowLayout in a 480 x 360 window, shows it without activating it, and calls [afterShow] (call on the UI thread). */
            fun showFrame(vararg components: WComponent, afterShow: () -> Unit): WFrame = WFrame("BeforeShowRequestsTest").also {
                it.layout = WFlowLayout()
                components.forEach { component -> it.add(component) }
                it.setSize(480, 360)
                it.setLocationRelativeTo(null)
                it.isAutoRequestFocus = false
                it.isVisible = true
                afterShow()
            }

            test("calling doClick() of a button right after showing opens the flyout set with setFlyout after layout (as with Swing's JButton)") {
                lateinit var popup: WMenuFlyout
                val frame = onUiThreadGet {
                    popup = WMenuFlyout().also {
                        it.add("Export as PDF")
                        it.add("Export as CSV")
                    }
                    val export = WButton("Export").also { it.flyout = popup }
                    showFrame(export) { export.doClick() }
                }
                try {
                    awaitUi { popup.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    onUiThread { popup.hide() }
                    awaitUi { !popup.isOpen } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("even calling doClick() of a button twice right after showing opens only one flyout after layout") {
                lateinit var popup: WMenuFlyout
                val frame = onUiThreadGet {
                    popup = WMenuFlyout().also { it.add("Export as PDF") }
                    val export = WButton("Export").also { it.flyout = popup }
                    showFrame(export) {
                        export.doClick()
                        export.doClick()
                    }
                }
                try {
                    awaitUi { popup.isOpen } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { popup.isOpen to openPopupCount(frame.content) } shouldBe (true to 1)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setting WToolTip.isOpen to true right after showing shows the hint after layout, and isOpen is true in the meantime") {
                lateinit var tip: WToolTip
                var openedRightAway = false
                val frame = onUiThreadGet {
                    val zip = WTextField(10).also { it.text = "12-345" }
                    tip = WToolTip().also { it.text = "Enter 7 digits" }
                    zip.setToolTip(tip)
                    showFrame(zip) {
                        tip.isOpen = true
                        openedRightAway = tip.isOpen
                    }
                }
                try {
                    openedRightAway shouldBe true
                    awaitUi { tip.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    Thread.sleep(500) // It must not close in a later layout
                    onUiThreadGet { tip.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    onUiThread { tip.isOpen = false }
                    awaitUi { !tip.isOpen } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setting WToolTip.isOpen to true right after showing and back to false shows no hint after layout") {
                lateinit var tip: WToolTip
                val frame = onUiThreadGet {
                    val zip = WTextField(10)
                    tip = WToolTip().also { it.text = "Enter 7 digits" }
                    zip.setToolTip(tip)
                    showFrame(zip) {
                        tip.isOpen = true
                        tip.isOpen = false
                    }
                }
                try {
                    awaitUi { frame.content.actualWidth > 0.0 } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { tip.isOpen to openPopupCount(frame.content) } shouldBe (false to 0)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setting WCommandBar.isOpen to true right after showing has the \"...\" menu open after layout") {
                lateinit var toolBar: WCommandBar
                val frame = onUiThreadGet {
                    toolBar = WCommandBar().also {
                        it.add(WAppBarButton("Add", Symbol.ADD))
                        it.addSecondaryCommand(WAppBarButton("Settings", Symbol.SETTING))
                        it.addSecondaryCommand(WAppBarButton("Help", Symbol.HELP))
                    }
                    showFrame(toolBar) { toolBar.isOpen = true }
                }
                try {
                    awaitUi { toolBar.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { toolBar.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    onUiThread { toolBar.isOpen = false }
                    awaitUi { !toolBar.isOpen } shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("calling WTeachingTip.show() right after showing opens the teaching tip right below the laid-out target after layout") {
                lateinit var tip: WTeachingTip
                lateinit var sync: WButton
                val frame = onUiThreadGet {
                    sync = WButton("Sync", Symbol.SYNC)
                    tip = WTeachingTip("New: Sync", "Click here to sync your files with the cloud.").also {
                        it.target = sync
                        it.actionButtonText = "Try it"
                        it.closeButtonText = "Got it"
                    }
                    showFrame(sync, WLabel(), tip) { tip.show() }
                }
                try {
                    awaitUi { tip.isOpen && openPopupCount(frame.content) > 0 } shouldBe true
                    Thread.sleep(1_000) // Wait for the opening animation and later layouts
                    onUiThreadGet {
                        val bounds = checkNotNull(firstOpenPopupBounds(frame.content)) { "the teaching tip's Popup is not open" }
                        val target = Xaml.positionInRoot(sync.uiElement)
                        // The teaching tip's center is at the target's center (laid out in the horizontal middle by WFlowLayout), and its top is below the target's bottom
                        val centerGap = Math.abs(bounds[0] + bounds[2] / 2 - (target[0] + sync.actualWidth / 2))
                        val below = bounds[1] >= target[1] + sync.actualHeight - 1.0
                        "centerGap=${centerGap.toInt()} below=$below" to (centerGap < 20.0 && below)
                    }.let { (detail, ok) -> (detail to ok) shouldBe (detail to true) }
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }
    }
}
