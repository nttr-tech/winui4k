package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * For tests: reads the state of the native window (HWND) of a [WFrame] directly with Win32. Without going through the WinUI4K API,
 * checks the styles (border, title bar, maximize button, topmost), the z-order and the icon.
 */
internal object WindowNatives {
    private val getWindowLongPtrW by lazy {
        Ffi.backend.function("user32.dll", "GetWindowLongPtrW", CallDescriptor(ValueKind.I64, ArgKind.PTR, ArgKind.I32))
    }
    private val getWindow by lazy {
        Ffi.backend.function("user32.dll", "GetWindow", CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.I32))
    }
    private val sendMessageW by lazy {
        Ffi.backend.function("user32.dll", "SendMessageW", CallDescriptor(ValueKind.I64, ArgKind.PTR, ArgKind.I32, ArgKind.I64, ArgKind.I64))
    }

    private const val GWL_STYLE = -16
    private const val GWL_EXSTYLE = -20
    private const val GW_HWNDNEXT = 2
    private const val WM_GETICON = 0x007F
    private const val ICON_SMALL = 0L
    private const val ICON_BIG = 1L

    const val WS_CAPTION = 0x00C00000L
    const val WS_THICKFRAME = 0x00040000L
    const val WS_MAXIMIZEBOX = 0x00010000L
    const val WS_EX_TOPMOST = 0x00000008L

    /** The HWND of [frame] (the WindowId value). */
    fun hwnd(frame: WFrame): Ptr = Ptr(frame.appWindow.id)

    /** The window style (GWL_STYLE). */
    fun style(frame: WFrame): Long = getWindowLongPtrW(hwnd(frame), GWL_STYLE) as Long

    /** The window extended style (GWL_EXSTYLE). */
    fun exStyle(frame: WFrame): Long = getWindowLongPtrW(hwnd(frame), GWL_EXSTYLE) as Long

    /** true if [upper] is in front of [lower] (following GW_HWNDNEXT from [upper] reaches [lower]). */
    fun isAbove(upper: WFrame, lower: WFrame): Boolean {
        val target = hwnd(lower)
        var current = getWindow(hwnd(upper), GW_HWNDNEXT) as Ptr
        while (!current.isNull) {
            if (current == target) return true
            current = getWindow(current, GW_HWNDNEXT) as Ptr
        }
        return false
    }

    /** The handles of the icons set on the window [large, small] (WM_GETICON). 0 if not set. */
    fun icons(frame: WFrame): List<Long> =
        listOf(sendMessageW(hwnd(frame), WM_GETICON, ICON_BIG, 0L) as Long, sendMessageW(hwnd(frame), WM_GETICON, ICON_SMALL, 0L) as Long)

    /** The contents of a [size] x [size] PNG (RGBA 8-bit) filled with one color. */
    @JvmStatic
    fun solidPng(size: Int, argb: Int): ByteArray {
        val raw = ByteArrayOutputStream()
        repeat(size) {
            raw.write(0) // No filter
            repeat(size) {
                raw.write(argb shr 16 and 0xFF)
                raw.write(argb shr 8 and 0xFF)
                raw.write(argb and 0xFF)
                raw.write(argb ushr 24)
            }
        }
        val deflater = Deflater()
        deflater.setInput(raw.toByteArray())
        deflater.finish()
        val compressed = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) compressed.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()

        val png = ByteArrayOutputStream()
        png.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A))
        val header = ByteArrayOutputStream()
        DataOutputStream(header).apply {
            writeInt(size)
            writeInt(size)
            writeByte(8) // Bit depth
            writeByte(6) // RGBA
            writeByte(0)
            writeByte(0)
            writeByte(0)
        }
        chunk(png, "IHDR", header.toByteArray())
        chunk(png, "IDAT", compressed.toByteArray())
        chunk(png, "IEND", ByteArray(0))
        return png.toByteArray()
    }

    private fun chunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val stream = DataOutputStream(out)
        stream.writeInt(data.size)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        stream.write(typeBytes)
        stream.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        stream.writeInt(crc.value.toInt())
    }
}
