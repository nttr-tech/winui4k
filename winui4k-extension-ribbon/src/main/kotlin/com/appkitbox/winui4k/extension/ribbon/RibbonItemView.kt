package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.FlyoutPlacement
import com.appkitbox.winui4k.WFlyoutBase
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItemBase
import com.appkitbox.winui4k.WSize
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommand
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandCatalog
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandStateListener
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupState
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonPropertyChangeListener
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings

/**
 * The layout specification of an item (RibbonItemLayout in RibbonSpace).
 * [showLabel] overrides whether the label is shown (QAT and toolbars). null follows the item's default.
 * [groupState] is used by containers to determine the size of their children.
 */
internal data class RibbonItemLayout(
    val size: RibbonItemSize,
    val metrics: RibbonMetrics,
    val isSimplified: Boolean = false,
    val showLabel: Boolean? = null,
    val groupState: RibbonGroupState = RibbonGroupState.LARGE,
)

/** What happens after an item is selected with a KeyTip (RibbonKeyTipResult in RibbonSpace). */
internal sealed class RibbonKeyTipResult {
    /** Ends KeyTip mode. */
    object Close : RibbonKeyTipResult()

    /**
     * Proceeds to the next level inside an opened popup or the like. [cleanup] cleans up when leaving that level (Esc
     * or cancel).
     */
    class Scope(val cleanup: (() -> Unit)? = null, val targets: () -> List<RibbonKeyTipTarget>) : RibbonKeyTipResult()

    /** Proceeds to the level of the opened menu's items (KeyTips are shown on the items after the menu opens). */
    class Menu(val menu: WMenuFlyout) : RibbonKeyTipResult()
}

/** Something selectable with a KeyTip (an item, a tab header, a group's collapsed button, and so on). */
internal interface RibbonKeyTipTarget {
    /** The label used to generate the KeyTip. */
    val keyTipLabel: String?

    /** The explicit KeyTip. */
    val explicitKeyTip: String?

    /** The element the KeyTip badge is positioned relative to. */
    val keyTipAnchor: XamlElement

    /** Whether it can be selected (badges of disabled items are shown dimmed). */
    val isKeyTipEnabled: Boolean get() = true

    /**
     * Whether to show the badge at the center of the element's bottom edge (large buttons and tab headers). If false,
     * it goes midway along the left edge.
     */
    val isLargeKeyTip: Boolean get() = false

    /** What happens when it is selected with a KeyTip. */
    fun onKeyTip(): RibbonKeyTipResult

    /** The target's model (an item, tab, group, or menu item; null if none). */
    val keyTipModel: com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel? get() = null
}

/**
 * The contract of what hosts items (the ribbon, toolbars, the QAT, status bars) (IRibbonItemOwner in RibbonSpace).
 */
internal interface RibbonItemHost {
    /** The dimensions. */
    val metrics: RibbonMetrics

    /** Text width measurement. */
    val textWidths: RibbonTextWidths

    /** The command catalog that resolves commandId. */
    val commandCatalog: RibbonCommandCatalog?

    /** The ribbon it belongs to (null for a standalone toolbar and the like). */
    val ribbon: WRibbon?

    /** Notifies that an item was invoked (the ribbon's item-invoked listeners and popup cleanup). */
    fun itemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?)

    /** The size of an item changed, so lay out again. */
    fun invalidateItemsLayout()

    /** The direction in which drop-downs open (upward in a status bar at the bottom edge of the window). */
    val dropDownPlacement: FlyoutPlacement get() = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT

    /** Shows the item's right-click menu. Returns false if it cannot be shown. */
    fun showItemContextMenu(view: RibbonItemView, x: Double, y: Double): Boolean = ribbon?.showItemContextMenu(view, x, y) ?: false

    /**
     * The items in the scope of toggles that behave as radio buttons ([com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel.groupName])
     * (as in RibbonSpace, the nearest scope in the order: owning group → toolbar → ribbon).
     */
    fun radioScope(): List<RibbonItemModel> = ribbon?.allItemModels().orEmpty()
}

/**
 * The common base of item views (the role of IRibbonItem and RibbonItemHelper in RibbonSpace).
 *
 * Observes changes to the model ([model]) to update the display, and writes user interaction back to the model. Even when
 * the same model is shown in multiple views, such as the QAT and the overflow menu, their state always stays in sync
 * through the model (equivalent to "linked copies" in RibbonSpace). The command is resolved from [RibbonItemModel.command]
 * or from [RibbonItemModel.commandId] in the command catalog.
 */
internal abstract class RibbonItemView(open val model: RibbonItemModel, val host: RibbonItemHost) : RibbonKeyTipTarget {
    /** The element placed in the parent panel. */
    abstract val element: XamlElement

    /** The current layout. */
    var layout: RibbonItemLayout = RibbonItemLayout(RibbonItemSize.MEDIUM, RibbonMetrics.COMFORTABLE)
        private set

    private val modelListener = RibbonPropertyChangeListener { event -> onModelChanged(event.propertyName) }
    private val canExecuteListener = Runnable { refreshEnabled() }
    private val catalogListener = RibbonCommandStateListener { event ->
        if (event.id.equals(model.commandId, ignoreCase = true)) onCatalogStateChanged(event.propertyName)
    }
    private var subscribedCommand: RibbonCommand? = null
    private var subscribedCatalog: RibbonCommandCatalog? = null
    private var screenTipModel: RibbonScreenTip? = null
    private val screenTipListener = RibbonPropertyChangeListener { refreshToolTip() }
    private var attached = false

    /** Starts observing the model and applies the common state. Subclasses call this after creating their elements. */
    open fun attach() {
        if (attached) return
        attached = true
        model.addPropertyChangeListener(modelListener)
        resubscribeCommand()
        refreshCommon()
        element.onRightTapped { e ->
            val position = e.position(element)
            if (host.showItemContextMenu(this, position[0], position[1])) e.markHandled()
        }
    }

    /** Stops observing the model (when the view is discarded). */
    open fun dispose() {
        if (!attached) return
        attached = false
        model.removePropertyChangeListener(modelListener)
        subscribedCommand?.removeCanExecuteChangedListener(canExecuteListener)
        subscribedCommand = null
        subscribedCatalog?.removeCommandStateListener(catalogListener)
        subscribedCatalog = null
        screenTipModel?.removePropertyChangeListener(screenTipListener)
        screenTipModel = null
    }

    /** Whether to show it (the model's visibility). */
    open val isShown: Boolean get() = model.isVisible

    /** The size for [layout] (including the outer border). */
    abstract fun measure(layout: RibbonItemLayout): WSize

    /** Applies [layout] (builds the content and sets the size). */
    fun applyLayout(layout: RibbonItemLayout) {
        this.layout = layout
        applyLayoutCore()
    }

    /** Builds the look for the current [layout]. */
    protected abstract fun applyLayoutCore()

    /** Whether to show the label for [layout] (ActualShowLabel in RibbonSpace). */
    protected fun showsLabel(layout: RibbonItemLayout): Boolean =
        layout.showLabel ?: if (layout.isSimplified) showsLabelInSimplified else model.showLabel

    /** Whether to show the label in the simplified ribbon (if not specified, only for items whose preferred size is large). */
    val showsLabelInSimplified: Boolean get() = model.showLabelInSimplified ?: (model.size == RibbonItemSize.LARGE)

    /** A model property changed. By default, applies the common state and lays out again for size-related properties. */
    protected open fun onModelChanged(name: String) {
        when (name) {
            "command", "commandId" -> resubscribeCommand()
            "screenTip", "description", "shortcut" -> refreshToolTip()
            "keyTip" -> {
                refreshToolTip()
                element.setAutomationAccessKey(model.keyTip)
            }
            "automationId" -> element.setAutomationId(model.automationId ?: model.id)
            "isEnabled" -> refreshEnabled()
            "isVisible" -> {
                element.isVisible = model.isVisible
                host.invalidateItemsLayout()
            }
            "label", "icon", "largeIcon", "size", "sizeDefinition", "effectiveSizeDefinition", "showLabel",
            "showLabelInSimplified", "simplifiedVisibility",
            -> {
                refreshToolTip()
                host.invalidateItemsLayout()
                applyLayoutCore()
            }
        }
    }

    /** The command catalog's descriptor (enabled, checked) changed. */
    protected open fun onCatalogStateChanged(propertyName: String?) {
        if (propertyName == "command") resubscribeCommand() else refreshEnabled()
    }

    /** Applies the common state (visibility, enabled, tooltip, automation). */
    protected open fun refreshCommon() {
        element.isVisible = model.isVisible
        refreshEnabled()
        refreshToolTip()
        element.setAutomationId(model.automationId ?: model.id)
        element.setAutomationAccessKey(model.keyTip)
    }

    /** The command used for execution (the model's command, or the catalog's command if there is none). */
    val effectiveCommand: RibbonCommand? get() = model.command ?: host.commandCatalog?.resolve(model.commandId)

    /** Whether it is enabled (the model, the catalog descriptor, and the command's can-execute all allow it). */
    open val isEffectivelyEnabled: Boolean
        get() {
            if (!model.isEnabled) return false
            val descriptor = host.commandCatalog?.find(model.commandId)
            if (descriptor != null && !descriptor.isEnabled) return false
            return effectiveCommand?.canExecute(commandParameter()) ?: true
        }

    override val isKeyTipEnabled: Boolean get() = isEffectivelyEnabled

    /** The argument passed to the command (overridden per item; the model's parameter by default). */
    protected open fun commandParameter(): Any? = model.commandParameter

    /** Applies the enabled state to the element. */
    open fun refreshEnabled() {
        val enabled = isEffectivelyEnabled
        runCatching { element.isControlEnabled = enabled }
        refreshToolTip()
    }

    private fun resubscribeCommand() {
        subscribedCommand?.removeCanExecuteChangedListener(canExecuteListener)
        subscribedCommand = effectiveCommand?.also { it.addCanExecuteChangedListener(canExecuteListener) }
        val catalog = host.commandCatalog
        if (catalog !== subscribedCatalog) {
            subscribedCatalog?.removeCommandStateListener(catalogListener)
            subscribedCatalog = catalog?.also { it.addCommandStateListener(catalogListener) }
        }
        host.commandCatalog?.find(model.commandId)?.isChecked?.let { applyCatalogChecked(it) }
        refreshEnabled()
    }

    /** Applies the catalog's checked state to the model (only toggle-like items override this). */
    protected open fun applyCatalogChecked(isChecked: Boolean) {
        // Items without a checked state do nothing
    }

    /** Applies the tooltip (ScreenTip) and the automation name. */
    open fun refreshToolTip() {
        val tip = model.screenTip
        if (tip !== screenTipModel) {
            screenTipModel?.removePropertyChangeListener(screenTipListener)
            screenTipModel = tip?.also { it.addPropertyChangeListener(screenTipListener) }
        }
        val target = toolTipTarget()
        target.setToolTipValue(RibbonScreenTips.create(model.label, tip, model.description, model.shortcut, isEffectivelyEnabled))
        target.setAutomationName(model.label?.replace('\n', ' '))
        target.setAutomationHelpText(tip?.description ?: model.description)
        target.setAutomationAcceleratorKey(model.shortcut)
    }

    /** The element the tooltip is attached to ([element] by default). */
    protected open fun toolTipTarget(): XamlElement = element

    /** Executes the command and notifies that the item was invoked (Execute + NotifyInvoked in RibbonSpace). */
    fun execute(parameter: Any? = commandParameter(), notifyParameter: Any? = parameter) {
        val command = model.command
        if (command != null) {
            if (command.canExecute(parameter)) command.execute(parameter)
        } else {
            val id = model.commandId
            if (id != null) host.commandCatalog?.execute(id, parameter)
        }
        // Apart from the command argument, the notification carries values such as a toggle's checked state
        // (NotifyInvoked in RibbonSpace)
        model.fireActionPerformed(notifyParameter)
        host.itemInvoked(model, model.commandId, notifyParameter)
    }

    /** The drop-down (a menu, palette, or expanded gallery). null for items without one. */
    open val dropDown: WFlyoutBase? get() = null

    /** Opens [flyout] below the item (above it in a status bar). */
    protected fun showDropDown(flyout: WFlyoutBase) {
        flyout.placement = host.dropDownPlacement
        flyout.showAt(element)
    }

    /** Opens the drop-down (does nothing for items without one). */
    open fun openDropDown() {
        // An item without a drop-down
    }

    /** Runs the primary action (command search, KeyTips). Returns false if it cannot be run. */
    open fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        execute()
        return true
    }

    override val keyTipLabel: String? get() = model.label
    override val explicitKeyTip: String? get() = model.keyTip
    override val keyTipAnchor: XamlElement get() = element
    override val keyTipModel: com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel get() = model

    override val isLargeKeyTip: Boolean get() = layout.size == RibbonItemSize.LARGE && !layout.isSimplified

    override fun onKeyTip(): RibbonKeyTipResult {
        invoke()
        return RibbonKeyTipResult.Close
    }

    /** The KeyTip targets (the child items for a container). */
    open fun keyTipTargets(): List<RibbonKeyTipTarget> = listOf(this)

    /** The items of the simplified ribbon's overflow menu. */
    open fun overflowMenuItems(): List<WMenuFlyoutItemBase> = listOf(RibbonMenus.commandItem(this))
}
