package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winui.Abi
import jp.hisano.winui4k.internal.winui.XamlStructs

/**
 * java.awt.LinearGradientPaint-like: WinUI 3's LinearGradientBrush.
 * [stops] is a sequence of "offset (0.0..1.0) -> color" pairs. [angle] is the gradient axis's
 * angle in degrees; 0.0 is left to right, 90.0 (the default) is top to bottom.
 */
class WLinearGradientPaint(
    val stops: List<Pair<Double, WColor>>,
    val angle: Double = 90.0,
) {
    /** Creates a new LinearGradientBrush for this definition. The caller must release it. */
    internal fun createBrush(): ComPtr {
        // Fill a GradientStopCollection (whose default interface is IVector<GradientStop>) with the
        // stops, then create the angled brush via ILinearGradientBrushFactory
        val collection = Activation.activate(Abi.CLS_GradientStopCollection)
            .queryInterface(Abi.IID_IVector_GradientStop)
        try {
            for ((offset, color) in stops) {
                val stop = Activation.activate(Abi.CLS_GradientStop).queryInterface(Abi.IID_IGradientStop)
                try {
                    XamlStructs.putColor(stop, Abi.IGradientStop_put_Color, color.alpha, color.red, color.green, color.blue)
                    stop.call(Abi.IGradientStop_put_Offset, offset)
                    collection.call(Abi.IVector_Append, stop)
                } finally {
                    stop.release()
                }
            }
            return Activation.factory(Abi.CLS_LinearGradientBrush, Abi.IID_ILinearGradientBrushFactory)
                .getPtr(
                    Abi.ILinearGradientBrushFactory_CreateInstanceWithGradientStopCollectionAndAngle,
                    collection,
                    angle,
                )
        } finally {
            collection.release()
        }
    }
}
