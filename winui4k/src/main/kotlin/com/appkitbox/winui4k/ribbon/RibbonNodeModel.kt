package com.appkitbox.winui4k.ribbon

import java.util.EventObject
import java.util.UUID

/**
 * Like java.awt.event.ActionEvent: indicates that a ribbon item, menu item, or the like was executed.
 * Regardless of the view it was executed from (the ribbon itself, the QAT, an overflow menu, a KeyTip, or command
 * search), the source is always the model ([getSource]).
 */
class RibbonActionEvent(
    source: RibbonNodeModel,
    /**
     * The parameter of the execution (the checked state of a toggle, the chosen gallery value, a color, entered text,
     * and so on).
     */
    val parameter: Any?,
) : EventObject(source) {
    /** The model that was executed. */
    override fun getSource(): RibbonNodeModel = super.getSource() as RibbonNodeModel

    override fun toString(): String = "RibbonActionEvent(${getSource().id}, parameter=$parameter)"
}

/** Like java.awt.event.ActionListener: receives the execution of items. */
fun interface RibbonActionListener {
    /** Called when an item is executed. */
    fun actionPerformed(event: RibbonActionEvent)
}

/**
 * The common base of the nodes of a ribbon model (tabs, groups, items, menu items, Backstage items, and so on).
 *
 * [id] is a stable identifier used for persistence, merging, KeyTips, the QAT, and command routing
 * (if omitted, a unique value is assigned automatically).
 */
abstract class RibbonNodeModel protected constructor(id: String?, label: String?) : RibbonObservable() {
    /** A stable identifier used for persistence, merging, KeyTips, the QAT, and UI Automation. */
    var id: String by observable(id ?: UUID.randomUUID().toString().replace("-", ""))

    /** Display label. */
    var label: String? by observable(label)

    /** The icon used at small and medium sizes. */
    var icon: RibbonIcon? by observable(null)

    /** The icon used at large size (null means [icon]). */
    var largeIcon: RibbonIcon? by observable(null)

    /** Whether the node is shown. */
    var isVisible: Boolean by observable(true)

    /** Whether the node is enabled (the command's executability is also taken into account). */
    var isEnabled: Boolean by observable(true)

    /** The explicit KeyTip. If null, one is generated from the label. */
    var keyTip: String? by observable(null)

    /** The rich tooltip. */
    var screenTip: RibbonScreenTip? by observable(null)

    /** Description used for the ScreenTip and command search. */
    var description: String? by observable(null)

    /** The sort order used when merging models. */
    var order: Int by observable(0)

    /** Arbitrary data for the app. */
    var tag: Any? by observable(null)

    /** The UI Automation id (null means [id]). */
    var automationId: String? by observable(null)

    /** How this node is handled when merged into another model. */
    var mergeAction: RibbonMergeAction by observable(RibbonMergeAction.MERGE)

    /** Search keywords and synonyms. */
    val keywords: RibbonList<String> = RibbonList()

    private val actionListeners = mutableListOf<RibbonActionListener>()

    /** Subscribes to execution (click, selection, commit) notifications. Called no matter which view executes it. */
    fun addActionListener(listener: RibbonActionListener) {
        actionListeners += listener
    }

    /** Removes a listener registered with [addActionListener]. */
    fun removeActionListener(listener: RibbonActionListener) {
        actionListeners -= listener
    }

    /** The registered listeners. */
    fun getActionListeners(): List<RibbonActionListener> = actionListeners.toList()

    /** Notifies [RibbonActionListener]s of an execution (called by views). */
    internal fun fireActionPerformed(parameter: Any?) {
        if (actionListeners.isEmpty()) return
        val event = RibbonActionEvent(this, parameter)
        for (listener in actionListeners.toList()) listener.actionPerformed(event)
    }

    override fun toString(): String = "${javaClass.simpleName}($id, $label)"
}
