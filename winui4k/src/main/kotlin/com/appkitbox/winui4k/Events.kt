package com.appkitbox.winui4k

/**
 * A "listener → event token" mapping. Removes one entry at a time, last-added first
 * (so the same listener can be added more than once and removed independently).
 * The common plumbing for subscribing to WinRT events (addEventHandler) lives in winrt.Events.
 */
internal class ListenerTokens<L : Any> {
    private val tokens = ArrayDeque<Pair<L, Long>>()

    fun add(listener: L, token: Long) {
        tokens.addLast(listener to token)
    }

    /** Removes and returns one token matching [listener]. Returns null if none is registered. */
    fun remove(listener: L): Long? {
        val index = tokens.indexOfLast { it.first === listener }
        if (index < 0) return null
        return tokens.removeAt(index).second
    }
}
