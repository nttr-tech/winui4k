package jp.hisano.winui4k

import jp.hisano.winui4k.swing.WButton
import jp.hisano.winui4k.swing.WFlyout
import jp.hisano.winui4k.swing.WFrame
import jp.hisano.winui4k.swing.WTextField
import jp.hisano.winui4k.winui.WinUiToolkit

/**
 * Demo: a window with just a text field and a button.
 *
 * Build the UI inside the WinUiToolkit.launch { ... } callback (= the WinUI UI thread),
 * the same way you would with SwingUtilities.invokeLater { ... }.
 */
fun main() {
    println("[winui4k] starting WinUI 3 ...")

    WinUiToolkit.launch {
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

    println("[winui4k] application exited.")
}
