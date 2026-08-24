package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/** A position and a color on a gradient. */
data class WGradientStop(
    val offset: Double,
    val color: WColor,
)

/**
 * java.awt.LinearGradientPaint-like: WinUI 3's LinearGradientBrush.
 * [stops] is a sequence of "offset (0.0..1.0) -> color" pairs. [angle] is the gradient axis's
 * angle in degrees; 0.0 is left to right, 90.0 (the default) is top to bottom.
 *
 * In Kotlin, a sequence of Pairs like `listOf(0.0 to WColor.BLUE, 1.0 to WColor.PURPLE)` can also be passed.
 */
class WLinearGradientPaint @JvmOverloads constructor(
    val stops: List<WGradientStop>,
    val angle: Double = 90.0,
) {
    /** A vararg constructor for Java. The angle defaults to 90 degrees. */
    constructor(vararg stops: WGradientStop) : this(stops.toList())

    /** A vararg constructor for Java. */
    constructor(angle: Double, vararg stops: WGradientStop) : this(stops.toList(), angle)

    /** Creates a new LinearGradientBrush for this definition. The caller must release it. */
    internal fun createBrush(): ComPtr {
        // Fill a GradientStopCollection (whose default interface is IVector<GradientStop>) with the
        // stops, then create the angled brush via ILinearGradientBrushFactory
        val collection = Activation.activate(XamlInterop.CLS_GradientStopCollection)
            .queryInterface(FoundationInterop.IID_IVector_GradientStop)
        try {
            for ((offset, color) in stops) {
                val stop = Activation.activate(XamlInterop.CLS_GradientStop, XamlInterop.IID_IGradientStop)
                try {
                    XamlStructs.putColor(stop, XamlInterop.IGradientStop_put_Color, color.alpha, color.red, color.green, color.blue)
                    stop.call(XamlInterop.IGradientStop_put_Offset, offset)
                    collection.call(FoundationInterop.IVector_Append, stop)
                } finally {
                    stop.release()
                }
            }
            return Activation.factory(XamlInterop.CLS_LinearGradientBrush, XamlInterop.IID_ILinearGradientBrushFactory)
                .getPtr(
                    XamlInterop.ILinearGradientBrushFactory_CreateInstanceWithGradientStopCollectionAndAngle,
                    collection,
                    angle,
                )
        } finally {
            collection.release()
        }
    }
}

/**
 * For Kotlin: creates a [WLinearGradientPaint] from a sequence of "offset to color" Pairs.
 * From Java, use the constructor that takes [WGradientStop] instead of assembling Pairs.
 */
@JvmSynthetic
@Suppress("FunctionNaming") // Intentionally named after the class so calls look like constructor invocations
fun WLinearGradientPaint(
    stops: List<Pair<Double, WColor>>,
    angle: Double = 90.0,
): WLinearGradientPaint = WLinearGradientPaint(stops.map { WGradientStop(it.first, it.second) }, angle)
