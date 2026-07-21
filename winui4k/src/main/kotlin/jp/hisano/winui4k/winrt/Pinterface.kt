package jp.hisano.winui4k.winrt

import java.security.MessageDigest

/**
 * Computes the actual IID of parameterized interfaces (IVector<T> and the like).
 * A pure-Kotlin implementation independent of any FFI backend (this knowledge comes
 * from WinRT's type system, so it lives in the winrt layer).
 */
internal object Pinterface {
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
    fun iid(signature: String): String {
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
