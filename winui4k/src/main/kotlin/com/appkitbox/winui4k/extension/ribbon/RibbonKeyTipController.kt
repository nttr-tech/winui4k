package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutItemBase
import com.appkitbox.winui4k.WMenuFlyoutSubItem
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyTipAssigner
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyTipMatch
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyTipNavigator
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyTipRequest
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyTipScope

/** The contract of a ribbon that shows KeyTips. */
internal interface RibbonKeyTipOwner {
    /** The top-level targets (the application button, tab headers, and tab-row items). */
    fun topLevelKeyTipTargets(): List<RibbonKeyTipTarget>

    /** The QAT item targets (assigned numbered KeyTips). */
    fun quickAccessKeyTipTargets(): List<RibbonKeyTipTarget>

    /** The basis for the popup that shows the badges (an element with a XamlRoot). */
    val keyTipRoot: WComponent

    /** The element that receives focus when entering KeyTip mode (the selected tab header). */
    fun keyTipFocusTarget(): XamlElement?

    /** Notifies a change of KeyTip mode. */
    fun keyTipModeChanged(active: Boolean)

    /** Also watch key input in popups (menus) with the ribbon's keyboard handling. */
    fun attachPopupKeyboard(element: XamlElement)
}

/**
 * KeyTip mode (the KeyTip part of Ribbon.Keyboard in RibbonSpace).
 *
 * Alt or F10 shows the badges of the top level (the application button, the QAT, tabs, and tab-row items); typing a
 * character runs the target or proceeds to the next level (the tab's commands, a collapsed group, an expanded panel, or a
 * menu). Esc goes back one level, and Backspace undoes one typed character. The badges are drawn on the Canvas of a popup
 * that covers the whole window and is not hit-testable.
 */
internal class RibbonKeyTipController(private val owner: RibbonKeyTipOwner) {
    private val navigator = RibbonKeyTipNavigator<RibbonKeyTipTarget>()
    private val cleanups = ArrayDeque<(() -> Unit)?>()
    private val canvas = XamlElement.load("<Canvas IsHitTestVisible=\"False\" />")
    private val popup = WPopup(canvas)
    private val badges = mutableListOf<Pair<String, XamlElement>>()
    private var focusBefore: com.appkitbox.winui4k.internal.com.ComPtr? = null

    init {
        popup.isLightDismissEnabled = false
        popup.uiElement.putBool(com.appkitbox.winui4k.internal.winui.XamlInterop.IUIElement_put_IsHitTestVisible, false)
    }

    /** Whether KeyTips are shown. */
    val isActive: Boolean get() = navigator.isActive

    /** The text of the badges currently shown (for tests and automation). */
    val activeKeyTips: List<String> get() = navigator.current?.entries?.map { it.tip }.orEmpty()

    /** The KeyTips currently shown and their targets. */
    val currentKeyTips: List<Pair<String, RibbonKeyTipTarget>> get() = navigator.current?.entries?.map { it.tip to it.target }.orEmpty()

    /** Shows the top level. */
    fun show(targets: List<RibbonKeyTipTarget>? = null) {
        hide()
        val qat = owner.quickAccessKeyTipTargets()
        pushScope(targets ?: (qat + owner.topLevelKeyTipTargets()), null, qat.toSet())
        moveFocusIntoRibbon()
        owner.keyTipModeChanged(true)
    }

    /** Hides the KeyTips (popups opened by navigation stay open). */
    fun hide() {
        val wasActive = navigator.isActive
        navigator.clear()
        cleanups.clear()
        clearBadges()
        popup.hide()
        if (wasActive) {
            WinUiUtilities.invokeLater { restoreFocus() }
            owner.keyTipModeChanged(false)
        }
    }

    /** Exits KeyTip mode and also closes all popups opened by navigation. */
    fun cancel() {
        while (cleanups.isNotEmpty()) cleanups.removeLast()?.invoke()
        hide()
    }

    /** Goes back one level (Esc). Returns false if the mode ends. */
    fun pop(): Boolean {
        if (!navigator.isActive) return false
        cleanups.removeLastOrNull()?.invoke()
        if (!navigator.pop()) {
            hide()
            return false
        }
        navigator.current?.reset()
        render()
        return true
    }

    /** Types one character. Returns true if it matches a KeyTip (partially or fully). */
    fun process(ch: Char): Boolean {
        val scope = navigator.current ?: return false
        val result = scope.process(ch)
        return when (result.match) {
            RibbonKeyTipMatch.PARTIAL -> {
                updateBadgeVisibility()
                true
            }
            RibbonKeyTipMatch.COMPLETE -> {
                result.target?.let { activate(it) }
                true
            }
            else -> {
                updateBadgeVisibility()
                false
            }
        }
    }

    /** Key input during KeyTip mode (Esc, F10, Backspace, characters). Returns true if handled. */
    fun handleKey(key: Int): Boolean {
        when (key) {
            VK_ESCAPE -> pop()
            VK_F10 -> cancel()
            VK_BACK -> {
                navigator.current?.backspace()
                updateBadgeVisibility()
            }
            else -> keyToChar(key)?.let { process(it) }
        }
        return true
    }

    private fun activate(target: RibbonKeyTipTarget) {
        if (!target.isKeyTipEnabled) return
        clearBadges()
        when (val result = target.onKeyTip()) {
            is RibbonKeyTipResult.Scope -> WinUiUtilities.invokeLater { if (navigator.isActive) pushScope(result.targets(), result.cleanup, emptySet()) }
            is RibbonKeyTipResult.Menu -> continueInMenu(result.menu)
            RibbonKeyTipResult.Close -> hide()
        }
    }

    /**
     * When the menu opens, proceeds to the KeyTip level of its items (including submenus). A menu is a separate popup, so
     * its key input is also watched by the ribbon. When the user closes the menu (click or Esc), KeyTip mode is exited.
     */
    private fun continueInMenu(menu: WMenuFlyout) {
        val entries = menu.addedItems.filter { it is WMenuFlyoutItem || it is WMenuFlyoutSubItem }
        if (entries.isEmpty()) {
            hide()
            return
        }
        clearBadges()
        val push = push@{
            if (!navigator.isActive) return@push
            Xaml.visualRoot(entries.first().inspectable)?.let { owner.attachPopupKeyboard(XamlElement(it)) }
            val depth = navigator.depth
            RibbonMenus.onClosed(menu) { if (navigator.isActive && navigator.depth == depth + 1) hide() }
            pushScope(entries.filter { it.isVisible }.map { MenuEntryTarget(it) }, { menu.hide() }, emptySet())
        }
        if (menu.isOpen) WinUiUtilities.invokeLater(push) else RibbonMenus.onOpened(menu) { WinUiUtilities.invokeLater(push) }
    }

    /** The KeyTip of a menu item (a submenu gets the focus so that it can be opened with Right / Enter). */
    private class MenuEntryTarget(private val item: WMenuFlyoutItemBase) : RibbonKeyTipTarget {
        override val keyTipLabel: String? get() = (item as? WMenuFlyoutItem)?.text ?: (item as? WMenuFlyoutSubItem)?.text
        override val explicitKeyTip: String? get() = RibbonMenus.modelOf(item)?.keyTip
        override val keyTipAnchor: XamlElement = XamlElement(item.inspectable.also { it.addRef() })
        override val isKeyTipEnabled: Boolean get() = item.isEnabled
        override val keyTipModel: com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel? get() = RibbonMenus.modelOf(item)

        override fun onKeyTip(): RibbonKeyTipResult {
            if (item is WMenuFlyoutItem) {
                item.performClick()
            } else {
                keyTipAnchor.focus(com.appkitbox.winui4k.internal.winui.XamlInterop.FocusState_Keyboard)
            }
            return RibbonKeyTipResult.Close
        }
    }

    private fun pushScope(targets: List<RibbonKeyTipTarget>, cleanup: (() -> Unit)?, quickAccess: Set<RibbonKeyTipTarget>) {
        val list = targets.filter { it.keyTipAnchor.isVisible }.distinct()
        val scope = RibbonKeyTipScope<RibbonKeyTipTarget>()
        val qatTips = RibbonKeyTipAssigner.assignQuickAccess(list.count { it in quickAccess })
        val reserved = mutableListOf<String>()
        val others = mutableListOf<RibbonKeyTipTarget>()
        var qatIndex = 0
        for (target in list) {
            if (target in quickAccess) {
                val tip = target.explicitKeyTip?.takeIf { RibbonKeyTipAssigner.normalize(it).isNotEmpty() } ?: qatTips[qatIndex]
                qatIndex++
                reserved += tip
                scope.add(tip, target)
            } else {
                others += target
            }
        }
        val tips = RibbonKeyTipAssigner.assign(others.map { RibbonKeyTipRequest(it.keyTipLabel, it.explicitKeyTip) }, reserved)
        others.forEachIndexed { i, target -> scope.add(tips[i], target) }
        navigator.push(scope)
        cleanups.addLast(cleanup)
        WinUiUtilities.invokeLater { render() }
    }

    private fun clearBadges() {
        canvas.clearChildren()
        badges.clear()
    }

    /**
     * Draws the badges of the current level (at the center of the bottom edge for large items; midway along the left
     * edge for others).
     */
    private fun render() {
        val scope = navigator.current ?: return
        clearBadges()
        val root = owner.keyTipRoot
        val rootSize = Xaml.rootSize(root) ?: return
        canvas.setSize(rootSize[0], rootSize[1])
        canvas.requestedTheme = root.actualTheme
        for ((tip, target) in scope.entries) {
            val anchor = target.keyTipAnchor
            if (!anchor.isVisible || anchor.actualWidth <= 0) continue
            val position = anchor.positionInRoot()
            val badge = XamlElement.load(
                "<Border Background=\"{ThemeResource RibbonKeyTipBackgroundBrush}\" BorderBrush=\"{ThemeResource RibbonKeyTipBorderBrush}\" " +
                    "BorderThickness=\"1\" CornerRadius=\"3\" Padding=\"4,1,4,2\" MinWidth=\"16\"" +
                    (if (target.isKeyTipEnabled) "" else " Opacity=\"0.5\"") + ">" +
                    "<TextBlock Text=\"${Xaml.escape(tip)}\" FontSize=\"11\" FontWeight=\"SemiBold\" HorizontalAlignment=\"Center\" " +
                    "TextLineBounds=\"Tight\" Margin=\"0,2,0,1\" Foreground=\"{ThemeResource RibbonKeyTipForegroundBrush}\" /></Border>",
            )
            canvas.addChild(badge)
            badge.updateLayout()
            val size = badge.desiredSize()
            val (x, y) = if (target.isLargeKeyTip) {
                position[0] + (anchor.actualWidth - size[0]) / 2 to position[1] + anchor.actualHeight - size[1] / 2 - 2
            } else {
                position[0] + 4 to position[1] + anchor.actualHeight / 2 - 1
            }
            badge.setCanvasPosition(
                x.coerceIn(0.0, maxOf(0.0, rootSize[0] - size[0])),
                y.coerceIn(0.0, maxOf(0.0, rootSize[1] - size[1])),
            )
            badges += RibbonKeyTipAssigner.normalize(tip) to badge
        }
        if (!popup.isOpen) popup.show(root)
        updateBadgeVisibility()
    }

    private fun updateBadgeVisibility() {
        val typed = navigator.current?.typed.orEmpty()
        for ((tip, badge) in badges) badge.isVisible = tip.startsWith(typed)
    }

    private fun moveFocusIntoRibbon() {
        val target = owner.keyTipFocusTarget() ?: return
        val root = owner.keyTipRoot.uiElement.getPtrOrNull(com.appkitbox.winui4k.internal.winui.XamlInterop.IUIElement_get_XamlRoot) ?: return
        try {
            focusBefore?.release()
            focusBefore = Xaml.focusedElement(root)
        } finally {
            root.release()
        }
        target.focus(com.appkitbox.winui4k.internal.winui.XamlInterop.FocusState_Programmatic)
    }

    private fun restoreFocus() {
        val before = focusBefore ?: return
        focusBefore = null
        try {
            before.queryInterfaceOrNull(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IUIElement)?.let { ui ->
                XamlElement(ui).focus(com.appkitbox.winui4k.internal.winui.XamlInterop.FocusState_Programmatic)
            }
        } finally {
            before.release()
        }
    }

    internal companion object {
        const val VK_BACK = 8
        const val VK_ESCAPE = 27
        const val VK_F10 = 121

        /** Converts a virtual key to a KeyTip character (letters, digits, numeric keypad). */
        fun keyToChar(key: Int): Char? = when (key) {
            in 'A'.code..'Z'.code -> key.toChar()
            in '0'.code..'9'.code -> key.toChar()
            in VK_NUMPAD0..VK_NUMPAD9 -> ('0'.code + key - VK_NUMPAD0).toChar()
            else -> null
        }

        private const val VK_NUMPAD0 = 96
        private const val VK_NUMPAD9 = 105
    }
}
