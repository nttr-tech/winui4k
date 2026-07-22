package com.appkitbox.winui4k.sample.filer

import java.io.File
import java.io.IOException

/**
 * File operations (copy / cut / paste / delete / rename / new folder).
 * Groups pure filesystem operations kept separate from the UI.
 *
 * The clipboard is an in-app clipboard (MVP). Interop with the OS clipboard (CF_HDROP)
 * is future work.
 */
internal object FileOperations {
    /** The current in-app clipboard contents. */
    var clipboardFiles: List<File> = emptyList()
        private set

    /** Whether the clipboard contents are a cut (moved on paste) rather than a copy. */
    var isCut: Boolean = false
        private set

    /** Puts [files] on the clipboard as a copy. */
    fun copy(files: List<File>) {
        clipboardFiles = files
        isCut = false
    }

    /** Puts [files] on the clipboard as a cut. */
    fun cut(files: List<File>) {
        clipboardFiles = files
        isCut = true
    }

    /**
     * Pastes the clipboard contents into [directory].
     * If a destination with the same name already exists, appends "- Copy" and keeps both,
     * matching Explorer. Pasting a cut moves the files and clears the clipboard.
     */
    @Throws(IOException::class)
    fun paste(directory: File) {
        val sources = clipboardFiles.filter { it.exists() }
        for (source in sources) {
            val destination = uniqueDestination(File(directory, source.name))
            if (isCut) {
                moveFile(source, destination)
            } else {
                copyFile(source, destination)
            }
        }
        if (isCut) {
            clipboardFiles = emptyList()
            isCut = false
        }
    }

    /**
     * Moves [files] to the Recycle Bin. Returns false on environments where the Recycle Bin isn't
     * available (the caller should confirm and fall back to permanent deletion).
     * Implemented via PowerShell (Microsoft.VisualBasic.FileIO.FileSystem) so AWT never needs to
     * be initialized. Multiple files are batched into a single process.
     */
    fun moveToRecycleBin(files: List<File>): Boolean {
        if (files.isEmpty()) return true
        val commands = files.joinToString(";") { file ->
            val escaped = file.absolutePath.replace("'", "''")
            val method = if (file.isDirectory) "DeleteDirectory" else "DeleteFile"
            "[Microsoft.VisualBasic.FileIO.FileSystem]::$method('$escaped','OnlyErrorDialogs','SendToRecycleBin')"
        }
        return try {
            val process = ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                "Add-Type -AssemblyName Microsoft.VisualBasic; $commands",
            ).redirectErrorStream(true).start()
            process.waitFor() == 0 && files.none { it.exists() }
        } catch (_: IOException) {
            false
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    /** Permanently deletes [files] (bypasses the Recycle Bin). */
    fun deletePermanently(files: List<File>) {
        for (file in files) {
            file.deleteRecursively()
        }
    }

    /** Renames [file] to [newName] within the same folder. Returns the renamed File on success. */
    @Throws(IOException::class)
    fun rename(file: File, newName: String): File {
        val destination = File(file.parentFile, newName)
        if (destination.exists()) throw IOException("A file with that name already exists: $newName")
        if (!file.renameTo(destination)) throw IOException("Couldn't rename: ${file.name}")
        return destination
    }

    /** Creates a folder named [name] under [directory]. Appends a number like "(2)" if that name already exists. */
    @Throws(IOException::class)
    fun createFolder(directory: File, name: String): File {
        val folder = uniqueDestination(File(directory, name))
        if (!folder.mkdirs()) throw IOException("Couldn't create the folder: ${folder.name}")
        return folder
    }

    /** Copies text to the clipboard (used for "Copy path"). */
    fun copyTextToClipboard(text: String) {
        try {
            val escaped = text.replace("'", "''")
            ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "Set-Clipboard -Value '$escaped'")
                .start()
        } catch (_: IOException) {
            // Failing to copy to the clipboard shouldn't fail the whole operation
        }
    }

    /** Opens [file] with its associated app (via cmd's start, so AWT is never initialized). */
    fun openWithShell(file: File) {
        try {
            ProcessBuilder("cmd", "/c", "start", "", file.absolutePath).start()
        } catch (_: IOException) {
            // Failing to open shouldn't fail the whole operation
        }
    }

    private fun copyFile(source: File, destination: File) {
        if (!source.copyRecursively(destination)) {
            throw IOException("Couldn't copy: ${source.name}")
        }
    }

    private fun moveFile(source: File, destination: File) {
        if (source.renameTo(destination)) return
        // If renameTo fails, such as when moving across drives, fall back to copy + delete
        copyFile(source, destination)
        if (!source.deleteRecursively()) {
            throw IOException("Couldn't delete the source after moving: ${source.name}")
        }
    }

    /** If [candidate] already exists, returns a non-conflicting destination in "name - Copy (n)" form. */
    private fun uniqueDestination(candidate: File): File {
        if (!candidate.exists()) return candidate
        val baseName = candidate.nameWithoutExtension
        val extension = if (candidate.extension.isEmpty()) "" else ".${candidate.extension}"
        var index = 1
        while (true) {
            val suffix = if (index == 1) " - Copy" else " - Copy ($index)"
            val next = File(candidate.parentFile, "$baseName$suffix$extension")
            if (!next.exists()) return next
            index++
        }
    }
}
