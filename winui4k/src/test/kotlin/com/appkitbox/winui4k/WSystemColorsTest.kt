package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Tests reading the system accent color palette (UISettings) and delivering its change notification on the UI thread. */
class WSystemColorsTest : FunSpec() {
    private fun WColor.luminance() = 0.2126 * red + 0.7152 * green + 0.0722 * blue

    init {
        test("the accent palette is opaque and runs from the darkest to the lightest variant in the order dark3 < dark2 < dark1 < accent < light1 < light2 < light3") {
            val p = onUiThreadGet { WSystemColors.accentPalette() }
            val ordered = listOf(p.dark3, p.dark2, p.dark1, p.accent, p.light1, p.light2, p.light3)
            ordered.forEach { withClue("alpha of $it") { it.alpha shouldBe 255 } }
            ordered.zipWithNext().forEach { (darker, lighter) ->
                withClue("$lighter should be lighter than $darker") { lighter.luminance() shouldBeGreaterThan darker.luminance() }
            }
        }

        test("accentColor is the palette's accent, and two reads give the same palette") {
            onUiThreadGet { WSystemColors.accentColor } shouldBe onUiThreadGet { WSystemColors.accentPalette().accent }
            onUiThreadGet { WSystemColors.accentPalette() } shouldBe onUiThreadGet { WSystemColors.accentPalette() }
        }

        test("the palette equals the AccentPalette Windows keeps in the registry (light3, light2, light1, accent, dark1, dark2, dark3 as RGBA)") {
            val registry = registryAccentPalette()
            if (registry != null) {
                val p = onUiThreadGet { WSystemColors.accentPalette() }
                listOf(p.light3, p.light2, p.light1, p.accent, p.dark1, p.dark2, p.dark3) shouldBe registry
            }
        }

        test("a change reported from a background thread reaches the listener on the UI thread with the current palette, and stops after removal") {
            val received = LinkedBlockingQueue<Pair<Thread, WAccentPalette>>()
            val listener = java.util.function.Consumer<WAccentPalette> { received.add(Thread.currentThread() to it) }
            val uiThread = onUiThreadGet { Thread.currentThread() }
            WSystemColors.addAccentChangeListener(listener)
            try {
                Thread { WSystemColors.notifyAccentChanged() }.start()
                val (thread, palette) = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: error("The listener was never called")
                thread shouldBe uiThread
                palette shouldBe onUiThreadGet { WSystemColors.accentPalette() }
            } finally {
                WSystemColors.removeAccentChangeListener(listener)
            }
            WSystemColors.notifyAccentChanged()
            onUiThreadGet { } // anything posted by the notification has run by now
            received.poll(SHORT_WAIT_MS, TimeUnit.MILLISECONDS) shouldBe null
        }

        test("two registered listeners are both called") {
            val calls = LinkedBlockingQueue<String>()
            val a = java.util.function.Consumer<WAccentPalette> { calls.add("a") }
            val b = java.util.function.Consumer<WAccentPalette> { calls.add("b") }
            WSystemColors.addAccentChangeListener(a)
            WSystemColors.addAccentChangeListener(b)
            try {
                Thread { WSystemColors.notifyAccentChanged() }.start()
                val first = calls.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                val second = calls.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                listOfNotNull(first, second).sorted() shouldContainExactly listOf("a", "b")
            } finally {
                WSystemColors.removeAccentChangeListener(a)
                WSystemColors.removeAccentChangeListener(b)
            }
        }
    }

    /** The first seven colors of HKCU Explorer Accent AccentPalette (null if the user has never picked an accent color). */
    private fun registryAccentPalette(): List<WColor>? {
        val process = ProcessBuilder(
            "reg",
            "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Accent",
            "/v",
            "AccentPalette",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val hex = Regex("REG_BINARY\\s+([0-9a-fA-F]+)").find(output)?.groupValues?.get(1) ?: return null
        val bytes = hex.chunked(2).map { it.toInt(HEX) }
        return (0 until PALETTE_COLORS).map { WColor(bytes[4 * it], bytes[4 * it + 1], bytes[4 * it + 2], 255) }
    }

    private companion object {
        const val HEX = 16
        const val PALETTE_COLORS = 7
        const val TIMEOUT_SECONDS = 10L
        const val SHORT_WAIT_MS = 300L
    }
}
