package com.appkitbox.winui4k.extension.ribbon.model

import java.text.Collator
import java.text.Normalizer

/**
 * A searchable command (an item of "Tell Me" / Microsoft Search / a command palette).
 * Items whose [isEnabled] is false are ranked lower.
 */
data class RibbonSearchEntry @JvmOverloads constructor(
    /** A unique id. */
    val id: String,
    /** The label. */
    val label: String,
    /** The location (e.g. "Home › Font"). */
    val path: String? = null,
    /** The description. */
    val description: String? = null,
    /** Keywords and synonyms. */
    val keywords: List<String>? = null,
    /** The shortcut text. */
    val shortcut: String? = null,
    /** The execution target (a model, an action, and so on). */
    val target: Any? = null,
    /** Whether it is enabled (disabled items are ranked lower). */
    val isEnabled: Boolean = true,
)

/** A ranked search result (a higher [score] ranks higher). */
data class RibbonSearchResult(
    /** Items. */
    val entry: RibbonSearchEntry,
    /** The score. */
    val score: Double,
)

/**
 * Ranked command search that ignores diacritical marks and case.
 * Every search term must match the label, keywords, location, or description
 * (prefix match, word start, substring, acronym, or fuzzy subsequence). Label matches rank highest.
 */
class RibbonSearchEngine {
    private val mutableEntries = mutableListOf<RibbonSearchEntry>()
    private val mutableRecent = mutableListOf<String>()

    /** The maximum number of recently used ids to remember. */
    var maxRecent: Int = 8

    /** The entries of the index. */
    val entries: List<RibbonSearchEntry> get() = mutableEntries

    /** The ids of recently executed entries (newest first). */
    val recent: List<String> get() = mutableRecent

    /** Replaces the index (for entries with the same id, only the first is kept). */
    fun setEntries(entries: Iterable<RibbonSearchEntry>) {
        mutableEntries.clear()
        for (entry in entries) {
            if (mutableEntries.none { it.id == entry.id }) mutableEntries += entry
        }
    }

    /** Adds an entry (replacing any entry with the same id). */
    fun add(entry: RibbonSearchEntry) {
        val index = mutableEntries.indexOfFirst { it.id == entry.id }
        if (index >= 0) mutableEntries[index] = entry else mutableEntries += entry
    }

    /** Records an executed entry (it ranks higher in later searches). */
    fun markUsed(id: String?) {
        if (id.isNullOrEmpty()) return
        mutableRecent.remove(id)
        mutableRecent.add(0, id)
        trimRecent()
    }

    /** Replaces the recently used list (to restore saved state, for example). Empty and duplicate ids are removed. */
    fun setRecent(ids: Iterable<String?>) {
        mutableRecent.clear()
        for (id in ids) {
            if (!id.isNullOrEmpty() && id !in mutableRecent) mutableRecent += id
        }
        trimRecent()
    }

    private fun trimRecent() {
        while (mutableRecent.size > maxOf(0, maxRecent)) mutableRecent.removeAt(mutableRecent.size - 1)
    }

    /** The recently used entries (only those in the index, newest first). */
    fun getRecentEntries(): List<RibbonSearchEntry> = mutableRecent.mapNotNull { id -> mutableEntries.firstOrNull { it.id == id } }

    /** Searches. Returns the recently used entries for an empty query. */
    @JvmOverloads
    fun search(query: String?, maxResults: Int = 12): List<RibbonSearchResult> {
        val terms = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) {
            return getRecentEntries().mapIndexed { i, entry -> RibbonSearchResult(entry, 100.0 - i) }.take(maxResults)
        }
        val results = mutableListOf<RibbonSearchResult>()
        for (entry in mutableEntries) {
            var score = scoreEntry(entry, terms)
            if (score <= 0) continue
            val recentIndex = mutableRecent.indexOf(entry.id)
            if (recentIndex >= 0) score += 5 - minOf(4.0, recentIndex * 0.5)
            if (!entry.isEnabled) score *= 0.5
            results += RibbonSearchResult(entry, score)
        }
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return results.sortedWith(
            compareByDescending<RibbonSearchResult> { it.score }
                .thenBy { it.entry.label.length }
                .thenComparator { a, b -> collator.compare(a.entry.label, b.entry.label) },
        ).take(maxResults)
    }

    companion object {
        /** The score of an entry for the normalized search terms [terms] (0 = no match). */
        @JvmStatic
        fun scoreEntry(entry: RibbonSearchEntry, terms: List<String>): Double {
            val label = normalize(entry.label)
            val keywords = entry.keywords?.map { normalize(it) }.orEmpty()
            val path = normalize(entry.path)
            val description = normalize(entry.description)
            if (terms.isEmpty()) return 0.0
            var total = 0.0
            for (term in terms) {
                val keywordScore = keywords.maxOfOrNull { scoreText(it, term, 0.8) } ?: 0.0
                val best = maxOf(scoreText(label, term, 1.0), keywordScore, scoreText(path, term, 0.45), scoreText(description, term, 0.35))
                if (best <= 0) return 0.0
                total += best
            }
            if (label == terms.joinToString(" ")) total += 50
            return total / terms.size
        }

        private fun scoreText(text: String, term: String, weight: Double): Double = when {
            text.isEmpty() -> 0.0
            text == term -> 100 * weight
            text.startsWith(term) -> 85 * weight
            text.contains(" $term") -> 70 * weight
            text.contains(term) -> 50 * weight
            isAcronym(text, term) -> 60 * weight
            else -> fuzzyScore(text, term) * weight
        }

        /** An acronym ("fp" → "format painter"). */
        private fun isAcronym(text: String, term: String): Boolean {
            val words = text.split(' ').filter { it.isNotEmpty() }
            return term.length >= 2 && words.size >= term.length && words.map { it[0] }.joinToString("").startsWith(term)
        }

        /** The score of a fuzzy subsequence (only for search terms of three or more characters; 0 if there is no match). */
        private fun fuzzyScore(text: String, term: String): Double {
            if (term.length < 3) return 0.0
            var ti = 0
            var gaps = 0
            var last = -1
            var i = 0
            while (i < text.length && ti < term.length) {
                if (text[i] == term[ti]) {
                    if (last >= 0 && i - last > 1) gaps++
                    last = i
                    ti++
                }
                i++
            }
            return if (ti == term.length && gaps <= maxOf(1, term.length / 2)) 30.0 - gaps * 5 else 0.0
        }

        /** Lowercases the text and removes diacritical marks and symbols. */
        @JvmStatic
        fun normalize(text: String?): String {
            if (text.isNullOrBlank()) return ""
            val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
            val builder = StringBuilder(decomposed.length)
            var lastSpace = true
            for (c in decomposed) {
                if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) continue
                if (c.isLetterOrDigit()) {
                    builder.append(foldLetter(c.lowercaseChar()))
                    lastSpace = false
                } else if (!lastSpace) {
                    builder.append(' ')
                    lastSpace = true
                }
            }
            return builder.toString().trim()
        }

        /** Maps characters with no Unicode decomposition (Polish ł, Nordic ø, German ß, and so on) to their base letters. */
        private fun foldLetter(lower: Char): String = when (lower) {
            'ł' -> "l"
            'ø' -> "o"
            'đ' -> "d"
            'ħ' -> "h"
            'ı' -> "i"
            'æ' -> "ae"
            'œ' -> "oe"
            'ß' -> "ss"
            'þ' -> "th"
            else -> lower.toString()
        }
    }
}
