package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.ink.DefaultInkStrokeModel
import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkRect
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.ink.InkStrokeModelEvent
import com.appkitbox.winui4k.ink.InkTransform
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.winui.InkInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Tests WInkCanvas: synchronization between the model and the native InkStrokeContainer; selection, moving, deletion,
 * the clipboard, saving, and loading; and the WInkPresenter settings.
 * Pen input cannot be reproduced, so the state "the OS collected / erased strokes" is created by manipulating the
 * container directly, and the body of the StrokesCollected / StrokesErased handling (collectStrokes / eraseStrokes)
 * is called to verify it.
 */
class WInkCanvasTest : FunSpec() {
    /** A two-point stroke extending 10 DIPs down and to the right, distinguishable by the x coordinate of its start point. */
    private fun stroke(x: Double, y: Double = 20.0) = InkStroke(listOf(InkPoint(x, y), InkPoint(x + 10.0, y + 10.0)))

    private fun xs(strokes: List<InkStroke>) = strokes.map { it.points[0].x }

    /** Saves the canvas's native strokes as ISF, loads them into a new canvas, and returns them in order. */
    private fun nativeStrokes(canvas: WInkCanvas): List<InkStroke> {
        val bytes = ByteArrayOutputStream().also { canvas.write(it, InkPersistenceFormat.ISF) }.toByteArray()
        return WInkCanvas().also { it.read(ByteArrayInputStream(bytes)) }.model.getStrokes()
    }

    /** Records the model's events as (type, firstIndex, lastIndex). */
    private fun WInkCanvas.recordModelEvents(): MutableList<List<Int>> {
        val events = mutableListOf<List<Int>>()
        model.addInkStrokeModelListener { e -> events += listOf(e.type, e.firstIndex, e.lastIndex) }
        return events
    }

    /** Creates the native object for [stroke] and adds it to the canvas's container (reproducing the OS having collected pen strokes). */
    private fun addNatively(canvas: WInkCanvas, stroke: InkStroke): ComPtr {
        val native = InkNative.createStroke(stroke)
        canvas.container.call(InkInterop.IInkStrokeContainer_AddStroke, native)
        return native
    }

    init {
        test("passing a model with initial contents displays its strokes as native strokes in the same order") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                xs(nativeStrokes(canvas))
            } shouldBe listOf(10.0, 100.0)
        }

        test("adding to, inserting into, and removing from the model are reflected in the native order") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                canvas.model.addStroke(stroke(200.0))
                canvas.model.insertStroke(0, stroke(5.0))
                canvas.model.removeStroke(2)
                xs(canvas.model.getStrokes()) to xs(nativeStrokes(canvas))
            } shouldBe (listOf(5.0, 10.0, 200.0) to listOf(5.0, 10.0, 200.0))
        }

        test("changing the point transform with setStroke moves that stroke's displayed position without changing the order") {
            val (before, after, order) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                val before = canvas.getStrokeBounds(0).x
                canvas.model.setStroke(0, canvas.model.getStroke(0).withPointTransform(InkTransform.translation(50.0, 0.0)))
                Triple(before, canvas.getStrokeBounds(0).x, nativeStrokes(canvas).map { it.getPointBounds().x })
            }
            after shouldBe (before + 50.0 plusOrMinus 0.01)
            order shouldBe listOf(60.0, 100.0)
        }

        test("changing the drawing attributes with setStroke also changes the native stroke's drawing attributes") {
            val red = InkDrawingAttributes.builder().color(WColor.RED).size(6.0).build()
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0))))
                canvas.model.setStroke(0, canvas.model.getStroke(0).withDrawingAttributes(red))
                nativeStrokes(canvas).single().drawingAttributes.color
            } shouldBe WColor.RED
        }

        test("changing the points with setStroke keeps the order even though the native stroke is recreated") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0), stroke(200.0))))
                canvas.model.setStroke(1, stroke(150.0))
                xs(nativeStrokes(canvas))
            } shouldBe listOf(10.0, 150.0, 200.0)
        }

        test("clear and replacing everything (setStrokes) are reflected natively") {
            val (afterClear, afterSet) = onUiThreadGet {
                val model = DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0)))
                val canvas = WInkCanvas(model)
                model.clear()
                val afterClear = nativeStrokes(canvas).size to canvas.boundingRect
                model.setStrokes(listOf(stroke(1.0), stroke(2.0), stroke(3.0)))
                afterClear to xs(nativeStrokes(canvas))
            }
            afterClear shouldBe (0 to InkRect(0.0, 0.0, 0.0, 0.0))
            afterSet shouldBe listOf(1.0, 2.0, 3.0)
        }

        test("replacing model displays the new model, and changes to the old model do not affect the display") {
            onUiThreadGet {
                val old = DefaultInkStrokeModel(listOf(stroke(10.0)))
                val canvas = WInkCanvas(old)
                canvas.model = DefaultInkStrokeModel(listOf(stroke(50.0), stroke(60.0)))
                old.addStroke(stroke(70.0))
                xs(nativeStrokes(canvas))
            } shouldBe listOf(50.0, 60.0)
        }

        test("sharing one model between two canvases shows model changes on both") {
            onUiThreadGet {
                val model = DefaultInkStrokeModel()
                val first = WInkCanvas(model)
                val second = WInkCanvas(model)
                model.addStroke(stroke(10.0))
                model.addStroke(stroke(20.0))
                model.removeStroke(0)
                xs(nativeStrokes(first)) to xs(nativeStrokes(second))
            } shouldBe (listOf(20.0) to listOf(20.0))
        }

        test("drawn strokes (collectStrokes) are appended to the model without duplicating the native strokes, and listeners are notified") {
            val received = mutableListOf<List<Double>>()
            val (model, native) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0))))
                canvas.inkPresenter.addStrokesCollectedListener { e -> received += xs(e.strokes) }
                canvas.collectStrokes(listOf(addNatively(canvas, stroke(30.0))))
                xs(canvas.model.getStrokes()) to xs(nativeStrokes(canvas))
            }
            model shouldBe listOf(10.0, 30.0)
            native shouldBe listOf(10.0, 30.0)
            received shouldBe listOf(listOf(30.0))
        }

        test("adding drawn strokes to a shared model also shows them on the other canvas") {
            onUiThreadGet {
                val model = DefaultInkStrokeModel()
                val first = WInkCanvas(model)
                val second = WInkCanvas(model)
                first.collectStrokes(listOf(addNatively(first, stroke(30.0))))
                xs(nativeStrokes(first)) to xs(nativeStrokes(second))
            } shouldBe (listOf(30.0) to listOf(30.0))
        }

        test("strokes that disappeared from the container before the notification arrived are not added to the model") {
            onUiThreadGet {
                val canvas = WInkCanvas()
                val native = InkNative.createStroke(stroke(30.0)) // A stroke that is not in the container
                canvas.collectStrokes(listOf(native))
                canvas.model.getStrokeCount()
            } shouldBe 0
        }

        test("erased strokes (eraseStrokes) are removed from the model, and listeners are notified of the erased strokes") {
            val received = mutableListOf<List<Double>>()
            val (model, native) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(20.0), stroke(30.0))))
                canvas.inkPresenter.addStrokesErasedListener { e -> received += xs(e.strokes) }
                // Create the state of strokes erased by the eraser: select only the second one, delete it, and notify its native object
                canvas.setStrokeSelected(1, true)
                val erased = canvas.container.getPtr(InkInterop.IInkStrokeContainer_GetStrokes).let { view ->
                    InkNative.readVectorView(view).also { view.release() }
                }
                InkNative.callForRect(canvas.container, InkInterop.IInkStrokeContainer_DeleteSelected)
                canvas.eraseStrokes(listOf(erased[1]))
                erased[0].release()
                erased[2].release()
                xs(canvas.model.getStrokes()) to xs(nativeStrokes(canvas))
            }
            model shouldBe listOf(10.0, 30.0)
            native shouldBe listOf(10.0, 30.0)
            received shouldBe listOf(listOf(20.0))
        }

        test("setStrokeSelected / selectAll / clearSelection change the selection state") {
            val (single, all, none) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0), stroke(200.0))))
                canvas.setStrokeSelected(1, true)
                val single = canvas.getSelectedStrokeIndices().toList() to canvas.isStrokeSelected(1)
                canvas.selectAll()
                val all = canvas.getSelectedStrokeIndices().toList()
                canvas.clearSelection()
                Triple(single, all, canvas.getSelectedStrokeIndices().toList())
            }
            single shouldBe (listOf(1) to true)
            all shouldBe listOf(0, 1, 2)
            none shouldBe emptyList()
        }

        test("selectWithLine selects strokes that intersect the line segment, and selectWithPolyLine selects the enclosed strokes") {
            val (byLine, byPolyLine) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0), stroke(200.0))))
                canvas.selectWithLine(105.0, 0.0, 105.0, 100.0)
                val byLine = canvas.getSelectedStrokeIndices().toList()
                canvas.clearSelection()
                canvas.selectWithPolyLine(listOf(InkPoint(0.0, 0.0), InkPoint(50.0, 0.0), InkPoint(50.0, 60.0), InkPoint(0.0, 60.0)))
                byLine to canvas.getSelectedStrokeIndices().toList()
            }
            byLine shouldBe listOf(1)
            byPolyLine shouldBe listOf(0)
        }

        test("moveSelected moves the point transform of the selected strokes and reflects it in the model") {
            val (transforms, events, nativeX) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                val events = canvas.recordModelEvents()
                canvas.setStrokeSelected(1, true)
                canvas.moveSelected(30.0, 5.0)
                Triple(canvas.model.getStrokes().map { it.pointTransform }, events, nativeStrokes(canvas).map { it.getPointBounds().x })
            }
            transforms shouldBe listOf(InkTransform.IDENTITY, InkTransform.translation(30.0, 5.0))
            events shouldBe listOf(listOf(InkStrokeModelEvent.UPDATE, 1, 1))
            nativeX shouldBe listOf(10.0, 130.0)
        }

        test("deleteSelected deletes the selected strokes and also removes them from the model") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0), stroke(200.0))))
                canvas.setStrokeSelected(0, true)
                canvas.setStrokeSelected(2, true)
                canvas.deleteSelected()
                xs(canvas.model.getStrokes()) to xs(nativeStrokes(canvas))
            } shouldBe (listOf(100.0) to listOf(100.0))
        }

        test("removing a stroke from the model keeps the selection state of the other strokes") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0), stroke(200.0))))
                canvas.setStrokeSelected(0, true)
                canvas.model.removeStroke(1)
                canvas.getSelectedStrokeIndices().toList()
            } shouldBe listOf(0)
        }

        test("copying the selection to the clipboard and pasting it appends the pasted strokes to the end of the model") {
            val canvas = onUiThreadGet {
                WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0)))).also { it.setStrokeSelected(0, true) }
            }
            // Other processes (such as clipboard history) may have the clipboard open temporarily,
            // so retry a few times until the copy takes effect
            var canPaste = false
            var attempts = 0
            while (!canPaste && attempts++ < CLIPBOARD_ATTEMPTS) {
                canPaste = onUiThreadGet {
                    runCatching { canvas.copySelectedToClipboard() }
                    canvas.canPasteFromClipboard()
                }
                if (!canPaste) Thread.sleep(CLIPBOARD_RETRY_MILLIS)
            }
            canPaste shouldBe true
            val (model, native) = onUiThreadGet {
                canvas.pasteFromClipboard(300.0, 300.0)
                canvas.model.getStrokeCount() to nativeStrokes(canvas).size
            }
            model shouldBe 3
            native shouldBe 3
        }

        test("reading back written ISF / GIF puts strokes with the same points into the model") {
            val original = listOf(stroke(10.0), stroke(100.0))
            val (fromIsf, fromGif) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(original))
                val isf = ByteArrayOutputStream().also { canvas.write(it, InkPersistenceFormat.ISF) }.toByteArray()
                val gif = ByteArrayOutputStream().also { canvas.write(it) }.toByteArray()
                val fromIsf = WInkCanvas().also { it.read(ByteArrayInputStream(isf)) }.model.getStrokes()
                val fromGif = WInkCanvas().also { it.read(ByteArrayInputStream(gif)) }.model.getStrokes()
                fromIsf to fromGif
            }
            fromIsf.map { s -> s.points.map { it.x to it.y } } shouldBe original.map { s -> s.points.map { it.x to it.y } }
            fromGif.map { s -> s.points.map { it.x to it.y } } shouldBe original.map { s -> s.points.map { it.x to it.y } }
        }

        test("writing in GIF format writes the bytes of a GIF image") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0))))
                String(ByteArrayOutputStream().also { canvas.write(it) }.toByteArray().copyOfRange(0, 6))
            } shouldBe "GIF89a"
        }

        test("read replaces the existing strokes with the loaded contents") {
            onUiThreadGet {
                val source = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(300.0))))
                val bytes = ByteArrayOutputStream().also { source.write(it, InkPersistenceFormat.ISF) }.toByteArray()
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                canvas.read(ByteArrayInputStream(bytes))
                xs(canvas.model.getStrokes()) to xs(nativeStrokes(canvas))
            } shouldBe (listOf(300.0) to listOf(300.0))
        }

        test("positions can be looked up from stroke IDs, and strokes added in code have pointer ID 0 and are not recognized") {
            val (index, pointerId, recognized) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                Triple(canvas.indexOfStrokeId(canvas.getStrokeId(1)), canvas.getStrokePointerId(1), canvas.isStrokeRecognized(1))
            }
            index shouldBe 1
            pointerId shouldBe 0
            recognized shouldBe false
        }

        test("indexOfStrokeId is -1 for a nonexistent ID, and an out-of-range index throws") {
            val canvas = onUiThreadGet { WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0)))) }
            onUiThreadGet { canvas.indexOfStrokeId(Int.MAX_VALUE) } shouldBe -1
            onUiThreadGet { runCatching { canvas.getStrokeBounds(1) }.exceptionOrNull() is IndexOutOfBoundsException } shouldBe true
        }

        test("the bounding rectangle and the rendering segments contain the stroke's position") {
            val (bounds, segments) = onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0), stroke(100.0))))
                canvas.boundingRect to canvas.getRenderingSegments(1)
            }
            (bounds.x <= 10.0 && bounds.x + bounds.width >= 110.0) shouldBe true
            segments.last().x shouldBe (110.0 plusOrMinus 0.01)
            segments.last().y shouldBe (30.0 plusOrMinus 0.01)
        }

        test("InkCanvas accepts only pen input by default, and settings such as input devices, input enabled, and processing mode round-trip") {
            val (defaults, changed) = onUiThreadGet {
                val presenter = WInkCanvas().inkPresenter
                val defaults = listOf(presenter.inputDeviceTypes, presenter.isInputEnabled, presenter.inputProcessingMode)
                presenter.inputDeviceTypes = setOf(CoreInputDeviceType.MOUSE, CoreInputDeviceType.TOUCH)
                presenter.isInputEnabled = false
                presenter.inputProcessingMode = InkInputProcessingMode.ERASING
                presenter.rightDragAction = InkInputRightDragAction.ALLOW_PROCESSING
                presenter.highContrastAdjustment = InkHighContrastAdjustment.USE_ORIGINAL_COLORS
                presenter.isPrimaryBarrelButtonInputEnabled = false
                presenter.isEraserInputEnabled = false
                presenter.setPredefinedConfiguration(InkPresenterPredefinedConfiguration.SIMPLE_MULTIPLE_POINTER)
                defaults to listOf(
                    presenter.inputDeviceTypes,
                    presenter.isInputEnabled,
                    presenter.inputProcessingMode,
                    presenter.rightDragAction,
                    presenter.highContrastAdjustment,
                    presenter.isPrimaryBarrelButtonInputEnabled,
                    presenter.isEraserInputEnabled,
                )
            }
            defaults shouldBe listOf(setOf(CoreInputDeviceType.PEN), true, InkInputProcessingMode.INKING)
            changed shouldBe listOf(
                setOf(CoreInputDeviceType.MOUSE, CoreInputDeviceType.TOUCH),
                false,
                InkInputProcessingMode.ERASING,
                InkInputRightDragAction.ALLOW_PROCESSING,
                InkHighContrastAdjustment.USE_ORIGINAL_COLORS,
                false,
                false,
            )
        }

        test("defaultDrawingAttributes round-trips pen and pencil drawing attributes") {
            val pen = InkDrawingAttributes.builder().color(WColor.BLUE).size(8.0, 4.0).drawAsHighlighter(true).fitToCurve(false).build()
            val pencil = InkDrawingAttributes.pencilBuilder().color(WColor.GREEN).size(3.0).pencilOpacity(0.4).build()
            val (readPen, readPencil) = onUiThreadGet {
                val presenter = WInkCanvas().inkPresenter
                presenter.defaultDrawingAttributes = pen
                val readPen = presenter.defaultDrawingAttributes
                presenter.defaultDrawingAttributes = pencil
                readPen to presenter.defaultDrawingAttributes
            }
            readPen shouldBe pen
            readPencil shouldBe pencil
        }

        test("listeners for stroke input and unprocessed input can be added and removed (the native events can be subscribed to)") {
            onUiThreadGet {
                val presenter = WInkCanvas().inkPresenter
                val strokeListener = object : InkStrokeInputListener {}
                val unprocessedListener = object : InkUnprocessedInputListener {}
                presenter.addStrokeInputListener(strokeListener)
                presenter.addUnprocessedInputListener(unprocessedListener)
                presenter.removeStrokeInputListener(strokeListener)
                presenter.removeUnprocessedInputListener(unprocessedListener)
                presenter.addStrokeInputListener(strokeListener)
                true
            } shouldBe true
        }

        test("after activateCustomDrying the canvas does not sync with the model, stroke operations throw, and the same synchronizer is returned") {
            val (active, same, emptyDry) = onUiThreadGet {
                val canvas = WInkCanvas()
                val synchronizer = canvas.inkPresenter.activateCustomDrying()
                canvas.model.addStroke(stroke(10.0)) // Nothing happens because it does not sync
                // BeginDry fails with E_UNEXPECTED in WinUI when there are no finished strokes
                val emptyDry = runCatching { synchronizer.beginDry() }.exceptionOrNull() as? WindowsRuntimeException
                Triple(canvas.inkPresenter.isCustomDryingActive, canvas.inkPresenter.activateCustomDrying() === synchronizer, emptyDry?.hresult)
            }
            active shouldBe true
            same shouldBe true
            emptyDry shouldBe 0x8000FFFF.toInt()
            val canvas = onUiThreadGet { WInkCanvas().also { it.inkPresenter.activateCustomDrying() } }
            onUiThreadGet { runCatching { canvas.selectAll() }.exceptionOrNull() is IllegalStateException } shouldBe true
            onUiThreadGet { runCatching { canvas.write(ByteArrayOutputStream()) }.exceptionOrNull() is IllegalStateException } shouldBe true
        }

        test("activateCustomDrying after strokes are displayed is rejected by WinUI") {
            onUiThreadGet {
                val canvas = WInkCanvas(DefaultInkStrokeModel(listOf(stroke(10.0))))
                runCatching { canvas.inkPresenter.activateCustomDrying() }.exceptionOrNull() is WindowsRuntimeException
            } shouldBe true
        }

        test("strokes drawn after activateCustomDrying do not go into the model, but listeners are notified") {
            val received = mutableListOf<List<Double>>()
            onUiThreadGet {
                val canvas = WInkCanvas()
                canvas.inkPresenter.activateCustomDrying()
                canvas.inkPresenter.addStrokesCollectedListener { e -> received += xs(e.strokes) }
                canvas.collectStrokes(listOf(InkNative.createStroke(stroke(30.0))))
                canvas.model.getStrokeCount()
            } shouldBe 0
            received shouldBe listOf(listOf(30.0))
        }
    }

    private companion object {
        const val CLIPBOARD_ATTEMPTS = 20
        const val CLIPBOARD_RETRY_MILLIS = 250L
    }
}
