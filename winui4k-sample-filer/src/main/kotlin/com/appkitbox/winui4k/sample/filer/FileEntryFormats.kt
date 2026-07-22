package com.appkitbox.winui4k.sample.filer

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The display format for the "Modified" column. Chosen so that a plain string sort still comes out chronological. */
private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)

/** The display string for the "Modified" column. */
internal fun formatDate(file: File): String = dateFormat.format(Date(file.lastModified()))

/** The display string for the "Size" column. Rounds up to KB just like Explorer. Empty for folders. */
internal fun formatSize(file: File): String {
    if (file.isDirectory) return ""
    val kiloBytes = (file.length() + 1023) / 1024
    return String.format(Locale.JAPAN, "%,d KB", kiloBytes)
}

/** The display string for the "Type" column. A simplified stand-in for Explorer's "Type" column. */
internal fun formatKind(file: File): String {
    if (file.isDirectory) return "File folder"
    val extension = file.extension
    return if (extension.isEmpty()) "File" else "${extension.uppercase(Locale.ROOT)} File"
}

/** The sort order for the "Name" column (case-insensitive). */
internal val NAME_COMPARATOR: Comparator<String> = String.CASE_INSENSITIVE_ORDER

/** The sort order for the "Size" column. Compares only the numeric part pulled out of "1,234 KB" (a folder's empty string sorts first). */
internal val SIZE_COMPARATOR: Comparator<String> = Comparator { a, b ->
    parseSizeKb(a).compareTo(parseSizeKb(b))
}

private fun parseSizeKb(text: String): Long {
    val digits = text.filter { it.isDigit() }
    return if (digits.isEmpty()) -1L else digits.toLong()
}

/** An approximate size display for the status bar (a one-decimal number + unit, like "12.4 MB"). */
internal fun formatSizeApprox(bytes: Long): String {
    val units = listOf("bytes", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "$bytes ${units[0]}"
    } else {
        String.format(Locale.JAPAN, "%.1f %s", value, units[unitIndex])
    }
}

/** The default ordering for a directory listing: folders first, then ascending by name (matching Explorer). */
internal fun sortEntries(entries: List<File>): List<File> =
    entries.sortedWith(
        compareByDescending<File> { it.isDirectory }
            .thenComparator { a, b -> NAME_COMPARATOR.compare(a.name, b.name) },
    )
