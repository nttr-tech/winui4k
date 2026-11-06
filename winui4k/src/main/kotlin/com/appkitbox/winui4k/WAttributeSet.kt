package com.appkitbox.winui4k

import java.util.Collections
import java.util.Enumeration

/**
 * AttributeSet-like: a read-only collection of attributes (key and value pairs) such as text formatting.
 * Returned by [WTextPane.getCharacterAttributes] and passed to [WTextPane.setCharacterAttributes] etc. The keys are the constants of [WStyleConstants].
 *
 * Unlike Swing, there is no mechanism for following a parent collection (getResolveParent / ResolveAttribute).
 */
interface WAttributeSet {
    /** AttributeSet.getAttributeCount-like: the number of attributes. */
    val attributeCount: Int

    /** AttributeSet.isDefined-like: whether the attribute [attrName] exists. */
    fun isDefined(attrName: Any): Boolean

    /** AttributeSet.isEqual-like: whether it has the same attributes (keys and values) as [attr], and the same number of them. */
    fun isEqual(attr: WAttributeSet): Boolean

    /** AttributeSet.copyAttributes-like: returns a collection of the same attributes that does not change even if the original changes later. */
    fun copyAttributes(): WAttributeSet

    /** AttributeSet.getAttribute-like: the value of the attribute [key]. null if none. */
    fun getAttribute(key: Any): Any?

    /** AttributeSet.getAttributeNames-like: the keys of the attributes (in no particular order). */
    val attributeNames: Enumeration<*>

    /** AttributeSet.containsAttribute-like: whether the attribute [name] is present with a value equal to [value]. */
    fun containsAttribute(name: Any, value: Any): Boolean

    /** AttributeSet.containsAttributes-like: whether it has all the attributes (keys and values) of [attributes]. */
    fun containsAttributes(attributes: WAttributeSet): Boolean
}

/**
 * MutableAttributeSet-like: a collection of attributes that can be modified. Usually [WSimpleAttributeSet] is used.
 * Unlike Swing, there is no setResolveParent.
 */
interface WMutableAttributeSet : WAttributeSet {
    /** MutableAttributeSet.addAttribute-like: sets the attribute [name] to [value] (replacing it if present). */
    fun addAttribute(name: Any, value: Any)

    /** MutableAttributeSet.addAttributes-like: adds all attributes of [attributes] (replacing those with the same keys). */
    fun addAttributes(attributes: WAttributeSet)

    /** MutableAttributeSet.removeAttribute-like: removes the attribute [name]. Does nothing if absent. */
    fun removeAttribute(name: Any)

    /** MutableAttributeSet.removeAttributes(Enumeration)-like: removes the attributes with the keys in [names], regardless of their values. */
    fun removeAttributes(names: Enumeration<*>)

    /** MutableAttributeSet.removeAttributes(AttributeSet)-like: removes only the attributes with the same keys and equal values as in [attributes]. */
    fun removeAttributes(attributes: WAttributeSet)
}

/**
 * SimpleAttributeSet-like: a modifiable collection of attributes held in a hash table.
 * Formatting is usually put in with the setters of [WStyleConstants] (`WStyleConstants.setBold(attrs, true)`).
 * As in Swing, [equals] is equal to a [WAttributeSet] with the same attributes (keys and values). Usable from threads other than the UI thread (it does not touch the UI).
 */
class WSimpleAttributeSet() : WMutableAttributeSet, Cloneable {
    /** SimpleAttributeSet(AttributeSet)-like: creates a collection with all attributes of [source] copied. */
    constructor(source: WAttributeSet) : this() {
        addAttributes(source)
    }

    private var table = LinkedHashMap<Any, Any>()

    /** SimpleAttributeSet.isEmpty-like: whether there are no attributes. */
    val isEmpty: Boolean get() = table.isEmpty()

    override val attributeCount: Int get() = table.size

    override fun isDefined(attrName: Any): Boolean = table.containsKey(attrName)

    override fun isEqual(attr: WAttributeSet): Boolean = attributeCount == attr.attributeCount && containsAttributes(attr)

    override fun copyAttributes(): WAttributeSet = clone()

    override fun getAttribute(key: Any): Any? = table[key]

    override val attributeNames: Enumeration<*> get() = Collections.enumeration(table.keys.toList())

    override fun containsAttribute(name: Any, value: Any): Boolean = table[name] == value

    override fun containsAttributes(attributes: WAttributeSet): Boolean =
        attributes.attributeNames.toList().all { name -> attributes.getAttribute(name!!).let { it != null && table[name] == it } }

    override fun addAttribute(name: Any, value: Any) {
        table[name] = value
    }

    override fun addAttributes(attributes: WAttributeSet) {
        for (name in attributes.attributeNames.toList()) {
            val value = attributes.getAttribute(name!!) ?: continue
            table[name] = value
        }
    }

    override fun removeAttribute(name: Any) {
        table.remove(name)
    }

    override fun removeAttributes(names: Enumeration<*>) {
        for (name in names.toList()) table.remove(name)
    }

    override fun removeAttributes(attributes: WAttributeSet) {
        if (attributes === this) {
            table.clear()
            return
        }
        for (name in attributes.attributeNames.toList()) {
            if (table[name] == attributes.getAttribute(name!!)) table.remove(name)
        }
    }

    /** SimpleAttributeSet.clone-like: another collection with the same attributes. */
    public override fun clone(): WSimpleAttributeSet {
        val copy = super.clone() as WSimpleAttributeSet
        copy.table = LinkedHashMap(table)
        return copy
    }

    override fun equals(other: Any?): Boolean = this === other || (other is WAttributeSet && isEqual(other))

    override fun hashCode(): Int = table.hashCode()

    override fun toString(): String = table.entries.joinToString(" ", postfix = if (table.isEmpty()) "" else " ") { "${it.key}=${it.value}" }

    companion object {
        /** SimpleAttributeSet.EMPTY-like: an unmodifiable collection without any attributes. */
        @JvmField
        val EMPTY: WAttributeSet = EmptyAttributeSet
    }
}

/** The content of [WSimpleAttributeSet.EMPTY]. */
private object EmptyAttributeSet : WAttributeSet {
    override val attributeCount: Int get() = 0

    override fun isDefined(attrName: Any): Boolean = false

    override fun isEqual(attr: WAttributeSet): Boolean = attr.attributeCount == 0

    override fun copyAttributes(): WAttributeSet = this

    override fun getAttribute(key: Any): Any? = null

    override val attributeNames: Enumeration<*> get() = Collections.emptyEnumeration<Any>()

    override fun containsAttribute(name: Any, value: Any): Boolean = false

    override fun containsAttributes(attributes: WAttributeSet): Boolean = attributes.attributeCount == 0

    override fun equals(other: Any?): Boolean = this === other || (other is WAttributeSet && other.attributeCount == 0)

    override fun hashCode(): Int = 0

    override fun toString(): String = ""
}
