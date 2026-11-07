package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify buttons and labels with a [WImageIcon] in a [WFlowLayout] [WLayoutPanel] are laid out with their size after the image loads
 * (Swing's ImageIcon loads the image when created, so they are laid out with the image size from the start).
 *
 * The expected values are the sizes of reference components placed in a horizontal [WPanel] (StackPanel) that WinUI itself lays out. Both the WLayoutPanel's Canvas
 * and a horizontal StackPanel measure children without constraints, so the sizes match when it lays out again correctly.
 */
@Tags("E2E")
class WImageIconLayoutTest : FunSpec() {
    /** A file with an opaque [size] x [size] PNG written to a temporary folder. */
    private fun pngFile(size: Int): File {
        val folder = Files.createTempDirectory("winui4k image icon layout ").toFile().also { it.deleteOnExit() }
        val file = File(folder, "icon.png").also { it.deleteOnExit() }
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until size) for (x in 0 until size) image.setRGB(x, y, OPAQUE_GREEN)
        ImageIO.write(image, "png", file)
        return file
    }

    /**
     * Shows a component made with [make] in a 600-wide panel with a left-aligned [WFlowLayout] and another in a horizontal reference [WPanel],
     * and after the reference's height reaches [iconSize] or more (the image has loaded), returns the sizes of the panel's component and the reference.
     */
    private suspend fun measured(iconSize: Int, make: () -> WComponent): Pair<WSize, WSize> {
        val (root, actual, sample) = onUiThreadGet {
            val actual = make()
            val sample = make()
            val panel = WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also {
                it.width = 600.0
                it.height = 200.0
                it.add(actual)
            }
            val root = WPanel().also {
                it.horizontalAlignment = HorizontalAlignment.LEFT
                it.verticalAlignment = VerticalAlignment.TOP
                it.add(panel)
                it.add(WPanel(0.0, Orientation.HORIZONTAL).also { row -> row.add(sample) })
            }
            Triple(root, actual, sample)
        }
        UiTestHarness.attachAndAwaitLoaded(root)
        try {
            eventually(5.seconds) { onUiThreadGet { sample.size.height } shouldBeGreaterThanOrEqual iconSize.toDouble() }
            repeat(SETTLE_TURNS) { onUiThread { root.updateLayout() } }
            return onUiThreadGet { actual.size to sample.size }
        } finally {
            UiTestHarness.detach(root)
        }
    }

    init {
        test("a WLabel with text to the right of an image icon gets the reference size after the image loads, and the text is not cut") {
            val icon = WImageIcon(pngFile(ICON_SIZE).path)
            val (actual, expected) = measured(ICON_SIZE) { WLabel("Signed in as alice", icon) }
            actual shouldBe expected
        }

        test("a WLabel with only an image icon gets the image size") {
            val icon = WImageIcon(pngFile(ICON_SIZE).path)
            val (actual, expected) = measured(ICON_SIZE) { WLabel(icon) }
            actual shouldBe expected
            actual shouldBe WSize(ICON_SIZE.toDouble(), ICON_SIZE.toDouble())
        }

        test("a WLabel with text below an image icon gets the reference size") {
            val icon = WImageIcon(pngFile(ICON_SIZE).path)
            val (actual, expected) = measured(ICON_SIZE) {
                WLabel("Profile", icon).also {
                    it.horizontalTextPosition = HorizontalTextPosition.CENTER
                    it.verticalTextPosition = VerticalTextPosition.BOTTOM
                }
            }
            actual shouldBe expected
        }

        test("a WButton with text below an image icon gets the reference size") {
            val icon = WImageIcon(pngFile(ICON_SIZE).path)
            val (actual, expected) = measured(ICON_SIZE) {
                WButton("Download", icon).also {
                    it.horizontalTextPosition = HorizontalTextPosition.CENTER
                    it.verticalTextPosition = VerticalTextPosition.BOTTOM
                }
            }
            actual shouldBe expected
        }

        test("a WButton with only an image icon gets the reference size") {
            val icon = WImageIcon(pngFile(ICON_SIZE).path)
            val (actual, expected) = measured(ICON_SIZE) { WButton(icon) }
            actual shouldBe expected
        }

        test("a WLabel with an icon whose image cannot be loaded also gets the reference size (the size of the text alone)") {
            val missing = File(Files.createTempDirectory("winui4k missing icon ").toFile(), "missing.png")
            val icon = WImageIcon(missing.path)
            val (actual, expected) = measured(0) { WLabel("No image", icon) }
            actual shouldBe expected
        }
    }

    private companion object {
        const val ICON_SIZE = 48
        const val SETTLE_TURNS = 6
        const val OPAQUE_GREEN = 0xFF20A040.toInt()
    }
}
