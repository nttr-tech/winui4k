package com.appkitbox.winui4k.ink

import com.appkitbox.winui4k.WColor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.time.Instant
import kotlin.math.PI

/** Tests the value objects of the Ink model (InkPoint / InkTransform / InkDrawingAttributes / InkStroke / InkRect) (no UI needed). */
class InkValuesTest : FunSpec() {
    init {
        test("InkPoint defaults to pressure 0.5, tilt 0, and timestamp 0") {
            val point = InkPoint(1.0, 2.0)
            listOf(point.pressure, point.tiltX, point.tiltY) shouldBe listOf(0.5f, 0f, 0f)
            point.timestamp shouldBe 0L
        }

        test("InkPoint rejects pressure outside 0 to 1 and tilt outside -90 to 90 (boundary values are accepted)") {
            InkPoint(0.0, 0.0, pressure = 0f, tiltX = -90f, tiltY = 90f).pressure shouldBe 0f
            InkPoint(0.0, 0.0, pressure = 1f).pressure shouldBe 1f
            shouldThrow<IllegalArgumentException> { InkPoint(0.0, 0.0, pressure = 1.01f) }
            shouldThrow<IllegalArgumentException> { InkPoint(0.0, 0.0, pressure = -0.01f) }
            shouldThrow<IllegalArgumentException> { InkPoint(0.0, 0.0, tiltX = 90.5f) }
            shouldThrow<IllegalArgumentException> { InkPoint(0.0, 0.0, tiltY = -91f) }
        }

        test("InkTransform maps points by translation, scaling, and rotation") {
            InkTransform.IDENTITY.transform(3.0, 4.0) shouldBe InkPoint(3.0, 4.0)
            InkTransform.translation(10.0, 20.0).transform(3.0, 4.0) shouldBe InkPoint(13.0, 24.0)
            InkTransform.scale(2.0, 3.0).transform(3.0, 4.0) shouldBe InkPoint(6.0, 12.0)
            val rotated = InkTransform.rotation(PI / 2).transform(1.0, 0.0)
            rotated.x shouldBe (0.0 plusOrMinus 1e-12)
            rotated.y shouldBe (1.0 plusOrMinus 1e-12)
        }

        test("InkTransform.then applies the next transform after this one") {
            val scaleThenMove = InkTransform.scale(2.0, 2.0).then(InkTransform.translation(10.0, 0.0))
            scaleThenMove.transform(1.0, 1.0) shouldBe InkPoint(12.0, 2.0)
            val moveThenScale = InkTransform.translation(10.0, 0.0).then(InkTransform.scale(2.0, 2.0))
            moveThenScale.transform(1.0, 1.0) shouldBe InkPoint(22.0, 2.0)
            InkTransform.IDENTITY.isIdentity shouldBe true
            InkTransform.translation(1.0, 0.0).isIdentity shouldBe false
        }

        test("InkDrawingAttributes defaults are the same as the defaults of WinUI's InkDrawingAttributes") {
            val attributes = InkDrawingAttributes.DEFAULT
            attributes.kind shouldBe InkDrawingAttributesKind.DEFAULT
            attributes.color shouldBe WColor(0, 0, 0, 255)
            attributes.penTip shouldBe PenTipShape.CIRCLE
            listOf(attributes.width, attributes.height) shouldBe listOf(2.0, 2.0)
            listOf(attributes.ignorePressure, attributes.fitToCurve, attributes.drawAsHighlighter, attributes.ignoreTilt) shouldBe
                listOf(false, true, false, true)
            attributes.penTipTransform shouldBe InkTransform.IDENTITY
            attributes.pencilOpacity shouldBe 1.0
            attributes.modelerAttributes shouldBe
                InkModelerAttributes(Duration.ofMillis(15), 2540f / 96f, useVelocityBasedPressure = false)
        }

        test("values set with the builder are reflected, and rebuilding with toBuilder gives an equal value") {
            val attributes = InkDrawingAttributes.builder()
                .color(WColor.RED)
                .penTip(PenTipShape.RECTANGLE)
                .size(4.0, 8.0)
                .ignorePressure(true)
                .fitToCurve(false)
                .penTipTransform(InkTransform.rotation(1.0))
                .drawAsHighlighter(true)
                .ignoreTilt(false)
                .modelerAttributes(InkModelerAttributes(Duration.ofMillis(5), 1f, true))
                .build()
            listOf(attributes.color, attributes.penTip, attributes.width, attributes.height) shouldBe
                listOf(WColor.RED, PenTipShape.RECTANGLE, 4.0, 8.0)
            listOf(attributes.ignorePressure, attributes.fitToCurve, attributes.drawAsHighlighter, attributes.ignoreTilt) shouldBe
                listOf(true, false, true, false)
            attributes.toBuilder().build() shouldBe attributes
            attributes.toBuilder().build().hashCode() shouldBe attributes.hashCode()
            attributes.toBuilder().size(5.0).build() shouldNotBe attributes
        }

        test("the pencil builder has the kind PENCIL and can set the opacity") {
            val pencil = InkDrawingAttributes.pencilBuilder().pencilOpacity(0.5).build()
            pencil.kind shouldBe InkDrawingAttributesKind.PENCIL
            pencil.pencilOpacity shouldBe 0.5
            pencil.toBuilder().build().kind shouldBe InkDrawingAttributesKind.PENCIL
        }

        test("for the pencil, the pen tip shape, pen tip transform, and highlighter cannot be changed from their defaults (because WinUI's pencil does not accept them)") {
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.pencilBuilder().penTip(PenTipShape.RECTANGLE).build() }
            shouldThrow<IllegalArgumentException> {
                InkDrawingAttributes.pencilBuilder().penTipTransform(InkTransform.rotation(1.0)).build()
            }
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.pencilBuilder().drawAsHighlighter(true).build() }
            val pen = InkDrawingAttributes.builder().penTip(PenTipShape.RECTANGLE).drawAsHighlighter(true).build()
            pen.penTip shouldBe PenTipShape.RECTANGLE
        }

        test("the pen tip size must be positive, and the pencil opacity must be 0.01 to 5.0 (boundary values are accepted)") {
            InkDrawingAttributes.pencilBuilder().pencilOpacity(0.01).pencilOpacity(5.0).build().pencilOpacity shouldBe 5.0
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.builder().size(0.0) }
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.builder().size(1.0, -1.0) }
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.pencilBuilder().pencilOpacity(0.0) }
            shouldThrow<IllegalArgumentException> { InkDrawingAttributes.pencilBuilder().pencilOpacity(5.01) }
        }

        test("an InkStroke cannot be created with no points and is not affected by later changes to the list passed in") {
            shouldThrow<IllegalArgumentException> { InkStroke(emptyList()) }
            val points = mutableListOf(InkPoint(1.0, 2.0))
            val stroke = InkStroke(points)
            points += InkPoint(3.0, 4.0)
            stroke.points shouldBe listOf(InkPoint(1.0, 2.0))
        }

        test("an InkStroke's drawing duration cannot be negative") {
            shouldThrow<IllegalArgumentException> { InkStroke(listOf(InkPoint(0.0, 0.0)), strokeDuration = Duration.ofMillis(-1)) }
        }

        test("InkStroke's with methods return a new stroke with only the specified field changed") {
            val original = InkStroke(listOf(InkPoint(1.0, 2.0)))
            val red = InkDrawingAttributes.builder().color(WColor.RED).build()
            val started = Instant.ofEpochSecond(1_000)
            val changed = original
                .withDrawingAttributes(red)
                .withPointTransform(InkTransform.translation(1.0, 1.0))
                .withTiming(started, Duration.ofMillis(300))
                .withPoints(listOf(InkPoint(5.0, 6.0)))
            changed.points shouldBe listOf(InkPoint(5.0, 6.0))
            changed.drawingAttributes shouldBe red
            changed.pointTransform shouldBe InkTransform.translation(1.0, 1.0)
            changed.strokeStartedTime shouldBe started
            changed.strokeDuration shouldBe Duration.ofMillis(300)
            original.drawingAttributes shouldBe InkDrawingAttributes.DEFAULT
        }

        test("InkStrokes with the same contents are equal") {
            val a = InkStroke(listOf(InkPoint(1.0, 2.0, 0.3f)), InkDrawingAttributes.builder().size(3.0).build())
            val b = InkStroke(listOf(InkPoint(1.0, 2.0, 0.3f)), InkDrawingAttributes.builder().size(3.0).build())
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a shouldNotBe a.withPoints(listOf(InkPoint(1.0, 2.0, 0.4f)))
        }

        test("getPointBounds returns the bounding rectangle after the point transform is applied") {
            val stroke = InkStroke(listOf(InkPoint(0.0, 0.0), InkPoint(10.0, 5.0), InkPoint(4.0, -3.0)))
            stroke.getPointBounds() shouldBe InkRect(0.0, -3.0, 10.0, 8.0)
            stroke.withPointTransform(InkTransform.translation(100.0, 200.0)).getPointBounds() shouldBe
                InkRect(100.0, 197.0, 10.0, 8.0)
        }

        test("InkRect.EMPTY is empty, and a rectangle with a size is not empty") {
            InkRect.EMPTY.isEmpty shouldBe true
            InkRect(0.0, 0.0, 0.0, 0.0).isEmpty shouldBe false
            InkRect(1.0, 2.0, 3.0, 4.0).isEmpty shouldBe false
        }
    }
}
