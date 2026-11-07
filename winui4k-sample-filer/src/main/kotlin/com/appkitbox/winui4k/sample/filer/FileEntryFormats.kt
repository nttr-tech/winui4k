package com.appkitbox.winui4k.sample.filer

import java.io.File
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/** The OS locale used to format dates and numbers. */
private val formatLocale: Locale = Locale.getDefault(Locale.Category.FORMAT)

/**
 * The format of the date modified column: the short date and time of [locale]. The year has 4 digits, as in Explorer, and the hour has 2 digits in the 24-hour clock
 * (e.g. Japanese "2025/04/02 09:27", English (United States) "4/2/2025 3:27 PM").
 */
internal fun dateTimeFormat(locale: Locale): DateFormat {
    val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
    if (format is SimpleDateFormat) {
        format.applyPattern(
            format.toPattern().replace(Regex("y+"), "yyyy").replace(Regex("(?<!H)H(?!H)"), "HH"),
        )
    }
    return format
}

/** The display format of the date modified column (used only on the UI thread). */
private val dateFormat = dateTimeFormat(formatLocale)

/** The display string for the "Modified" column. */
internal fun formatDate(file: File): String = dateFormat.format(file.lastModified())

/**
 * The sort order of the date modified column. Locale formats are not chronological in string order (English month/day/year etc.), so
 * times read back with [format] are compared. Unreadable strings come first.
 */
internal fun dateComparator(format: DateFormat): Comparator<String> {
    fun parse(text: String): Long = try {
        format.parse(text).time
    } catch (_: ParseException) {
        Long.MIN_VALUE
    }
    return Comparator { a, b -> parse(a).compareTo(parse(b)) }
}

/** The sort order of the date modified column. */
internal val DATE_COMPARATOR: Comparator<String> = dateComparator(dateFormat)

/** The display string for the "Size" column. Rounds up to KB just like Explorer. Empty for folders. */
internal fun formatSize(file: File): String {
    if (file.isDirectory) return ""
    val kiloBytes = (file.length() + 1023) / 1024
    return "${NumberFormat.getIntegerInstance(formatLocale).format(kiloBytes)} KB"
}

/** The display string for the "Type" column. A simplified stand-in for Explorer's "Type" column. */
internal fun formatKind(file: File): String {
    if (file.isDirectory) return Messages.get("kind.folder")
    val extension = file.extension
    return if (extension.isEmpty()) {
        Messages.get("kind.file")
    } else {
        Messages.get("kind.file.extension", extension.uppercase(Locale.ROOT))
    }
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

/** The approximate size display for the status bar (one decimal place plus a unit, such as "12.4 MB"; the number of bytes below 1 KB). */
internal fun formatSizeApprox(bytes: Long): String {
    val units = listOf("", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        Messages.get("size.bytes", bytes)
    } else {
        String.format(formatLocale, "%.1f %s", value, units[unitIndex])
    }
}

/** The default ordering for a directory listing: folders first, then ascending by name (matching Explorer). */
internal fun sortEntries(entries: List<File>): List<File> =
    entries.sortedWith(
        compareByDescending<File> { it.isDirectory }
            .thenComparator { a, b -> NAME_COMPARATOR.compare(a.name, b.name) },
    )
