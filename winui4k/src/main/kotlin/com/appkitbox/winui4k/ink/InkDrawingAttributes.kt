package com.appkitbox.winui4k.ink

import com.appkitbox.winui4k.WColor
import java.time.Duration

/**
 * Windows.UI.Input.Inking.PenTipShape (the shape of the pen tip).
 * Values are extracted from Windows.Foundation.UniversalApiContract.winmd (Circle=0, Rectangle=1).
 */
enum class PenTipShape(internal val native: Int) {
    /** A circular (elliptical) pen tip (default). */
    CIRCLE(0),

    /** A rectangular pen tip (for highlighters, etc.). */
    RECTANGLE(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): PenTipShape = entries.first { it.native == native }
    }
}

/**
 * Windows.UI.Input.Inking.InkDrawingAttributesKind (the kind of drawing attributes).
 * Values are extracted from Windows.Foundation.UniversalApiContract.winmd (Default=0, Pencil=1).
 */
enum class InkDrawingAttributesKind(internal val native: Int) {
    /** Pen drawing (ballpoint pen / highlighter). */
    DEFAULT(0),

    /** Pencil drawing (drawn with a grainy texture; [InkDrawingAttributes.pencilOpacity] takes effect). */
    PENCIL(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkDrawingAttributesKind = entries.first { it.native == native }
    }
}

/**
 * Like Windows.UI.Input.Inking.InkModelerAttributes: settings of the modeler that turns input into lines for drawing.
 * The defaults are the same as the defaults of WinUI's InkDrawingAttributes.
 */
data class InkModelerAttributes @JvmOverloads constructor(
    /** How far ahead the pen movement is predicted and drawn (PredictionTime; 0 to 20 ms, default 15 ms). */
    val predictionTime: Duration = DEFAULT_PREDICTION_TIME,
    /** The scale factor used when converting input coordinates from HIMETRIC (ScalingFactor; default 2540 / 96). */
    val scalingFactor: Float = DEFAULT_SCALING_FACTOR,
    /** Whether the line thickness is determined by pen speed instead of pressure (UseVelocityBasedPressure; default false). */
    val useVelocityBasedPressure: Boolean = false,
) {
    companion object {
        /** The default value of PredictionTime (15 ms). */
        @JvmField
        val DEFAULT_PREDICTION_TIME: Duration = Duration.ofMillis(15)

        /** The default value of ScalingFactor (2540 / 96; the HIMETRIC → DIP conversion). */
        const val DEFAULT_SCALING_FACTOR: Float = 2540f / 96f

        /** Settings with all default values. */
        @JvmField
        val DEFAULT = InkModelerAttributes()
    }
}

/**
 * Like Windows.UI.Input.Inking.InkDrawingAttributes: how a stroke is drawn (color, size, pen tip, pencil/highlighter,
 * etc.).
 *
 * An immutable value object. To change attributes, rebuild it from [toBuilder]
 * (unlike javax.swing.text.AttributeSet, it is not modified even when shared by strokes).
 * The defaults are the same as the defaults of WinUI's InkDrawingAttributes (black, size 2, circular pen tip, curve
 * fitting on).
 *
 * ```kotlin
 * val red = InkDrawingAttributes.builder().color(WColor.RED).size(4.0).build()
 * val pencil = InkDrawingAttributes.pencilBuilder().pencilOpacity(0.5).build()
 * ```
 */
class InkDrawingAttributes private constructor(builder: Builder) {
    /** The kind (pen or pencil). A pencil is created with [pencilBuilder] (InkDrawingAttributes.CreateForPencil). */
    val kind: InkDrawingAttributesKind = builder.kind

    /** The line color (Color). */
    val color: WColor = builder.color

    /** The shape of the pen tip (PenTip). */
    val penTip: PenTipShape = builder.penTip

    /** The width of the pen tip (Size.Width, in DIPs). */
    val width: Double = builder.width

    /** The height of the pen tip (Size.Height, in DIPs). */
    val height: Double = builder.height

    /** Whether to ignore pressure and draw with a constant thickness (IgnorePressure). */
    val ignorePressure: Boolean = builder.ignorePressure

    /** Whether to interpolate between points with Bézier curves (FitToCurve; a polyline if false). */
    val fitToCurve: Boolean = builder.fitToCurve

    /** The transform applied to the pen tip shape (PenTipTransform; the translation components are ignored). */
    val penTipTransform: InkTransform = builder.penTipTransform

    /** Whether to draw as a highlighter (DrawAsHighlighter; drawn semi-transparently over the lines beneath). */
    val drawAsHighlighter: Boolean = builder.drawAsHighlighter

    /** Whether to ignore pen tilt (IgnoreTilt). */
    val ignoreTilt: Boolean = builder.ignoreTilt

    /** The pencil opacity (PencilProperties.Opacity, 0.01 to 5.0). Takes effect only when [kind] is [InkDrawingAttributesKind.PENCIL]. */
    val pencilOpacity: Double = builder.pencilOpacity

    /** The settings of the input modeler (ModelerAttributes). */
    val modelerAttributes: InkModelerAttributes = builder.modelerAttributes

    /** A builder initialized with this value. */
    fun toBuilder(): Builder = Builder(kind).also {
        it.color = color
        it.penTip = penTip
        it.width = width
        it.height = height
        it.ignorePressure = ignorePressure
        it.fitToCurve = fitToCurve
        it.penTipTransform = penTipTransform
        it.drawAsHighlighter = drawAsHighlighter
        it.ignoreTilt = ignoreTilt
        it.pencilOpacity = pencilOpacity
        it.modelerAttributes = modelerAttributes
    }

    override fun equals(other: Any?): Boolean = other is InkDrawingAttributes && fields() == other.fields()

    override fun hashCode(): Int = fields().hashCode()

    override fun toString(): String =
        "InkDrawingAttributes(kind=$kind, color=$color, penTip=$penTip, width=$width, height=$height, " +
            "ignorePressure=$ignorePressure, fitToCurve=$fitToCurve, penTipTransform=$penTipTransform, " +
            "drawAsHighlighter=$drawAsHighlighter, ignoreTilt=$ignoreTilt, pencilOpacity=$pencilOpacity, " +
            "modelerAttributes=$modelerAttributes)"

    private fun fields(): List<Any> = listOf(
        kind, color, penTip, width, height, ignorePressure, fitToCurve,
        penTipTransform, drawAsHighlighter, ignoreTilt, pencilOpacity, modelerAttributes,
    )

    /** A builder for [InkDrawingAttributes]. Each method returns the builder itself, so calls can be chained. */
    class Builder internal constructor(internal val kind: InkDrawingAttributesKind) {
        internal var color: WColor = WColor.BLACK
        internal var penTip: PenTipShape = PenTipShape.CIRCLE
        internal var width: Double = DEFAULT_SIZE
        internal var height: Double = DEFAULT_SIZE
        internal var ignorePressure: Boolean = false
        internal var fitToCurve: Boolean = true
        internal var penTipTransform: InkTransform = InkTransform.IDENTITY
        internal var drawAsHighlighter: Boolean = false
        internal var ignoreTilt: Boolean = true
        internal var pencilOpacity: Double = DEFAULT_PENCIL_OPACITY
        internal var modelerAttributes: InkModelerAttributes = InkModelerAttributes.DEFAULT

        /** The line color. */
        fun color(color: WColor): Builder = apply { this.color = color }

        /** The shape of the pen tip. */
        fun penTip(penTip: PenTipShape): Builder = apply { this.penTip = penTip }

        /** Sets both the width and height of the pen tip to [size] (DIPs). */
        fun size(size: Double): Builder = size(size, size)

        /** The width [width] and height [height] of the pen tip (DIPs). */
        fun size(width: Double, height: Double): Builder = apply {
            require(width > 0.0 && height > 0.0) { "Pen tip size must be positive: $width x $height" }
            this.width = width
            this.height = height
        }

        /** Whether to ignore pressure. */
        fun ignorePressure(ignorePressure: Boolean): Builder = apply { this.ignorePressure = ignorePressure }

        /** Whether to interpolate with curves. */
        fun fitToCurve(fitToCurve: Boolean): Builder = apply { this.fitToCurve = fitToCurve }

        /** The transform applied to the pen tip shape. */
        fun penTipTransform(transform: InkTransform): Builder = apply { this.penTipTransform = transform }

        /** Whether to draw as a highlighter. */
        fun drawAsHighlighter(drawAsHighlighter: Boolean): Builder = apply { this.drawAsHighlighter = drawAsHighlighter }

        /** Whether to ignore pen tilt. */
        fun ignoreTilt(ignoreTilt: Boolean): Builder = apply { this.ignoreTilt = ignoreTilt }

        /** The pencil opacity (0.01 to 5.0). Used for drawing only with a pencil ([pencilBuilder]). */
        fun pencilOpacity(opacity: Double): Builder = apply {
            require(opacity in MIN_PENCIL_OPACITY..MAX_PENCIL_OPACITY) { "Pencil opacity must be between 0.01 and 5.0: $opacity" }
            this.pencilOpacity = opacity
        }

        /** The settings of the input modeler. */
        fun modelerAttributes(attributes: InkModelerAttributes): Builder = apply { this.modelerAttributes = attributes }

        fun build(): InkDrawingAttributes = InkDrawingAttributes(this)
    }

    companion object {
        /** The default pen tip size (2 DIPs). */
        const val DEFAULT_SIZE: Double = 2.0

        /** The default pencil opacity (1.0). */
        const val DEFAULT_PENCIL_OPACITY: Double = 1.0

        private const val MIN_PENCIL_OPACITY = 0.01
        private const val MAX_PENCIL_OPACITY = 5.0

        /** Pen drawing attributes with all default values. */
        @JvmField
        val DEFAULT: InkDrawingAttributes = Builder(InkDrawingAttributesKind.DEFAULT).build()

        /** A builder for pen (ballpoint pen / highlighter) drawing attributes. */
        @JvmStatic
        fun builder(): Builder = Builder(InkDrawingAttributesKind.DEFAULT)

        /** A builder for pencil drawing attributes (InkDrawingAttributes.CreateForPencil). */
        @JvmStatic
        fun pencilBuilder(): Builder = Builder(InkDrawingAttributesKind.PENCIL)
    }
}
