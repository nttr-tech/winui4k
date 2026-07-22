package jp.hisano.winui4k.internal.com

import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.Ptr

/** Native representation of a GUID (16 bytes, mixed little-endian layout). */
internal object Guid {
    private val cache = HashMap<String, Ptr>()

    /** Converts "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" to a native GUID struct (cached in the global scope). */
    @Synchronized
    fun of(iid: String): Ptr = cache.getOrPut(iid.lowercase()) {
        val hex = iid.replace("-", "")
        require(hex.length == 32) { "bad GUID: $iid" }
        val memory = Ffi.backend.memory
        val guid = Ffi.backend.globalScope.allocate(16)
        memory.putInt(guid, 0, hex.substring(0, 8).toLong(16).toInt())
        memory.putShort(guid, 4, hex.substring(8, 12).toInt(16).toShort())
        memory.putShort(guid, 6, hex.substring(12, 16).toInt(16).toShort())
        for (i in 0 until 8) {
            memory.putByte(guid, 8L + i, hex.substring(16 + i * 2, 18 + i * 2).toInt(16).toByte())
        }
        guid
    }

    /** Reads a native GUID as a lowercase canonical string. */
    fun read(ptr: Ptr): String {
        val memory = Ffi.backend.memory
        val d1 = memory.getInt(ptr, 0).toLong() and 0xFFFFFFFFL
        val d2 = memory.getShort(ptr, 4).toInt() and 0xFFFF
        val d3 = memory.getShort(ptr, 6).toInt() and 0xFFFF
        val b = ByteArray(8) { memory.getByte(ptr, 8L + it) }
        return "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
            d1, d2, d3, b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7],
        )
    }
}
