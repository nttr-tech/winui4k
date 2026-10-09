package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Tests for ranking in command search (RibbonSearchEngine) and for managing recently used items. */
class RibbonSearchEngineTest : FunSpec() {
    private fun engine(): RibbonSearchEngine = RibbonSearchEngine().apply {
        setEntries(
            listOf(
                RibbonSearchEntry("paste", "Paste", "Home › Clipboard", keywords = listOf("clipboard")),
                RibbonSearchEntry("format-painter", "Format Painter", "Home › Clipboard"),
                RibbonSearchEntry("bold", "Bold", "Home › Font", "Make your text bold.", shortcut = "Ctrl+B"),
                RibbonSearchEntry("font-color", "Font Color", "Home › Font"),
                RibbonSearchEntry("insert-table", "Table", "Insert › Tables", keywords = listOf("grid", "tabulka")),
                RibbonSearchEntry("pivot", "PivotTable", "Insert › Tables", isEnabled = false),
                RibbonSearchEntry("page-color", "Page Color", "Design › Page Background"),
            ),
        )
    }

    private fun ids(results: List<RibbonSearchResult>): List<String> = results.map { it.entry.id }

    init {
        test("an item whose label matches exactly ranks first") {
            engine().search("bold")[0].entry.id shouldBe "bold"
        }

        test("multiple search terms return only items that match all of them") {
            val results = engine().search("font color")
            results[0].entry.id shouldBe "font-color"
            ids(results).contains("paste") shouldBe false
        }

        test("keywords, locations, and acronyms also match") {
            val engine = engine()
            ids(engine.search("grid")).contains("insert-table") shouldBe true
            ids(engine.search("clipboard")).contains("format-painter") shouldBe true
            engine.search("fp")[0].entry.id shouldBe "format-painter"
        }

        test("a word found only in the description also matches") {
            ids(engine().search("text")) shouldBe listOf("bold")
        }

        test("tolerates fuzzy subsequences and differences in diacritical marks and case") {
            val engine = engine()
            ids(engine.search("pste")).contains("paste") shouldBe true
            ids(engine.search("PÁSTE")).contains("paste") shouldBe true
        }

        test("disabled items rank lower, and recently used items rank higher") {
            val engine = engine()
            engine.search("table")[0].entry.id shouldBe "insert-table"
            engine.markUsed("page-color")
            engine.search("")[0].entry.id shouldBe "page-color"
            engine.search("color")[0].entry.id shouldBe "page-color"
        }

        test("a search term that matches nothing returns an empty result") {
            engine().search("zzzzqqq").shouldBeEmpty()
        }

        test("results are limited to maxResults") {
            engine().search("o", maxResults = 2) shouldHaveSize 2
        }

        test("normalize removes symbols and collapses whitespace into one space") {
            RibbonSearchEngine.normalize("Sort & Filter!") shouldBe "sort filter"
            RibbonSearchEngine.normalize("  ") shouldBe ""
            RibbonSearchEngine.normalize(null) shouldBe ""
        }

        test("normalize maps characters without a decomposition (ł, ß) to their base letters") {
            RibbonSearchEngine.normalize("Łącz") shouldBe "lacz"
            RibbonSearchEngine.normalize("Straße") shouldBe "strasse"
        }

        test("scoreEntry returns 0 when there are no search terms") {
            RibbonSearchEngine.scoreEntry(RibbonSearchEntry("x", "X"), emptyList()) shouldBe 0.0
        }

        test("the recent list respects its maximum size and avoids duplicates, and adding an item with the same id replaces it") {
            val engine = RibbonSearchEngine().apply { maxRecent = 2 }
            engine.setRecent(listOf("a", "b", "a", "c"))
            engine.recent shouldBe listOf("a", "b")
            engine.add(RibbonSearchEntry("a", "A"))
            engine.add(RibbonSearchEntry("a", "A2"))
            engine.entries shouldHaveSize 1
            engine.entries[0].label shouldBe "A2"
        }

        test("markUsed ignores an empty id and moves an existing id to the front") {
            val engine = engine()
            engine.markUsed("bold")
            engine.markUsed("paste")
            engine.markUsed("bold")
            engine.markUsed("")
            engine.recent shouldBe listOf("bold", "paste")
            engine.getRecentEntries().map { it.id } shouldBe listOf("bold", "paste")
        }

        test("setEntries keeps only the first of items with the same id") {
            val engine = RibbonSearchEngine()
            engine.setEntries(listOf(RibbonSearchEntry("a", "First"), RibbonSearchEntry("a", "Second")))
            engine.entries.map { it.label } shouldBe listOf("First")
        }
    }
}
