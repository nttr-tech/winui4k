package com.appkitbox.winui4k.sample.formwithmiglayout

import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLayoutPanel
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.layout.miglayout.MigLayoutManager

/**
 * A simple MigLayout form sample. Follows the Fluent Design System's spacing widths and design
 * conventions:
 *
 * - Every gap is a multiple of the 4epx grid
 * - 24epx page margins, 16epx between fields, 24epx above and below the heading and command row
 * - Labels sit above their input field (WinUI's Header renders at Fluent's standard position/gap)
 * - The heading uses the type ramp's Title (28epx SemiBold)
 * - Command buttons sit bottom-right, 8epx apart, 120epx minimum width, with the primary
 *   (accent) button on the left (the same convention as ContentDialog)
 *
 * Run with: gradlew :winui4k-sample-form-with-miglayout:run
 */
fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "MigLayout Form Sample")

        // Single column, auto-wrapping. 24epx page margins, 16epx between fields (MigLayout's
        // default insets are about 7epx, so they're overridden explicitly to match Fluent's
        // 24epx). fill is given per input field via growx rather than on the column, so that only
        // the button row is excluded from stretching and stays right-aligned
        val form = WLayoutPanel(
            MigLayoutManager(
                layoutConstraints = "wrap 1, insets 24, gapy 16",
                columnConstraints = "[grow]",
            ),
        )

        // The type ramp's Title (28epx SemiBold). Leaves the default line gap of 16 + 8 = 24epx
        // below the heading
        val title = WLabel("Contact Us")
        title.fontSize = 28.0
        title.fontWeight = 600
        form.add(title, "gapbottom 8")

        val nameField = WTextField("John Doe")
        nameField.header = "Name"
        form.add(nameField, "growx")

        val mailField = WTextField("john@example.com")
        mailField.header = "Email"
        form.add(mailField, "growx")

        val categoryComboBox = WComboBox(listOf("Contact request", "Bug report", "Other"))
        categoryComboBox.header = "Category"
        categoryComboBox.selectedIndex = 0
        form.add(categoryComboBox, "growx")

        val bodyField = WTextField()
        bodyField.header = "Message"
        bodyField.acceptsReturn = true
        bodyField.textWrapping = TextWrapping.WRAP
        form.add(bodyField, "grow, h 120::, pushy")

        val submitButton = WButton("Send")
        submitButton.isAccent = true
        submitButton.addActionListener {
            println("Submit: name=${nameField.text}, email=${mailField.text}, message=${bodyField.text}")
        }
        val cancelButton = WButton("Cancel")
        cancelButton.addActionListener {
            nameField.text = ""
            mailField.text = ""
            bodyField.text = ""
        }

        // Command row: right-aligned, split into 2, sg + wmin for equal width (120epx minimum),
        // 8epx between buttons, 16 + 8 = 24epx above the row (gaptop is applied to both because it
        // acts per cell within a split). The primary Send button is on the left, Cancel on the right
        form.add(submitButton, "split 2, align right, sg button, wmin 120, gaptop 8")
        form.add(cancelButton, "sg button, wmin 120, gapleft 8, gaptop 8")

        frame.setContentPane(form)
        frame.appWindow.resizeClient(720, 900)
        frame.isVisible = true
    }
}
