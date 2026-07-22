package jp.hisano.winui4k

import jp.hisano.winui4k.WButton
import jp.hisano.winui4k.WFrame
import jp.hisano.winui4k.WTextField
import jp.hisano.winui4k.WinUiUtilities

/**
 * Demo: a window with just a text field and a button.
 *
 * Build the UI inside WinUiUtilities.invokeLater { ... } (= the WinUI UI thread), the
 * same way you would with SwingUtilities.invokeLater { ... }. WinUI starts automatically
 * on the first invokeLater call, and the JVM stays alive until the last window closes.
 */
fun main() {
    println("[winui4k] starting WinUI 3 ...")

    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "WinUI for Kotlin Demo — WinUI 3 from Kotlin/JVM (Panama)")

        val nameField = WTextField(placeholder = "Enter your name")
        val greetButton = WButton("Greet")

        greetButton.addActionListener {
            val name = nameField.text.ifBlank { "world" }
            println("[winui4k] click! text = \"$name\"")
            greetButton.text = "Hello, $name!"
        }

        frame.add(nameField)
        frame.add(greetButton)
        frame.isVisible = true
    }
}
