package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.text.NumberFormat

/**
 * The implementation of JProgressBar's value model, ChangeListeners and progress string shared by the progress components ([WProgressBar] / [WProgressRing]).
 *
 * The range of values is held by [model] (a [WBoundedRangeModel]); on every model change it is copied to the native side with [writeNative] ([push]) and
 * passed on to [changeListeners] (the source is [owner]). These are not components whose value the user changes, so nothing is written back from the native side.
 * The model strongly references the component through [modelListener], but the native side does not reference the component, so there is no cycle across the boundary.
 */
internal class ProgressRange(
    private val owner: WComponent,
    private val writeNative: (minimum: Double, maximum: Double, value: Double) -> Unit,
) {
    /** The ChangeListeners (no native handler is used, and the ListenerAnchor does not keep the component alive). */
    val changeListeners = ChangeListeners(owner, anchored = false) {}

    private val modelListener = WChangeListener { onModelChanged() }

    private var current: WBoundedRangeModel = WDefaultBoundedRangeModel().also { it.addChangeListener(modelListener) }

    private var customString: String? = null

    /** Whether the progress string has been put into ItemStatus (to clear it when turned off). */
    private var statusShown = false

    /** The range model. Replacing it updates the native side and, as with JProgressBar, sets the model's extent to 0. */
    var model: WBoundedRangeModel
        get() = current
        set(newModel) {
            if (newModel === current) return
            current.removeChangeListener(modelListener)
            current = newModel
            newModel.addChangeListener(modelListener)
            push()
            newModel.extent = 0.0 // Same as JProgressBar.setModel (if the extent changes, the model's notification tells the listeners)
        }

    /** `(value - minimum) / (maximum - minimum)`. NaN if the minimum and maximum are the same (same as Swing). */
    val percentComplete: Double
        get() {
            val m = current
            return (m.value - m.minimum) / (m.maximum - m.minimum)
        }

    /** The string set. If null, [percentComplete] written with NumberFormat.getPercentInstance (same as Swing; "50%" in English or Japanese). */
    var string: String?
        get() = customString ?: NumberFormat.getPercentInstance().format(percentComplete)
        set(value) {
            customString = value
            updateAutomation()
        }

    /** Whether to expose the progress string as the UI Automation ItemStatus. */
    var isStringPainted: Boolean = false
        set(value) {
            field = value
            updateAutomation()
        }

    /** Copies the model state to the native side. */
    fun push() {
        val m = current
        writeNative(m.minimum, m.maximum, m.value)
        updateAutomation()
    }

    private fun onModelChanged() {
        push()
        changeListeners.fire()
    }

    private fun updateAutomation() {
        if (!isStringPainted && !statusShown) return
        Xaml.setAutomation(owner.automationTarget, XamlInterop.IAutomationPropertiesStatics_SetItemStatus, if (isStringPainted) string else null)
        statusShown = isStringPainted
    }
}
