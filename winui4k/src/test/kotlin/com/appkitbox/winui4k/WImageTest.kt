package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for specifying the image of WImage (an Image in the style of JLabel + ImageIcon) by file path, URI or WImageIcon,
 * and for reading it back from the native Image.Source. The PNGs for the tests are written to a temporary folder.
 */
class WImageTest : FunSpec() {
    /** A file with a PNG of width [width] and height [height], written to a folder whose name contains spaces. */
    private fun pngFile(width: Int, height: Int): File {
        val folder = Files.createTempDirectory("winui4k image test ").toFile().also { it.deleteOnExit() }
        val file = File(folder, "test image.png").also { it.deleteOnExit() }
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", file)
        return file
    }

    init {
        test("a WImage created without arguments has no image, and sourceUri and icon are null") {
            val (uri, icon) = onUiThreadGet {
                val image = WImage()
                image.sourceUri to image.icon
            }
            uri shouldBe null
            icon shouldBe null
        }

        test("a WImage created with a URI reads back sourceUri as the passed URI") {
            onUiThreadGet { WImage("ms-appx:///Assets/logo.png").sourceUri } shouldBe "ms-appx:///Assets/logo.png"
            onUiThreadGet { WImage("https://example.com/a/b.png").sourceUri } shouldBe "https://example.com/a/b.png"
        }

        test("a WImage created with a file path reads back sourceUri as the file URI of the absolute path (spaces as %20)") {
            val file = pngFile(8, 4)
            val expected = file.toPath().toUri().toString()
            onUiThreadGet { WImage(file.path).sourceUri } shouldBe expected
            onUiThreadGet { WImage(file.path.replace('\\', '/')).sourceUri } shouldBe expected
            expected.contains("test%20image.png") shouldBe true
        }

        test("setting a path to sourceUri reads back the same as a URI, and null removes the image") {
            val file = pngFile(8, 4)
            val (set, cleared) = onUiThreadGet {
                val image = WImage()
                image.sourceUri = file.path
                val set = image.sourceUri
                image.sourceUri = null
                set to image.sourceUri
            }
            set shouldBe file.toPath().toUri().toString()
            cleared shouldBe null
        }

        test("setting a WImageIcon to icon makes sourceUri its URI, and icon returns an equal WImageIcon") {
            val file = pngFile(8, 4)
            val icon = WImageIcon(file.path)
            val (uri, read) = onUiThreadGet {
                val image = WImage()
                image.icon = icon
                image.sourceUri to image.icon
            }
            uri shouldBe icon.uri
            read shouldBe icon
        }

        test("a WImage created with a WImageIcon (the same shape as JLabel(Icon)) has an icon equal to that WImageIcon") {
            val icon = WImageIcon(pngFile(8, 4).path)
            onUiThreadGet { WImage(icon).icon } shouldBe icon
        }

        test("icon = null removes the image") {
            val icon = WImageIcon(pngFile(8, 4).path)
            val (before, after) = onUiThreadGet {
                val image = WImage(icon)
                val before = image.icon
                image.icon = null
                before to image.icon
            }
            before shouldBe icon
            after shouldBe null
        }

        test("Symbol and WFontIcon cannot be shown in an Image, so they throw IllegalArgumentException and the image does not change") {
            val icon = WImageIcon(pngFile(8, 4).path)
            val image = onUiThreadGet { WImage(icon) }
            shouldThrow<IllegalArgumentException> { onUiThread { image.icon = Symbol.FIND } }
            shouldThrow<IllegalArgumentException> { onUiThread { image.icon = WFontIcon("\uE74E") } }
            onUiThreadGet { image.icon } shouldBe icon
        }

        test("stretch reads back the set value, and the default is UNIFORM") {
            val (initial, changed) = onUiThreadGet {
                val image = WImage()
                val initial = image.stretch
                image.stretch = Stretch.FILL
                initial to image.stretch
            }
            initial shouldBe Stretch.UNIFORM
            changed shouldBe Stretch.FILL
        }

        test("with stretch NONE the loaded image is shown at its original size (width 40, height 20), and replacing icon gives the new image's size") {
            val small = WImageIcon(pngFile(40, 20).path)
            val large = WImageIcon(pngFile(80, 60).path)
            val image = onUiThreadGet { WImage(small).also { it.stretch = Stretch.NONE } }
            UiTestHarness.attachAndAwaitLoaded(image)
            try {
                eventually(5.seconds) {
                    onUiThreadGet {
                        image.updateLayout()
                        image.actualWidth to image.actualHeight
                    } shouldBe (40.0 to 20.0)
                }
                onUiThread { image.icon = large }
                eventually(5.seconds) {
                    val (width, height) = onUiThreadGet {
                        image.updateLayout()
                        image.actualWidth to image.actualHeight
                    }
                    width shouldBe (80.0 plusOrMinus 0.5)
                    height shouldBe (60.0 plusOrMinus 0.5)
                }
            } finally {
                UiTestHarness.detach(image)
            }
        }

        test("a path to a nonexistent file does not throw and shows nothing (the size stays 0)") {
            val image = onUiThreadGet { WImage("C:/winui4k-test/no-such-folder/missing.png") }
            UiTestHarness.attachAndAwaitLoaded(image)
            try {
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet {
                    image.updateLayout()
                    image.actualWidth to image.actualHeight
                } shouldBe (0.0 to 0.0)
            } finally {
                UiTestHarness.detach(image)
            }
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 700L
    }
}
