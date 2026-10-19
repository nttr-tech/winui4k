package com.appkitbox.winui4k

import java.util.EventListener

/**
 * KeyEvent-style: an event that delivers key operations on the focused component to [WKeyListener].
 *
 * [keyCode] has the same values as AWT's KeyEvent.VK_* (keys whose Windows virtual-key values differ are mapped: Enter is
 * [VK_ENTER] = 10 rather than 13, Delete is [VK_DELETE] = 127 rather than 46, and so on). The WinUI key is available as
 * [virtualKey].
 * [keyChar] is the typed character for [KEY_TYPED] ('\n' for Enter), and [CHAR_UNDEFINED] for [KEY_PRESSED] /
 * [KEY_RELEASED] (AWT may put the character the key represents there, but WinUI's KeyDown does not pass a character).
 * The [keyCode] of [KEY_TYPED] is [VK_UNDEFINED].
 *
 * This class does not depend on java.awt, so it holds its own constants with the same values as AWT's KeyEvent.
 */
@Suppress("LongParameterList") // The parameters follow the order of AWT's KeyEvent constructor (plus the WinUI key)
class WKeyEvent @JvmOverloads constructor(
    source: WComponent,
    id: Int,
    `when`: Long,
    modifiersEx: Int,
    /** The key code (the same values as AWT's KeyEvent.VK_*; equivalent to KeyEvent.getKeyCode). [VK_UNDEFINED] for [KEY_TYPED]. */
    val keyCode: Int,
    /** The typed character (equivalent to KeyEvent.getKeyChar). [CHAR_UNDEFINED] for [KEY_PRESSED] / [KEY_RELEASED]. */
    val keyChar: Char,
    /** The WinUI key (Windows.System.VirtualKey). [VirtualKey.NONE] for [KEY_TYPED]. */
    val virtualKey: VirtualKey = VirtualKey.NONE,
) : WInputEvent(source, id, `when`, modifiersEx) {
    /** Whether this is a key that does not type a character (arrows, function keys such as F1, Home, etc.; equivalent to KeyEvent.isActionKey). */
    val isActionKey: Boolean get() = keyCode in ACTION_KEYS

    companion object {
        /** The smallest key event ID (the same value as AWT's KeyEvent.KEY_FIRST). */
        const val KEY_FIRST: Int = 400

        /** The largest key event ID (the same value as AWT's KeyEvent.KEY_LAST). */
        const val KEY_LAST: Int = 402

        /** A character was typed (the same value as AWT's KeyEvent.KEY_TYPED). */
        const val KEY_TYPED: Int = 400

        /** A key was pressed (the same value as AWT's KeyEvent.KEY_PRESSED). */
        const val KEY_PRESSED: Int = 401

        /** A key was released (the same value as AWT's KeyEvent.KEY_RELEASED). */
        const val KEY_RELEASED: Int = 402

        /** No character (the same value as AWT's KeyEvent.CHAR_UNDEFINED). */
        const val CHAR_UNDEFINED: Char = '\uFFFF'

        /** There is no corresponding AWT key code (the same value as AWT's KeyEvent.VK_UNDEFINED). */
        const val VK_UNDEFINED: Int = 0

        const val VK_CANCEL: Int = 3
        const val VK_BACK_SPACE: Int = 8
        const val VK_TAB: Int = 9

        /** The Enter key (AWT's value is '\n' = 10, unlike the Windows virtual key 13). */
        const val VK_ENTER: Int = 10
        const val VK_CLEAR: Int = 12
        const val VK_SHIFT: Int = 16
        const val VK_CONTROL: Int = 17
        const val VK_ALT: Int = 18
        const val VK_PAUSE: Int = 19
        const val VK_CAPS_LOCK: Int = 20
        const val VK_KANA: Int = 21
        const val VK_FINAL: Int = 24
        const val VK_KANJI: Int = 25
        const val VK_ESCAPE: Int = 27
        const val VK_CONVERT: Int = 28
        const val VK_NONCONVERT: Int = 29
        const val VK_ACCEPT: Int = 30
        const val VK_MODECHANGE: Int = 31
        const val VK_SPACE: Int = 32
        const val VK_PAGE_UP: Int = 33
        const val VK_PAGE_DOWN: Int = 34
        const val VK_END: Int = 35
        const val VK_HOME: Int = 36
        const val VK_LEFT: Int = 37
        const val VK_UP: Int = 38
        const val VK_RIGHT: Int = 39
        const val VK_DOWN: Int = 40
        const val VK_COMMA: Int = 44
        const val VK_MINUS: Int = 45
        const val VK_PERIOD: Int = 46
        const val VK_SLASH: Int = 47
        const val VK_0: Int = 48
        const val VK_1: Int = 49
        const val VK_2: Int = 50
        const val VK_3: Int = 51
        const val VK_4: Int = 52
        const val VK_5: Int = 53
        const val VK_6: Int = 54
        const val VK_7: Int = 55
        const val VK_8: Int = 56
        const val VK_9: Int = 57
        const val VK_SEMICOLON: Int = 59
        const val VK_EQUALS: Int = 61
        const val VK_A: Int = 65
        const val VK_B: Int = 66
        const val VK_C: Int = 67
        const val VK_D: Int = 68
        const val VK_E: Int = 69
        const val VK_F: Int = 70
        const val VK_G: Int = 71
        const val VK_H: Int = 72
        const val VK_I: Int = 73
        const val VK_J: Int = 74
        const val VK_K: Int = 75
        const val VK_L: Int = 76
        const val VK_M: Int = 77
        const val VK_N: Int = 78
        const val VK_O: Int = 79
        const val VK_P: Int = 80
        const val VK_Q: Int = 81
        const val VK_R: Int = 82
        const val VK_S: Int = 83
        const val VK_T: Int = 84
        const val VK_U: Int = 85
        const val VK_V: Int = 86
        const val VK_W: Int = 87
        const val VK_X: Int = 88
        const val VK_Y: Int = 89
        const val VK_Z: Int = 90
        const val VK_OPEN_BRACKET: Int = 91
        const val VK_BACK_SLASH: Int = 92
        const val VK_CLOSE_BRACKET: Int = 93
        const val VK_NUMPAD0: Int = 96
        const val VK_NUMPAD1: Int = 97
        const val VK_NUMPAD2: Int = 98
        const val VK_NUMPAD3: Int = 99
        const val VK_NUMPAD4: Int = 100
        const val VK_NUMPAD5: Int = 101
        const val VK_NUMPAD6: Int = 102
        const val VK_NUMPAD7: Int = 103
        const val VK_NUMPAD8: Int = 104
        const val VK_NUMPAD9: Int = 105
        const val VK_MULTIPLY: Int = 106
        const val VK_ADD: Int = 107
        const val VK_SEPARATOR: Int = 108
        const val VK_SUBTRACT: Int = 109
        const val VK_DECIMAL: Int = 110
        const val VK_DIVIDE: Int = 111
        const val VK_F1: Int = 112
        const val VK_F2: Int = 113
        const val VK_F3: Int = 114
        const val VK_F4: Int = 115
        const val VK_F5: Int = 116
        const val VK_F6: Int = 117
        const val VK_F7: Int = 118
        const val VK_F8: Int = 119
        const val VK_F9: Int = 120
        const val VK_F10: Int = 121
        const val VK_F11: Int = 122
        const val VK_F12: Int = 123

        /** The Delete key (AWT's value is 127, unlike the Windows virtual key 46). */
        const val VK_DELETE: Int = 127
        const val VK_NUM_LOCK: Int = 144
        const val VK_SCROLL_LOCK: Int = 145
        const val VK_PRINTSCREEN: Int = 154

        /** The Insert key (AWT's value is 155, unlike the Windows virtual key 45). */
        const val VK_INSERT: Int = 155
        const val VK_HELP: Int = 156
        const val VK_META: Int = 157
        const val VK_BACK_QUOTE: Int = 192
        const val VK_QUOTE: Int = 222

        /** F13 to F24 (from 0xF000 in AWT, unlike the Windows virtual keys 124 to 135). */
        const val VK_F13: Int = 0xF000
        const val VK_F14: Int = 0xF001
        const val VK_F15: Int = 0xF002
        const val VK_F16: Int = 0xF003
        const val VK_F17: Int = 0xF004
        const val VK_F18: Int = 0xF005
        const val VK_F19: Int = 0xF006
        const val VK_F20: Int = 0xF007
        const val VK_F21: Int = 0xF008
        const val VK_F22: Int = 0xF009
        const val VK_F23: Int = 0xF00A
        const val VK_F24: Int = 0xF00B

        /** The Windows key (the same value as AWT's KeyEvent.VK_WINDOWS). */
        const val VK_WINDOWS: Int = 0x020C

        /** The Application (context menu) key (the same value as AWT's KeyEvent.VK_CONTEXT_MENU). */
        const val VK_CONTEXT_MENU: Int = 0x020D

        /** Keys that do not type a character ([isActionKey]): those in AWT's KeyEvent.isActionKey table that occur with Windows keys. */
        private val ACTION_KEYS: Set<Int> = buildSet {
            addAll(listOf(VK_HOME, VK_END, VK_PAGE_UP, VK_PAGE_DOWN, VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT))
            addAll(VK_F1..VK_F12)
            addAll(VK_F13..VK_F24)
            addAll(listOf(VK_PRINTSCREEN, VK_SCROLL_LOCK, VK_CAPS_LOCK, VK_NUM_LOCK, VK_PAUSE, VK_INSERT, VK_HELP))
            addAll(listOf(VK_WINDOWS, VK_CONTEXT_MENU))
            addAll(listOf(VK_FINAL, VK_CONVERT, VK_NONCONVERT, VK_ACCEPT, VK_MODECHANGE, VK_KANA, VK_KANJI))
        }

        /**
         * Maps a Windows virtual key ([virtualKey]; WinUI's KeyRoutedEventArgs.Key) to an AWT key code
         * (the same mapping as the table in AWT's awt_Component.cpp on Windows). Returns [VK_UNDEFINED] if there is no mapping.
         */
        @Suppress("CyclomaticComplexMethod") // The branches correspond 1:1 to the table of keys whose values differ from AWT
        @JvmSynthetic
        internal fun keyCodeOf(virtualKey: Int): Int = when (virtualKey) {
            WIN_RETURN -> VK_ENTER
            WIN_SNAPSHOT -> VK_PRINTSCREEN
            WIN_INSERT -> VK_INSERT
            WIN_DELETE -> VK_DELETE
            WIN_HELP -> VK_HELP
            WIN_LWIN, WIN_RWIN -> VK_WINDOWS
            WIN_APPS -> VK_CONTEXT_MENU
            WIN_LSHIFT, WIN_RSHIFT -> VK_SHIFT
            WIN_LCONTROL, WIN_RCONTROL -> VK_CONTROL
            WIN_LMENU, WIN_RMENU -> VK_ALT
            WIN_OEM_1 -> VK_SEMICOLON
            WIN_OEM_PLUS -> VK_EQUALS
            WIN_OEM_COMMA -> VK_COMMA
            WIN_OEM_MINUS -> VK_MINUS
            WIN_OEM_PERIOD -> VK_PERIOD
            WIN_OEM_2 -> VK_SLASH
            WIN_OEM_3 -> VK_BACK_QUOTE
            WIN_OEM_4 -> VK_OPEN_BRACKET
            WIN_OEM_5 -> VK_BACK_SLASH
            WIN_OEM_6 -> VK_CLOSE_BRACKET
            WIN_OEM_7 -> VK_QUOTE
            in WIN_F13..WIN_F24 -> VK_F13 + (virtualKey - WIN_F13)
            in SAME_AS_WINDOWS -> virtualKey
            else -> VK_UNDEFINED
        }

        // Windows virtual keys (Windows.System.VirtualKey / VK_* in WinUser.h) whose values differ from AWT
        private const val WIN_RETURN = 13
        private const val WIN_SNAPSHOT = 44
        private const val WIN_INSERT = 45
        private const val WIN_DELETE = 46
        private const val WIN_HELP = 47
        private const val WIN_LWIN = 91
        private const val WIN_RWIN = 92
        private const val WIN_APPS = 93
        private const val WIN_F13 = 124
        private const val WIN_F24 = 135
        private const val WIN_LSHIFT = 160
        private const val WIN_RSHIFT = 161
        private const val WIN_LCONTROL = 162
        private const val WIN_RCONTROL = 163
        private const val WIN_LMENU = 164
        private const val WIN_RMENU = 165
        private const val WIN_OEM_1 = 186
        private const val WIN_OEM_PLUS = 187
        private const val WIN_OEM_COMMA = 188
        private const val WIN_OEM_MINUS = 189
        private const val WIN_OEM_PERIOD = 190
        private const val WIN_OEM_2 = 191
        private const val WIN_OEM_3 = 192
        private const val WIN_OEM_4 = 219
        private const val WIN_OEM_5 = 220
        private const val WIN_OEM_6 = 221
        private const val WIN_OEM_7 = 222

        /** Keys whose Windows virtual key and AWT key code have the same value. */
        private val SAME_AS_WINDOWS: Set<Int> = buildSet {
            addAll(listOf(VK_CANCEL, VK_BACK_SPACE, VK_TAB, VK_CLEAR, VK_SHIFT, VK_CONTROL, VK_ALT, VK_PAUSE, VK_CAPS_LOCK))
            addAll(listOf(VK_KANA, VK_FINAL, VK_KANJI, VK_ESCAPE, VK_CONVERT, VK_NONCONVERT, VK_ACCEPT, VK_MODECHANGE))
            addAll(VK_SPACE..VK_DOWN)
            addAll(VK_0..VK_9)
            addAll(VK_A..VK_Z)
            addAll(VK_NUMPAD0..VK_DIVIDE)
            addAll(VK_F1..VK_F12)
            addAll(listOf(VK_NUM_LOCK, VK_SCROLL_LOCK))
        }
    }
}

/** KeyListener-style: the listener passed to [WComponent.addKeyListener]. [WKeyAdapter] lets you skip the methods you do not use. */
interface WKeyListener : EventListener {
    /** Called when a character is typed ([WKeyEvent.keyChar] is the typed character). */
    fun keyTyped(e: WKeyEvent)

    /** Called when a key is pressed (called repeatedly while the key is held down). */
    fun keyPressed(e: WKeyEvent)

    /** Called when a key is released. */
    fun keyReleased(e: WKeyEvent)
}

/** KeyAdapter-style: a base class that implements every method of [WKeyListener] as empty. Override only the methods you need. */
abstract class WKeyAdapter : WKeyListener {
    override fun keyTyped(e: WKeyEvent) {
        // Does nothing by default
    }

    override fun keyPressed(e: WKeyEvent) {
        // Does nothing by default
    }

    override fun keyReleased(e: WKeyEvent) {
        // Does nothing by default
    }
}
