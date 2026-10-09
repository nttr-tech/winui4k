package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/** Tests for formatting and parsing spinner numbers ([RibbonNumberFormat]). */
class RibbonNumberFormatTest : FunSpec({
    test("# digits appear only when needed, and 0 digits always appear") {
        RibbonNumberFormat.format(1.5, "0.##", null, Locale.ROOT) shouldBe "1.5"
        RibbonNumberFormat.format(2.0, "0.##", null, Locale.ROOT) shouldBe "2"
        RibbonNumberFormat.format(2.0, "0.00", null, Locale.ROOT) shouldBe "2.00"
        RibbonNumberFormat.format(3.25, "00.0", null, Locale.ROOT) shouldBe "03.3"
    }

    test("rounding goes away from zero as in .NET (symmetrically for negative numbers)") {
        RibbonNumberFormat.format(0.125, "0.##", null, Locale.ROOT) shouldBe "0.13"
        RibbonNumberFormat.format(-0.125, "0.##", null, Locale.ROOT) shouldBe "-0.13"
        RibbonNumberFormat.format(2.5, "0", null, Locale.ROOT) shouldBe "3"
    }

    test("a negative number that rounds to 0 gets no sign") {
        RibbonNumberFormat.format(-0.001, "0.##", null, Locale.ROOT) shouldBe "0"
    }

    test("if the integer part is only #, the leading 0 of numbers below 1 is omitted") {
        RibbonNumberFormat.format(0.5, "#.##", null, Locale.ROOT) shouldBe ".5"
    }

    test("units of two or more characters are separated by a space, and one-character units are appended directly") {
        RibbonNumberFormat.format(12.0, "0.##", "pt", Locale.ROOT) shouldBe "12 pt"
        RibbonNumberFormat.format(45.0, "0", "°", Locale.ROOT) shouldBe "45°"
    }

    test("the decimal separator follows the locale, and parsing accepts that locale's separator") {
        RibbonNumberFormat.format(1.5, "0.##", "cm", Locale.GERMANY) shouldBe "1,5 cm"
        RibbonNumberFormat.parse("1,5 cm", "cm", Locale.GERMANY) shouldBe 1.5
        RibbonNumberFormat.parse("1.5", "cm", Locale.GERMANY) shouldBe 1.5
    }

    test("parsing strips a trailing unit case-insensitively") {
        RibbonNumberFormat.parse(" 12 PT ", "pt", Locale.ROOT) shouldBe 12.0
        RibbonNumberFormat.parse("-3", null, Locale.ROOT) shouldBe -3.0
    }

    test("non-numeric, empty, and infinite input yields null") {
        RibbonNumberFormat.parse("abc", null, Locale.ROOT) shouldBe null
        RibbonNumberFormat.parse("pt", "pt", Locale.ROOT) shouldBe null
        RibbonNumberFormat.parse("", null, Locale.ROOT) shouldBe null
        RibbonNumberFormat.parse("Infinity", null, Locale.ROOT) shouldBe null
    }
})
