package com.appkitbox.winui4k.ribbon

import java.util.Locale

/** Modifier keys of a [RibbonKeyGesture] (bit flags; combine them with or). */
object RibbonModifierKeys {
    /** No modifier keys. */
    const val NONE = 0

    /** Ctrl (also satisfied by macOS's Command if [RibbonKeyGesture.treatMetaAsControl]). */
    const val CONTROL = 1

    /** Shift. */
    const val SHIFT = 2

    /** Alt / Option. */
    const val ALT = 4

    /** The Windows / Command key. */
    const val META = 8
}

/**
 * A parsed keyboard shortcut ("Ctrl+Shift+L", "F5", "Alt+Down", "Ctrl+]", etc.).
 * [key] is the name of a Windows VirtualKey ("B", "F5", "Number1", "OemCloseBracket", "Add", etc.), and
 * [modifiers] is a combination of [RibbonModifierKeys] bits.
 */
data class RibbonKeyGesture(
    /** Modifier keys (a combination of [RibbonModifierKeys] bits). */
    val modifiers: Int,
    /** Key (the name of a VirtualKey). */
    val key: String,
) {
    /** Whether the pressed key [pressedKey] (the name of a VirtualKey) and modifier keys [pressedModifiers] match this shortcut. */
    fun matches(pressedKey: String?, pressedModifiers: Int): Boolean {
        if (pressedKey.isNullOrEmpty() || key.isEmpty()) return false
        val sameKey = normalizeKey(pressedKey).equals(key, ignoreCase = true) ||
            (key.startsWith("Number") && pressedKey.equals("NumberPad" + key.last(), ignoreCase = true))
        if (!sameKey) return false
        var effective = pressedModifiers
        if (treatMetaAsControl && has(effective, RibbonModifierKeys.META) && !has(modifiers, RibbonModifierKeys.META)) {
            effective = (effective and RibbonModifierKeys.META.inv()) or RibbonModifierKeys.CONTROL
        }
        return effective == modifiers
    }

    /** Human-readable notation (e.g. "Ctrl+Shift+L"). */
    override fun toString(): String = toDisplayString(false)

    /** Creates the notation. If [macStyle], uses the ⌘ ⌥ ⇧ ⌃ symbols. */
    fun toDisplayString(macStyle: Boolean): String {
        val parts = mutableListOf<String>()
        if (macStyle) {
            val controlIsCommand = treatMetaAsControl && !has(modifiers, RibbonModifierKeys.META)
            if (has(modifiers, RibbonModifierKeys.CONTROL)) parts += if (controlIsCommand) "⌘" else "⌃"
            if (has(modifiers, RibbonModifierKeys.ALT)) parts += "⌥"
            if (has(modifiers, RibbonModifierKeys.SHIFT)) parts += "⇧"
            if (has(modifiers, RibbonModifierKeys.META)) parts += "⌘"
            return parts.joinToString("") + keyDisplay(key)
        }
        if (has(modifiers, RibbonModifierKeys.CONTROL)) parts += "Ctrl"
        if (has(modifiers, RibbonModifierKeys.SHIFT)) parts += "Shift"
        if (has(modifiers, RibbonModifierKeys.ALT)) parts += "Alt"
        if (has(modifiers, RibbonModifierKeys.META)) parts += "Win"
        parts += keyDisplay(key)
        return parts.joinToString("+")
    }

    companion object {
        /** If true (the default), macOS's Command key also matches Ctrl shortcuts. */
        @JvmStatic
        var treatMetaAsControl: Boolean = true

        private val KEY_ALIASES: Map<String, String> = caseInsensitive(
            "Esc" to "Escape", "Del" to "Delete", "Ins" to "Insert", "Return" to "Enter",
            "PgUp" to "PageUp", "PgDn" to "PageDown", "Plus" to "Add", "+" to "Add", "-" to "Subtract",
            "Minus" to "Subtract", "=" to "Add", "Spacebar" to "Space", "Backspace" to "Back", "*" to "Multiply",
            "Up" to "Up", "Down" to "Down", "Left" to "Left", "Right" to "Right",
            "[" to "OemOpenBracket", "]" to "OemCloseBracket", "," to "OemComma", "." to "OemPeriod",
            "/" to "OemQuestion", ";" to "OemSemicolon", "'" to "OemQuote", "\\" to "OemBackslash", "`" to "OemTilde",
        )

        private val CANONICAL_KEYS: Map<String, String> = caseInsensitive(
            *listOf(
                "Escape", "Delete", "Insert", "Enter", "PageUp", "PageDown", "Home", "End", "Tab", "Back", "Space", "Up", "Down",
                "Left", "Right", "Add", "Subtract", "Multiply", "Divide", "Decimal", "Pause", "Print", "Snapshot", "Scroll",
                "Apps", "Help", "Clear", "Select", "Execute", "Sleep",
            ).map { it to it }.toTypedArray(),
        )

        private val DISPLAY_NAMES: Map<String, String> = caseInsensitive(
            "Add" to "+", "Subtract" to "-", "OemOpenBracket" to "[", "OemCloseBracket" to "]", "OemComma" to ",",
            "OemPeriod" to ".", "OemQuestion" to "/", "OemSemicolon" to ";", "OemQuote" to "'", "OemBackslash" to "\\",
            "OemTilde" to "`", "Escape" to "Esc", "Delete" to "Del", "PageUp" to "PgUp", "PageDown" to "PgDn",
        )

        /** Parses a shortcut. The separator is '+' (the '+' key itself can also be written as "Plus"). Throws if the format is invalid. */
        @JvmStatic
        fun parse(text: String): RibbonKeyGesture =
            tryParse(text) ?: throw IllegalArgumentException("'$text' is not a keyboard shortcut")

        /** Parses a shortcut. Returns null if the format is invalid (does not throw). */
        @JvmStatic
        fun tryParse(text: String?): RibbonKeyGesture? {
            if (text.isNullOrBlank()) return null
            var modifiers = RibbonModifierKeys.NONE
            var key: String? = null
            for (part in splitParts(text.trim())) {
                if (part.isEmpty()) return null
                val modifier = MODIFIER_NAMES[part.uppercase(Locale.ROOT)]
                if (modifier != null) {
                    modifiers = modifiers or modifier
                } else {
                    if (key != null) return null
                    key = normalizeKey(part)
                }
            }
            return key?.let { RibbonKeyGesture(modifiers, it) }
        }

        /** Modifier key name (uppercase) → [RibbonModifierKeys] bit. */
        private val MODIFIER_NAMES: Map<String, Int> = mapOf(
            "CTRL" to RibbonModifierKeys.CONTROL, "CONTROL" to RibbonModifierKeys.CONTROL, "STRG" to RibbonModifierKeys.CONTROL,
            "SHIFT" to RibbonModifierKeys.SHIFT,
            "ALT" to RibbonModifierKeys.ALT, "OPTION" to RibbonModifierKeys.ALT, "OPT" to RibbonModifierKeys.ALT,
            "WIN" to RibbonModifierKeys.META, "META" to RibbonModifierKeys.META, "CMD" to RibbonModifierKeys.META,
            "COMMAND" to RibbonModifierKeys.META,
        )

        /** Splits on '+'. '+' is a separator only when something precedes it; otherwise it is the '+' key itself ("Ctrl + +"). */
        private fun splitParts(trimmed: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            for (c in trimmed) {
                if (c == '+' && current.toString().trim().isNotEmpty()) {
                    parts += current.toString().trim()
                    current.setLength(0)
                } else {
                    current.append(c)
                }
            }
            if (current.isNotEmpty()) parts += current.toString().trim()
            return parts
        }

        private fun normalizeKey(raw: String): String {
            val key = raw.trim()
            if (key.isEmpty()) return ""
            KEY_ALIASES[key.uppercase(Locale.ROOT)]?.let { return it }
            CANONICAL_KEYS[key.uppercase(Locale.ROOT)]?.let { return it }
            if (key.length in 2..3 && key[0].uppercaseChar() == 'F') {
                val function = key.substring(1).toIntOrNull()
                if (function != null && function in 1..24) return "F$function"
            }
            if (key.length == 1 && key[0].isLetter()) return key.uppercase(Locale.ROOT)
            if (key.length == 1 && key[0].isDigit()) return "Number$key"
            return key[0].uppercaseChar() + key.substring(1)
        }

        private fun keyDisplay(key: String): String {
            DISPLAY_NAMES[key.uppercase(Locale.ROOT)]?.let { return it }
            return if (key.startsWith("Number") && key.length == 7) key.substring(6) else key
        }

        private fun has(modifiers: Int, flag: Int): Boolean = modifiers and flag == flag

        /** A lookup table with uppercased keys (equivalent to a .NET dictionary with OrdinalIgnoreCase). */
        private fun caseInsensitive(vararg pairs: Pair<String, String>): Map<String, String> =
            pairs.associate { (k, v) -> k.uppercase(Locale.ROOT) to v }
    }
}
