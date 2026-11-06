package com.appkitbox.winui4k.tree

import java.util.EventObject

/**
 * javax.swing.event.TreeModelEvent-style: the details of a change to a [TreeModel].
 *
 * [getTreePath] is the path to the changed node (the parent), and [getChildIndices] and [getChildren] are the children of it that changed
 * (in ascending order of position). For structure changes and root changes, both are null.
 */
class TreeModelEvent(
    source: Any,
    private val path: TreePath?,
    private val childIndices: IntArray?,
    private val children: Array<out Any?>?,
) : EventObject(source) {
    /** Takes the path as an array (the root first). [IllegalArgumentException] if [path] is empty or null. */
    constructor(source: Any, path: Array<out Any?>?, childIndices: IntArray?, children: Array<out Any?>?) :
        this(source, TreePath(requireNotNull(path) { "path in TreePath must be non null and not empty." }), childIndices, children)

    /** A notification without child information (a structure change and so on), with the path as an array. [IllegalArgumentException] if [path] is null. */
    constructor(source: Any, path: Array<out Any?>?) :
        this(source, TreePath(requireNotNull(path) { "path in TreeModelEvent cannot be null" }), null, null)

    /** A notification without child information (a structure change and so on). */
    constructor(source: Any, path: TreePath?) : this(source, path, null, null)

    /** The path to the changed node (the parent) (the root first), or null if none. */
    fun getPath(): Array<Any>? = path?.getPath()

    /** The path to the changed node (the parent), or null if none. */
    fun getTreePath(): TreePath? = path

    /** The changed children (in the same order as [getChildIndices]), or null if none. Returns a copy. */
    fun getChildren(): Array<Any?>? = children?.let { arrayOf<Any?>(*it) }

    /** The positions of the changed children in the parent (ascending), or null if none. Returns a copy. */
    fun getChildIndices(): IntArray? = childIndices?.copyOf()

    override fun toString(): String = buildString {
        append(javaClass.name).append('@').append(Integer.toHexString(hashCode()))
        if (path != null) append(" path ").append(path)
        if (childIndices != null) append(" indices ").append(childIndices.joinToString(prefix = "[", postfix = "]"))
        if (children != null) append(" children ").append(children.joinToString(prefix = "[", postfix = "]"))
    }
}
