package jp.hisano.winui4k.winrt

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Guid
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.ffi.Native
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.security.MessageDigest

/** Utilities around WinRT activation (RoGetActivationFactory). */
object WinRt {
    private const val IID_IACTIVATION_FACTORY = "00000035-0000-0000-c000-000000000046"
    private const val IID_IPROPERTY_VALUE_STATICS = "629bdbc8-d932-4ff4-96b9-8d96c5c1e858"

    /** Windows.Foundation.IPropertyValue (the retrieval side of a boxed value). From Windows.Foundation.winmd. */
    private const val IID_IPROPERTY_VALUE = "4bd682dd-7554-40e9-9a9b-82654ede7e62"
    private const val IPropertyValue_GetString = 19 // GetString(out HSTRING)

    private val roGetActivationFactory by lazy {
        Native.downcall(
            Native.combase, "RoGetActivationFactory",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
        )
    }

    /** Gets the activation factory for a runtime class. */
    fun factory(runtimeClass: String, iid: String): ComPtr = Arena.ofConfined().use { a ->
        val out = a.allocate(ADDRESS)
        val hr = roGetActivationFactory.invokeWithArguments(
            Hstring.ofCached(runtimeClass), Guid.of(iid), out,
        ) as Int
        Native.checkHr(hr, "RoGetActivationFactory($runtimeClass)")
        ComPtr(out.get(ADDRESS, 0))
    }

    /** Equivalent to a default constructor (IActivationFactory::ActivateInstance). */
    fun activate(runtimeClass: String): ComPtr =
        factory(runtimeClass, IID_IACTIVATION_FACTORY).getPtr(6)

    /**
     * Instantiates a composable (inheritable) class.
     * Passing outer = NULL creates a plain, non-derived instance (the same convention
     * as C++/WinRT). A factory's CreateInstance is always vtbl[6]:
     *   HRESULT CreateInstance(IInspectable* outer, IInspectable** inner, T** instance)
     */
    fun composeDefault(runtimeClass: String, factoryIid: String): ComPtr =
        Arena.ofConfined().use { a ->
            val f = factory(runtimeClass, factoryIid)
            val inner = a.allocate(ADDRESS)
            val instance = a.allocate(ADDRESS)
            f.call(6, MemorySegment.NULL, inner, instance)
            f.release()
            ComPtr(instance.get(ADDRESS, 0))
        }

    /**
     * Boxes a Kotlin String into an IInspectable
     * (Windows.Foundation.PropertyValue.CreateString).
     * Used to pass a string into an Object-typed property, such as Button.Content.
     */
    fun boxString(s: String): ComPtr {
        val statics = factory("Windows.Foundation.PropertyValue", IID_IPROPERTY_VALUE_STATICS)
        return Hstring.use(s) { h ->
            val boxed = statics.getPtr(18, h) // vtbl[18] = CreateString (from the FoundationContract winmd)
            statics.release()
            boxed
        }
    }

    /**
     * The reverse of [boxString]: extracts the string if the IInspectable is a boxed string.
     * Returns null if it isn't a boxed string (PropertyValue), e.g. when it holds a UIElement.
     */
    fun unboxString(boxed: ComPtr): String? {
        val pv = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            pv.getString(IPropertyValue_GetString)
        } finally {
            pv.release()
        }
    }

    // ------------------------------------------------------------------
    // IID computation for parameterized interfaces (IVector<T>, etc.)
    // ------------------------------------------------------------------

    /** Big-endian representation of the WinRT pinterface namespace GUID {11f47ad5-7b73-42c0-abae-878b1e16adee}. */
    private val PINTERFACE_NAMESPACE = byteArrayOf(
        0x11, 0xF4.toByte(), 0x7A, 0xD5.toByte(),
        0x7B, 0x73, 0x42, 0xC0.toByte(),
        0xAB.toByte(), 0xAE.toByte(), 0x87.toByte(), 0x8B.toByte(),
        0x1E, 0x16, 0xAD.toByte(), 0xEE.toByte(),
    )

    /**
     * Computes the actual IID of a generic interface such as IVector<UIElement> from its
     * WinRT specification signature string (an RFC 4122 SHA-1 name-based UUID).
     *
     * Example: pinterface({913337e9-...IVector`1...};rc(Microsoft.UI.Xaml.UIElement;{c3c01020-...}))
     *
     * Verified: pinterface({faa585ea-6214-4217-afda-7f46de5869b3};string)
     *       → matches e2fcc7c1-3bfc-5a0b-b2b0-72e769d1cb7e, the known value for IIterable<String>.
     */
    fun pinterfaceIid(signature: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(PINTERFACE_NAMESPACE)
        md.update(signature.toByteArray(Charsets.US_ASCII))
        val h = md.digest()
        h[6] = ((h[6].toInt() and 0x0F) or 0x50).toByte() // version 5
        h[8] = ((h[8].toInt() and 0x3F) or 0x80).toByte() // RFC 4122 variant
        val sb = StringBuilder(36)
        for (i in 0 until 16) {
            if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-')
            sb.append("%02x".format(h[i]))
        }
        return sb.toString()
    }
}
