package com.appkitbox.winui4k.sample.notes

import com.appkitbox.winui4k.CommandBarDefaultLabelPosition
import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.ItemsViewSelectionMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.SystemBackdropType
import com.appkitbox.winui4k.TextTrimming
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WAppBarButton
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WCommandBar
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WItemContainer
import com.appkitbox.winui4k.WItemsView
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTitleBar
import com.appkitbox.winui4k.WUniformGridLayout
import com.appkitbox.winui4k.WinUiUtilities
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The equivalent of the official WinUI Notes tutorial's ApplicationData.Current.LocalFolder.
// Since this is an unpackaged app, notes are saved under the home directory
private val storageDirectory = File(System.getProperty("user.home"), ".winui4k-notes").also { it.mkdirs() }

/** The same "4/2/2025 3:27:02 PM" format as the official sample's Note.Date (DateTime.ToString()). */
private val dateFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US)

/** Whether the current theme is dark. Updated by the root element's ActualThemeChanged. */
private var isDarkTheme = false

/** A card's background (a translucent color matching CardBackgroundFillColorDefault). */
private val CARD_BACKGROUND: WColor
    get() = if (isDarkTheme) WColor(255, 255, 255, 13) else WColor(255, 255, 255, 179)

/** A card's border (matching CardStrokeColorDefault). */
private val CARD_BORDER: WColor
    get() = if (isDarkTheme) WColor(0, 0, 0, 25) else WColor(229, 229, 229)

/** The background of the card's bottom date footer (matching ControlAltFillColorSecondary). */
private val FOOTER_BACKGROUND: WColor
    get() = if (isDarkTheme) WColor(255, 255, 255, 11) else WColor(0, 0, 0, 6)

/** A subdued text color for things like the date (matching TextFillColorSecondary). */
private val TEXT_SECONDARY: WColor
    get() = if (isDarkTheme) WColor(255, 255, 255, 197) else WColor(97, 97, 97)

/**
 * The equivalent of the official sample's Models/Note. Saves text as one file per note.
 * Following the official sample's DateTime.Now.ToBinary(), the file name is generated from the creation time.
 */
private class Note(
    val file: File = File(storageDirectory, "${System.currentTimeMillis()}.txt"),
    var text: String = "",
    val date: Date = Date(),
) {
    fun save() {
        file.writeText(text)
    }

    fun delete() {
        if (file.exists()) file.delete()
    }
}

/** The equivalent of the official sample's Models/AllNotes. Loads every note in the storage directory, newest first. */
private fun loadAllNotes(): List<Note> {
    val files = storageDirectory.listFiles { file -> file.extension == "txt" } ?: emptyArray()
    return files
        .map { Note(it, it.readText(), Date(it.lastModified())) }
        .sortedByDescending { it.date }
}

/**
 * A notes app equivalent to Microsoft's WinUI Notes tutorial.
 * Below a TitleBar (a sticky-note icon plus a back button), switches between the note list
 * (equivalent to AllNotesPage) and the editing page (equivalent to NotePage).
 */
@Suppress("LongMethod") // Declarative UI-building sample code
fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "WinUINotes")

        // The official sample's MainWindow's TitleBar. The icon is Segoe Fluent Icons' QuickNote (U+F4AA)
        val titleBar = WTitleBar()
        titleBar.title = "WinUI Notes"
        titleBar.iconGlyph = ""
        titleBar.isBackButtonVisible = true
        titleBar.isBackButtonEnabled = false // equivalent to rootFrame.CanGoBack

        // Equivalent to the official sample's rootFrame. Being a Grid, its child stretches to fill the window
        val contentArea = WGrid()
        contentArea.addRow(GridLength.star())
        contentArea.addColumn(GridLength.star())

        var isOnNotePage = false
        lateinit var showAllNotesPage: () -> Unit
        lateinit var showNotePage: (Note) -> Unit

        showNotePage = { note ->
            isOnNotePage = true
            titleBar.isBackButtonEnabled = true
            contentArea.removeAll()
            contentArea.add(
                buildNotePage(
                    note,
                    onDelete = { showAllNotesPage() },
                ),
            )
        }

        showAllNotesPage = {
            isOnNotePage = false
            titleBar.isBackButtonEnabled = false
            contentArea.removeAll()
            contentArea.add(
                buildAllNotesPage(
                    notes = loadAllNotes(),
                    onNewNote = { showNotePage(Note()) },
                    onNoteInvoked = { note -> showNotePage(note) },
                ),
            )
        }

        titleBar.addBackRequestedListener {
            if (isOnNotePage) {
                showAllNotesPage()
            }
        }

        showAllNotesPage()

        val rootGrid = WGrid()
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.star())
        rootGrid.add(titleBar, row = 0, column = 0)
        rootGrid.add(contentArea, row = 1, column = 0)

        // When the theme changes, rebuild the visible page with theme-derived colors (CARD_BACKGROUND etc.)
        isDarkTheme = rootGrid.actualTheme == ElementTheme.DARK
        rootGrid.addActualThemeChangedListener {
            isDarkTheme = rootGrid.actualTheme == ElementTheme.DARK
            WinUiUtilities.invokeLater { showAllNotesPage() }
        }

        frame.setContentPane(rootGrid)
        frame.extendsContentIntoTitleBar = true
        frame.setTitleBar(titleBar)
        frame.systemBackdrop = SystemBackdropType.MICA // the official sample's MicaBackdrop Kind="Base"
        frame.isVisible = true
    }
}

/**
 * Equivalent to the official sample's Views/AllNotesPage: a CommandBar (a "Quick notes" heading
 * and a New note button) on top, and an ItemsView + UniformGridLayout card grid below.
 */
private fun buildAllNotesPage(
    notes: List<Note>,
    onNewNote: () -> Unit,
    onNoteInvoked: (Note) -> Unit,
): WComponent {
    // A heading equivalent to the official sample's SubtitleTextBlockStyle (20px SemiBold)
    val heading = WLabel("Quick notes")
    heading.fontSize = 20.0
    heading.fontWeight = 600
    heading.setMargin(12.0, 8.0, 12.0, 8.0)

    val newNoteButton = WAppBarButton("New note", Symbol.ADD)
    newNoteButton.addActionListener { onNewNote() }

    val commandBar = WCommandBar()
    commandBar.defaultLabelPosition = CommandBarDefaultLabelPosition.RIGHT
    commandBar.content = heading
    commandBar.addPrimaryCommand(newNoteButton)

    val layout = WUniformGridLayout()
    layout.minItemWidth = 200.0
    layout.minColumnSpacing = 20.0
    layout.minRowSpacing = 20.0

    val itemsView = WItemsView()
    itemsView.margin = 24.0
    itemsView.layout = layout
    itemsView.selectionMode = ItemsViewSelectionMode.NONE
    itemsView.isItemInvokedEnabled = true
    itemsView.setItems(notes.map { buildNoteCard(it) })
    itemsView.addItemInvokedListener { index -> onNoteInvoked(notes[index]) }

    val page = WGrid()
    page.addRow(GridLength.AUTO)
    page.addRow(GridLength.star())
    page.addColumn(GridLength.star())
    page.add(commandBar, row = 0, column = 0)
    page.add(itemsView, row = 1, column = 0)
    return page
}

/**
 * A card equivalent to the official sample's NoteItemTemplate: a height-120 body preview on top,
 * a date footer (a band with a faint background) below. The whole thing is framed with an 8-corner-radius border.
 */
private fun buildNoteCard(note: Note): WItemContainer {
    val preview = WLabel(note.text)
    preview.textWrapping = TextWrapping.WRAP
    preview.textTrimming = TextTrimming.WORD_ELLIPSIS
    preview.margin = 4.0

    val dateLabel = WLabel(dateFormat.format(note.date))
    dateLabel.fontSize = 12.0 // matching CaptionTextBlockStyle
    dateLabel.foreground = TEXT_SECONDARY

    val footer = WBorder(dateLabel)
    footer.background = FOOTER_BACKGROUND
    footer.setPadding(4.0, 6.0, 0.0, 6.0)

    val cardGrid = WGrid()
    cardGrid.addRow(GridLength.pixel(120.0))
    cardGrid.addRow(GridLength.AUTO)
    cardGrid.addColumn(GridLength.star())
    cardGrid.add(preview, row = 0, column = 0)
    cardGrid.add(footer, row = 1, column = 0)

    val card = WBorder(cardGrid)
    card.background = CARD_BACKGROUND
    card.borderColor = CARD_BORDER
    card.borderThickness = 1.0
    card.cornerRadius = 8.0 // matching OverlayCornerRadius

    val container = WItemContainer(card)
    container.cornerRadius = 8.0
    return container
}

/**
 * Equivalent to the official sample's Views/NotePage: a multi-line TextBox with a date header
 * centered (width 400), with Save (accent-colored) and Delete buttons at the bottom-right.
 */
private fun buildNotePage(note: Note, onDelete: () -> Unit): WComponent {
    val editor = WTextField()
    editor.acceptsReturn = true
    editor.textWrapping = TextWrapping.WRAP
    editor.placeholderText = "Enter your note"
    editor.header = dateFormat.format(note.date)
    editor.maxWidth = 400.0
    editor.text = note.text

    val saveButton = WButton("Save")
    saveButton.isAccent = true // matching the official sample's AccentButtonStyle
    saveButton.addActionListener {
        note.text = editor.text
        note.save()
    }

    val deleteButton = WButton("Delete")
    deleteButton.addActionListener {
        note.delete()
        onDelete()
    }

    val buttonRow = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    buttonRow.horizontalAlignment = HorizontalAlignment.RIGHT
    buttonRow.add(saveButton)
    buttonRow.add(deleteButton)

    // The same 3-column Grid (* / 400 / *) as the official sample, placing the editor in the center column
    val page = WGrid()
    page.margin = 16.0
    page.rowSpacing = 8.0
    page.addRow(GridLength.star())
    page.addRow(GridLength.AUTO)
    page.addColumn(GridLength.star())
    page.addColumn(GridLength.pixel(400.0))
    page.addColumn(GridLength.star())
    page.add(editor, row = 0, column = 1)
    page.add(buttonRow, row = 1, column = 1)
    return page
}
