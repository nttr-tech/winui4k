package com.appkitbox.winui4k

/**
 * SpinnerListModel-style: a [WSpinnerModel] that steps through the elements of a list ([list]) in order. The first value is the first element of the list.
 *
 * As in Swing:
 * - Only elements in the list can be set as [value] (searched with equals; [IllegalArgumentException] if absent)
 * - [nextValue] returns null at the end and [previousValue] at the start (no wrapping)
 * - The list passed in is used as is, without copying
 *
 * At this point [WSpinner] can display only numeric models ([WSpinnerNumberModel]), so this model cannot be attached to a spinner.
 *
 * @throws IllegalArgumentException when an empty list is passed
 */
class WSpinnerListModel(values: List<*>) : WAbstractSpinnerModel() {
    /** Like SpinnerListModel(Object[]): creates a model that steps through the elements of an array. */
    constructor(values: Array<*>) : this(requireNotEmpty(values).asList())

    /** Like SpinnerListModel(): creates it with a list holding only the single element "empty" (same as Swing). */
    constructor() : this(listOf("empty"))

    private var index = 0

    /**
     * The list to step through. Setting it (if the contents differ by equals) makes the first element the value and notifies.
     * @throws IllegalArgumentException when an empty list is set
     */
    var list: List<*> = requireNotEmpty(values)
        set(newList) {
            require(newList.isNotEmpty()) { "invalid list" }
            if (newList == field) return
            field = newList
            index = 0
            fireStateChanged()
        }

    /**
     * The current value (an element of the list).
     * @throws IllegalArgumentException when a value not in the list is set
     */
    override var value: Any?
        get() = list[index]
        set(element) {
            val newIndex = list.indexOf(element)
            require(newIndex >= 0) { "invalid sequence element" }
            if (newIndex == index) return
            index = newIndex
            fireStateChanged()
        }

    /** The next element in the list. Null at the end. */
    override val nextValue: Any? get() = if (index >= list.size - 1) null else list[index + 1]

    /** The previous element in the list. Null at the start. */
    override val previousValue: Any? get() = if (index <= 0) null else list[index - 1]

    private companion object {
        fun requireNotEmpty(values: List<*>): List<*> {
            require(values.isNotEmpty()) { "SpinnerListModel(List) expects non-null non-empty List" }
            return values
        }

        fun requireNotEmpty(values: Array<*>): Array<*> {
            require(values.isNotEmpty()) { "SpinnerListModel(Object[]) expects non-null non-empty Object[]" }
            return values
        }
    }
}
