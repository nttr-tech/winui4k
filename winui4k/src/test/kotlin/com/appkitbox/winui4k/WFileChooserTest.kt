package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.io.File
import java.nio.file.Files
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the JFileChooser-like [WFileChooser] and [WFileNameExtensionFilter]. The values come from the behavior of JFileChooser / FileNameExtensionFilter.
 * How settings are mapped to the OS dialogs is checked by reading the picker made by [WFileChooser.createPicker] (without opening it),
 * and only one test opens and closes a dialog (finding the dialog window class #32770 and sending WM_CLOSE).
 */
class WFileChooserTest : FunSpec() {
    /** The WindowId of the shared window. sharedFrame waits for the UI thread, so read it first outside the UI thread. */
    private var windowId = 0L

    init {
        beforeSpec {
            val frame = UiTestHarness.sharedFrame()
            windowId = onUiThreadGet { frame.appWindow.id }
        }
        val tempDir: File = Files.createTempDirectory("wfilechooser").toFile().canonicalFile
        val existingFile = File(tempDir, "memo.txt").also { it.writeText("x") }
        afterSpec { tempDir.deleteRecursively() }

        var savedLocale = Locale.getDefault()
        beforeTest {
            savedLocale = Locale.getDefault()
            Locale.setDefault(Locale.JAPAN)
        }
        afterTest { Locale.setDefault(savedLocale) }

        test("the defaults are the same as JFileChooser (open, files only, single selection, only the All Files file type)") {
            val chooser = WFileChooser()
            chooser.dialogType shouldBe WFileChooser.OPEN_DIALOG
            chooser.fileSelectionMode shouldBe WFileChooser.FILES_ONLY
            chooser.isFileSelectionEnabled shouldBe true
            chooser.isDirectorySelectionEnabled shouldBe false
            chooser.isMultiSelectionEnabled shouldBe false
            chooser.isAcceptAllFileFilterUsed shouldBe true
            chooser.choosableFileFilters.toList() shouldContainExactly listOf(chooser.acceptAllFileFilter)
            chooser.fileFilter shouldBeSameInstanceAs chooser.acceptAllFileFilter
            chooser.selectedFile.shouldBeNull()
            chooser.selectedFiles.toList().shouldBeEmpty()
            chooser.dialogTitle.shouldBeNull()
            chooser.approveButtonText.shouldBeNull()
            chooser.currentDirectory.shouldBeNull()
        }

        test("the constants have the same values as JFileChooser") {
            listOf(
                WFileChooser.APPROVE_OPTION, WFileChooser.CANCEL_OPTION, WFileChooser.ERROR_OPTION,
                WFileChooser.OPEN_DIALOG, WFileChooser.SAVE_DIALOG, WFileChooser.CUSTOM_DIALOG,
                WFileChooser.FILES_ONLY, WFileChooser.DIRECTORIES_ONLY, WFileChooser.FILES_AND_DIRECTORIES,
            ) shouldContainExactly listOf(0, 1, -1, 0, 1, 2, 0, 1, 2)
        }

        test("putting out-of-range values into dialogType and fileSelectionMode throws IllegalArgumentException, and the values do not change") {
            val chooser = WFileChooser()
            shouldThrow<IllegalArgumentException> { chooser.dialogType = 3 }
            shouldThrow<IllegalArgumentException> { chooser.dialogType = -1 }
            shouldThrow<IllegalArgumentException> { chooser.fileSelectionMode = 3 }
            chooser.dialogType shouldBe WFileChooser.OPEN_DIALOG
            chooser.fileSelectionMode shouldBe WFileChooser.FILES_ONLY
            chooser.fileSelectionMode = WFileChooser.FILES_AND_DIRECTORIES
            chooser.isFileSelectionEnabled shouldBe true
            chooser.isDirectorySelectionEnabled shouldBe true
        }

        test("passing a file or a nonexistent path to currentDirectory goes up to an existing folder") {
            WFileChooser(tempDir).currentDirectory shouldBe tempDir
            WFileChooser(tempDir.path).currentDirectory shouldBe tempDir
            val chooser = WFileChooser()
            chooser.currentDirectory = existingFile
            chooser.currentDirectory shouldBe tempDir
            chooser.currentDirectory = File(tempDir, "no/such/dir")
            chooser.currentDirectory shouldBe tempDir
            chooser.currentDirectory = null
            chooser.currentDirectory.shouldBeNull()
        }

        test("setting an absolute-path selectedFile makes the folder containing it currentDirectory (a relative path does not change it)") {
            val chooser = WFileChooser()
            chooser.selectedFile = File("relative.txt")
            chooser.currentDirectory.shouldBeNull()
            chooser.selectedFile = File(tempDir, "new.txt")
            chooser.selectedFile shouldBe File(tempDir, "new.txt")
            chooser.currentDirectory shouldBe tempDir
            chooser.selectedFile = null
            chooser.selectedFile.shouldBeNull()
            chooser.currentDirectory shouldBe tempDir
        }

        test("setting selectedFiles makes the first one selectedFile, and null if empty. The array read is a copy") {
            val chooser = WFileChooser()
            val a = File(tempDir, "a.txt")
            val b = File(tempDir, "b.txt")
            chooser.selectedFiles = arrayOf(a, b)
            chooser.selectedFile shouldBe a
            chooser.selectedFiles[0] = b
            chooser.selectedFiles.toList() shouldContainExactly listOf(a, b)
            chooser.selectedFiles = emptyArray()
            chooser.selectedFile.shouldBeNull()
        }

        test("addChoosableFileFilter adds at the end and makes it fileFilter, and does not add the same one twice") {
            val chooser = WFileChooser()
            val text = WFileNameExtensionFilter("Text", "txt")
            val images = WFileNameExtensionFilter("Images", "png", "jpg")
            chooser.addChoosableFileFilter(text)
            chooser.addChoosableFileFilter(images)
            chooser.addChoosableFileFilter(text)
            chooser.choosableFileFilters.toList() shouldContainExactly listOf(chooser.acceptAllFileFilter, text, images)
            chooser.fileFilter shouldBeSameInstanceAs text
        }

        test("removing the selected file type makes All Files fileFilter if used, otherwise the first remaining one") {
            val chooser = WFileChooser()
            val text = WFileNameExtensionFilter("Text", "txt")
            val images = WFileNameExtensionFilter("Images", "png")
            chooser.addChoosableFileFilter(text)
            chooser.addChoosableFileFilter(images)
            chooser.removeChoosableFileFilter(images) shouldBe true
            chooser.fileFilter shouldBeSameInstanceAs chooser.acceptAllFileFilter
            chooser.removeChoosableFileFilter(images) shouldBe false

            chooser.isAcceptAllFileFilterUsed = false
            chooser.choosableFileFilters.toList() shouldContainExactly listOf(text)
            chooser.fileFilter shouldBeSameInstanceAs text
            chooser.removeChoosableFileFilter(text) shouldBe true
            chooser.fileFilter.shouldBeNull()
        }

        test("setting isAcceptAllFileFilterUsed back to true puts All Files last and makes it fileFilter. reset restores it") {
            val chooser = WFileChooser()
            val text = WFileNameExtensionFilter("Text", "txt")
            chooser.addChoosableFileFilter(text)
            chooser.isAcceptAllFileFilterUsed = false
            chooser.isAcceptAllFileFilterUsed = true
            chooser.choosableFileFilters.toList() shouldContainExactly listOf(text, chooser.acceptAllFileFilter)
            chooser.fileFilter shouldBeSameInstanceAs chooser.acceptAllFileFilter
            chooser.resetChoosableFileFilters()
            chooser.choosableFileFilters.toList() shouldContainExactly listOf(chooser.acceptAllFileFilter)
            chooser.fileFilter shouldBeSameInstanceAs chooser.acceptAllFileFilter
        }

        test("the description of All Files is Japanese or English depending on the locale") {
            WFileChooser().acceptAllFileFilter.description shouldBe "\u3059\u3079\u3066\u306e\u30d5\u30a1\u30a4\u30eb"
            Locale.setDefault(Locale.US)
            WFileChooser().acceptAllFileFilter.description shouldBe "All Files"
        }

        test("WFileNameExtensionFilter compares the part after the last dot case-insensitively, and always includes folders") {
            val filter = WFileNameExtensionFilter("Text", "txt", "MD")
            filter.accept(File(tempDir, "a.TXT")) shouldBe true
            filter.accept(File(tempDir, "b.md")) shouldBe true
            filter.accept(File(tempDir, "c.tar.txt")) shouldBe true
            filter.accept(File(tempDir, "d.csv")) shouldBe false
            filter.accept(File(tempDir, ".txt")) shouldBe false // A dot only at the start means no extension
            filter.accept(File(tempDir, "e.")) shouldBe false
            filter.accept(File(tempDir, "txt")) shouldBe false
            filter.accept(tempDir) shouldBe true
            filter.description shouldBe "Text"
            filter.extensions.toList() shouldContainExactly listOf("txt", "MD")
        }

        test("WFileNameExtensionFilter throws IllegalArgumentException without extensions or with an empty extension") {
            shouldThrow<IllegalArgumentException> { WFileNameExtensionFilter("None") }
            shouldThrow<IllegalArgumentException> { WFileNameExtensionFilter("Empty", "txt", "") }
        }

        test("open dialog: the title, approve button, opening location, file types with descriptions and the selected file type are mapped to the picker") {
            val (picker, chooser) = onUiThreadGet {
                val chooser = WFileChooser(tempDir)
                chooser.dialogTitle = "Open a document"
                chooser.approveButtonText = "Load"
                chooser.addChoosableFileFilter(WFileNameExtensionFilter("Text", "txt", "md"))
                chooser.addChoosableFileFilter(WFileNameExtensionFilter("Images", "png", "txt"))
                chooser.fileFilter = chooser.choosableFileFilters[1]
                chooser.createPicker(windowId) to chooser
            }
            picker.shouldBeInstanceOf<WFileOpenPicker>()
            onUiThreadGet {
                listOf(picker.title, picker.commitButtonText, picker.suggestedFolder)
            } shouldContainExactly listOf("Open a document", "Load", tempDir.path)
            onUiThreadGet { picker.fileTypeChoices } shouldContainExactly listOf(
                chooser.acceptAllFileFilter.description!! to listOf("*"),
                "Text" to listOf(".txt", ".md"),
                "Images" to listOf(".png", ".txt"),
            )
            onUiThreadGet { picker.initialFileTypeIndex } shouldBe 1
            // The extension-only list for older runtimes (no descriptions; All Files is last)
            onUiThreadGet { picker.fileTypeFilter } shouldContainExactly listOf(".txt", ".md", ".png", "*")
        }

        test("open dialog: if nothing is set, the title, approve button and opening location are empty (OS default), and the only file type is All Files") {
            val picker = onUiThreadGet { WFileChooser().createPicker(windowId) as WFileOpenPicker }
            onUiThreadGet { listOf(picker.title, picker.commitButtonText, picker.suggestedFolder) } shouldContainExactly listOf("", "", "")
            onUiThreadGet { picker.fileTypeFilter } shouldContainExactly listOf("*")
            onUiThreadGet { picker.fileTypeChoices } shouldContainExactly listOf("\u3059\u3079\u3066\u306e\u30d5\u30a1\u30a4\u30eb" to listOf("*"))
            onUiThreadGet { picker.initialFileTypeIndex } shouldBe 0
        }

        test("open dialog: without All Files and without extension file types, the extension list is only * and there are no file types with descriptions") {
            val picker = onUiThreadGet {
                val chooser = WFileChooser()
                chooser.isAcceptAllFileFilterUsed = false
                chooser.addChoosableFileFilter(
                    object : WFileFilter() {
                        override fun accept(f: File) = f.name.startsWith("a")
                        override val description = "Starts with a"
                    },
                )
                chooser.createPicker(windowId) as WFileOpenPicker
            }
            onUiThreadGet { picker.fileTypeFilter } shouldContainExactly listOf("*")
            onUiThreadGet { picker.fileTypeChoices }.shouldBeEmpty()
            onUiThreadGet { picker.initialFileTypeIndex } shouldBe -1
        }

        test("extensions containing a dot are not shown in the dialog, and a file type with a null description uses its list of extensions as the description") {
            val picker = onUiThreadGet {
                val chooser = WFileChooser()
                chooser.isAcceptAllFileFilterUsed = false
                chooser.addChoosableFileFilter(WFileNameExtensionFilter(null, "csv", "tar.gz", "tsv"))
                chooser.addChoosableFileFilter(WFileNameExtensionFilter("Archive", "tar.gz"))
                chooser.createPicker(windowId) as WFileOpenPicker
            }
            onUiThreadGet { picker.fileTypeChoices } shouldContainExactly listOf("*.csv;*.tsv" to listOf(".csv", ".tsv"))
            onUiThreadGet { picker.fileTypeFilter } shouldContainExactly listOf(".csv", ".tsv")
            onUiThreadGet { picker.initialFileTypeIndex } shouldBe -1 // The selected "Archive" does not appear in the dialog
        }

        test("save dialog: the selected file becomes the file name and opening location, and the file types, title and approve button are mapped") {
            val picker = onUiThreadGet {
                val chooser = WFileChooser()
                chooser.dialogType = WFileChooser.SAVE_DIALOG
                chooser.selectedFile = File(tempDir, "report.md")
                chooser.dialogTitle = "Save the report"
                chooser.approveButtonText = "Export"
                chooser.addChoosableFileFilter(WFileNameExtensionFilter("Markdown", "md"))
                chooser.createPicker(windowId)
            }
            picker.shouldBeInstanceOf<WFileSavePicker>()
            onUiThreadGet {
                listOf(picker.suggestedFileName, picker.suggestedFolder, picker.title, picker.commitButtonText)
            } shouldContainExactly listOf("report.md", tempDir.path, "Save the report", "Export")
            onUiThreadGet { picker.fileTypeChoices } shouldContainExactly listOf(
                "\u3059\u3079\u3066\u306e\u30d5\u30a1\u30a4\u30eb" to listOf("*"),
                "Markdown" to listOf(".md"),
            )
            onUiThreadGet { picker.initialFileTypeIndex } shouldBe 1
        }

        test("DIRECTORIES_ONLY becomes folder selection regardless of the dialog type, and the title, approve button and opening location are mapped") {
            val picker = onUiThreadGet {
                val chooser = WFileChooser(tempDir)
                chooser.dialogType = WFileChooser.SAVE_DIALOG
                chooser.fileSelectionMode = WFileChooser.DIRECTORIES_ONLY
                chooser.dialogTitle = "Output folder"
                chooser.approveButtonText = "Choose here"
                chooser.createPicker(windowId)
            }
            picker.shouldBeInstanceOf<WFolderPicker>()
            onUiThreadGet {
                listOf(picker.title, picker.commitButtonText, picker.suggestedFolder)
            } shouldContainExactly listOf("Output folder", "Choose here", tempDir.path)
        }

        test("FILES_AND_DIRECTORIES becomes a dialog that opens files") {
            onUiThreadGet {
                val chooser = WFileChooser()
                chooser.fileSelectionMode = WFileChooser.FILES_AND_DIRECTORIES
                chooser.createPicker(windowId)
            }.shouldBeInstanceOf<WFileOpenPicker>()
        }

        test("showDialog changes the approve button text and makes it CUSTOM_DIALOG, and does not change it if null") {
            val chooser = WFileChooser()
            chooser.approveButtonText = "Previous text"
            shouldThrow<IllegalArgumentException> { onUiThreadGet { chooser.showDialog(null, null) } }
            chooser.dialogType shouldBe WFileChooser.OPEN_DIALOG
            chooser.approveButtonText shouldBe "Previous text"
            shouldThrow<IllegalArgumentException> { onUiThreadGet { chooser.showDialog(null, "Import") } }
            chooser.dialogType shouldBe WFileChooser.CUSTOM_DIALOG
            chooser.approveButtonText shouldBe "Import"
        }

        test("a null parent or one not in a window throws IllegalArgumentException (the dialog does not open)") {
            val chooser = WFileChooser()
            shouldThrow<IllegalArgumentException> { onUiThreadGet { chooser.showOpenDialog(null) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { chooser.showSaveDialog(WButton("Not loaded")) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { chooser.showOpenDialog(WButton("Not loaded")) { } } }
            chooser.dialogType shouldBe WFileChooser.OPEN_DIALOG
        }

        test("calling a blocking form from outside the UI thread throws IllegalStateException") {
            val button = onUiThreadGet { WButton("Owner") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                shouldThrow<IllegalStateException> { WFileChooser().showOpenDialog(button) }
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("closing the opened OS dialog makes showOpenDialog return CANCEL_OPTION, and the title becomes the dialog's window title") {
            val button = onUiThreadGet { WButton("Open") }
            UiTestHarness.attachAndAwaitLoaded(button)
            val title = "WFileChooserTest dialog"
            var result: Int? = null
            val previous = File(tempDir, "previous.txt")
            val chooser = WFileChooser(tempDir)
            chooser.dialogTitle = title
            chooser.selectedFile = previous
            try {
                WinUiUtilities.invokeLater { result = chooser.showOpenDialog(button) }
                val dialog = eventually(10.seconds) { TopLevelWindows.find(TopLevelWindows.DIALOG_CLASS, title).shouldNotBeNull() }
                TopLevelWindows.close(dialog)
                eventually(5.seconds) { result shouldBe WFileChooser.CANCEL_OPTION }
                chooser.selectedFile shouldBe previous // Canceling keeps the previous value
            } finally {
                TopLevelWindows.find(TopLevelWindows.DIALOG_CLASS, title)?.let { TopLevelWindows.close(it) }
                UiTestHarness.detach(button)
            }
        }
    }
}
