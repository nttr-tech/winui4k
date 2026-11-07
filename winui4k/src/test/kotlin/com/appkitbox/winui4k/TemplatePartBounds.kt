package com.appkitbox.winui4k

/**
 * The top and bottom edges, in the coordinates of [relativeTo], of the part named [name] in the template of [control] (call on the UI thread).
 * null if its size is 0 (not shown).
 *
 * @throws IllegalStateException if there is no such part
 */
internal fun templatePartVerticalEdges(control: WControl, name: String, relativeTo: WComponent = control): Pair<Double, Double>? {
    val element = checkNotNull(TemplateParts.find(control, name)) { "template part not found: $name" }
    try {
        val (width, height) = VisualHitTest.actualSizeOf(element)
        if (width == 0.0 || height == 0.0) return null
        val (_, top) = VisualHitTest.transform(element, relativeTo.uiElement, 0.0, 0.0)
        return top to top + height
    } finally {
        element.release()
    }
}
