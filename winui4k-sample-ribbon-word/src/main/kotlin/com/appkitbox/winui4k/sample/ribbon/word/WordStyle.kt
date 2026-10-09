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
    NORMAL("Normal", "Paragraph", "AaBbCcDd", 14.0, null, 30.0, "#1F3864"),
    NO_SPACING("No Spacing", "Paragraph", "AaBbCcDd", 14.0, null, 30.0, "#1F3864"),
    HEADING1("Heading 1", "Heading", "AaBbCc", 17.0, "#2F5496", 30.0, "#2F5496"),
    HEADING2("Heading 2", "Heading", "AaBbCcE", 15.0, "#2F5496", 24.0, "#2F5496"),
    TITLE("Title", "Heading", "AaB", 22.0, null, 40.0, "#000000"),
    SUBTITLE("Subtitle", "Heading", "AaBbCcD", 14.0, "#595959", 22.0, "#595959"),
    EMPHASIS("Emphasis", "Character", "AaBbCcDd", 14.0, null, 30.0, "#1F3864"),
    STRONG("Strong", "Character", "AaBbCcDd", 14.0, null, 30.0, "#1F3864"),
    QUOTE("Quote", "Paragraph", "AaBbCcDd", 14.0, "#404040", 26.0, "#404040"),
    INTENSE_QUOTE("Intense Quote", "Paragraph", "AaBbCcDd", 14.0, "#2F5496", 26.0, "#404040"),
    ;

    /** Whether this is an italic style. */
    val isItalic: Boolean get() = this == EMPHASIS || this == QUOTE || this == INTENSE_QUOTE
}
