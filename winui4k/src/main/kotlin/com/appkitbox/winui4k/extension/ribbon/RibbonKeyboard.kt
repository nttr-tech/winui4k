package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.XamlKeyEvent
import com.appkitbox.winui4k.XamlPointerEvent
import com.appkitbox.winui4k.extension.ribbon.model.RibbonKeyGesture
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModifierKeys
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The ribbon's keyboard handling (the key handling and shortcut routing of Ribbon.Keyboard in RibbonSpace).
 *
 * Watches key input on the window content (XamlRoot.Content) and the ribbon's popups. Pressing and releasing Alt alone or
 * F10 shows KeyTips, Ctrl+F1 toggles minimization, Alt+Q opens search, and Esc closes the Backstage. An item's shortcut
 * ("Ctrl+B") runs that item wherever the focus is (keys without modifiers are not routed inside text input).
 *
 * Keys during KeyTip mode are received in PreviewKeyDown (parents first) and handled before the focused control.
 * Pressing anywhere in the window (even on a button) dismisses the KeyTips, and a ribbon temporarily revealed in full-screen
 * mode is hidden when the user presses outside the ribbon (on the document).
 */
internal class RibbonKeyboard(private val ribbon: WRibbon) {
    private var root: XamlElement? = null
    private val tokens = mutableListOf<Pair<Int, Long>>()
    private val popupRoots = mutableListOf<XamlElement>()
    private var altAlone = false
    private var shortcuts: List<Pair<RibbonKeyGesture, com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel>>? = null
    private var removePointerHandler: (() -> Unit)? = null

    /** Starts watching key input on the window content (on load). */
    fun attach() {
        val xamlRoot = ribbon.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return
        val content = try {
            xamlRoot.getPtrOrNull(XamlInterop.IXamlRoot_get_Content)
        } finally {
            xamlRoot.release()
        } ?: return
        val current = root
        if (current != null && Xaml.sameObject(current.inspectable, content)) {
            content.release()
            return
        }
        detach()
        val element = XamlElement(content)
        root = element
        tokens += XamlInterop.IUIElement_remove_PreviewKeyDown to element.onPreviewKeyDown { onPreviewKeyDown(it) }
        tokens += XamlInterop.IUIElement_remove_KeyDown to element.onKeyDown { onKeyDown(it) }
        tokens += XamlInterop.IUIElement_remove_PreviewKeyUp to element.onPreviewKeyUp { onPreviewKeyUp(it) }
        removePointerHandler = element.onPointerHandledToo(XamlInterop.IUIElementStatics_get_PointerPressedEvent) { onRootPointerPressed(it) }
    }

    /** Stops watching key input (on unload). */
    fun detach() {
        val element = root ?: return
        tokens.forEach { (slot, token) -> element.removeUiHandler(slot, token) }
        tokens.clear()
        removePointerHandler?.invoke()
        removePointerHandler = null
        root = null
        popupRoots.clear()
    }

    /** Also watches key input in a popup (outside the window content). */
    fun attachPopup(element: XamlElement) {
        if (popupRoots.any { it === element }) return
        popupRoots += element
        element.onPreviewKeyDown { onPreviewKeyDown(it) }
        element.onKeyDown { onKeyDown(it) }
        element.onPreviewKeyUp { onPreviewKeyUp(it) }
    }

    /** Something in the window was pressed (events that a child marked handled also arrive). */
    private fun onRootPointerPressed(e: XamlPointerEvent) {
        altAlone = false
        if (ribbon.keyTips.isActive) ribbon.cancelKeyTips()
        if (ribbon.isFullScreenRevealed && !isWithin(e, ribbon)) ribbon.isFullScreenRevealed = false
    }

    private fun isWithin(e: XamlPointerEvent, component: WComponent): Boolean {
        val (x, y) = e.position(component).let { it[0] to it[1] }
        return x >= 0 && y >= 0 && x < component.actualWidth && y < component.actualHeight
    }

    private fun onPreviewKeyDown(e: XamlKeyEvent) {
        if (e.key == VK_MENU) {
            altAlone = true
            return
        }
        altAlone = false
        if (ribbon.keyTips.isActive) e.handled = ribbon.keyTips.handleKey(e.key)
    }

    private fun onPreviewKeyUp(e: XamlKeyEvent) {
        if (e.key != VK_MENU || !altAlone || !ribbon.isKeyTipsEnabled) return
        altAlone = false
        if (ribbon.keyTips.isActive) {
            ribbon.cancelKeyTips()
            e.handled = true
        } else if (ribbon.isVisible && ribbon.actualWidth > 0) {
            ribbon.showKeyTips()
            e.handled = true
        }
    }

    @Suppress("CyclomaticComplexMethod") // Routes the keys the ribbon handles (Ctrl+F1, F10, Alt+Q, Esc, shortcuts) in one place
    private fun onKeyDown(e: XamlKeyEvent) {
        if (e.handled || ribbon.keyTips.isActive || !ribbon.isVisible) return
        val modifiers = currentModifiers()
        when {
            e.key == VK_F1 && modifiers == RibbonModifierKeys.CONTROL && ribbon.isCollapsible -> ribbon.toggleMinimized()
            e.key == VK_F10 && modifiers == RibbonModifierKeys.NONE && ribbon.isKeyTipsEnabled -> ribbon.showKeyTips()
            e.key == VK_Q && modifiers == RibbonModifierKeys.ALT && ribbon.fireSearchRequested() -> Unit
            e.key == VK_ESCAPE && ribbon.isBackstageOpen -> ribbon.isBackstageOpen = false
            ribbon.isShortcutRoutingEnabled && routeShortcut(e.key, modifiers) -> Unit
            else -> return
        }
        e.handled = true
    }

    /** Discards the shortcut cache. */
    fun invalidateShortcuts() {
        shortcuts = null
    }

    private fun routeShortcut(key: Int, modifiers: Int): Boolean {
        if (!isRoutable(key, modifiers)) return false
        val name = keyName(key) ?: return false
        val table = shortcuts ?: buildShortcuts().also { shortcuts = it }
        // If several items have the same shortcut (such as the same command on two tabs), the first one that can run wins
        return table.asSequence()
            .filter { (gesture, _) -> gesture.matches(name, modifiers) }
            .mapNotNull { (_, item) -> ribbon.host.findView(item) }
            .any { view -> isReachable(view) && view.invoke() }
    }

    /**
     * Whether the key may be routed (without modifiers only F keys; inside text input, Shift-only keys are not routed
     * either).
     */
    private fun isRoutable(key: Int, modifiers: Int): Boolean {
        val isFunctionKey = key in VK_F1..VK_F24
        if (modifiers == RibbonModifierKeys.NONE && !isFunctionKey) return false
        val plain = modifiers == RibbonModifierKeys.NONE || modifiers == RibbonModifierKeys.SHIFT
        return !(plain && isTextInputFocused())
    }

    private fun buildShortcuts(): List<Pair<RibbonKeyGesture, com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel>> =
        ribbon.allItemModels().flatMap { item ->
            item.shortcut.orEmpty().split(',').mapNotNull { text -> RibbonKeyGesture.tryParse(text.trim())?.let { it to item } }
        }

    /** Whether it is an item of a shown tab and a shown group (items of unselected tabs can also run). */
    private fun isReachable(view: RibbonItemView): Boolean {
        if (!view.isEffectivelyEnabled || !view.model.isVisible) return false
        val group = ribbon.host.groupOf(view) ?: return true
        if (!group.model.isVisible || group.isHiddenByCustomization) return false
        val tab = ribbon.tabViews.entries.firstOrNull { (_, tabView) -> group in tabView.groupViews() }?.key ?: return true
        return tab in ribbon.visibleTabs || ribbon.model.visibilityMode == RibbonVisibilityMode.FULL_SCREEN
    }

    private fun isTextInputFocused(): Boolean {
        val xamlRoot = ribbon.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        val focused = try {
            Xaml.focusedElement(xamlRoot)
        } finally {
            xamlRoot.release()
        } ?: return false
        return try {
            TEXT_INPUT_IIDS.any { iid -> focused.queryInterfaceOrNull(iid)?.also(ComPtr::release) != null }
        } finally {
            focused.release()
        }
    }

    internal companion object {
        const val VK_MENU = 18
        const val VK_ESCAPE = 27
        const val VK_Q = 81
        const val VK_F1 = 112
        const val VK_F10 = 121
        const val VK_F24 = 135

        private val TEXT_INPUT_IIDS = listOf(
            XamlInterop.IID_ITextBox,
            XamlInterop.IID_IPasswordBox,
            XamlInterop.IID_IRichEditBox,
            XamlInterop.IID_IAutoSuggestBox,
        )

        /** The modifier keys being pressed (bits of [RibbonModifierKeys]). */
        fun currentModifiers(): Int {
            val pressed = Xaml.currentModifiers()
            var modifiers = RibbonModifierKeys.NONE
            if (pressed and Xaml.MODIFIER_CONTROL != 0) modifiers = modifiers or RibbonModifierKeys.CONTROL
            if (pressed and Xaml.MODIFIER_SHIFT != 0) modifiers = modifiers or RibbonModifierKeys.SHIFT
            if (pressed and Xaml.MODIFIER_MENU != 0) modifiers = modifiers or RibbonModifierKeys.ALT
            if (pressed and Xaml.MODIFIER_WINDOWS != 0) modifiers = modifiers or RibbonModifierKeys.META
            return modifiers
        }

        /** Converts a virtual-key code to a [RibbonKeyGesture] key name (the WinRT VirtualKey name). */
        @Suppress("CyclomaticComplexMethod") // A table of names for each range of virtual-key codes
        fun keyName(key: Int): String? = when (key) {
            in 'A'.code..'Z'.code -> key.toChar().toString()
            in '0'.code..'9'.code -> "Number" + key.toChar()
            in VK_NUMPAD0..VK_NUMPAD9 -> "NumberPad" + (key - VK_NUMPAD0)
            in VK_F1..VK_F24 -> "F" + (key - VK_F1 + 1)
            else -> NAMED_KEYS[key]
        }

        private const val VK_NUMPAD0 = 96
        private const val VK_NUMPAD9 = 105

        private val NAMED_KEYS: Map<Int, String> = mapOf(
            8 to "Back", 9 to "Tab", 13 to "Enter", 27 to "Escape", 32 to "Space", 33 to "PageUp", 34 to "PageDown",
            35 to "End", 36 to "Home", 37 to "Left", 38 to "Up", 39 to "Right", 40 to "Down", 45 to "Insert", 46 to "Delete",
            106 to "Multiply", 107 to "Add", 109 to "Subtract", 110 to "Decimal", 111 to "Divide",
            186 to "OemSemicolon", 187 to "Add", 188 to "OemComma", 189 to "Subtract", 190 to "OemPeriod", 191 to "OemQuestion",
            192 to "OemTilde", 219 to "OemOpenBracket", 220 to "OemBackslash", 221 to "OemCloseBracket", 222 to "OemQuote",
        )
    }
}
