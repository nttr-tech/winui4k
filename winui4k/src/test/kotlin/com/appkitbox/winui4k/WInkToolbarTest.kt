package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.ink.DefaultInkStrokeModel
import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.ink.PenTipShape
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests WInkToolbar and each of its buttons (tool, pen, toggle, menu, custom pen, flyout items). */
class WInkToolbarTest : FunSpec() {
    /** Places a toolbar set up by [configure] in the shared window, runs [block] after it loads (its template is applied), and removes it. */
    private fun <T> withLoadedToolbar(configure: (WInkToolbar) -> Unit = {}, block: (WInkToolbar) -> T): T {
        val toolbar = onUiThreadGet { WInkToolbar().also(configure) }
        UiTestHarness.attachAndAwaitLoaded(toolbar)
        try {
            return onUiThreadGet { block(toolbar) }
        } finally {
            UiTestHarness.detach(toolbar)
        }
    }

    /** A custom pen that returns the recorded arguments. Its drawing attributes use a rectangular pen tip whose height is twice the selected size. */
    private class RecordingPen : WInkToolbarCustomPen() {
        val calls = mutableListOf<Pair<WColor?, Double>>()

        override fun createInkDrawingAttributesCore(color: WColor?, strokeWidth: Double): InkDrawingAttributes {
            calls += color to strokeWidth
            return InkDrawingAttributes.builder()
                .color(color ?: WColor.BLACK)
                .penTip(PenTipShape.RECTANGLE)
                .size(strokeWidth, strokeWidth * 2)
                .build()
        }
    }

    init {
        test("before loading there are no built-in buttons, and after loading wrappers of the matching button types are obtained") {
            val before = onUiThreadGet { WInkToolbar().getToolButton(InkToolbarTool.BALLPOINT_PEN) }
            before shouldBe null
            val buttons = withLoadedToolbar { toolbar ->
                listOf(
                    toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN),
                    toolbar.getToolButton(InkToolbarTool.PENCIL),
                    toolbar.getToolButton(InkToolbarTool.HIGHLIGHTER),
                    toolbar.getToolButton(InkToolbarTool.ERASER),
                    toolbar.getMenuButton(InkToolbarMenuKind.STENCIL),
                )
            }
            buttons[0].shouldBeInstanceOf<WInkToolbarBallpointPenButton>()
            buttons[1].shouldBeInstanceOf<WInkToolbarPencilButton>()
            buttons[2].shouldBeInstanceOf<WInkToolbarHighlighterButton>()
            buttons[3].shouldBeInstanceOf<WInkToolbarEraserButton>()
            buttons[4].shouldBeInstanceOf<WInkToolbarStencilButton>()
        }

        test("the same native button returns the same wrapper, and the first tool is the ballpoint pen") {
            withLoadedToolbar { toolbar ->
                val pen = toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN)
                Triple(pen, toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN), toolbar.activeTool)
            }.let { (pen, again, active) ->
                again shouldBeSameInstanceAs pen
                active shouldBeSameInstanceAs pen
            }
        }

        test("the tool kind and menu kind of each button can be determined") {
            withLoadedToolbar { toolbar ->
                listOf(
                    toolbar.getToolButton(InkToolbarTool.PENCIL)!!.toolKind,
                    toolbar.getToolButton(InkToolbarTool.ERASER)!!.toolKind,
                    toolbar.getMenuButton(InkToolbarMenuKind.STENCIL)!!.menuKind,
                )
            } shouldBe listOf(InkToolbarTool.PENCIL, InkToolbarTool.ERASER, InkToolbarMenuKind.STENCIL)
        }

        test("setting activeTool to the eraser puts the target canvas in erase mode and notifies ActiveToolChanged") {
            var changed = 0
            val canvas = onUiThreadGet { WInkCanvas() }
            val mode = withLoadedToolbar({ it.targetInkCanvas = canvas }) { toolbar ->
                toolbar.addActiveToolChangedListener { changed++ }
                toolbar.activeTool = toolbar.getToolButton(InkToolbarTool.ERASER)
                canvas.inkPresenter.inputProcessingMode
            }
            mode shouldBe InkInputProcessingMode.ERASING
            (changed >= 1) shouldBe true
        }

        test("a pen button's palette, selected color, and size can be read and written and are reflected in the toolbar's and canvas's drawing attributes") {
            val canvas = onUiThreadGet { WInkCanvas() }
            val result = withLoadedToolbar({ it.targetInkCanvas = canvas }) { toolbar ->
                val pen = toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN) as WInkToolbarPenButton
                val defaultPaletteSize = pen.palette.size
                pen.palette = listOf(WColor.RED, WColor.GREEN, WColor.BLUE)
                pen.selectedBrushIndex = 2
                pen.selectedStrokeWidth = 6.0
                listOf(
                    defaultPaletteSize > 0,
                    pen.palette,
                    pen.selectedColor,
                    pen.selectedStrokeWidth,
                    toolbar.inkDrawingAttributes?.color,
                    canvas.inkPresenter.defaultDrawingAttributes.color,
                )
            }
            result shouldBe listOf(true, listOf(WColor.RED, WColor.GREEN, WColor.BLUE), WColor.BLUE, 6.0, WColor.BLUE, WColor.BLUE)
        }

        test("a pen button's size range and a tool button's extension glyph can be read and written") {
            withLoadedToolbar { toolbar ->
                val pen = toolbar.getToolButton(InkToolbarTool.PENCIL) as WInkToolbarPenButton
                pen.minStrokeWidth = 2.0
                pen.maxStrokeWidth = 30.0
                pen.isExtensionGlyphShown = true
                listOf(pen.minStrokeWidth, pen.maxStrokeWidth, pen.isExtensionGlyphShown)
            } shouldBe listOf(2.0, 30.0, true)
        }

        test("the eraser's \"Erase all ink\" notifies listeners and then empties the target canvas's model") {
            val seenByListener = mutableListOf<Int>()
            val (canvas, toolbar) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(InkStroke(listOf(InkPoint(1.0, 1.0), InkPoint(9.0, 9.0))))))
                val toolbar = WInkToolbar().also { it.targetInkCanvas = canvas }
                toolbar.addEraseAllClickedListener { seenByListener += canvas.model.getStrokeCount() }
                canvas to toolbar
            }
            onUiThread { toolbar.onEraseAllClicked() }
            seenByListener shouldBe listOf(1)
            onUiThreadGet { canvas.model.getStrokeCount() } shouldBe 0
        }

        test("the target of \"Erase all ink\" prefers TargetInkPresenter over TargetInkCanvas") {
            val (presenterCanvas, otherCanvas) = onUiThreadGet {
                val stroke = InkStroke(listOf(InkPoint(1.0, 1.0)))
                val presenterCanvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke)))
                val otherCanvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke)))
                val toolbar = WInkToolbar()
                toolbar.targetInkCanvas = otherCanvas
                toolbar.targetInkPresenter = presenterCanvas.inkPresenter
                toolbar.onEraseAllClicked()
                presenterCanvas to otherCanvas
            }
            onUiThreadGet { presenterCanvas.model.getStrokeCount() to otherCanvas.model.getStrokeCount() } shouldBe (0 to 1)
        }

        test("the visibility of the eraser's \"Erase all ink\" can be toggled") {
            withLoadedToolbar { toolbar ->
                val eraser = toolbar.getToolButton(InkToolbarTool.ERASER) as WInkToolbarEraserButton
                val before = eraser.isClearAllVisible
                eraser.isClearAllVisible = false
                before to eraser.isClearAllVisible
            } shouldBe (true to false)
        }

        test("a custom pen's createInkDrawingAttributes creates drawing attributes with the overridden Core") {
            val pen = onUiThreadGet { RecordingPen() }
            val attributes = onUiThreadGet { pen.createInkDrawingAttributes(WColor.RED, 5.0) }
            pen.calls shouldBe listOf(WColor.RED to 5.0)
            listOf(attributes.color, attributes.penTip, attributes.width, attributes.height) shouldBe
                listOf(WColor.RED, PenTipShape.RECTANGLE, 5.0, 10.0)
        }

        test("adding and selecting a custom pen button passes the custom pen's drawing attributes to the toolbar and canvas") {
            val pen = onUiThreadGet { RecordingPen() }
            val canvas = onUiThreadGet { WInkCanvas() }
            val button = onUiThreadGet {
                WInkToolbarCustomPenButton().also {
                    it.customPen = pen
                    it.configurationContent = WInkToolbarPenConfigurationControl()
                    it.palette = listOf(WColor.GREEN)
                }
            }
            val (found, tip, kind) = withLoadedToolbar({
                it.targetInkCanvas = canvas
                it.add(button)
            }) { toolbar ->
                toolbar.activeTool = button
                Triple(
                    toolbar.activeTool,
                    listOf(canvas.inkPresenter.defaultDrawingAttributes.penTip, toolbar.inkDrawingAttributes?.penTip),
                    button.toolKind,
                )
            }
            found shouldBeSameInstanceAs button
            tip shouldBe listOf(PenTipShape.RECTANGLE, PenTipShape.RECTANGLE)
            kind shouldBe InkToolbarTool.CUSTOM_PEN
            (pen.calls.isNotEmpty()) shouldBe true
        }

        test("selecting a custom tool puts the target canvas in a mode that does not process input as ink") {
            val canvas = onUiThreadGet { WInkCanvas() }
            val tool = onUiThreadGet { WInkToolbarCustomToolButton().also { it.configurationContent = WLabel("Lasso") } }
            withLoadedToolbar({
                it.targetInkCanvas = canvas
                it.add(tool)
            }) { toolbar ->
                toolbar.activeTool = tool
                tool.toolKind to canvas.inkPresenter.inputProcessingMode
            } shouldBe (InkToolbarTool.CUSTOM_TOOL to InkInputProcessingMode.NONE)
        }

        test("an added ruler button is returned by GetToggleButton as the same button, but a custom toggle is not (WinUI behavior)") {
            val ruler = onUiThreadGet { WInkToolbarRulerButton() }
            val custom = onUiThreadGet { WInkToolbarCustomToggleButton() }
            withLoadedToolbar({
                it.add(ruler)
                it.add(custom)
            }) { toolbar ->
                Triple(toolbar.getToggleButton(InkToolbarToggle.RULER), toolbar.getToggleButton(InkToolbarToggle.CUSTOM), custom.toggleKind)
            }.let { (foundRuler, foundCustom, kind) ->
                foundRuler shouldBeSameInstanceAs ruler
                foundCustom shouldBe null
                kind shouldBe InkToolbarToggle.CUSTOM
            }
        }

        test("custom pens and tools are not returned by GetToolButton (WinUI behavior) but are included in getItems") {
            val tool = onUiThreadGet { WInkToolbarCustomToolButton() }
            withLoadedToolbar({ it.add(tool) }) { toolbar ->
                toolbar.getToolButton(InkToolbarTool.CUSTOM_TOOL) to toolbar.getItems()
            }.let { (found, items) ->
                found shouldBe null
                items shouldBe listOf(tool)
            }
        }

        test("the kind of a built-in button created on its own can also be determined") {
            onUiThreadGet {
                listOf(
                    WInkToolbarBallpointPenButton().toolKind,
                    WInkToolbarPencilButton().toolKind,
                    WInkToolbarHighlighterButton().toolKind,
                    WInkToolbarEraserButton().toolKind,
                    WInkToolbarRulerButton().toggleKind,
                    WInkToolbarStencilButton().menuKind,
                )
            } shouldBe listOf(
                InkToolbarTool.BALLPOINT_PEN,
                InkToolbarTool.PENCIL,
                InkToolbarTool.HIGHLIGHTER,
                InkToolbarTool.ERASER,
                InkToolbarToggle.RULER,
                InkToolbarMenuKind.STENCIL,
            )
        }

        test("the stencil button's selection and item visibility can be read and written, and the ruler and protractor properties are null") {
            withLoadedToolbar { toolbar ->
                val stencil = toolbar.getMenuButton(InkToolbarMenuKind.STENCIL) as WInkToolbarStencilButton
                stencil.selectedStencil = InkToolbarStencilKind.PROTRACTOR
                stencil.isRulerItemVisible = false
                stencil.isExtensionGlyphShown = false
                listOf(stencil.selectedStencil, stencil.isRulerItemVisible, stencil.isProtractorItemVisible, stencil.isExtensionGlyphShown, stencil.ruler, stencil.protractor)
            } shouldBe listOf(InkToolbarStencilKind.PROTRACTOR, false, true, false, null, null)
        }

        test("turning on the stencil button notifies IsStencilButtonCheckedChanged") {
            val events = mutableListOf<InkToolbarStencilKind>()
            val canvas = onUiThreadGet { WInkCanvas() }
            withLoadedToolbar({ it.targetInkCanvas = canvas }) { toolbar ->
                toolbar.addStencilCheckedListener { events += it.stencilKind }
                toolbar.isStencilButtonChecked = true
                toolbar.isStencilButtonChecked
            } shouldBe true
            events shouldBe listOf(InkToolbarStencilKind.RULER)
        }

        test("the toolbar orientation, flyout orientation, built-in buttons, and ruler button state can be read and written") {
            onUiThreadGet {
                val toolbar = WInkToolbar()
                val defaults = listOf(toolbar.orientation, toolbar.buttonFlyoutPlacement, toolbar.initialControls)
                toolbar.orientation = Orientation.VERTICAL
                toolbar.buttonFlyoutPlacement = InkToolbarButtonFlyoutPlacement.RIGHT
                toolbar.initialControls = InkToolbarInitialControls.PENS_ONLY
                toolbar.isRulerButtonChecked = true
                defaults to listOf(toolbar.orientation, toolbar.buttonFlyoutPlacement, toolbar.initialControls, toolbar.isRulerButtonChecked)
            } shouldBe (
                listOf(Orientation.HORIZONTAL, InkToolbarButtonFlyoutPlacement.AUTO, InkToolbarInitialControls.ALL) to
                    listOf(Orientation.VERTICAL, InkToolbarButtonFlyoutPlacement.RIGHT, InkToolbarInitialControls.PENS_ONLY, true)
                )
        }

        test("with the setting that places no built-in buttons, only the added buttons are present") {
            val pen = onUiThreadGet { WInkToolbarPencilButton() }
            withLoadedToolbar({
                it.initialControls = InkToolbarInitialControls.NONE
                it.add(pen)
            }) { toolbar ->
                toolbar.getToolButton(InkToolbarTool.BALLPOINT_PEN) to toolbar.getToolButton(InkToolbarTool.PENCIL)
            }.let { (ballpoint, pencil) ->
                ballpoint shouldBe null
                pencil shouldBeSameInstanceAs pen
            }
        }

        test("add / add at a position / remove / removeAll change the order of the added buttons") {
            onUiThreadGet {
                val toolbar = WInkToolbar()
                val a = WInkToolbarCustomToolButton()
                val b = WInkToolbarCustomToggleButton()
                val c = WInkToolbarPencilButton()
                toolbar.add(a)
                toolbar.add(b)
                toolbar.add(c, 0)
                val afterAdd = toolbar.getItems()
                toolbar.remove(a)
                val afterRemove = toolbar.getItems()
                toolbar.removeAll()
                Triple(afterAdd == listOf(c, a, b), afterRemove == listOf(c, b), toolbar.getItems().isEmpty())
            } shouldBe Triple(true, true, true)
        }

        test("flyout items can read and write their kind and checked state, and notify changes in the checked state") {
            val received = mutableListOf<Boolean>()
            val item = onUiThreadGet { WInkToolbarFlyoutItem("Item") }
            UiTestHarness.attachAndAwaitLoaded(item)
            try {
                onUiThreadGet {
                    item.kind = InkToolbarFlyoutItemKind.CHECK
                    item.addItemListener { received += it }
                    item.isChecked = true
                    item.isChecked = false
                    listOf(item.kind, item.text)
                } shouldBe listOf(InkToolbarFlyoutItemKind.CHECK, "Item")
            } finally {
                UiTestHarness.detach(item)
            }
            received shouldBe listOf(true, false)
        }

        test("a pen configuration control has no target pen until it appears in a flyout") {
            onUiThreadGet { WInkToolbarPenConfigurationControl().penButton } shouldBe null
        }
    }
}
