package com.appkitbox.winui4k.ink

import java.time.Duration
import java.time.Instant

/**
 * Like Windows.UI.Input.Inking.InkStroke: a single stroke (a sequence of points and how it is drawn).
 *
 * An immutable value object that is an element of an [InkStrokeModel]. Lines drawn on the canvas are written to the
 * model as this object, and strokes put into the model are drawn on the canvas.
 * To change a stroke, recreate it with the `with` methods and replace it with [InkStrokeModel.setStroke].
 *
 * The selection state (Selected) and ID (Id) of WinUI's InkStroke are per-canvas display state, so they are handled by
 * [com.appkitbox.winui4k.WInkCanvas] rather than this class.
 */
class InkStroke @JvmOverloads constructor(
    points: List<InkPoint>,
    /** How it is drawn (DrawingAttributes). */
    val drawingAttributes: InkDrawingAttributes = InkDrawingAttributes.DEFAULT,
    /** The transform applied to the points (PointTransform). Drawing and the bounding rectangle use the transformed positions. */
    val pointTransform: InkTransform = InkTransform.IDENTITY,
    /** The time drawing started (StrokeStartedTime), or null if unknown. */
    val strokeStartedTime: Instant? = null,
    /** The time it took to draw (StrokeDuration), or null if unknown. */
    val strokeDuration: Duration? = null,
) {
    /** The sequence of points (GetInkPoints). At least one point. */
    val points: List<InkPoint> = java.util.Collections.unmodifiableList(ArrayList(points))

    init {
        require(this.points.isNotEmpty()) { "A stroke needs at least one point" }
        require(strokeDuration == null || !strokeDuration.isNegative) { "strokeDuration must be 0 or more: $strokeDuration" }
    }

    /** A stroke with its points replaced by [points]. */
    fun withPoints(points: List<InkPoint>): InkStroke =
        InkStroke(points, drawingAttributes, pointTransform, strokeStartedTime, strokeDuration)

    /** A stroke with its drawing attributes replaced by [attributes]. */
    fun withDrawingAttributes(attributes: InkDrawingAttributes): InkStroke =
        InkStroke(points, attributes, pointTransform, strokeStartedTime, strokeDuration)

    /** A stroke with its point transform replaced by [transform]. */
    fun withPointTransform(transform: InkTransform): InkStroke =
        InkStroke(points, drawingAttributes, transform, strokeStartedTime, strokeDuration)

    /** A stroke with its drawing time and duration replaced. */
    fun withTiming(startedTime: Instant?, duration: Duration?): InkStroke =
        InkStroke(points, drawingAttributes, pointTransform, startedTime, duration)

    /**
     * The bounding rectangle of the point positions after applying [pointTransform] (not including the line thickness).
     * The drawn area including line thickness and curve fitting is obtained with
     * [com.appkitbox.winui4k.WInkCanvas.getStrokeBounds].
     */
    fun getPointBounds(): InkRect {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (point in points) {
            val p = pointTransform.transform(point.x, point.y)
            minX = minOf(minX, p.x)
            minY = minOf(minY, p.y)
            maxX = maxOf(maxX, p.x)
            maxY = maxOf(maxY, p.y)
        }
        return InkRect(minX, minY, maxX - minX, maxY - minY)
    }

    override fun equals(other: Any?): Boolean =
        other is InkStroke &&
            points == other.points &&
            drawingAttributes == other.drawingAttributes &&
            pointTransform == other.pointTransform &&
            strokeStartedTime == other.strokeStartedTime &&
            strokeDuration == other.strokeDuration

    override fun hashCode(): Int =
        listOf(points, drawingAttributes, pointTransform, strokeStartedTime, strokeDuration).hashCode()

    override fun toString(): String =
        "InkStroke(points=${points.size}, drawingAttributes=$drawingAttributes, pointTransform=$pointTransform, " +
            "strokeStartedTime=$strokeStartedTime, strokeDuration=$strokeDuration)"
}

/**
 * Like Windows.UI.Input.Inking.InkStrokeRenderingSegment: one segment of a stroke curve-fitted for rendering
 * (a cubic Bézier curve from the end point of the previous segment to ([x], [y])).
 * Obtained with [com.appkitbox.winui4k.WInkCanvas.getRenderingSegments].
 */
data class InkStrokeRenderingSegment(
    /** The X coordinate of the segment's end point (Position.X). */
    val x: Double,
    /** The Y coordinate of the segment's end point (Position.Y). */
    val y: Double,
    /** The X coordinate of the first control point (BezierControlPoint1.X). */
    val controlPoint1X: Double,
    /** The Y coordinate of the first control point (BezierControlPoint1.Y). */
    val controlPoint1Y: Double,
    /** The X coordinate of the second control point (BezierControlPoint2.X). */
    val controlPoint2X: Double,
    /** The Y coordinate of the second control point (BezierControlPoint2.Y). */
    val controlPoint2Y: Double,
    /** The pressure (0.0 to 1.0). */
    val pressure: Float,
    /** The tilt along the X axis (degrees). */
    val tiltX: Float,
    /** The tilt along the Y axis (degrees). */
    val tiltY: Float,
    /** The rotation of the pen around its axis (degrees). */
    val twist: Float,
)
