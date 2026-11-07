package com.appkitbox.winui4k.internal.ffi.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import java.util.ServiceLoader

/**
 * Checks that [MemoryScope.allocate] returns zero-filled memory with every FFI backend (the same as Panama's Arena).
 * Some WinRT methods do not write the out argument even when they return success (S_OK) (FrameworkElement.Parent, etc.), so
 * if the out area keeps its previous content, the garbage read is Released as a pointer and corrupts the heap.
 * A block that was just freed tends to be reused by the next allocation, so the test fills it with a marker value, frees it, allocates the same size again and checks.
 */
@Tags("Pure")
class MemoryScopeZeroFillTest : FunSpec() {
    private val providers = ServiceLoader.load(FfiBackendProvider::class.java).filter { it.isAvailable() }

    init {
        test("the Panama, JNA and JNR backends are all on the test classpath") {
            providers.map { it.name } shouldContainAll listOf("panama", "jna", "jnr")
        }

        for (provider in providers) {
            test("${provider.name}: allocate of a temporary scope returns zero-filled memory even after memory filled with a marker value was freed") {
                val backend = provider.create()
                val dirty = mutableListOf<String>()
                for (size in BLOCK_SIZES) {
                    backend.newConfinedScope().use { scope ->
                        repeat(BLOCK_COUNT) {
                            val block = scope.allocate(size)
                            for (offset in 0 until size step Long.SIZE_BYTES.toLong()) backend.memory.putLong(block, offset, MARK)
                        }
                    }
                    backend.newConfinedScope().use { scope ->
                        repeat(BLOCK_COUNT) { index ->
                            val block = scope.allocate(size)
                            val nonZero = (0 until size).count { backend.memory.getByte(block, it) != 0.toByte() }
                            if (nonZero > 0) dirty += "allocation $index of size $size: $nonZero bytes are not zero"
                        }
                    }
                }
                dirty.shouldBeEmpty()
            }
        }
    }

    private companion object {
        /** The sizes (multiples of 8) of a COM out argument (one pointer), a small struct, a GUID and a larger struct. */
        val BLOCK_SIZES = listOf(8L, 16L, 24L, 64L)
        const val BLOCK_COUNT = 256
        const val MARK = 0x5A5A_5A5A_5A5A_5A5AL
    }
}
