package jp.hisano.winui4k.internal.winui

import jp.hisano.winui4k.internal.ffi.api.ArgKind
import jp.hisano.winui4k.internal.ffi.api.CallDescriptor
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.Ptr
import jp.hisano.winui4k.internal.ffi.api.ValueKind
import jp.hisano.winui4k.internal.winrt.KComObject

/**
 * A Kotlin implementation of Windows.Foundation.IReference`1<Windows.UI.Color>.
 * Windows.Foundation.PropertyValue has no CreateColor, so when putting a value into
 * AppWindowTitleBar's 12 color properties, this creates the box by hand (the get side just needs
 * to call vtbl[6] get_Value directly on the IReference<Color> WinUI returns, so no box is needed there).
 *
 * The IID is computed at runtime as pinterface({IReference's base IID};struct(Windows.UI.Color;u1;u1;u1;u1))
 * ([Abi.IID_IReference_Color]). Its only member is get_Value (vtbl[6]).
 *
 * Putting into AppWindowTitleBar has been verified on real hardware (2026-07-12). No QueryInterface
 * to IPropertyValue is required — writing the A,R,G,B values in vtbl[6] get_Value alone is enough
 * for the color to take effect.
 */
internal object ColorReference {
    /** Creates an IReference<Color> COM object from ARGB components (each 0..255). */
    fun create(alpha: Int, red: Int, green: Int, blue: Int): KComObject =
        KComObject("WinUI4K.ColorReference").addInterface(
            Abi.IID_IReference_Color,
            listOf(
                // vtbl[6] get_Value(this, out Windows.UI.Color)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    val out = args[1] as Ptr
                    val memory = Ffi.backend.memory
                    memory.putByte(out, 0, alpha.toByte())
                    memory.putByte(out, 1, red.toByte())
                    memory.putByte(out, 2, green.toByte())
                    memory.putByte(out, 3, blue.toByte())
                    KComObject.S_OK
                },
            ),
        )

    private val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
}
