package com.appkitbox.winui4k.extension.ribbon.model

/**
 * A mapping from the adaptive layout state of a group ([RibbonGroupState]) to the size of an item ([RibbonItemSize]).
 * As a string, it is written as "Large, Medium, Small" (the item's size when the group is in the large, medium, and
 * small state). A single value ("Large") fixes the size.
 */
data class RibbonSizeDefinition(
    /** The size when the group is [RibbonGroupState.LARGE] (and inside the popup when collapsed). */
    val large: RibbonItemSize,
    /** The size when the group is [RibbonGroupState.MEDIUM]. */
    val medium: RibbonItemSize,
    /** The size when the group is [RibbonGroupState.SMALL]. */
    val small: RibbonItemSize,
) {
    /** The size of the item in the given group state (collapsed uses the large size inside the popup). */
    fun getSize(state: RibbonGroupState): RibbonItemSize = when (state) {
        RibbonGroupState.MEDIUM -> medium
        RibbonGroupState.SMALL -> small
        else -> large
    }

    override fun toString(): String = "${display(large)}, ${display(medium)}, ${display(small)}"

    companion object {
        /** The default for large items: large → medium → small. */
        @JvmField
        val LARGE_MEDIUM_SMALL = RibbonSizeDefinition(RibbonItemSize.LARGE, RibbonItemSize.MEDIUM, RibbonItemSize.SMALL)

        /** Always large ([Paste], [New Slide], and so on). */
        @JvmField
        val ALWAYS_LARGE = RibbonSizeDefinition(RibbonItemSize.LARGE, RibbonItemSize.LARGE, RibbonItemSize.LARGE)

        /** The default for medium items: medium → small → small. */
        @JvmField
        val MEDIUM_SMALL = RibbonSizeDefinition(RibbonItemSize.MEDIUM, RibbonItemSize.SMALL, RibbonItemSize.SMALL)

        /** Always medium. */
        @JvmField
        val ALWAYS_MEDIUM = RibbonSizeDefinition(RibbonItemSize.MEDIUM, RibbonItemSize.MEDIUM, RibbonItemSize.MEDIUM)

        /** Always small (icon only). */
        @JvmField
        val ALWAYS_SMALL = RibbonSizeDefinition(RibbonItemSize.SMALL, RibbonItemSize.SMALL, RibbonItemSize.SMALL)

        /** The default size definition for a preferred size. */
        @JvmStatic
        fun forPreferredSize(size: RibbonItemSize): RibbonSizeDefinition = when (size) {
            RibbonItemSize.LARGE -> LARGE_MEDIUM_SMALL
            RibbonItemSize.MEDIUM -> MEDIUM_SMALL
            RibbonItemSize.SMALL -> ALWAYS_SMALL
        }

        /**
         * Parses "Large", "Large,Medium", and "Large, Medium, Small" (case-insensitive; "Middle" means Medium).
         * Throws if the format is invalid.
         */
        @JvmStatic
        fun parse(text: String): RibbonSizeDefinition = tryParse(text)
            ?: throw IllegalArgumentException("'$text' is not a size definition. Specify it like \"Large, Medium, Small\"")

        /** Parses a size definition. Returns null if the format is invalid. */
        @JvmStatic
        fun tryParse(text: String?): RibbonSizeDefinition? {
            if (text.isNullOrBlank()) return null
            val parts = text.split(',', ' ', ';').filter { it.isNotEmpty() }
            if (parts.size !in 1..3) return null
            val sizes = (0 until 3).map { i ->
                val part = parts[minOf(i, parts.size - 1)]
                val name = if (part.equals("Middle", ignoreCase = true)) "Medium" else part
                RibbonItemSize.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
            }
            return RibbonSizeDefinition(sizes[0], sizes[1], sizes[2])
        }

        private fun display(size: RibbonItemSize): String = size.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
