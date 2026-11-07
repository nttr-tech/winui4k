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
        Messages.get("clipboard.description"),
    )
    page.add(buildClipboardGuideExample())
    page.add(buildCopyTextExample())
    page.add(buildPasteTextExample())
    page.add(buildCopyPasteFilesExample())
    page.add(buildHistoryRoamingExample())
    page.add(buildOtherOperationsExample())
    return page
}

/** A bulleted list with one item per line (file paths or format names). */
private fun bulletLines(items: List<String>): String = items.joinToString("\n") { Messages.get("clipboard.list.item", it) }

/** Guide: the roles of the classes involved, the copy/paste flow, and caveats. */
private fun buildClipboardGuideExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel(Messages.get("clipboard.guide.clipboard")))
    body.add(WLabel(Messages.get("clipboard.guide.datapackage")))
    body.add(WLabel(Messages.get("clipboard.guide.datapackageview")))
    body.add(WLabel(Messages.get("clipboard.guide.standarddataformats")))
    body.add(WLabel(Messages.get("clipboard.guide.contentoptions")))
    body.add(
        WLabel(
            Messages.get("clipboard.guide.async"),
        ).also { it.textWrapping = TextWrapping.WRAP },
    )

    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = Messages.get("clipboard.guide.note")
    body.add(note)
    return buildExample(Messages.get("clipboard.guide.title"), body)
}

/** Copying text: DataPackage.setText -> Clipboard.setContent. */
private fun buildCopyTextExample(): WComponent {
    val input = WTextField().apply { placeholderText = Messages.get("clipboard.input.placeholder") }
    input.text = Messages.get("clipboard.copytext.sample")
    input.width = 400.0
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("clipboard.copytext.button"))

    button.addActionListener {
        runCatching {
            val dataPackage = DataPackage()
            dataPackage.setText(input.text)
            Clipboard.setContent(dataPackage)
            result.text = Messages.get("clipboard.copytext.copied")
        }.onFailure { result.text = Messages.get("clipboard.copyfailed", it.message) }
    }

    val body = WPanel(spacing = 8.0)
    body.add(input)
    body.add(button)
    body.add(result)
    return buildExample(Messages.get("clipboard.copytext.title"), body)
}

/** Pasting text: Clipboard.getContent -> contains -> getText. */
private fun buildPasteTextExample(): WComponent {
    val result = WLabel(Messages.get("clipboard.pastetext.initial")).also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("clipboard.pastetext.button"))

    button.addActionListener {
        runCatching {
            val view = Clipboard.getContent()
            if (view.contains(StandardDataFormats.TEXT)) {
                view.getText { text -> result.text = Messages.get("clipboard.pastetext.result", text) }
            } else {
                result.text = Messages.get("clipboard.pastetext.notext")
            }
        }.onFailure { result.text = Messages.get("clipboard.pastefailed", it.message) }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)
    return buildExample(Messages.get("clipboard.pastetext.title"), body)
}

/** Copying and pasting files: setStorageItems / getStorageItems and RequestedOperation. */
private fun buildCopyPasteFilesExample(): WComponent {
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val copyButton = WButton(Messages.get("clipboard.files.copybutton"))
    val pasteButton = WButton(Messages.get("clipboard.files.pastebutton"))

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
                        result.text = Messages.get("clipboard.files.copied", paths.size.toString())
                    } else {
                        result.text = Messages.get("clipboard.files.cancelled")
                    }
                }.onFailure { result.text = Messages.get("clipboard.copyfailed", it.message) }
                copyButton.isEnabled = true
            }
        }.onFailure {
            result.text = Messages.get("clipboard.files.dialogfailed", it.message)
            copyButton.isEnabled = true
        }
    }

    pasteButton.addActionListener {
        runCatching {
            val view = Clipboard.getContent()
            if (view.contains(StandardDataFormats.STORAGE_ITEMS)) {
                val requested = view.requestedOperation
                view.getStorageItems { paths ->
                    result.text = Messages.get("clipboard.files.pasted", requested.toString(), bulletLines(paths))
                }
            } else {
                result.text = Messages.get("clipboard.files.nofiles")
            }
        }.onFailure { result.text = Messages.get("clipboard.pastefailed", it.message) }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(copyButton)
    buttons.add(pasteButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("clipboard.files.operation")))
    options.add(operation)
    return buildExample(Messages.get("clipboard.files.title"), body, options)
}

/** History and roaming: setContent with ClipboardContentOptions, and checking the OS settings. */
private fun buildHistoryRoamingExample(): WComponent {
    val input = WTextField().apply { placeholderText = Messages.get("clipboard.input.placeholder") }
    input.text = Messages.get("clipboard.history.sample")
    input.width = 400.0
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("clipboard.history.button"))

    val allowHistory = WToggleSwitch(Messages.get("clipboard.history.allowhistory"))
    allowHistory.isOn = true
    val allowRoaming = WToggleSwitch(Messages.get("clipboard.history.allowroaming"))
    allowRoaming.isOn = true

    button.addActionListener {
        runCatching {
            val dataPackage = DataPackage()
            dataPackage.setText(input.text)
            val options = ClipboardContentOptions()
            options.isAllowedInHistory = allowHistory.isOn
            options.isRoamable = allowRoaming.isOn
            result.text = if (Clipboard.setContent(dataPackage, options)) {
                Messages.get(
                    "clipboard.history.copied",
                    if (options.isAllowedInHistory) Messages.get("clipboard.history.keep") else Messages.get("clipboard.history.nokeep"),
                    if (options.isRoamable) Messages.get("clipboard.history.sync") else Messages.get("clipboard.history.nosync"),
                )
            } else {
                Messages.get("clipboard.history.setfailed")
            }
        }.onFailure { result.text = Messages.get("clipboard.copyfailed", it.message) }
    }

    val status = WLabel(
        Messages.get(
            "clipboard.history.ossettings",
            if (Clipboard.isHistoryEnabled) Messages.get("clipboard.history.enabled") else Messages.get("clipboard.history.disabled"),
            if (Clipboard.isRoamingEnabled) Messages.get("clipboard.history.enabled") else Messages.get("clipboard.history.disabled"),
        ),
    ).also { it.textWrapping = TextWrapping.WRAP }

    val body = WPanel(spacing = 8.0)
    body.add(input)
    body.add(button)
    body.add(result)
    body.add(status)

    val options = WPanel(spacing = 8.0)
    options.add(allowHistory)
    options.add(allowRoaming)
    return buildExample(Messages.get("clipboard.history.title"), body, options)
}

/** Other operations: listing formats, clearing, deleting history, and watching ContentChanged. */
private fun buildOtherOperationsExample(): WComponent {
    val result = WLabel("").also { it.textWrapping = TextWrapping.WRAP }

    val showFormatsButton = WButton(Messages.get("clipboard.other.showformats"))
    showFormatsButton.addActionListener {
        runCatching {
            val formats = Clipboard.getContent().availableFormats
            result.text = if (formats.isNotEmpty()) {
                Messages.get("clipboard.other.formats", bulletLines(formats))
            } else {
                Messages.get("clipboard.other.empty")
            }
        }.onFailure { result.text = Messages.get("clipboard.other.getfailed", it.message) }
    }

    val clearButton = WButton(Messages.get("clipboard.other.clear"))
    clearButton.addActionListener {
        runCatching {
            Clipboard.clear()
            result.text = Messages.get("clipboard.other.cleared")
        }.onFailure { result.text = Messages.get("clipboard.other.clearfailed", it.message) }
    }

    val clearHistoryButton = WButton(Messages.get("clipboard.other.clearhistory"))
    clearHistoryButton.addActionListener {
        runCatching {
            result.text = if (Clipboard.clearHistory()) {
                Messages.get("clipboard.other.historycleared")
            } else {
                Messages.get("clipboard.other.historynotcleared")
            }
        }.onFailure { result.text = Messages.get("clipboard.other.historyclearfailed", it.message) }
    }

    // Subscribe the listener only while monitoring. Copies in other apps are notified too
    val contentChangedListener: () -> Unit = {
        result.text = Messages.get("clipboard.other.changed")
    }
    val monitor = WToggleSwitch(Messages.get("clipboard.other.monitor"))
    monitor.addItemListener { isOn ->
        if (isOn) {
            Clipboard.addContentChangedListener(contentChangedListener)
            result.text = Messages.get("clipboard.other.monitorstarted")
        } else {
            Clipboard.removeContentChangedListener(contentChangedListener)
            result.text = Messages.get("clipboard.other.monitorstopped")
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
    return buildExample(Messages.get("clipboard.other.title"), body)
}

// endregion
