package com.appkitbox.winui4k.ribbon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotStartWith

/** Tests for KeyTip assignment (RibbonKeyTipAssigner) and input matching (RibbonKeyTipScope / Navigator). */
class RibbonKeyTipTest : FunSpec() {
    private fun assign(vararg labels: String): List<String> = RibbonKeyTipAssigner.assign(labels.map { RibbonKeyTipRequest(it) })

    /** Asserts that there are no duplicates and no KeyTip is a prefix of another. */
    private fun assertPrefixFree(tips: List<String>) {
        tips.toSet() shouldHaveSize tips.size
        for (a in tips) {
            a.isNotEmpty() shouldBe true
            for (b in tips) {
                if (a != b) b shouldNotStartWith a
            }
        }
    }

    init {
        test("uses the first letter of each label when they do not collide") {
            assign("Home", "Insert", "Design") shouldBe listOf("H", "I", "D")
        }

        test("colliding first letters still yield unique KeyTips that are not prefixes of each other") {
            assertPrefixFree(assign("Paste", "Page Layout", "Print", "Picture", "Pen", "Paragraph"))
        }

        test("explicit KeyTips take priority, and reserved KeyTips and their prefixes are not used") {
            val tips = RibbonKeyTipAssigner.assign(
                listOf(RibbonKeyTipRequest("Home", "H"), RibbonKeyTipRequest("Help"), RibbonKeyTipRequest("File menu")),
                reserved = listOf("F"),
            )
            tips[0] shouldBe "H"
            tips[1] shouldNotBe "H"
            tips[2] shouldNotStartWith "F"
        }

        test("a conflicting explicit KeyTip falls back to generation") {
            val tips = RibbonKeyTipAssigner.assign(listOf(RibbonKeyTipRequest("Bold", "B"), RibbonKeyTipRequest("Bullets", "B")))
            tips[0] shouldBe "B"
            tips[1] shouldNotStartWith "B"
            assertPrefixFree(tips)
        }

        test("empty labels are also assigned distinct non-empty KeyTips") {
            val tips = assign("", "", "")
            assertPrefixFree(tips)
        }

        test("80 identical labels all get distinct KeyTips") {
            assertPrefixFree(assign(*Array(80) { "Bold" }))
        }

        test("builds KeyTips from characters with diacritical marks removed") {
            assign("Éditer")[0] shouldBe "E"
        }

        test("normalize uppercases, keeps only letters and digits, and returns an empty string for whitespace only") {
            RibbonKeyTipAssigner.normalize(" f-p ") shouldBe "FP"
            RibbonKeyTipAssigner.normalize("   ") shouldBe ""
            RibbonKeyTipAssigner.normalize(null) shouldBe ""
        }

        listOf(36, 57, 80, 400).forEach { count ->
            test("assigns prefix-free KeyTips to all items even in a large scope of $count items") {
                val words = listOf(
                    "Paste", "Cut", "Copy", "Format", "Bold", "Italic", "Underline", "Strike", "Subscript", "Superscript",
                    "Highlight", "Color", "Zoom", "Find", "Replace", "Select", "Styles", "Bullets", "Numbering", "Indent",
                    "Outdent", "Sort", "Show", "Align", "Justify", "Line", "Shading", "Borders", "Dictate", "Editor", "Reuse",
                    "Grow", "Shrink", "Case", "Clear", "Effects", "Normal", "Heading", "Title", "Quote", "Emphasis", "Wrap",
                    "Merge", "Insert", "Delete", "Sum", "Fill", "Filter", "Analyze", "Sensitivity", "Add-ins", "Comments",
                    "Share", "Translate", "Read", "Track", "Accept",
                )
                val labels = (0 until count).map { i -> words[i % words.size] + if (i >= words.size) " $i" else "" }
                val tips = RibbonKeyTipAssigner.assign(labels.map { RibbonKeyTipRequest(it) })
                tips shouldHaveSize count
                assertPrefixFree(tips)
            }
        }

        test("QAT KeyTips follow the Office convention (1..9, then 09, 08, ...)") {
            RibbonKeyTipAssigner.assignQuickAccess(12) shouldBe
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "09", "08", "07")
        }

        test("QAT KeyTips stay prefix-free beyond 44 items, and a negative count yields none") {
            val tips = RibbonKeyTipAssigner.assignQuickAccess(80)
            tips shouldHaveSize 80
            tips[0] shouldBe "1"
            tips[9] shouldBe "09"
            tips[18] shouldBe "0A"
            tips[44] shouldBe "009"
            assertPrefixFree(tips)
            RibbonKeyTipAssigner.assignQuickAccess(-3) shouldBe emptyList()
            RibbonKeyTipAssigner.assignQuickAccess(0) shouldBe emptyList()
        }

        test("a scope matches multi-key KeyTips by prefix, ignoring non-matching keys and keeping the typed prefix") {
            val scope = RibbonKeyTipScope<String>()
            scope.add("FP", "format painter")
            scope.add("FF", "font")
            scope.add("V", "paste")
            scope.process('f') shouldBe RibbonKeyTipProcessResult(RibbonKeyTipMatch.PARTIAL, null)
            scope.candidates shouldHaveSize 2
            scope.typed shouldBe "F"
            scope.process('x') shouldBe RibbonKeyTipProcessResult(RibbonKeyTipMatch.NONE, null)
            scope.typed shouldBe "F"
            scope.process('p') shouldBe RibbonKeyTipProcessResult(RibbonKeyTipMatch.COMPLETE, "format painter")
            scope.typed shouldBe ""
            scope.process('x').match shouldBe RibbonKeyTipMatch.NONE
            scope.process('v') shouldBe RibbonKeyTipProcessResult(RibbonKeyTipMatch.COMPLETE, "paste")
        }

        test("KeyTips added to a scope are normalized, and KeyTips with no letters or digits are rejected") {
            val scope = RibbonKeyTipScope<String>()
            scope.add("f p", "format painter")
            scope.entries[0].tip shouldBe "FP"
            shouldThrow<IllegalArgumentException> { scope.add("--", "invalid") }
        }

        test("backspace undoes only the last key and returns false when nothing is typed") {
            val scope = RibbonKeyTipScope<String>()
            scope.add("AB", "x")
            scope.process('a')
            scope.backspace() shouldBe true
            scope.typed shouldBe ""
            scope.backspace() shouldBe false
        }

        test("the navigator stacks levels, and leaving the last level ends KeyTip mode") {
            val navigator = RibbonKeyTipNavigator<String>()
            navigator.isActive shouldBe false
            val top = RibbonKeyTipScope<String>()
            val nested = RibbonKeyTipScope<String>()
            navigator.push(top)
            navigator.push(nested)
            navigator.depth shouldBe 2
            (navigator.current === nested) shouldBe true
            navigator.pop() shouldBe true
            (navigator.current === top) shouldBe true
            navigator.pop() shouldBe false
            navigator.isActive shouldBe false
            navigator.pop() shouldBe false
        }
    }
}
