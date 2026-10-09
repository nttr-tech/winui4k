package com.appkitbox.winui4k.extension.ribbon.model

/**
 * A rich tooltip (Office's "ScreenTip"): a bold title with a shortcut, a description, an image, and a help footer.
 *
 * [extendedDescription] / [extendedImage] are a detailed description shown a little later while the pointer stays on
 * the item (an AutoCAD-style progressive tooltip).
 */
class RibbonScreenTip @JvmOverloads constructor(
    title: String? = null,
    description: String? = null,
    shortcut: String? = null,
) : RibbonObservable() {
    /** The bold title. If null, the label of the item. */
    var title: String? by observable(title)

    /** The description. */
    var description: String? by observable(description)

    /** The shortcut shown next to the title (e.g. "Ctrl+B"). */
    var shortcut: String? by observable(shortcut)

    /** The footer ("Tell me more", "Press F1 for more help", and so on). */
    var helpText: String? by observable(null)

    /** The reason the command is disabled. */
    var disabledReason: String? by observable(null)

    /** The illustration of the description. */
    var image: RibbonIcon? by observable(null)

    /** The detailed description shown while the pointer stays on the item (a progressive tooltip). */
    var extendedDescription: String? by observable(null)

    /** The illustration shown with the detailed description (a progressive tooltip). */
    var extendedImage: RibbonIcon? by observable(null)

    /** Whether it has a detailed description ([extendedDescription] / [extendedImage]). */
    val hasExtendedContent: Boolean get() = !extendedDescription.isNullOrEmpty() || extendedImage != null

    companion object {
        /** The title followed by the shortcut (e.g. "Bold (Ctrl+B)"). */
        @JvmStatic
        fun formatTitle(title: String?, shortcut: String?): String =
            if (shortcut.isNullOrBlank()) title.orEmpty() else "$title ($shortcut)"
    }
}
