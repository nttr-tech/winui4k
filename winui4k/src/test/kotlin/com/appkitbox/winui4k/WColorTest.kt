package com.appkitbox.winui4k

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests WColor's value equality (equals / hashCode) (no UI needed). */
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

        test("toString shows the four components") {
            WColor(1, 2, 3, 4).toString() shouldBe "WColor(red=1, green=2, blue=3, alpha=4)"
        }
    }
}
