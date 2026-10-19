package com.appkitbox.winui4k

/**
 * The difference between the text before and after a change: [removedLength] characters were removed and
 * [insertedLength] characters were inserted at [offset] (positions and lengths are in UTF-16 chars, as in Swing's
 * Document).
 * WinUI's input field change notifications (TextBox.TextChanging etc.) do not pass a position and length, so they are
 * derived from the text before and after and used as the offset / length of [WDocumentEvent].
 */
internal data class TextDiff(val offset: Int, val removedLength: Int, val insertedLength: Int) {
    companion object {
        /**
         * The difference from [old] to [new]. null if the strings are the same.
         *
         * Derived as the smallest single replacement after excluding the parts that match at the start and end. For an
         * insertion or deletion where the same character repeats (adding "a" to "aa" to get "aaa") the position is not
         * unique, so [caretAfter] (the caret position after the change; negative if unknown) is used as a hint: if it is
         * consistent to assume the inserted characters end right before the caret (for a deletion, that they were removed
         * at the caret position), that position is used; otherwise the position after the matching part (the rightmost).
         *
         * Positions and lengths are in UTF-16 chars, but a difference never starts or ends in the middle of a surrogate pair
         * (changing 😀 to 😁, which share the high surrogate, gives offset 0 length 2, not offset 1 length 1).
         */
        fun between(old: String, new: String, caretAfter: Int = -1): TextDiff? {
            if (old == new) return null
            val shorter = minOf(old.length, new.length)
            var prefix = 0
            while (prefix < shorter && old[prefix] == new[prefix]) prefix++
            var suffix = 0
            while (suffix < shorter - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
            // Move the matching range back to before the pair so that the difference does not start or end in the middle of a surrogate pair
            if (prefix > 0 && old[prefix - 1].isHighSurrogate()) prefix--
            if (suffix > 0 && old[old.length - suffix].isLowSurrogate()) suffix--
            val removed = old.length - prefix - suffix
            val inserted = new.length - prefix - suffix
            val hinted = caretAfter - inserted
            // Within the matching leading range (0..prefix), if what follows matches, the same difference can be placed at that position
            val offset = if (caretAfter >= 0 && hinted in 0..prefix && sameTail(old, new, hinted, removed, inserted)) {
                hinted
            } else {
                prefix
            }
            return TextDiff(offset, removed, inserted)
        }

        /** Whether, after removing [removed] characters and inserting [inserted] characters at [offset], the rest of the text matches between [old] and [new]. */
        private fun sameTail(old: String, new: String, offset: Int, removed: Int, inserted: Int): Boolean =
            old.regionMatches(offset + removed, new, offset + inserted, old.length - offset - removed)
    }
}
