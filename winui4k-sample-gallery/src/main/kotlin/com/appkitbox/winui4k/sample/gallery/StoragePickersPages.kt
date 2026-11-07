package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.PickerLocationId
import com.appkitbox.winui4k.PickerViewMode
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFileOpenPicker
import com.appkitbox.winui4k.WFileSavePicker
import com.appkitbox.winui4k.WFolderPicker
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WInfoBar
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField
import java.io.File

/*
 * System category: the demo page for the Storage pickers (WFileOpenPicker / WFileSavePicker / WFolderPicker).
 */

// region Storage pickers page

/** Storage pickers page: a guide plus demos that exercise all three pickers. */
internal fun buildStoragePickersPage(): WComponent {
    val page = buildPage(
        "Storage pickers",
        Messages.get("storagepickers.description"),
    )
    page.add(buildPickerGuideExample())
    page.add(buildPickSingleFileExample())
    page.add(buildPickMultipleFilesExample())
    page.add(buildSaveFileExample())
    page.add(buildPickFolderExample())
    return page
}

/** Guide: when to use each picker, their shared usage, and the first-showing-only settings caveat. */
private fun buildPickerGuideExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel(Messages.get("storagepickers.guide.open")))
    body.add(WLabel(Messages.get("storagepickers.guide.save")))
    body.add(WLabel(Messages.get("storagepickers.guide.folder")))
    body.add(
        WLabel(Messages.get("storagepickers.guide.usage")).also { it.textWrapping = TextWrapping.WRAP },
    )

    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = Messages.get("storagepickers.guide.note")
    body.add(note)
    return buildExample(Messages.get("storagepickers.guide.title"), body)
}

/** The file type choices (combo box display name -> extensions passed to addFileTypeFilter). */
private val fileTypeFilters = linkedMapOf(
    Messages.get("storagepickers.filetype.all") to arrayOf("*"),
    Messages.get("storagepickers.filetype.text") to arrayOf(".txt"),
    Messages.get("storagepickers.filetype.image") to arrayOf(".jpg", ".png"),
)

/** The "folder shown first" selection combo box. */
private fun buildLocationComboBox(): WComboBox {
    val comboBox = WComboBox(PickerLocationId.entries.map { it.name })
    comboBox.selectedIndex = 0
    return comboBox
}

/** The "view mode" selection combo box. */
private fun buildViewModeComboBox(): WComboBox {
    val comboBox = WComboBox(PickerViewMode.entries.map { it.name })
    comboBox.selectedIndex = 0
    return comboBox
}

/** Picking a single file: FileTypeFilter / CommitButtonText / SuggestedStartLocation / ViewMode. */
private fun buildPickSingleFileExample(): WComponent {
    val result = WLabel(Messages.get("storagepickers.result.none")).also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("storagepickers.single.button"))

    val fileType = WComboBox(fileTypeFilters.keys.toList())
    fileType.selectedIndex = 0
    val commitText = WTextField().apply { placeholderText = Messages.get("storagepickers.single.commit.placeholder") }
    commitText.text = Messages.get("storagepickers.single.commit.default")
    val location = buildLocationComboBox()
    val viewMode = buildViewModeComboBox()

    button.addActionListener {
        button.isEnabled = false
        runCatching {
            val picker = WFileOpenPicker(galleryFrame)
            picker.addFileTypeFilter(*fileTypeFilters.values.toList()[fileType.selectedIndex])
            if (commitText.text.isNotEmpty()) picker.commitButtonText = commitText.text
            picker.suggestedStartLocation = PickerLocationId.entries[location.selectedIndex]
            picker.viewMode = PickerViewMode.entries[viewMode.selectedIndex]
            picker.pickSingleFile { path ->
                result.text = if (path != null) {
                    Messages.get("storagepickers.result.selected", path)
                } else {
                    Messages.get("storagepickers.result.cancelled")
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = Messages.get("storagepickers.result.error", it.message.toString())
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("storagepickers.option.filetype")))
    options.add(fileType)
    options.add(optionsLabel(Messages.get("storagepickers.option.commit")))
    options.add(commitText)
    options.add(optionsLabel(Messages.get("storagepickers.option.location")))
    options.add(location)
    options.add(optionsLabel(Messages.get("storagepickers.option.viewmode")))
    options.add(viewMode)
    return buildExample(Messages.get("storagepickers.single.title"), body, options)
}

/** Picking multiple files: receiving PickMultipleFilesAsync's list. */
private fun buildPickMultipleFilesExample(): WComponent {
    val result = WLabel(Messages.get("storagepickers.result.none")).also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("storagepickers.multiple.button"))

    val fileType = WComboBox(fileTypeFilters.keys.toList())
    fileType.selectedIndex = 0
    val location = buildLocationComboBox()
    val viewMode = buildViewModeComboBox()

    button.addActionListener {
        button.isEnabled = false
        runCatching {
            val picker = WFileOpenPicker(galleryFrame)
            picker.addFileTypeFilter(*fileTypeFilters.values.toList()[fileType.selectedIndex])
            picker.suggestedStartLocation = PickerLocationId.entries[location.selectedIndex]
            picker.viewMode = PickerViewMode.entries[viewMode.selectedIndex]
            picker.pickMultipleFiles { paths ->
                result.text = if (paths.isNotEmpty()) {
                    paths.joinToString("\n") { Messages.get("storagepickers.result.selected", it) }
                } else {
                    Messages.get("storagepickers.result.cancelled")
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = Messages.get("storagepickers.result.error", it.message.toString())
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("storagepickers.option.filetype")))
    options.add(fileType)
    options.add(optionsLabel(Messages.get("storagepickers.option.location")))
    options.add(location)
    options.add(optionsLabel(Messages.get("storagepickers.option.viewmode")))
    options.add(viewMode)
    return buildExample(Messages.get("storagepickers.multiple.title"), body, options)
}

/** Saving a file: FileTypeChoices / DefaultFileExtension / SuggestedFileName / SuggestedFolder. */
@Suppress("LongMethod") // Declarative UI-building for the sample
private fun buildSaveFileExample(): WComponent {
    val content = WTextField().apply { placeholderText = Messages.get("storagepickers.save.content.placeholder") }
    content.text = "Hello, WinUI4K!"
    content.width = 320.0
    val result = WLabel(Messages.get("storagepickers.save.result.none")).also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("storagepickers.save.button"))

    val txtCheckBox = WCheckBox(Messages.get("storagepickers.filetype.text"))
    txtCheckBox.isChecked = true
    val jsonCheckBox = WCheckBox("JSON (.json)")
    val xmlCheckBox = WCheckBox("XML (.xml)")
    val fileName = WTextField().apply { placeholderText = Messages.get("storagepickers.save.filename.placeholder") }
    fileName.text = "NewDocument"
    val location = buildLocationComboBox()
    val folder = WTextField().apply { placeholderText = Messages.get("storagepickers.save.folder.placeholder") }
    val folderButton = WButton(Messages.get("storagepickers.save.folder.browse"))
    folderButton.addActionListener {
        folderButton.isEnabled = false
        WFolderPicker(galleryFrame).pickSingleFolder { path ->
            if (path != null) folder.text = path
            folderButton.isEnabled = true
        }
    }

    button.addActionListener {
        button.isEnabled = false
        runCatching {
            val picker = WFileSavePicker(galleryFrame)
            if (txtCheckBox.isChecked == true) picker.addFileTypeChoice(Messages.get("storagepickers.save.choice.text"), ".txt")
            if (jsonCheckBox.isChecked == true) picker.addFileTypeChoice("JSON", ".json")
            if (xmlCheckBox.isChecked == true) picker.addFileTypeChoice("XML", ".xml")
            picker.suggestedFileName = fileName.text
            picker.suggestedStartLocation = PickerLocationId.entries[location.selectedIndex]
            if (folder.text.isNotEmpty()) picker.suggestedFolder = folder.text
            picker.pickSaveFile { path ->
                result.text = if (path != null) {
                    // The picker only returns a path, so the app does the writing
                    File(path).writeText(content.text)
                    Messages.get("storagepickers.save.saved", path)
                } else {
                    Messages.get("storagepickers.result.cancelled")
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = Messages.get("storagepickers.result.error", it.message.toString())
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(optionsLabel(Messages.get("storagepickers.save.content.label")))
    body.add(content)
    body.add(button)
    body.add(result)

    // The button gets its content-sized width (Auto) and the text field shrinks to the remaining
    // width (star). Even if a language change makes the button caption longer, the button won't
    // be clipped at the Options card's right edge
    val folderRow = WGrid()
    folderRow.columnSpacing = 8.0
    folderRow.addColumn(GridLength.star())
    folderRow.addColumn(GridLength.AUTO)
    folderRow.addRow(GridLength.AUTO)
    folderRow.add(folder, row = 0, column = 0)
    folderRow.add(folderButton, row = 0, column = 1)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("storagepickers.save.option.filetype")))
    options.add(txtCheckBox)
    options.add(jsonCheckBox)
    options.add(xmlCheckBox)
    options.add(optionsLabel(Messages.get("storagepickers.save.option.filename")))
    options.add(fileName)
    options.add(optionsLabel(Messages.get("storagepickers.option.location")))
    options.add(location)
    options.add(optionsLabel(Messages.get("storagepickers.save.option.folder")))
    options.add(folderRow)
    return buildExample(Messages.get("storagepickers.save.title"), body, options)
}

/** Selecting a folder: PickSingleFolderAsync. */
private fun buildPickFolderExample(): WComponent {
    val result = WLabel(Messages.get("storagepickers.result.none")).also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton(Messages.get("storagepickers.folder.button"))

    val commitText = WTextField().apply { placeholderText = Messages.get("storagepickers.folder.commit.placeholder") }
    commitText.text = Messages.get("storagepickers.folder.commit.default")
    val location = buildLocationComboBox()
    val viewMode = buildViewModeComboBox()

    button.addActionListener {
        button.isEnabled = false
        runCatching {
            val picker = WFolderPicker(galleryFrame)
            if (commitText.text.isNotEmpty()) picker.commitButtonText = commitText.text
            picker.suggestedStartLocation = PickerLocationId.entries[location.selectedIndex]
            picker.viewMode = PickerViewMode.entries[viewMode.selectedIndex]
            picker.pickSingleFolder { path ->
                result.text = if (path != null) {
                    Messages.get("storagepickers.result.selected", path)
                } else {
                    Messages.get("storagepickers.result.cancelled")
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = Messages.get("storagepickers.result.error", it.message.toString())
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel(Messages.get("storagepickers.option.commit")))
    options.add(commitText)
    options.add(optionsLabel(Messages.get("storagepickers.option.location")))
    options.add(location)
    options.add(optionsLabel(Messages.get("storagepickers.option.viewmode")))
    options.add(viewMode)
    return buildExample(Messages.get("storagepickers.folder.title"), body, options)
}

// endregion
