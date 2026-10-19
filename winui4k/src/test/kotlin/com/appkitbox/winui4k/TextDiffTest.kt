package com.appkitbox.winui4k

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for [TextDiff.between], which computes the DocumentEvent offset and length from the before and after strings (no
 * window is used).
 * The expected values are the offsets and lengths of insertString / remove when the same edit is made in a Swing Document.
 */
class TextDiffTest : FunSpec() {
    init {
        test("identical strings have no difference (null)") {
            TextDiff.between("", "").shouldBeNull()
            TextDiff.between("ab", "ab").shouldBeNull()
        }

        test("typing into an empty string is an insertion at offset 0") {
            TextDiff.between("", "abc") shouldBe TextDiff(0, 0, 3)
        }

        test("deleting everything is a removal of the original length from offset 0") {
            TextDiff.between("abc", "") shouldBe TextDiff(0, 3, 0)
        }

        test("appending at the end, prepending at the start and inserting in the middle give the inserted offset and length") {
            TextDiff.between("ab", "abc") shouldBe TextDiff(2, 0, 1)
            TextDiff.between("bc", "abc") shouldBe TextDiff(0, 0, 1)
            TextDiff.between("ac", "abbc") shouldBe TextDiff(1, 0, 2)
        }

        test("a removal in the middle gives the removed offset and length") {
            TextDiff.between("abcdef", "abef") shouldBe TextDiff(2, 2, 0)
        }

        test("a replacement is a removal and insertion of the range excluding the matching start and end") {
            TextDiff.between("abcdef", "abXYZef") shouldBe TextDiff(2, 2, 3)
            TextDiff.between("abc", "xyz") shouldBe TextDiff(0, 3, 3)
        }

        test("the length does not become negative even when the matching start and end overlap") {
            TextDiff.between("aba", "abba") shouldBe TextDiff(2, 0, 1)
            TextDiff.between("abba", "aba") shouldBe TextDiff(2, 1, 0)
        }

        test("an insertion within a run of the same character goes after the matching part when the caret is unknown") {
            TextDiff.between("aa", "aaa") shouldBe TextDiff(2, 0, 1)
        }

        test("an insertion within a run of the same character ends right before the caret (typing a at the start puts the caret at 1)") {
            TextDiff.between("aa", "aaa", caretAfter = 1) shouldBe TextDiff(0, 0, 1)
            TextDiff.between("aa", "aaa", caretAfter = 2) shouldBe TextDiff(1, 0, 1)
            TextDiff.between("xaay", "xaaay", caretAfter = 2) shouldBe TextDiff(1, 0, 1)
        }

        test("a removal within a run of the same character is taken to be at the caret position (deleting the first a puts the caret at 0)") {
            TextDiff.between("aaa", "aa", caretAfter = 0) shouldBe TextDiff(0, 1, 0)
            TextDiff.between("aaa", "aa", caretAfter = 1) shouldBe TextDiff(1, 1, 0)
        }

        test("a caret position inconsistent with the difference is not used as a hint") {
            // In "abc" -> "abXc", the caret right after the inserted X is 3. Caret 1 is inconsistent, so after the matching part (2)
            TextDiff.between("abc", "abXc", caretAfter = 1) shouldBe TextDiff(2, 0, 1)
            // A caret beyond the string or before the inserted length is also inconsistent
            TextDiff.between("aa", "aaa", caretAfter = 10) shouldBe TextDiff(2, 0, 1)
            TextDiff.between("aa", "aaaa", caretAfter = 1) shouldBe TextDiff(2, 0, 2)
        }

        test("the caret also determines the position for a replacement (retyping a selected a as b)") {
            // Selecting the 2nd character (a) of "aab" and typing b gives "abb", with the caret at 2
            TextDiff.between("aab", "abb", caretAfter = 2) shouldBe TextDiff(1, 1, 1)
        }
    }
}
