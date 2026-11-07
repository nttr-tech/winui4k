package com.appkitbox.winui4k

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.awt.Color

/** Tests WColor's value equality (equals / hashCode) (no UI needed). */
@Tags("Pure")
class WColorTest : FunSpec() {
    init {
        test("colors with the same four components are equal even as different instances, and their hash codes match") {
            val a = WColor(10, 20, 30, 40)
            val b = WColor(10, 20, 30, 40)
            (a == b) shouldBe true
            a.hashCode() shouldBe b.hashCode()
        }

        test("a color with alpha omitted equals the color with alpha = 255") {
            WColor(1, 2, 3) shouldBe WColor(1, 2, 3, 255)
        }

        test("colors that differ in even one component are not equal") {
            val base = WColor(10, 20, 30, 40)
            listOf(WColor(11, 20, 30, 40), WColor(10, 21, 30, 40), WColor(10, 20, 31, 40), WColor(10, 20, 30, 41))
                .forEach { it shouldNotBe base }
        }

        test("is not equal to objects other than WColor") {
            WColor.BLACK.equals("WColor(red=0, green=0, blue=0, alpha=255)") shouldBe false
        }

        test("the named colors have the same values as the java.awt.Color colors of the same names (replacing Color.RED with WColor.RED does not change the color)") {
            val pairs = listOf(
                "BLACK" to (WColor.BLACK to Color.BLACK),
                "WHITE" to (WColor.WHITE to Color.WHITE),
                "GRAY" to (WColor.GRAY to Color.GRAY),
                "LIGHT_GRAY" to (WColor.LIGHT_GRAY to Color.LIGHT_GRAY),
                "RED" to (WColor.RED to Color.RED),
                "GREEN" to (WColor.GREEN to Color.GREEN),
                "BLUE" to (WColor.BLUE to Color.BLUE),
                "YELLOW" to (WColor.YELLOW to Color.YELLOW),
                "ORANGE" to (WColor.ORANGE to Color.ORANGE),
                "DARK_GRAY" to (WColor.DARK_GRAY to Color.DARK_GRAY),
                "PINK" to (WColor.PINK to Color.PINK),
                "MAGENTA" to (WColor.MAGENTA to Color.MAGENTA),
                "CYAN" to (WColor.CYAN to Color.CYAN),
            )
            pairs.map { (name, colors) -> name to colors.first } shouldBe
                pairs.map { (name, colors) -> name to colors.second.let { WColor(it.red, it.green, it.blue, it.alpha) } }
        }

        test("toString shows the four components") {
            WColor(1, 2, 3, 4).toString() shouldBe "WColor(red=1, green=2, blue=3, alpha=4)"
        }
    }
}
