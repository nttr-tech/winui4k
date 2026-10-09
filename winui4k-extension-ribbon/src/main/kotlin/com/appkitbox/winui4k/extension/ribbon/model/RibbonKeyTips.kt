package com.appkitbox.winui4k.extension.ribbon.model

import java.text.Normalizer

/** A request for a KeyTip assignment (generated from [label]; [explicit] takes precedence when valid and not a duplicate). */
data class RibbonKeyTipRequest @JvmOverloads constructor(
    /** The label the KeyTip is generated from. */
    val label: String?,
    /** The explicit KeyTip. */
    val explicit: String? = null,
)

/**
 * Assigns KeyTips ("H", "N", "FP", "1") that are unique and not prefixes of one another to a scope such as the tab row
 * or the commands of one tab. Explicit KeyTips take precedence; generated KeyTips are chosen in this order: the first
 * letter of the label → the first letter of each word → the other letters of the label → two-letter pairs → digits.
 */
object RibbonKeyTipAssigner {
    private const val SINGLE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    private const val PAIR_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val DIGITS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

    /**
     * Assigns KeyTips. The result has one KeyTip per request (no nulls and no duplicates).
     * [reserved] lists the KeyTips already used in the scope (such as "F" for the [File] button).
     */
    @JvmStatic
    @JvmOverloads
    fun assign(requests: List<RibbonKeyTipRequest>, reserved: Iterable<String>? = null): List<String> {
        val used = HashSet<String>()
        reserved?.forEach { used += normalize(it) }
        val result = arrayOfNulls<String>(requests.size)

        // Commit the explicit KeyTips in order. Explicit KeyTips that collide fall through to generation
        for (i in requests.indices) {
            val tip = normalize(requests[i].explicit)
            if (tip.isNotEmpty() && isAvailable(tip, used)) {
                used += tip
                result[i] = tip
            }
        }

        var singleBudget = singleBudget(result.count { it == null }, used)
        singleBudget = assignSingles(result, used, singleBudget) { singleCandidates(requests[it].label) }
        // Items whose label had no free letter also get a single-key digit while slots remain
        assignSingles(result, used, singleBudget) { DIGITS.asSequence() }

        // The remaining items get two-letter KeyTips whose first letter is not a single-letter KeyTip
        for (index in requests.indices) {
            if (result[index] != null) continue
            val tip = doubleCandidates(requests[index].label).firstOrNull { isAvailable(it, used) } ?: fallback(used)
            result[index] = tip
            used += tip
        }
        return result.map { it!! }
    }

    /**
     * The number of single-letter KeyTips to hand out. It is limited to keep letters available as the first letter of
     * the two-letter KeyTips of the remaining items (each free first letter allows 36 two-letter KeyTips). Without the
     * limit, a large scope would use up every letter and leave no non-prefix KeyTips for the remaining items.
     */
    private fun singleBudget(pending: Int, used: Set<String>): Int {
        val freeSingles = SINGLE_ALPHABET.count { isAvailable(it.toString(), used) }
        val freePrefixes = freeSingles + if (isAvailable("0", used)) 1 else 0
        if (pending <= freeSingles) return pending
        return ((PAIR_ALPHABET.length * freePrefixes - pending) / (PAIR_ALPHABET.length - 1)).coerceIn(0, freeSingles)
    }

    /**
     * Hands out the first free KeyTip from [candidates] to unassigned items while [budget] slots remain. Returns the
     * remaining slots.
     */
    private fun assignSingles(
        result: Array<String?>,
        used: MutableSet<String>,
        budget: Int,
        candidates: (Int) -> Sequence<String>,
    ): Int {
        var remaining = budget
        var i = 0
        while (i < result.size && remaining > 0) {
            if (result[i] == null) {
                val candidate = candidates(i).firstOrNull { isAvailable(it, used) }
                if (candidate != null) {
                    used += candidate
                    result[i] = candidate
                    remaining--
                }
            }
            i++
        }
        return remaining
    }

    /** Normalizes a KeyTip (uppercases it and keeps only letters and digits). */
    @JvmStatic
    fun normalize(keyTip: String?): String {
        if (keyTip.isNullOrBlank()) return ""
        val builder = StringBuilder()
        for (c in keyTip.trim()) {
            if (c.isLetterOrDigit()) builder.append(c.uppercaseChar())
        }
        return builder.toString()
    }

    /**
     * The numeric KeyTips of the Quick Access Toolbar: 1..9, then 09, 08, ..., 01, then 0A..0Z (the Office convention).
     * Longer toolbars continue with 001..009, 00A..00Z, 0001..., so that every item gets a KeyTip that is not a prefix
     * of another.
     */
    @JvmStatic
    fun assignQuickAccess(count: Int): List<String> {
        val total = maxOf(0, count)
        val tips = ArrayList<String>(total)
        for (i in 1..minOf(9, total)) tips += i.toString()
        var prefix = "0"
        while (tips.size < total) {
            var digit = 9
            while (digit >= 1 && tips.size < total) {
                tips += prefix + digit
                digit--
            }
            var c = 'A'
            while (c <= 'Z' && tips.size < total) {
                tips += prefix + c
                c++
            }
            prefix += "0"
        }
        return tips
    }

    private fun isAvailable(tip: String, used: Set<String>): Boolean =
        used.none { existing -> existing.startsWith(tip) || tip.startsWith(existing) }

    /** The label's ASCII letters and digits in uppercase, with separators (spaces, &, -, /, parentheses) turned into spaces. */
    private fun letters(label: String?): String {
        if (label.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(label, Normalizer.Form.NFD)
        val builder = StringBuilder()
        for (c in normalized) {
            if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) continue
            if (c.isLetterOrDigit() && c.code < 128) {
                builder.append(c.uppercaseChar())
            } else if (c.isWhitespace() || c in "&-/()") {
                builder.append(' ')
            }
        }
        return builder.toString()
    }

    private fun words(letters: String): List<String> = letters.split(' ').filter { it.isNotEmpty() }

    private fun singleCandidates(label: String?): Sequence<String> = sequence {
        val letters = letters(label)
        for (word in words(letters)) yield(word[0].toString())
        for (c in letters.replace(" ", "")) yield(c.toString())
    }

    private fun doubleCandidates(label: String?): Sequence<String> = sequence {
        val letters = letters(label)
        val words = words(letters)
        if (words.size >= 2) yield("${words[0][0]}${words[1][0]}")
        val compact = letters.replace(" ", "")
        for (i in compact.indices) {
            for (j in i + 1 until compact.length) yield("${compact[i]}${compact[j]}")
        }
        for (i in compact.indices) {
            for (d in 1..9) yield("${compact[i]}$d")
        }
    }

    /** Searches breadth-first for ever longer KeyTips. The set of used KeyTips is finite, so one is always found. */
    private fun fallback(used: Set<String>): String {
        var level: List<String> = listOf("0") + SINGLE_ALPHABET.map { it.toString() }
        while (true) {
            val next = level.flatMap { prefix -> PAIR_ALPHABET.map { prefix + it } }
            next.firstOrNull { isAvailable(it, used) }?.let { return it }
            level = next.filter { it !in used }
        }
    }
}

/** The kind of result of [RibbonKeyTipScope.process]. */
enum class RibbonKeyTipMatch {
    /** No KeyTip continues the typed keys with this key. The key is ignored and the typed prefix is kept (Office behavior). */
    NONE,

    /** The typed keys are a prefix of one or more KeyTips. Waits for the next key. */
    PARTIAL,

    /** A KeyTip has been typed completely. */
    COMPLETE,
}

/** An entry of a [RibbonKeyTipScope] (a normalized KeyTip and its target). */
data class RibbonKeyTipEntry<T : Any>(
    /** The normalized KeyTip. */
    val tip: String,
    /** The target (a UI element or a model). */
    val target: T,
)

/** The result of [RibbonKeyTipScope.process] ([target] is the resolved target when [match] is [RibbonKeyTipMatch.COMPLETE]). */
data class RibbonKeyTipProcessResult<T : Any>(
    /** The result of the match. */
    val match: RibbonKeyTipMatch,
    /** The target of the completely typed KeyTip (null unless COMPLETE). */
    val target: T?,
)

/** One level of KeyTips (such as the tab row or the commands of one tab). Matches multi-key prefixes. */
class RibbonKeyTipScope<T : Any> {
    private val mutableEntries = mutableListOf<RibbonKeyTipEntry<T>>()

    /** The entries of the scope. */
    val entries: List<RibbonKeyTipEntry<T>> get() = mutableEntries

    /** The keys typed so far. */
    var typed: String = ""
        private set

    /** Adds an entry. Throws for a KeyTip that is empty after normalization, because it cannot be typed. */
    fun add(tip: String, target: T) {
        val normalized = RibbonKeyTipAssigner.normalize(tip)
        require(normalized.isNotEmpty()) { "A KeyTip needs at least one letter or digit: '$tip'" }
        mutableEntries += RibbonKeyTipEntry(normalized, target)
    }

    /** The entries still reachable with the typed prefix. */
    val candidates: List<RibbonKeyTipEntry<T>> get() = mutableEntries.filter { it.tip.startsWith(typed) }

    /** Processes one key. */
    fun process(key: Char): RibbonKeyTipProcessResult<T> {
        val next = typed + key.uppercaseChar()
        val matches = mutableEntries.filter { it.tip.startsWith(next) }
        if (matches.isEmpty()) return RibbonKeyTipProcessResult(RibbonKeyTipMatch.NONE, null)
        val exact = matches.firstOrNull { it.tip == next }
        if (exact != null) {
            typed = ""
            return RibbonKeyTipProcessResult(RibbonKeyTipMatch.COMPLETE, exact.target)
        }
        typed = next
        return RibbonKeyTipProcessResult(RibbonKeyTipMatch.PARTIAL, null)
    }

    /** Undoes the last typed key. Returns false if nothing has been typed. */
    fun backspace(): Boolean {
        if (typed.isEmpty()) return false
        typed = typed.dropLast(1)
        return true
    }

    /** Clears the typed keys. */
    fun reset() {
        typed = ""
    }
}

/** A stack of KeyTip levels (the Office KeyTip mode: Alt → tab → group popup → menu ...). */
class RibbonKeyTipNavigator<T : Any> {
    private val scopes = ArrayDeque<RibbonKeyTipScope<T>>()

    /** Whether KeyTips are shown. */
    val isActive: Boolean get() = scopes.isNotEmpty()

    /** The current level. */
    val current: RibbonKeyTipScope<T>? get() = scopes.lastOrNull()

    /** The depth of the levels (1 = top level). */
    val depth: Int get() = scopes.size

    /** Enters a new level. */
    fun push(scope: RibbonKeyTipScope<T>) {
        scopes.addLast(scope)
    }

    /** Leaves the current level. Returns false if KeyTip mode ends. */
    fun pop(): Boolean {
        scopes.removeLastOrNull()
        return scopes.isNotEmpty()
    }

    /** Ends KeyTip mode. */
    fun clear() {
        scopes.clear()
    }
}
