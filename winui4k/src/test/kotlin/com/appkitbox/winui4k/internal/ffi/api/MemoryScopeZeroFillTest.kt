package com.appkitbox.winui4k.internal.ffi.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

/**
 * Checks that [MemoryScope.allocate] returns zero-filled memory with every FFI backend (the same as Panama's Arena).
 * Some WinRT methods do not write the out argument even when they return success (S_OK) (FrameworkElement.Parent, etc.), so
 * if the out area keeps its previous content, the garbage read is Released as a pointer and corrupts the heap.
 * A block that was just freed tends to be reused by the next allocation, so the test fills it with a marker value, frees it, allocates the same size again and checks.
 */
@Tags("Pure")
class MemoryScopeZeroFillTest : FunSpec() {
    private val providers = loadProviders().filter { it.isAvailable() }

    init {
        // Panama is available only on JDK 22 or later (it is excluded in testOnJava8 / 9). JNR is checked on every JDK.
        // JNA is Windows x64 only (JnaBackendProvider.isAvailable), so it is not a target on Arm64 JDKs
        test("the JNR backend (and JNA on x64 and Panama on JDK 22 or later) can be loaded and is among the targets checked") {
            val expected = listOf("jnr") +
                (if (isX64()) listOf("jna") else emptyList()) +
                (if (isPanamaJdk()) listOf("panama") else emptyList())
            providers.map { it.name } shouldContainAll expected
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

    /**
     * Loads the FFI backends on the classpath in the same way as [Ffi]. Before JDK 22, the Panama provider (Java 22 bytecode)
     * cannot be loaded and throws UnsupportedClassVersionError, so it is skipped (Java 8's ServiceLoader does not wrap LinkageError).
     */
    private fun loadProviders(): List<FfiBackendProvider> {
        val iterator = ServiceLoader.load(FfiBackendProvider::class.java, FfiBackendProvider::class.java.classLoader).iterator()
        val providers = ArrayList<FfiBackendProvider>()
        while (true) {
            try {
                if (!iterator.hasNext()) break
                providers.add(iterator.next())
            } catch (_: ServiceConfigurationError) {
            } catch (_: LinkageError) {
            }
        }
        return providers
    }

    private fun isX64(): Boolean = System.getProperty("os.arch").orEmpty().lowercase() in setOf("amd64", "x86_64")

    private fun isPanamaJdk(): Boolean =
        System.getProperty("java.specification.version").substringAfter("1.").toInt() >= PANAMA_JDK

    private companion object {
        const val PANAMA_JDK = 22

        /** The sizes (multiples of 8) of a COM out argument (one pointer), a small struct, a GUID and a larger struct. */
        val BLOCK_SIZES = listOf(8L, 16L, 24L, 64L)
        const val BLOCK_COUNT = 256
        const val MARK = 0x5A5A_5A5A_5A5A_5A5AL
    }
}
