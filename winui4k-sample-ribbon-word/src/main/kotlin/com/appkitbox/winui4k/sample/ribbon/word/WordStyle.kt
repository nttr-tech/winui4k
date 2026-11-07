package com.appkitbox.winui4k.sample.ribbon.word

/**
 * An item of the [Styles] gallery and how that style looks when applied to the document heading (same values as
 * ApplyStyle in WordPage.xaml.cs).
 */
@Suppress("LongParameterList") // Lays out each enum value's gallery sample and heading look as one table row per line
internal enum class WordStyle(
    val label: String,
    val category: String,
    val previewText: String,
    val previewFontSize: Double,
    val previewForeground: String?,
    /** Font size when applied to the heading. */
    val headingSize: Double,
    /** Text color when applied to the heading (#RRGGBB). */
    val headingColor: String,
) {
    NORMAL(Messages.get("style.normal"), Messages.get("common.paragraph"), Messages.get("style.preview"), 14.0, null, 30.0, "#1F3864"),
    NO_SPACING(Messages.get("style.no.spacing"), Messages.get("common.paragraph"), Messages.get("style.preview"), 14.0, null, 30.0, "#1F3864"),
    HEADING1(Messages.get("style.heading1"), Messages.get("style.category.heading"), Messages.get("style.preview.heading1"), 17.0, "#2F5496", 30.0, "#2F5496"),
    HEADING2(Messages.get("style.heading2"), Messages.get("style.category.heading"), Messages.get("style.preview.heading2"), 15.0, "#2F5496", 24.0, "#2F5496"),
    TITLE(Messages.get("style.title"), Messages.get("style.category.heading"), Messages.get("style.preview.title"), 22.0, null, 40.0, "#000000"),
    SUBTITLE(Messages.get("style.subtitle"), Messages.get("style.category.heading"), Messages.get("style.preview"), 14.0, "#595959", 22.0, "#595959"),
    EMPHASIS(Messages.get("style.emphasis"), Messages.get("style.category.character"), Messages.get("style.preview"), 14.0, null, 30.0, "#1F3864"),
    STRONG(Messages.get("style.strong"), Messages.get("style.category.character"), Messages.get("style.preview"), 14.0, null, 30.0, "#1F3864"),
    QUOTE(Messages.get("style.quote"), Messages.get("common.paragraph"), Messages.get("style.preview"), 14.0, "#404040", 26.0, "#404040"),
    INTENSE_QUOTE(Messages.get("style.intense.quote"), Messages.get("common.paragraph"), Messages.get("style.preview"), 14.0, "#2F5496", 26.0, "#404040"),
    ;

    /** Whether this is an italic style. */
    val isItalic: Boolean get() = this == EMPHASIS || this == QUOTE || this == INTENSE_QUOTE
}
