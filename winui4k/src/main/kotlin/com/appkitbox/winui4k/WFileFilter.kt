package com.appkitbox.winui4k

import java.io.File
import java.util.Locale

/**
 * javax.swing.filechooser.FileFilter-like: one "file type" of a [WFileChooser].
 *
 * The OS file dialogs cannot call [accept], so only [WFileNameExtensionFilter] and [WFileChooser.acceptAllFileFilter]
 * become "file types" of the dialog. Other subclasses do not appear in the dialog ([accept] can be used from code).
 */
abstract class WFileFilter {
    /** Whether to include [f] in this file type. */
    abstract fun accept(f: File): Boolean

    /** The description shown in "file type" ("Text files" etc.). */
    abstract val description: String?
}

/**
 * javax.swing.filechooser.FileNameExtensionFilter-like: a [WFileFilter] that selects by extension.
 * Write [extensions] without a dot, like "txt"; they are compared case-insensitively.
 *
 * They are passed to the dialog in the ".txt" form. Extensions containing a dot, like "tar.gz", do not match even in Swing's [accept] (only the part after the last dot is compared)
 * and the OS does not accept them, so they are not shown in the dialog.
 */
class WFileNameExtensionFilter(
    override val description: String?,
    vararg extensions: String,
) : WFileFilter() {
    private val extensionList: List<String>
    private val lowerCaseExtensions: List<String>

    init {
        require(extensions.isNotEmpty()) { "Extensions must be non-null and not empty" }
        require(extensions.all { it.isNotEmpty() }) { "Each extension must be non-null and not empty" }
        extensionList = extensions.toList()
        lowerCaseExtensions = extensionList.map { it.lowercase(Locale.ENGLISH) }
    }

    /** The list of extensions (as written when created; a copy). */
    val extensions: Array<String> get() = extensionList.toTypedArray()

    /** Includes folders and files whose part after the last dot matches one of [extensions] (case-insensitive). */
    override fun accept(f: File): Boolean {
        if (f.isDirectory) return true
        val name = f.name
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot >= name.length - 1) return false
        val extension = name.substring(dot + 1).lowercase(Locale.ENGLISH)
        return extension in lowerCaseExtensions
    }

    override fun toString(): String = "${super.toString()}[description=$description extensions=$extensionList]"

    /** The extensions in the ".txt" form passed to the dialog (excluding forms the OS does not accept). */
    internal val dialogExtensions: List<String>
        get() = extensionList.map { it.removePrefix(".") }
            .filter { it.isNotEmpty() && it.none { c -> c == '.' || c == '*' || c == '?' } }
            .map { ".$it" }
            .distinct()
}
