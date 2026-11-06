package com.appkitbox.winui4k

import java.util.concurrent.ConcurrentHashMap

/**
 * KeyStroke-style: an immutable value representing a combination of a key and modifiers (Ctrl+S and so on). Passed as a menu item accelerator
 * ([WAction.ACCELERATOR_KEY]) or to [WComponent.addKeyboardAccelerator].
 * Created with [getKeyStroke] (there is no constructor). Equal values may be reused, but being the same instance is not guaranteed
 * (use [equals] to compare).
 *
 * The values and string form are the same as Swing's KeyStroke:
 * - [keyCode] has the same values as AWT's KeyEvent.VK_* ([WKeyEvent.VK_S] and so on). For character input ([KEY_TYPED][WKeyEvent.KEY_TYPED]) it is
 *   [WKeyEvent.VK_UNDEFINED].
 * - [modifiers] has the bits of both the new style ([WInputEvent.CTRL_DOWN_MASK] and so on) and the old style ([WInputEvent.CTRL_MASK] and so on)
 *   (as in Swing, whichever style is passed, both are filled in; Ctrl+S is 130 = 128 + 2). Old-style mouse buttons (BUTTON1_MASK and so on)
 *   are dropped.
 * - [toString] has AWT's form ("ctrl pressed S", "shift typed a", "alt released F4"). It can be read back with [getKeyStroke] (String).
 *
 * WinUI's keyboard accelerators fire when a key is pressed, so key strokes for release ([isOnKeyRelease]) and for character input
 * can be created as values but cannot be used as accelerators (passing them throws [IllegalArgumentException]).
 * AltGraph and mouse button modifiers cannot be used for accelerators either.
 */
class WKeyStroke private constructor(
    /** The character typed (equivalent to KeyStroke.getKeyChar). [WKeyEvent.CHAR_UNDEFINED] except for character-input key strokes. */
    val keyChar: Char,
    /** The key code (same values as AWT's KeyEvent.VK_*; equivalent to KeyStroke.getKeyCode). [WKeyEvent.VK_UNDEFINED] for character input. */
    val keyCode: Int,
    /** The modifiers (bits of both the new and the old style; equivalent to KeyStroke.getModifiers). */
    val modifiers: Int,
    /** Whether the key stroke is for key release (equivalent to KeyStroke.isOnKeyRelease). */
    val isOnKeyRelease: Boolean,
) {
    /**
     * The kind of key event corresponding to this key stroke (equivalent to KeyStroke.getKeyEventType).
     * One of [WKeyEvent.KEY_TYPED] / [WKeyEvent.KEY_PRESSED] / [WKeyEvent.KEY_RELEASED].
     */
    val keyEventType: Int
        get() = when {
            keyCode == WKeyEvent.VK_UNDEFINED -> WKeyEvent.KEY_TYPED
            isOnKeyRelease -> WKeyEvent.KEY_RELEASED
            else -> WKeyEvent.KEY_PRESSED
        }

    override fun equals(other: Any?): Boolean =
        other is WKeyStroke &&
            other.keyChar == keyChar &&
            other.keyCode == keyCode &&
            other.modifiers == modifiers &&
            other.isOnKeyRelease == isOnKeyRelease

    /** The same formula as AWT's AWTKeyStroke.hashCode (the same key stroke gives the same hash as Swing). */
    override fun hashCode(): Int =
        (keyChar.code + 1) * (2 * (keyCode + 1)) * (modifiers + 1) + if (isOnKeyRelease) 1 else 2

    /** A string in AWT's form ("ctrl pressed S" and so on). Modifiers are in the order shift, ctrl, meta, alt, altGraph, button1-3. */
    override fun toString(): String {
        val prefix = modifiersText(modifiers)
        return if (keyCode == WKeyEvent.VK_UNDEFINED) {
            "${prefix}typed $keyChar"
        } else {
            "$prefix${if (isOnKeyRelease) "released" else "pressed"} ${vkText(keyCode)}"
        }
    }

    /**
     * The Key (Windows virtual key) and Modifiers (VirtualKeyModifiers) values for WinUI's KeyboardAccelerator.
     * [IllegalArgumentException] if the key stroke cannot be an accelerator.
     */
    @JvmSynthetic
    internal fun toAccelerator(): Pair<Int, Int> {
        require(keyCode != WKeyEvent.VK_UNDEFINED) { "a character-input key stroke cannot be an accelerator: $this" }
        require(!isOnKeyRelease) { "WinUI accelerators fire only on key press (key release cannot be used): $this" }
        require(modifiers and UNSUPPORTED_ACCELERATOR_MASKS == 0) {
            "AltGraph and mouse buttons cannot be accelerator modifiers: $this"
        }
        val key = requireNotNull(WKeyEvent.windowsKeyOf(keyCode)) { "key code with no corresponding Windows key: $this" }
        return key to WInputEvent.toVirtualKeyModifiers(modifiers)
    }

    companion object {
        private val cache = ConcurrentHashMap<WKeyStroke, WKeyStroke>()

        /** The bits that cannot be accelerator modifiers (AltGraph and mouse buttons). */
        private const val UNSUPPORTED_ACCELERATOR_MASKS = WInputEvent.ALT_GRAPH_DOWN_MASK or
            WInputEvent.BUTTON1_DOWN_MASK or WInputEvent.BUTTON2_DOWN_MASK or WInputEvent.BUTTON3_DOWN_MASK

        /** The new-style bits a key stroke can have (the bits AWT's AWTKeyStroke.mapOldModifiers keeps). */
        private const val DOWN_MASKS = WInputEvent.SHIFT_DOWN_MASK or WInputEvent.ALT_DOWN_MASK or
            WInputEvent.ALT_GRAPH_DOWN_MASK or WInputEvent.CTRL_DOWN_MASK or WInputEvent.META_DOWN_MASK or
            WInputEvent.BUTTON1_DOWN_MASK or WInputEvent.BUTTON2_DOWN_MASK or WInputEvent.BUTTON3_DOWN_MASK

        /** Modifier names and their bits (the same table as the modifierKeywords of AWT's getKeyStroke(String)). */
        private val MODIFIER_KEYWORDS: Map<String, Int> = mapOf(
            "shift" to WInputEvent.SHIFT_DOWN_MASK,
            "control" to WInputEvent.CTRL_DOWN_MASK,
            "ctrl" to WInputEvent.CTRL_DOWN_MASK,
            "meta" to WInputEvent.META_DOWN_MASK,
            "alt" to WInputEvent.ALT_DOWN_MASK,
            "altGraph" to WInputEvent.ALT_GRAPH_DOWN_MASK,
            "button1" to WInputEvent.BUTTON1_DOWN_MASK,
            "button2" to WInputEvent.BUTTON2_DOWN_MASK,
            "button3" to WInputEvent.BUTTON3_DOWN_MASK,
        )

        /** The order of modifiers in [toString] (same as AWT's getModifiersText). */
        private val MODIFIER_TEXTS: List<Pair<Int, String>> = listOf(
            WInputEvent.SHIFT_DOWN_MASK to "shift",
            WInputEvent.CTRL_DOWN_MASK to "ctrl",
            WInputEvent.META_DOWN_MASK to "meta",
            WInputEvent.ALT_DOWN_MASK to "alt",
            WInputEvent.ALT_GRAPH_DOWN_MASK to "altGraph",
            WInputEvent.BUTTON1_DOWN_MASK to "button1",
            WInputEvent.BUTTON2_DOWN_MASK to "button2",
            WInputEvent.BUTTON3_DOWN_MASK to "button3",
        )

        /** The names (without VK_) and values of [WKeyEvent]'s VK_* constants. Looked up from the constant declarations, as AWT does. */
        private val VK_NAMES: Map<String, Int> by lazy {
            WKeyEvent::class.java.fields
                .filter { it.name.startsWith("VK_") && it.type == Int::class.javaPrimitiveType }
                .associate { it.name.removePrefix("VK_") to it.getInt(null) }
        }

        private val VK_TEXTS: Map<Int, String> by lazy { VK_NAMES.entries.associate { (name, code) -> code to name } }

        /** The key stroke for typing the character [keyChar] (equivalent to KeyStroke.getKeyStroke(char); "typed a"). */
        @JvmStatic
        fun getKeyStroke(keyChar: Char): WKeyStroke = cached(keyChar, WKeyEvent.VK_UNDEFINED, 0, false)

        /**
         * A keystroke that types the character [keyChar] while pressing the modifier keys [modifiers] (equivalent to KeyStroke.getKeyStroke(Character, int);
         * "ctrl typed a"). [modifiers] may be in the new or old form. As in Swing, the argument is a Java Character, and
         * writing `getKeyStroke('a', mask)` in Java selects the (int, int) overload, so pass `Character.valueOf('a')`.
         *
         * @throws IllegalArgumentException if [keyChar] is null (as in Swing)
         */
        @JvmStatic
        fun getKeyStroke(keyChar: Char?, modifiers: Int): WKeyStroke {
            requireNotNull(keyChar) { "keyChar must not be null" }
            return cached(keyChar, WKeyEvent.VK_UNDEFINED, modifiers, false)
        }

        /**
         * The key stroke for pressing the key [keyCode] (same values as AWT's KeyEvent.VK_*) with the modifiers [modifiers]
         * (equivalent to KeyStroke.getKeyStroke(int, int)). [modifiers] may be in the new style such as [WInputEvent.CTRL_DOWN_MASK] or
         * in the old style such as [WInputEvent.CTRL_MASK].
         *
         * As in Swing, writing `getKeyStroke('a', 0)` in Java gives key code 97 (numpad 1), not a character.
         */
        @JvmStatic
        fun getKeyStroke(keyCode: Int, modifiers: Int): WKeyStroke = getKeyStroke(keyCode, modifiers, false)

        /**
         * [getKeyStroke] (Int, Int) plus whether it is for key release ([onKeyRelease])
         * (equivalent to KeyStroke.getKeyStroke(int, int, boolean)). Key strokes for release cannot be used as accelerators.
         */
        @JvmStatic
        fun getKeyStroke(keyCode: Int, modifiers: Int, onKeyRelease: Boolean): WKeyStroke =
            cached(WKeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease)

        /**
         * Creates a key stroke from a string in AWT's format (equivalent to KeyStroke.getKeyStroke(String)). The format is
         * `modifier* (typed char | [pressed | released] keyName)`. Modifiers are shift, control (ctrl), meta (the Windows key on Windows),
         * alt, altGraph and button1-3, and the key name is the name of KeyEvent.VK_* without VK_ (case-sensitive).
         * Examples: "ctrl shift S", "alt F4", "control DELETE", "typed a", "released ENTER".
         * Null if [s] is null, empty or malformed (no exception, same as Swing).
         */
        @JvmStatic
        fun getKeyStroke(s: String?): WKeyStroke? {
            if (s.isNullOrEmpty()) return null
            return parse(s.split(' ').filter { it.isNotEmpty() })
        }

        /** The key stroke corresponding to the key event [e] (equivalent to KeyStroke.getKeyStrokeForEvent). The modifiers are [e]'s [WInputEvent.modifiersEx]. */
        @JvmStatic
        fun getKeyStrokeForEvent(e: WKeyEvent): WKeyStroke = when (e.id) {
            WKeyEvent.KEY_TYPED -> cached(e.keyChar, WKeyEvent.VK_UNDEFINED, e.modifiersEx, false)
            else -> cached(WKeyEvent.CHAR_UNDEFINED, e.keyCode, e.modifiersEx, e.id == WKeyEvent.KEY_RELEASED)
        }

        /** Reads the tokens with the same procedure as AWT's getAWTKeyStroke(String). Null if malformed. */
        private fun parse(tokens: List<String>): WKeyStroke? {
            var mask = 0
            var typed = false
            var pressedOrReleased = false
            var released = false
            for ((index, token) in tokens.withIndex()) {
                val last = index == tokens.lastIndex
                when {
                    typed -> return if (token.length == 1 && last) cached(token[0], WKeyEvent.VK_UNDEFINED, mask, false) else null
                    pressedOrReleased || last -> {
                        if (!last) return null
                        val keyCode = VK_NAMES[token] ?: return null
                        return cached(WKeyEvent.CHAR_UNDEFINED, keyCode, mask, released)
                    }
                    token == "released" -> {
                        pressedOrReleased = true
                        released = true
                    }
                    token == "pressed" -> pressedOrReleased = true
                    token == "typed" -> typed = true
                    else -> mask = mask or (MODIFIER_KEYWORDS[token] ?: return null)
                }
            }
            return null
        }

        private fun cached(keyChar: Char, keyCode: Int, modifiers: Int, onKeyRelease: Boolean): WKeyStroke {
            val stroke = WKeyStroke(keyChar, keyCode, normalize(modifiers), onKeyRelease)
            return cache.putIfAbsent(stroke, stroke) ?: stroke
        }

        /** Fills in the bits of both the old and the new style (same as AWT's mapNewModifiers(mapOldModifiers(m))). */
        private fun normalize(modifiers: Int): Int {
            var down = modifiers
            if (modifiers and WInputEvent.SHIFT_MASK != 0) down = down or WInputEvent.SHIFT_DOWN_MASK
            if (modifiers and WInputEvent.ALT_MASK != 0) down = down or WInputEvent.ALT_DOWN_MASK
            if (modifiers and WInputEvent.ALT_GRAPH_MASK != 0) down = down or WInputEvent.ALT_GRAPH_DOWN_MASK
            if (modifiers and WInputEvent.CTRL_MASK != 0) down = down or WInputEvent.CTRL_DOWN_MASK
            if (modifiers and WInputEvent.META_MASK != 0) down = down or WInputEvent.META_DOWN_MASK
            down = down and DOWN_MASKS
            return down or WInputEvent.keyMasksOf(down)
        }

        private fun modifiersText(modifiers: Int): String =
            MODIFIER_TEXTS.filter { (mask, _) -> modifiers and mask != 0 }.joinToString("") { (_, name) -> "$name " }

        private fun vkText(keyCode: Int): String = VK_TEXTS[keyCode] ?: "UNKNOWN"
    }
}
