package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.ModalLoop
import java.io.File
import java.util.Locale
import java.util.function.IntConsumer

/**
 * JFileChooser-like: opens the OS file dialogs (the Windows App SDK [WFileOpenPicker], [WFileSavePicker] and [WFolderPicker]) with
 * the same names and constants as Swing. It is an object holding settings, and creates a picker from the settings each time it opens.
 *
 * - [showOpenDialog], [showSaveDialog] and [showDialog] block the caller until closed, as in Swing, and return [APPROVE_OPTION],
 *   [CANCEL_OPTION] or [ERROR_OPTION] (a nested message loop on the UI thread; input, drawing and invokeLater keep working meanwhile).
 *   Call them on the UI thread, outside layout notifications (listeners such as SizeChanged and Loaded); otherwise [IllegalStateException].
 * - The forms taking an [IntConsumer] as the last argument open without blocking and pass the same value when closed.
 * - Read the selected file with [selectedFile] ([selectedFiles] for multiple selection). Canceling keeps the previous value.
 * - As in Swing, [addActionListener] listeners receive the actionCommand [APPROVE_SELECTION] on approval and [CANCEL_SELECTION] on cancellation
 *   (in the order added, before the result is returned or the callback is called). They are not called when the dialog could not open or failed ([ERROR_OPTION]).
 *
 * Which picker opens: if [fileSelectionMode] is [DIRECTORIES_ONLY], folder selection; otherwise, if [dialogType] is
 * [SAVE_DIALOG], save; otherwise ([OPEN_DIALOG], [CUSTOM_DIALOG]), open.
 *
 * Differences from Swing (constraints of the OS dialogs):
 * - If the parent is null, it opens on the active [WFrame] (if none, the shown [WFrame] that became active last or
 *   was created last), the same as [WOptionPane]. Unlike Swing, no invisible shared frame is created, so if no window is shown at all
 *   it throws [IllegalStateException]. A parent that is not in a window throws [IllegalArgumentException]. Besides a [WComponent], a [WFrame]
 *   can also be passed as the parent ([WDialogParent]; it can be written the same way as with Swing's JFrame, which is a Component).
 * - The OS dialogs have no [FILES_AND_DIRECTORIES], so it selects files, the same as [FILES_ONLY].
 * - Only [WFileNameExtensionFilter] and [acceptAllFileFilter] appear in "file type" (other [WFileFilter]s do not).
 *   If two file types have the same description, only the later one remains.
 * - If [currentDirectory] is null, the location to open is left to the OS (which remembers the last location). [getCurrentDirectory] may return null.
 * - [dialogTitle], [currentDirectory] when opening, "file types" with descriptions, and multiple folder selection are features from Windows App SDK 2.3.
 *   On older runtimes, the title and opening location have no effect, file types become a list of extensions without descriptions, and multiple folder selection results in [ERROR_OPTION].
 * - Reworking the dialog, such as accessories, FileView, FileSystemView and setControlButtonsAreShown, is not possible.
 */
class WFileChooser @JvmOverloads constructor(currentDirectory: File? = null) {
    /** Makes [currentDirectoryPath] the initial location (JFileChooser(String)-like). null leaves it to the OS. */
    constructor(currentDirectoryPath: String?) : this(currentDirectoryPath?.let(::File))

    /** The dialog type ([OPEN_DIALOG], [SAVE_DIALOG], [CUSTOM_DIALOG]). The default is [OPEN_DIALOG]. Other values throw [IllegalArgumentException]. */
    var dialogType: Int = OPEN_DIALOG
        set(value) {
            require(value == OPEN_DIALOG || value == SAVE_DIALOG || value == CUSTOM_DIALOG) { "Incorrect Dialog Type: $value" }
            field = value
        }

    /** The dialog title (Title). null means the OS default ("Open" etc.). */
    var dialogTitle: String? = null

    /** The text of the approve button (CommitButtonText). null means the OS default ("Open", "Save", etc.). */
    var approveButtonText: String? = null

    /**
     * What to select ([FILES_ONLY], [DIRECTORIES_ONLY], [FILES_AND_DIRECTORIES]). The default is [FILES_ONLY]. Other values throw [IllegalArgumentException].
     * [FILES_AND_DIRECTORIES] becomes a dialog that selects files.
     */
    var fileSelectionMode: Int = FILES_ONLY
        set(value) {
            require(value == FILES_ONLY || value == DIRECTORIES_ONLY || value == FILES_AND_DIRECTORIES) {
                "Incorrect Mode for file selection: $value"
            }
            field = value
        }

    /** Whether files can be selected ([FILES_ONLY] or [FILES_AND_DIRECTORIES]). */
    val isFileSelectionEnabled: Boolean get() = fileSelectionMode != DIRECTORIES_ONLY

    /** Whether folders can be selected ([DIRECTORIES_ONLY] or [FILES_AND_DIRECTORIES]). */
    val isDirectorySelectionEnabled: Boolean get() = fileSelectionMode != FILES_ONLY

    /** Whether multiple items can be selected. The default is false. */
    var isMultiSelectionEnabled: Boolean = false

    /**
     * The location to open the dialog at. Passing a file makes it the folder containing it (if none, the nearest existing ancestor folder).
     * null leaves it to the OS. Selecting in the dialog makes it the folder containing the selection.
     */
    var currentDirectory: File? = null
        set(value) {
            field = value?.let(::existingDirectory)
        }

    /**
     * The selected file. Setting it puts it in the file name field of the save dialog, and the folder containing that file becomes [currentDirectory].
     * Set when selected in the dialog, and keeps the previous value when canceled.
     */
    var selectedFile: File? = null
        set(value) {
            field = value
            val parent = value?.takeIf { it.isAbsolute }?.parentFile
            if (parent != null && parent != currentDirectory) currentDirectory = parent
        }

    /** The files selected in a multiple selection ([isMultiSelectionEnabled]) dialog (a copy). Setting it makes the first one [selectedFile]. */
    var selectedFiles: Array<File>
        get() = selectedFileList.toTypedArray()
        set(value) {
            selectedFileList = value.toList()
            selectedFile = value.firstOrNull()
        }

    private var selectedFileList: List<File> = emptyList()

    /** The file type representing all files (the description is the Japanese for "All Files" if [Locale.getDefault] is Japanese, otherwise "All Files"). */
    val acceptAllFileFilter: WFileFilter = AcceptAllFileFilter

    private val filters = ArrayList<WFileFilter>().apply { add(AcceptAllFileFilter) }

    /** The file types selectable in the dialog (in the order added; a copy). */
    val choosableFileFilters: Array<WFileFilter> get() = filters.toTypedArray()

    /** The file type selected when the dialog opens. The default is [acceptAllFileFilter]. */
    var fileFilter: WFileFilter? = AcceptAllFileFilter

    /** Whether to include "All Files" in the selectable file types. The default is true. false removes it from [choosableFileFilters], and true puts it last and makes it [fileFilter]. */
    var isAcceptAllFileFilterUsed: Boolean = true
        set(value) {
            field = value
            removeChoosableFileFilter(AcceptAllFileFilter)
            if (value) addChoosableFileFilter(AcceptAllFileFilter) // Move it to the end and make it the selected file type, as in Swing
        }

    /** A reference that keeps the picker from being released while the dialog is open. */
    private var openPicker: Any? = null

    /** The listeners added with [addActionListener] (called in the order added). */
    private val actionListenerList = ArrayList<WActionListener>()

    /** JFileChooser.getActionListeners-like: the attached listeners (in the order added, a copy). */
    val actionListeners: Array<WActionListener> get() = actionListenerList.toTypedArray()

    init {
        this.currentDirectory = currentDirectory
    }

    /** Adds [filter] at the end of the selectable file types and makes it [fileFilter] (if already present, only makes it [fileFilter]). */
    fun addChoosableFileFilter(filter: WFileFilter) {
        if (filter !in filters) filters += filter
        fileFilter = filter
    }

    /**
     * Removes [filter] from the selectable file types. If the removed one is [fileFilter], "All Files" becomes [fileFilter] if used, otherwise the first remaining one
     * (null if none). true if removed.
     */
    fun removeChoosableFileFilter(filter: WFileFilter): Boolean {
        if (!filters.remove(filter)) return false
        if (fileFilter === filter) {
            fileFilter = if (isAcceptAllFileFilterUsed && filter !== AcceptAllFileFilter) AcceptAllFileFilter else filters.firstOrNull()
        }
        return true
    }

    /**
     * JFileChooser.accept-like: whether [f] is included in the current type ([fileFilter]). true if [fileFilter] is null.
     * It does not affect the list in the OS dialog (see the class description).
     */
    fun accept(f: File): Boolean = fileFilter?.accept(f) ?: true

    /**
     * JFileChooser.changeToParentDirectory-like: sets [selectedFile] to null and [currentDirectory] to its parent folder.
     * If [currentDirectory] is null (left to the OS) or is the top of a drive without a parent, [currentDirectory] does not change.
     */
    fun changeToParentDirectory() {
        selectedFile = null
        currentDirectory?.parentFile?.let { currentDirectory = it }
    }

    /** JFileChooser.addActionListener-like: adds a listener called on approval ([APPROVE_SELECTION]) and cancellation ([CANCEL_SELECTION]). */
    fun addActionListener(listener: WActionListener) {
        actionListenerList += listener
    }

    /** Removes one listener added with [addActionListener]. Does nothing if it is not attached. */
    fun removeActionListener(listener: WActionListener) {
        val index = actionListenerList.indexOfLast { it === listener }
        if (index >= 0) actionListenerList.removeAt(index)
    }

    /** Resets the selectable file types to only "All Files" (when [isAcceptAllFileFilterUsed] is true). */
    fun resetChoosableFileFilters() {
        filters.clear()
        fileFilter = null
        if (isAcceptAllFileFilterUsed) addChoosableFileFilter(AcceptAllFileFilter)
    }

    // ---- Opening ----

    /** Opens the "Open" dialog (sets [dialogType] to [OPEN_DIALOG]), waits until it is closed, and returns the result. */
    fun showOpenDialog(parent: WDialogParent?): Int {
        dialogType = OPEN_DIALOG
        return showAndWait(parent)
    }

    /** [showOpenDialog] that opens without blocking. Passes the result to [onResult] when closed. */
    fun showOpenDialog(parent: WDialogParent?, onResult: IntConsumer) {
        dialogType = OPEN_DIALOG
        start(parent, onResult)
    }

    /** Opens the "Save" dialog (sets [dialogType] to [SAVE_DIALOG]), waits until it is closed, and returns the result. */
    fun showSaveDialog(parent: WDialogParent?): Int {
        dialogType = SAVE_DIALOG
        return showAndWait(parent)
    }

    /** [showSaveDialog] that opens without blocking. Passes the result to [onResult] when closed. */
    fun showSaveDialog(parent: WDialogParent?, onResult: IntConsumer) {
        dialogType = SAVE_DIALOG
        start(parent, onResult)
    }

    /**
     * Opens a dialog with the approve button set to [approveButtonText], waits until it is closed, and returns the result. If [approveButtonText] is not null,
     * sets [dialogType] to [CUSTOM_DIALOG] (it becomes an open dialog). If null, keeps the current [dialogType] and [approveButtonText].
     */
    fun showDialog(parent: WDialogParent?, approveButtonText: String?): Int {
        customize(approveButtonText)
        return showAndWait(parent)
    }

    /** [showDialog] that opens without blocking. Passes the result to [onResult] when closed. */
    fun showDialog(parent: WDialogParent?, approveButtonText: String?, onResult: IntConsumer) {
        customize(approveButtonText)
        start(parent, onResult)
    }

    private fun customize(approveButtonText: String?) {
        if (approveButtonText != null) {
            this.approveButtonText = approveButtonText
            dialogType = CUSTOM_DIALOG
        }
    }

    private fun showAndWait(parent: WDialogParent?): Int {
        ModalLoop.checkRunnable()
        var result: Int? = null
        start(parent) { result = it }
        ModalLoop.run { result != null } // If false (the app is exiting), there is no result yet, so treat it as a cancel
        return result ?: CANCEL_OPTION
    }

    private fun start(parent: WDialogParent?, onResult: IntConsumer) {
        // A WFrame makes its window the owner directly (it can open even if the content is not placed yet)
        val windowId = (parent as? WFrame)?.appWindow?.id ?: StoragePickers.windowIdOf(WFrame.dialogOwnerOf(parent))
        val onError: (RuntimeException) -> Unit = { finish(null, onResult) }
        val onPicked: (List<String>) -> Unit = { paths -> finish(paths, onResult) }
        when (val picker = createPicker(windowId)) {
            is WFolderPicker -> if (isMultiSelectionEnabled) {
                picker.pickMultipleFolders(onError, onPicked)
            } else {
                picker.pickSingleFolder(onError) { onPicked(listOfNotNull(it)) }
            }
            is WFileSavePicker -> picker.pickSaveFile(onError) { onPicked(listOfNotNull(it)) }
            is WFileOpenPicker -> if (isMultiSelectionEnabled) {
                picker.pickMultipleFiles(onError, onPicked)
            } else {
                picker.pickSingleFile(onError) { onPicked(listOfNotNull(it)) }
            }
            else -> error("unknown picker: $picker")
        }
    }

    /** The dialog closed (failure if [paths] is null). If something was selected, updates [selectedFile] etc. and passes the result. */
    private fun finish(paths: List<String>?, onResult: IntConsumer) {
        openPicker = null
        val result = when {
            paths == null -> ERROR_OPTION
            paths.isEmpty() -> CANCEL_OPTION
            else -> {
                val files = paths.map(::File)
                if (isMultiSelectionEnabled) selectedFiles = files.toTypedArray() else selectedFile = files[0]
                APPROVE_OPTION
            }
        }
        val command = when (result) {
            APPROVE_OPTION -> APPROVE_SELECTION
            CANCEL_OPTION -> CANCEL_SELECTION
            else -> null
        }
        if (command != null) {
            val event = WActionEvent(this, command)
            for (listener in actionListenerList.toList()) listener.actionPerformed(event)
        }
        onResult.accept(result)
    }

    /** Creates a picker that opens on the window of [windowId] from the current settings (does not open it). */
    internal fun createPicker(windowId: Long): Any {
        val picker: Any = when {
            fileSelectionMode == DIRECTORIES_ONLY -> folderPicker(windowId)
            dialogType == SAVE_DIALOG -> savePicker(windowId)
            else -> openPicker(windowId)
        }
        openPicker = picker
        return picker
    }

    private fun folderPicker(windowId: Long) = WFolderPicker(windowId).also { picker ->
        approveButtonText?.let { picker.commitButtonText = it }
        ifSupported { dialogTitle?.let { picker.title = it } }
        ifSupported { currentDirectory?.let { picker.suggestedFolder = it.path } }
    }

    private fun savePicker(windowId: Long) = WFileSavePicker(windowId).also { picker ->
        approveButtonText?.let { picker.commitButtonText = it }
        ifSupported { dialogTitle?.let { picker.title = it } }
        selectedFile?.let { picker.suggestedFileName = it.name }
        currentDirectory?.let { picker.suggestedFolder = it.path }
        val choices = dialogChoices()
        for ((name, extensions) in choices) picker.addFileTypeChoice(name, *extensions.toTypedArray())
        initialChoiceIndex(choices)?.let { index -> ifSupported { picker.initialFileTypeIndex = index } }
    }

    private fun openPicker(windowId: Long) = WFileOpenPicker(windowId).also { picker ->
        approveButtonText?.let { picker.commitButtonText = it }
        ifSupported { dialogTitle?.let { picker.title = it } }
        ifSupported { currentDirectory?.let { picker.suggestedFolder = it.path } }
        val choices = dialogChoices()
        // FileTypeChoices with descriptions take precedence. On older runtimes it becomes a FileTypeFilter of extensions only
        val union = choices.flatMap { it.second }.distinct().let { if (ALL_FILES in it) it - ALL_FILES + ALL_FILES else it }
        picker.addFileTypeFilter(*union.ifEmpty { listOf(ALL_FILES) }.toTypedArray())
        ifSupported {
            for ((name, extensions) in choices) picker.addFileTypeChoice(name, *extensions.toTypedArray())
            initialChoiceIndex(choices)?.let { picker.initialFileTypeIndex = it }
        }
    }

    /** The dialog's "file types" (descriptions and extensions in the ".txt" form, in the order of the selectable file types; those that cannot be shown in the dialog are excluded). */
    private fun dialogChoices(): List<Pair<String, List<String>>> = dialogFilters().map { it.first }

    private fun dialogFilters(): List<Pair<Pair<String, List<String>>, WFileFilter>> {
        val list = if (fileFilter != null && fileFilter !in filters) listOf(fileFilter!!) + filters else filters
        return list.mapNotNull { filter ->
            when {
                filter === AcceptAllFileFilter -> (AcceptAllFileFilter.description to listOf(ALL_FILES)) to filter
                filter is WFileNameExtensionFilter -> filter.dialogExtensions.takeIf { it.isNotEmpty() }?.let { extensions ->
                    ((filter.description ?: extensions.joinToString(";") { "*$it" }) to extensions) to filter
                }
                else -> null
            }
        }
    }

    private fun initialChoiceIndex(choices: List<Pair<String, List<String>>>): Int? {
        val selected = fileFilter ?: return null
        val filters = dialogFilters()
        val entry = filters.firstOrNull { it.second === selected } ?: return null
        // File types with the same description are replaced by the later one, so count the position where each description first appears
        return choices.map { it.first }.distinct().indexOf(entry.first.first).takeIf { it >= 0 }
    }

    /** Settings from Windows App SDK 2.3 are silently skipped on older runtimes. */
    private inline fun ifSupported(block: () -> Unit) {
        try {
            block()
        } catch (_: UnsupportedOperationException) {
            // Older runtime: this setting has no effect (see the class description)
        }
    }

    private object AcceptAllFileFilter : WFileFilter() {
        override fun accept(f: File): Boolean = true

        override val description: String
            get() = if (Locale.getDefault().language == Locale.JAPANESE.language) "\u3059\u3079\u3066\u306e\u30d5\u30a1\u30a4\u30eb" else "All Files"
    }

    companion object {
        /** Return value: selected with the approve button. */
        const val APPROVE_OPTION = 0

        /** Return value: canceled (also with the Esc key or by closing the dialog). */
        const val CANCEL_OPTION = 1

        /** Return value: the dialog could not be opened or failed. */
        const val ERROR_OPTION = -1

        /** [dialogType]: open. */
        const val OPEN_DIALOG = 0

        /** [dialogType]: save. */
        const val SAVE_DIALOG = 1

        /** [dialogType]: an open dialog with a custom approve button text ([showDialog]). */
        const val CUSTOM_DIALOG = 2

        /** [fileSelectionMode]: files only. */
        const val FILES_ONLY = 0

        /** [fileSelectionMode]: folders only. */
        const val DIRECTORIES_ONLY = 1

        /** [fileSelectionMode]: files and folders (the OS dialogs do not have it, so files are selected, the same as [FILES_ONLY]). */
        const val FILES_AND_DIRECTORIES = 2

        /** The actionCommand of [addActionListener]: chosen with the approve button. */
        const val APPROVE_SELECTION = "ApproveSelection"

        /** The actionCommand of [addActionListener]: canceled. */
        const val CANCEL_SELECTION = "CancelSelection"

        private const val ALL_FILES = "*"

        /** [file] if it is a folder; otherwise goes up to an existing folder (null if none). */
        private fun existingDirectory(file: File): File? {
            var dir: File? = file.absoluteFile
            while (dir != null && !dir.isDirectory) dir = dir.parentFile
            return dir
        }
    }
}
