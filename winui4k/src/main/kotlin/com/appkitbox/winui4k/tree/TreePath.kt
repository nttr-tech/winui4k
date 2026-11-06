package com.appkitbox.winui4k.tree

/**
 * javax.swing.tree.TreePath-style: the sequence of nodes (or [TreeModel] elements) from the root to a node. Immutable.
 *
 * No element can be null. [equals] compares the elements in order with [Any.equals], and [hashCode] is the hash of the last element.
 */
open class TreePath protected constructor(
    private val parentPath: TreePath?,
    private val lastPathComponent: Any,
) {
    /**
     * Creates it from [path] (the root first). [IllegalArgumentException] if [path] is empty or contains null.
     */
    constructor(path: Array<out Any?>) : this(parentOf(path, path.size), lastOf(path, path.size))

    /** Creates a path with a single element. */
    constructor(singlePathComponent: Any) : this(null, singlePathComponent)

    /** Creates it from the first [length] elements of [path]. */
    protected constructor(path: Array<out Any?>, length: Int) : this(parentOf(path, length), lastOf(path, length))

    /** All elements of the path (the root first). */
    fun getPath(): Array<Any> {
        val result = arrayOfNulls<Any>(getPathCount())
        var index = result.size - 1
        var path: TreePath? = this
        while (path != null) {
            result[index--] = path.lastPathComponent
            path = path.parentPath
        }
        @Suppress("UNCHECKED_CAST")
        return result as Array<Any>
    }

    /** The last element of the path. */
    fun getLastPathComponent(): Any = lastPathComponent

    /** The number of elements in the path (1 or more). */
    fun getPathCount(): Int {
        var count = 0
        var path: TreePath? = this
        while (path != null) {
            count++
            path = path.parentPath
        }
        return count
    }

    /** The element at [index] (the root is 0). [IllegalArgumentException] if out of range. */
    fun getPathComponent(index: Int): Any {
        val length = getPathCount()
        require(index in 0 until length) { "Index $index is out of the specified range" }
        var path: TreePath = this
        repeat(length - 1 - index) { path = path.parentPath!! }
        return path.lastPathComponent
    }

    /** True if [aTreePath] is the same as this path or below it (an extension of it). */
    fun isDescendant(aTreePath: TreePath?): Boolean {
        if (aTreePath === this) return true
        if (aTreePath == null) return false
        val pathLength = getPathCount()
        var otherLength = aTreePath.getPathCount()
        // With fewer elements, it cannot be below this path
        if (otherLength < pathLength) return false
        var other: TreePath = aTreePath
        while (otherLength-- > pathLength) other = other.parentPath!!
        return this == other
    }

    /** A new path with [child] added to the end of this path. */
    fun pathByAddingChild(child: Any): TreePath = TreePath(this, child)

    /** The path without the last element, or null if there is only one element. */
    fun getParentPath(): TreePath? = parentPath

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is TreePath) return false
        if (getPathCount() != other.getPathCount()) return false
        var mine: TreePath? = this
        var theirs: TreePath? = other
        while (mine != null && theirs != null) {
            if (mine.lastPathComponent != theirs.lastPathComponent) return false
            mine = mine.parentPath
            theirs = theirs.parentPath
        }
        return true
    }

    override fun hashCode(): Int = lastPathComponent.hashCode()

    /** In the form `[root, ..., last]`. */
    override fun toString(): String = getPath().joinToString(prefix = "[", postfix = "]", separator = ", ")

    private companion object {
        fun parentOf(path: Array<out Any?>, length: Int): TreePath? {
            require(length > 0) { "path in TreePath must be non null and not empty." }
            return if (length > 1) TreePath(path, length - 1) else null
        }

        fun lastOf(path: Array<out Any?>, length: Int): Any {
            require(length > 0) { "path in TreePath must be non null and not empty." }
            return requireNotNull(path[length - 1]) { "Path elements must be non-null" }
        }
    }
}
