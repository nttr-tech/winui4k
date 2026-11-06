package com.appkitbox.winui4k

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Creates .ico files (because [WFrame.setIconImage] passes PNGs to AppWindow.SetIcon, and SetIcon reads only .ico).
 * Since Windows Vista, an .ico can hold PNG data as is as one image, so the conversion only adds headers.
 */
internal object IcoFiles {
    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)

    /** From the start of a PNG to the end of the IHDR width and height (signature 8 + length 4 + "IHDR" 4 + width 4 + height 4). */
    private const val PNG_HEADER_SIZE = 24
    private const val PNG_WIDTH_OFFSET = 16
    private const val PNG_HEIGHT_OFFSET = 20

    /** ICONDIR (6 bytes) and one ICONDIRENTRY (16 bytes). */
    private const val ICO_HEADER_SIZE = 22
    private const val ICO_TYPE_ICON: Short = 1
    private const val BITS_PER_PIXEL: Short = 32

    /** The ICONDIRENTRY width and height are 1 byte each, and 256 or more is written as 0. */
    private const val MAX_ENTRY_DIMENSION = 255

    /** The contents of an .ico holding only [png] (the contents of a PNG file). */
    fun icoFromPng(png: ByteArray): ByteArray {
        require(png.size >= PNG_HEADER_SIZE && png.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) {
            "Cannot be read as a PNG"
        }
        val header = ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN)
        val width = header.getInt(PNG_WIDTH_OFFSET)
        val height = header.getInt(PNG_HEIGHT_OFFSET)
        val ico = ByteBuffer.allocate(ICO_HEADER_SIZE + png.size).order(ByteOrder.LITTLE_ENDIAN)
        ico.putShort(0) // reserved
        ico.putShort(ICO_TYPE_ICON)
        ico.putShort(1) // The number of images
        ico.put(entryDimension(width))
        ico.put(entryDimension(height))
        ico.put(0) // The number of palette colors (none)
        ico.put(0) // reserved
        ico.putShort(1) // planes
        ico.putShort(BITS_PER_PIXEL)
        ico.putInt(png.size)
        ico.putInt(ICO_HEADER_SIZE)
        ico.put(png)
        return ico.array()
    }

    /** Writes an .ico holding only [png] to a temporary file (deleted when the JVM exits). */
    fun temporaryIcoFromPng(png: ByteArray): File {
        val ico = icoFromPng(png)
        return File.createTempFile("winui4k-icon", ".ico").also {
            it.deleteOnExit()
            it.writeBytes(ico)
        }
    }

    private fun entryDimension(value: Int): Byte = if (value in 1..MAX_ENTRY_DIMENSION) value.toByte() else 0
}
