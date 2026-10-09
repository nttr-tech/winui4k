package com.appkitbox.winui4k

/**
 * Returns the rendered width of strings (used to compute item sizes; tests replace it with an implementation that
 * returns fixed values).
 */
internal fun interface RibbonTextWidths {
    /** The width of each of [texts] when drawn at [fontSize] (bold if [semiBold]). */
    fun widths(texts: List<String>, fontSize: Double, semiBold: Boolean): List<Double>
}

/**
 * A [RibbonTextWidths] that actually measures strings with XAML.
 *
 * Places TextBlocks on a hidden Canvas ([host]) in the visual tree, measures them with UpdateLayout, and reads
 * DesiredSize (UIElement.Measure is not used because it passes a float struct by value, which the FFI cannot handle on ARM64).
 * Results are cached per string, size, and weight, and unmeasured strings are measured together in a single layout pass.
 * Widths are 0 while [host] is not yet in the tree, so use it after loading.
 */
internal class RibbonTextMeasurer(private val host: XamlElement) : RibbonTextWidths {
    private val cache = HashMap<String, Double>()

    override fun widths(texts: List<String>, fontSize: Double, semiBold: Boolean): List<Double> {
        val missing = texts.filter { it.isNotEmpty() && key(it, fontSize, semiBold) !in cache }.distinct()
        if (missing.isNotEmpty()) measure(missing, fontSize, semiBold)
        return texts.map { if (it.isEmpty()) 0.0 else cache[key(it, fontSize, semiBold)] ?: 0.0 }
    }

    /** The width of a single string. */
    fun width(text: String, fontSize: Double, semiBold: Boolean = false): Double = widths(listOf(text), fontSize, semiBold)[0]

    /** Discards the cache (when the font changes, for example). */
    fun clear() = cache.clear()

    private fun measure(texts: List<String>, fontSize: Double, semiBold: Boolean) {
        val weight = if (semiBold) " FontWeight=\"SemiBold\"" else ""
        val blocks = texts.map { text ->
            XamlElement.load(
                "<TextBlock Text=\"${Xaml.escape(text)}\" FontSize=\"${Xaml.num(fontSize)}\"$weight " +
                    "TextWrapping=\"NoWrap\" IsTextScaleFactorEnabled=\"False\" TextLineBounds=\"Full\" />",
            )
        }
        blocks.forEach { host.addChild(it) }
        host.updateLayout()
        var measured = true
        for ((index, block) in blocks.withIndex()) {
            val width = block.desiredSize()[0]
            if (width <= 0.0) measured = false
            cache[key(texts[index], fontSize, semiBold)] = width
        }
        blocks.forEach { host.removeChild(it) }
        // Values that could not be measured (such as before being added to the tree) are not kept in the cache
        // (measured again next time)
        if (!measured) texts.forEach { cache.remove(key(it, fontSize, semiBold)) }
    }

    private fun key(text: String, fontSize: Double, semiBold: Boolean) = "$fontSize|$semiBold|$text"
}
