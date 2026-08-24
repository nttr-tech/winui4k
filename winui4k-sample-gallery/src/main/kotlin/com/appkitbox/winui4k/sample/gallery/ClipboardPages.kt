package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.Clipboard
import com.appkitbox.winui4k.ClipboardContentOptions
import com.appkitbox.winui4k.DataPackage
import com.appkitbox.winui4k.DataPackageOperation
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.StandardDataFormats
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFileOpenPicker
import com.appkitbox.winui4k.WInfoBar
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WToggleSwitch

/*
 * System category: the demo page for the Clipboard (Clipboard / DataPackage / DataPackageView).
 */

// region Clipboard page

/** Clipboard page: a guide plus demos exercising copy / paste / history / monitoring. */
internal fun buildClipboardPage(): WComponent {
    val page = buildPage(
        "Clipboard",
        "Reading and writing the OS clipboard. To copy, fill a DataPackage and pass it to Clipboard.setContent; " +
            "to paste, receive a DataPackageView from Clipboard.getContent and read it.",
    )
    page.add(buildClipboardGuideExample())
    page.add(buildCopyTextExample())
    page.add(buildPasteTextExample())
    page.add(buildCopyPasteFilesExample())
    page.add(buildHistoryRoamingExample())
    page.add(buildOtherOperationsExample())
    return page
}

/** Guide: the roles of the classes involved, the copy/paste flow, and caveats. */
private fun buildClipboardGuideExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel("• Clipboard — the OS clipboard itself. setContent / getContent / clear / addContentChangedListener"))
    body.add(WLabel("• DataPackage — the container for copied content. setText / setHtmlFormat / setRtf / setUri / setStorageItems"))
    body.add(WLabel("• DataPackageView — the read-only view when pasting. Check the format with contains, then read via getText and friends"))
    body.add(WLabel("• StandardDataFormats — format name constants (TEXT / HTML / RTF / WEB_LINK / STORAGE_ITEMS, etc.)"))
    body.add(WLabel("• ClipboardContentOptions — options controlling history (Win+V) and roaming"))
    body.add(
        WLabel(
            "Reading involves requesting the data from the copying app, so it's asynchronous and " +
                "results arrive via a callback on the UI thread. A single DataPackage can hold multiple formats at once.",
        ).also { it.textWrapping = TextWrapping.WRAP },
    )

    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = "The clipboard is only accessible while the window is in the foreground. " +
        "Also, reading and writing images (the Bitmap format) isn't supported yet."
    body.add(note)
    return buildExample("Guide: class roles and the copy/paste flow", body)
}

/** Copying text: DataPackage.setText -> Clipboard.setContent. */
private fun buildCopyTextExample(): WComponent {
    val input = WTextField("Text to copy")
    input.text = "This text gets copied to the clipboard."
    input.width = 400.0
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Copy text")

    button.addActionListener {
        runCatching {
            val dataPackage = DataPackage()
            dataPackage.setText(input.text)
            Clipboard.setContent(dataPackage)
            result.text = "Copied to the clipboard."
        }.onFailure { result.text = "Couldn't copy: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(input)
    body.add(button)
    body.add(result)
    return buildExample("Copying text (DataPackage.setText / Clipboard.setContent)", body)
}

/** Pasting text: Clipboard.getContent -> contains -> getText. */
private fun buildPasteTextExample(): WComponent {
    val result = WLabel("Press the button to paste here.").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Paste text")

    button.addActionListener {
        runCatching {
            val view = Clipboard.getContent()
            if (view.contains(StandardDataFormats.TEXT)) {
                view.getText { text -> result.text = "Pasted: $text" }
            } else {
                result.text = "There's no text on the clipboard."
            }
        }.onFailure { result.text = "Couldn't paste: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)
    return buildExample("Pasting text (Clipboard.getContent / DataPackageView.getText)", body)
}

/** Copying and pasting files: setStorageItems / getStorageItems and RequestedOperation. */
private fun buildCopyPasteFilesExample(): WComponent {
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val copyButton = WButton("Pick files and copy")
    val pasteButton = WButton("Paste files")

    val operation = WComboBox(DataPackageOperation.entries.map { it.name })
    operation.selectedIndex = DataPackageOperation.entries.indexOf(DataPackageOperation.COPY)

    copyButton.addActionListener {
        copyButton.isEnabled = false
        runCatching {
            val picker = WFileOpenPicker(galleryFrame)
            picker.addFileTypeFilter("*")
            picker.pickMultipleFiles { paths ->
                runCatching {
                    if (paths.isNotEmpty()) {
                        val dataPackage = DataPackage()
                        dataPackage.setStorageItems(*paths.toTypedArray())
                        dataPackage.requestedOperation = DataPackageOperation.entries[operation.selectedIndex]
                        Clipboard.setContent(dataPackage)
                        result.text = "Copied ${paths.size} file(s). They can be pasted into Explorer."
                    } else {
                        result.text = "Canceled."
                    }
                }.onFailure { result.text = "Couldn't copy: ${it.message}" }
                copyButton.isEnabled = true
            }
        }.onFailure {
            result.text = "Couldn't open the dialog: ${it.message}"
            copyButton.isEnabled = true
        }
    }

    pasteButton.addActionListener {
        runCatching {
            val view = Clipboard.getContent()
            if (view.contains(StandardDataFormats.STORAGE_ITEMS)) {
                val requested = view.requestedOperation
                view.getStorageItems { paths ->
                    result.text = "Requested operation: $requested\n" + paths.joinToString("\n") { "• $it" }
                }
            } else {
                result.text = "There are no files on the clipboard. Copy files in Explorer to try this."
            }
        }.onFailure { result.text = "Couldn't paste: ${it.message}" }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(copyButton)
    buttons.add(pasteButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Operation requested of the paste target (RequestedOperation)"))
    options.add(operation)
    return buildExample("Copying and pasting files (setStorageItems / getStorageItems)", body, options)
}

/** History and roaming: setContent with ClipboardContentOptions, and checking the OS settings. */
private fun buildHistoryRoamingExample(): WComponent {
    val input = WTextField("Text to copy")
    input.text = "Text whose addition to history can be controlled"
    input.width = 400.0
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Copy with options")

    val allowHistory = WToggleSwitch("Keep in history (Win+V) (IsAllowedInHistory)")
    allowHistory.isOn = true
    val allowRoaming = WToggleSwitch("Sync to other devices (IsRoamable)")
    allowRoaming.isOn = true

    button.addActionListener {
        runCatching {
            val dataPackage = DataPackage()
            dataPackage.setText(input.text)
            val options = ClipboardContentOptions()
            options.isAllowedInHistory = allowHistory.isOn
            options.isRoamable = allowRoaming.isOn
            result.text = if (Clipboard.setContent(dataPackage, options)) {
                "Copied. History: ${if (options.isAllowedInHistory) "kept" else "not kept"} / " +
                    "sync: ${if (options.isRoamable) "on" else "off"}"
            } else {
                "Couldn't set the clipboard content."
            }
        }.onFailure { result.text = "Couldn't copy: ${it.message}" }
    }

    val status = WLabel(
        "OS settings — history: ${if (Clipboard.isHistoryEnabled) "enabled" else "disabled"} / " +
            "sync: ${if (Clipboard.isRoamingEnabled) "enabled" else "disabled"}",
    ).also { it.textWrapping = TextWrapping.WRAP }

    val body = WPanel(spacing = 8.0)
    body.add(input)
    body.add(button)
    body.add(result)
    body.add(status)

    val options = WPanel(spacing = 8.0)
    options.add(allowHistory)
    options.add(allowRoaming)
    return buildExample("History and roaming (ClipboardContentOptions)", body, options)
}

/** Other operations: listing formats, clearing, deleting history, and watching ContentChanged. */
private fun buildOtherOperationsExample(): WComponent {
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }

    val showFormatsButton = WButton("Show current formats")
    showFormatsButton.addActionListener {
        runCatching {
            val formats = Clipboard.getContent().availableFormats
            result.text = if (formats.isNotEmpty()) {
                "Formats on the clipboard:\n" + formats.joinToString("\n") { "• $it" }
            } else {
                "The clipboard is empty."
            }
        }.onFailure { result.text = "Couldn't read the formats: ${it.message}" }
    }

    val clearButton = WButton("Empty the clipboard")
    clearButton.addActionListener {
        runCatching {
            Clipboard.clear()
            result.text = "Emptied the clipboard."
        }.onFailure { result.text = "Couldn't clear: ${it.message}" }
    }

    val clearHistoryButton = WButton("Delete history")
    clearHistoryButton.addActionListener {
        runCatching {
            result.text = if (Clipboard.clearHistory()) "Deleted the history (Win+V)." else "Couldn't delete the history."
        }.onFailure { result.text = "Couldn't delete the history: ${it.message}" }
    }

    // Subscribe the listener only while monitoring. Copies in other apps are notified too
    val contentChangedListener: () -> Unit = {
        result.text = "The clipboard content changed. Check it with \"Show current formats\"."
    }
    val monitor = WToggleSwitch("Watch for content changes (ContentChanged)")
    monitor.addItemListener { isOn ->
        if (isOn) {
            Clipboard.addContentChangedListener(contentChangedListener)
            result.text = "Started watching. Copying in this app or another app triggers a notification."
        } else {
            Clipboard.removeContentChangedListener(contentChangedListener)
            result.text = "Stopped watching."
        }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(showFormatsButton)
    buttons.add(clearButton)
    buttons.add(clearHistoryButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(monitor)
    body.add(result)
    return buildExample("Other operations (availableFormats / clear / clearHistory / ContentChanged)", body)
}

// endregion
