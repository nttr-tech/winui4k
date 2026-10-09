package com.appkitbox.winui4k

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.math.BigDecimal

/** Tests for TableCellValues.convert, which converts cell values written by editing to the model column's type (no UI needed). */
class TableCellValuesTest : FunSpec() {
    init {
        test("text box strings are converted to the numeric column's type (surrounding whitespace is ignored)") {
            listOf(
                TableCellValues.convert(" 12 ", Integer::class.java),
                TableCellValues.convert("1234567890123", java.lang.Long::class.java),
                TableCellValues.convert("2.5", java.lang.Double::class.java),
                TableCellValues.convert("10.25", BigDecimal::class.java),
            ) shouldBe listOf(12, 1234567890123L, 2.5, BigDecimal("10.25"))
        }

        test("a Double from a NumberBox / Slider is converted for an integer column if it has no fractional part") {
            TableCellValues.convert(3.0, Integer::class.java) shouldBe 3
        }

        test("numbers with a fractional part or out of range cannot be written to an integer column") {
            TableCellValues.convert(3.5, Integer::class.java) shouldBeSameInstanceAs TableCellValues.INVALID
            TableCellValues.convert(300.0, java.lang.Byte::class.java) shouldBeSameInstanceAs TableCellValues.INVALID
        }

        test("non-numeric strings cannot be written to a numeric column") {
            TableCellValues.convert("abc", Integer::class.java) shouldBeSameInstanceAs TableCellValues.INVALID
            TableCellValues.convert("", java.lang.Double::class.java) shouldBeSameInstanceAs TableCellValues.INVALID
        }

        test("a Boolean column accepts a check box Boolean and the strings true / false") {
            listOf(
                TableCellValues.convert(true, java.lang.Boolean::class.java),
                TableCellValues.convert("FALSE", java.lang.Boolean::class.java),
            ) shouldBe listOf(true, false)
            TableCellValues.convert("yes", java.lang.Boolean::class.java) shouldBeSameInstanceAs TableCellValues.INVALID
        }

        test("a String column gets a string, an Object column gets the written value as is, and null stays null") {
            listOf(
                TableCellValues.convert(42, String::class.java),
                TableCellValues.convert("as is", Any::class.java),
                TableCellValues.convert(null, Integer::class.java),
            ) shouldBe listOf("42", "as is", null)
        }
    }
}
