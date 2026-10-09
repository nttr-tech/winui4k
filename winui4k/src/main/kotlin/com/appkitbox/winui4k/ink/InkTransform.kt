package com.appkitbox.winui4k.ink

import kotlin.math.cos
import kotlin.math.sin

/**
 * Like java.awt.geom.AffineTransform: a two-dimensional affine transform (Windows.Foundation.Numerics.Matrix3x2).
 *
 * Maps a point (x, y) to `(x * m11 + y * m21 + m31, x * m12 + y * m22 + m32)` (the WinRT convention of row vector ×
 * matrix). Used for the transform of stroke points ([InkStroke.pointTransform]) and of the pen tip shape
 * ([InkDrawingAttributes.penTipTransform]). WinUI holds each element as float.
 */
data class InkTransform(
    val m11: Double,
    val m12: Double,
    val m21: Double,
    val m22: Double,
    val m31: Double,
    val m32: Double,
) {
    /** Whether this is the identity transform. */
    val isIdentity: Boolean
        get() = this == IDENTITY

    /** The [InkPoint] of the transformed point ([x], [y]) (pressure etc. at their defaults). */
    fun transform(x: Double, y: Double): InkPoint = InkPoint(x * m11 + y * m21 + m31, x * m12 + y * m22 + m32)

    /** The composite transform that applies [next] after this transform (this * next of Matrix3x2). */
    fun then(next: InkTransform): InkTransform = InkTransform(
        m11 * next.m11 + m12 * next.m21,
        m11 * next.m12 + m12 * next.m22,
        m21 * next.m11 + m22 * next.m21,
        m21 * next.m12 + m22 * next.m22,
        m31 * next.m11 + m32 * next.m21 + next.m31,
        m31 * next.m12 + m32 * next.m22 + next.m32,
    )

    companion object {
        /** The identity transform (Matrix3x2.Identity). */
        @JvmField
        val IDENTITY = InkTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

        /** A translation by ([dx], [dy]). */
        @JvmStatic
        fun translation(dx: Double, dy: Double): InkTransform = InkTransform(1.0, 0.0, 0.0, 1.0, dx, dy)

        /** A scale by ([sx], [sy]) around the origin. */
        @JvmStatic
        fun scale(sx: Double, sy: Double): InkTransform = InkTransform(sx, 0.0, 0.0, sy, 0.0, 0.0)

        /** A rotation by [radians] around the origin (positive is clockwise, as seen in screen coordinates where the y axis points down). */
        @JvmStatic
        fun rotation(radians: Double): InkTransform {
            val c = cos(radians)
            val s = sin(radians)
            return InkTransform(c, s, -s, c, 0.0, 0.0)
        }
    }
}
