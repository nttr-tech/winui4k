package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Reads and writes the AbstractButton.setMnemonic-style access key (UIElement.AccessKey). Shared by [WButtonBase] and
 * [WToggleSwitch].
 * Key codes have the same values as AWT's KeyEvent.VK_A..VK_Z (65..90) / VK_0..VK_9 (48..57), and 0 clears it.
 */
internal object Mnemonics {
    private const val ASCII_LIMIT = 128

    /** Returns the AccessKey of [uiElement] (IUIElement) as a key code, or 0 if unset. */
    fun read(uiElement: ComPtr): Int {
        val key = uiElement.getString(XamlInterop.IUIElement_get_AccessKey)
        return if (key.length == 1) key[0].code else 0
    }

    /** Sets the AccessKey of [uiElement] (IUIElement) to [keyCode]. 0 clears it. Anything but uppercase letters and digits throws [IllegalArgumentException]. */
    fun write(uiElement: ComPtr, keyCode: Int) {
        require(keyCode == 0 || isKeyCode(keyCode)) { "only key codes of uppercase letters and digits can be used as a mnemonic: $keyCode" }
        val key = if (keyCode == 0) "" else keyCode.toChar().toString()
        Hstring.use(key) { h -> uiElement.call(XamlInterop.IUIElement_put_AccessKey, h) }
    }

    /** Converts the character of setMnemonic(char) to a key code. Letters are uppercased and '\u0000' becomes 0. Anything but ASCII letters and digits throws [IllegalArgumentException]. */
    fun keyCodeOf(mnemonic: Char): Int {
        require(mnemonic == '\u0000' || (mnemonic.code < ASCII_LIMIT && mnemonic.isLetterOrDigit())) {
            "mnemonic must be an ASCII letter or digit: '$mnemonic'"
        }
        return mnemonic.uppercaseChar().code
    }

    /** Whether it is AWT's KeyEvent.VK_A..VK_Z (65..90) or VK_0..VK_9 (48..57). */
    private fun isKeyCode(keyCode: Int): Boolean = keyCode in 'A'.code..'Z'.code || keyCode in '0'.code..'9'.code
}
