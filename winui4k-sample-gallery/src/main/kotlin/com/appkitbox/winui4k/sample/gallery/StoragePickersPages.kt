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
        "WinAppSDK pickers that open the OS file/folder selection dialogs. " +
            "Try WFileOpenPicker (Open), WFileSavePicker (Save As), and WFolderPicker (Select Folder).",
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
    body.add(WLabel("• WFileOpenPicker — opens files. pickSingleFile (single) / pickMultipleFiles (multiple)"))
    body.add(WLabel("• WFileSavePicker — Save As. pickSaveFile (only returns the destination path; doesn't create the file)"))
    body.add(WLabel("• WFolderPicker — selects a folder. pickSingleFolder"))
    body.add(
        WLabel(
            "Usage: create the picker with its owner WFrame, configure file types and other options, then open it. " +
                "The dialog stays open until the user closes it, so results arrive via a callback on the UI thread " +
                "(null on cancel; an empty list for multiple selection).",
        ).also { it.textWrapping = TextWrapping.WRAP },
    )

    val note = WInfoBar()
    note.isOpen = true
    note.isClosable = false
    note.message = "Pickers remember and restore the last-selected location and view mode. " +
        "SuggestedStartLocation and ViewMode only take effect on the first showing (before any memory exists)."
    body.add(note)
    return buildExample("Guide: choosing a picker and shared usage", body)
}

/** The file type choices (combo box display name -> extensions passed to addFileTypeFilter). */
private val fileTypeFilters = linkedMapOf(
    "All files (*)" to arrayOf("*"),
    "Text (.txt)" to arrayOf(".txt"),
    "Images (.jpg / .png)" to arrayOf(".jpg", ".png"),
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
    val result = WLabel("Nothing picked").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Pick a file")

    val fileType = WComboBox(fileTypeFilters.keys.toList())
    fileType.selectedIndex = 0
    val commitText = WTextField("The default is \"Open\"")
    commitText.text = "Open this file"
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
                result.text = if (path != null) "Picked: $path" else "Canceled"
                button.isEnabled = true
            }
        }.onFailure {
            result.text = "Couldn't open the dialog: ${it.message}"
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("File types (FileTypeFilter)"))
    options.add(fileType)
    options.add(optionsLabel("Commit button caption (CommitButtonText)"))
    options.add(commitText)
    options.add(optionsLabel("Folder shown first (SuggestedStartLocation)"))
    options.add(location)
    options.add(optionsLabel("View mode (ViewMode)"))
    options.add(viewMode)
    return buildExample("Picking a single file (WFileOpenPicker.pickSingleFile)", body, options)
}

/** Picking multiple files: receiving PickMultipleFilesAsync's list. */
private fun buildPickMultipleFilesExample(): WComponent {
    val result = WLabel("Nothing picked").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Pick multiple files")

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
                    paths.joinToString("\n") { "Picked: $it" }
                } else {
                    "Canceled"
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = "Couldn't open the dialog: ${it.message}"
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("File types (FileTypeFilter)"))
    options.add(fileType)
    options.add(optionsLabel("Folder shown first (SuggestedStartLocation)"))
    options.add(location)
    options.add(optionsLabel("View mode (ViewMode)"))
    options.add(viewMode)
    return buildExample("Picking multiple files (WFileOpenPicker.pickMultipleFiles)", body, options)
}

/** Saving a file: FileTypeChoices / DefaultFileExtension / SuggestedFileName / SuggestedFolder. */
@Suppress("LongMethod") // Declarative UI-building for the sample
private fun buildSaveFileExample(): WComponent {
    val content = WTextField("Content to save")
    content.text = "Hello, WinUI4K!"
    content.width = 320.0
    val result = WLabel("Not saved").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Save As")

    val txtCheckBox = WCheckBox("Text (.txt)")
    txtCheckBox.isChecked = true
    val jsonCheckBox = WCheckBox("JSON (.json)")
    val xmlCheckBox = WCheckBox("XML (.xml)")
    val fileName = WTextField("Initial file name")
    fileName.text = "NewDocument"
    val location = buildLocationComboBox()
    val folder = WTextField("Initial destination folder (optional)")
    val folderButton = WButton("Browse...")
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
            if (txtCheckBox.isChecked == true) picker.addFileTypeChoice("Text", ".txt")
            if (jsonCheckBox.isChecked == true) picker.addFileTypeChoice("JSON", ".json")
            if (xmlCheckBox.isChecked == true) picker.addFileTypeChoice("XML", ".xml")
            picker.suggestedFileName = fileName.text
            picker.suggestedStartLocation = PickerLocationId.entries[location.selectedIndex]
            if (folder.text.isNotEmpty()) picker.suggestedFolder = folder.text
            picker.pickSaveFile { path ->
                result.text = if (path != null) {
                    // The picker only returns a path, so the app does the writing
                    File(path).writeText(content.text)
                    "Saved: $path"
                } else {
                    "Canceled"
                }
                button.isEnabled = true
            }
        }.onFailure {
            result.text = "Couldn't open the dialog: ${it.message}"
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(optionsLabel("Content to write to the file"))
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
    options.add(optionsLabel("File types (FileTypeChoices)"))
    options.add(txtCheckBox)
    options.add(jsonCheckBox)
    options.add(xmlCheckBox)
    options.add(optionsLabel("Initial file name (SuggestedFileName)"))
    options.add(fileName)
    options.add(optionsLabel("Folder shown first (SuggestedStartLocation)"))
    options.add(location)
    options.add(optionsLabel("Initial destination folder (SuggestedFolder)"))
    options.add(folderRow)
    return buildExample("Saving a file (WFileSavePicker.pickSaveFile)", body, options)
}

/** Selecting a folder: PickSingleFolderAsync. */
private fun buildPickFolderExample(): WComponent {
    val result = WLabel("Nothing picked").also { it.textWrapping = TextWrapping.WRAP }
    val button = WButton("Pick a folder")

    val commitText = WTextField("The default is \"Select Folder\"")
    commitText.text = "Use this folder"
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
                result.text = if (path != null) "Picked: $path" else "Canceled"
                button.isEnabled = true
            }
        }.onFailure {
            result.text = "Couldn't open the dialog: ${it.message}"
            button.isEnabled = true
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(button)
    body.add(result)

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Commit button caption (CommitButtonText)"))
    options.add(commitText)
    options.add(optionsLabel("Folder shown first (SuggestedStartLocation)"))
    options.add(location)
    options.add(optionsLabel("View mode (ViewMode)"))
    options.add(viewMode)
    return buildExample("Selecting a folder (WFolderPicker.pickSingleFolder)", body, options)
}

// endregion
