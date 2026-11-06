package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A helper for tests (Kotlin and Java) that looks at and operates a ContentDialog open in a window from outside. Used to test APIs that,
 * like [WOptionPane], do not return the dialog's wrapper. The dialog is searched for as a child of the open Popups of the owner window's XamlRoot
 * (VisualTreeHelper.GetOpenPopupsForXamlRoot). Can be called from outside the UI thread (it hands off to the UI thread internally).
 */
internal object OpenContentDialogs {
    /** How an open dialog looks. */
    data class Description(
        val title: String,
        val primary: String,
        val secondary: String,
        val close: String,
        val defaultButton: ContentDialogButton,
        /** The non-empty TextBlock texts in the content (in visual tree order; excluding the empty text of icon-only WLabels and the glyphs in FontIcons). */
        val texts: List<String>,
        /** The glyphs of the FontIcons in the content. */
        val glyphs: List<String>,
    )

    /** Whether [component] is Loaded (FrameworkElement.IsLoaded). */
    @JvmStatic
    fun isLoaded(component: WComponent): Boolean = onUiThreadGet { isLoadedOnUi(component) }

    /** [isLoaded] called on the UI thread. */
    fun isLoadedOnUi(component: WComponent): Boolean = component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)

    /** Whether a Loaded dialog is open in [owner]'s window. */
    @JvmStatic
    fun isOpen(owner: WComponent): Boolean = onUiThreadGet {
        withDialog(owner) { dialog -> isLoadedElement(dialog) } ?: false
    }

    /** How the dialog open in [owner]'s window looks. Null if none is open. */
    @JvmStatic
    fun describe(owner: WComponent): Description? = onUiThreadGet {
        withDialog(owner) { dialog ->
            val texts = mutableListOf<String>()
            val glyphs = mutableListOf<String>()
            content(dialog)?.let { content -> walk(content) { element -> collect(element, texts, glyphs) } }
            Description(
                title = title(dialog),
                primary = dialog.getString(XamlInterop.IContentDialog_get_PrimaryButtonText),
                secondary = dialog.getString(XamlInterop.IContentDialog_get_SecondaryButtonText),
                close = dialog.getString(XamlInterop.IContentDialog_get_CloseButtonText),
                defaultButton = ContentDialogButton.of(dialog.getInt(XamlInterop.IContentDialog_get_DefaultButton)),
                texts = texts - glyphs.toSet(), // The TextBlocks inside FontIcons show the glyphs
                glyphs = glyphs,
            )
        }
    }

    /** A native weak reference to the dialog open in [owner]'s window (to check its destruction). Null if none is open. */
    @JvmStatic
    fun probe(owner: WComponent): NativeProbe? = onUiThreadGet { withDialog(owner) { dialog -> NativeProbe(dialog) } }

    /** Presses [partName] ("PrimaryButton", "SecondaryButton", "CloseButton") of the template of the dialog in [owner]'s window. */
    @JvmStatic
    fun press(owner: WComponent, partName: String) = onUiThread {
        val done = withDialog(owner) { dialog ->
            val button = findNamed(dialog, partName) ?: error("the dialog has no $partName")
            try {
                Xaml.invoke(button)
            } finally {
                button.release()
            }
        }
        checkNotNull(done) { "no dialog is open" }
    }

    /** Sets the text of the first input field (TextBox) in the dialog's content to [text]. */
    @JvmStatic
    fun setText(owner: WComponent, text: String) = onUiThread {
        val done = withDialog(owner) { dialog ->
            val box = firstOf(dialog, XamlInterop.IID_ITextBox) ?: error("the dialog has no input field")
            try {
                Hstring.use(text) { h -> box.call(XamlInterop.ITextBox_put_Text, h) }
            } finally {
                box.release()
            }
        }
        checkNotNull(done) { "no dialog is open" }
    }

    /** Selects the [index]th item of the first combo box in the dialog's content. */
    @JvmStatic
    fun select(owner: WComponent, index: Int) = onUiThread {
        val done = withDialog(owner) { dialog ->
            val combo = firstOf(dialog, XamlInterop.IID_IComboBox) ?: error("the dialog has no combo box")
            val selector = combo.queryInterface(XamlInterop.IID_ISelector)
            try {
                selector.call(XamlInterop.ISelector_put_SelectedIndex, index)
            } finally {
                selector.release()
                combo.release()
            }
        }
        checkNotNull(done) { "no dialog is open" }
    }

    /** Moves the keyboard focus to the first combo box in the dialog content (so that it can be opened with keys). */
    @JvmStatic
    fun focusComboBox(owner: WComponent) = onUiThread {
        val done = withDialog(owner) { dialog ->
            val combo = firstOf(dialog, XamlInterop.IID_IComboBox) ?: error("the dialog has no combo box")
            val element = combo.queryInterface(XamlInterop.IID_IUIElement)
            try {
                Ffi.backend.withScope { scope ->
                    val out = scope.allocate(1, 1)
                    element.call(XamlInterop.IUIElement_Focus, XamlInterop.FocusState_Keyboard, out)
                    Ffi.backend.memory.getByte(out, 0) != 0.toByte()
                }
            } finally {
                element.release()
                combo.release()
            }
        }
        check(done == true) { "Cannot move the focus to the combo box" }
    }

    /** Whether the drop-down of the first combo box in the dialog content is open (ComboBox.IsDropDownOpen). null if there is no dialog. */
    @JvmStatic
    fun isDropDownOpen(owner: WComponent): Boolean? = onUiThreadGet {
        withDialog(owner) { dialog ->
            val combo = firstOf(dialog, XamlInterop.IID_IComboBox) ?: error("the dialog has no combo box")
            try {
                combo.getBool(XamlInterop.IComboBox_get_IsDropDownOpen)
            } finally {
                combo.release()
            }
        }
    }

    /** Lends [block] the first ContentDialog (IContentDialog) among the children of the Popups open in [owner]'s window. Null if none. */
    private fun <T> withDialog(owner: WComponent, block: (ComPtr) -> T): T? {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val popups = try {
            statics.getPtr(XamlInterop.IVisualTreeHelperStatics_GetOpenPopupsForXamlRoot, root.ptr)
        } finally {
            statics.release()
            root.release()
        }
        try {
            for (index in 0 until popups.getInt(FoundationInterop.IVectorView_get_Size)) {
                val dialog = dialogAt(popups, index) ?: continue
                return try {
                    block(dialog)
                } finally {
                    dialog.release()
                }
            }
            return null
        } finally {
            popups.release()
        }
    }

    /** If the child at [index] of the open Popups [popups] is a ContentDialog, its IContentDialog (the caller releases it). */
    private fun dialogAt(popups: ComPtr, index: Int): ComPtr? {
        val popup = popups.getPtr(FoundationInterop.IVectorView_GetAt, index)
        val child = try {
            popup.getPtrOrNull(XamlInterop.IPopup_get_Child)
        } finally {
            popup.release()
        } ?: return null
        return try {
            child.queryInterfaceOrNull(XamlInterop.IID_IContentDialog)
        } finally {
            child.release()
        }
    }

    private fun isLoadedElement(element: ComPtr): Boolean {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        return try {
            framework.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)
        } finally {
            framework.release()
        }
    }

    private fun title(dialog: ComPtr): String {
        val boxed = dialog.getPtrOrNull(XamlInterop.IContentDialog_get_Title) ?: return ""
        return try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    private fun content(dialog: ComPtr): ComPtr? {
        val contentControl = dialog.queryInterface(XamlInterop.IID_IContentControl)
        return try {
            contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content)
        } finally {
            contentControl.release()
        }
    }

    private fun collect(element: ComPtr, texts: MutableList<String>, glyphs: MutableList<String>): Boolean {
        element.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { block ->
            try {
                block.getString(XamlInterop.ITextBlock_get_Text).takeIf { it.isNotEmpty() }?.let { texts += it }
            } finally {
                block.release()
            }
        }
        element.queryInterfaceOrNull(XamlInterop.IID_IFontIcon)?.let { icon ->
            try {
                glyphs += icon.getString(XamlInterop.IFontIcon_get_Glyph)
            } finally {
                icon.release()
            }
        }
        return false
    }

    /** The first element named [name] (FrameworkElement.Name) found depth-first from [root] (the caller releases it). */
    private fun findNamed(root: ComPtr, name: String): ComPtr? {
        var found: ComPtr? = null
        walk(root) { element ->
            val framework = element.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement)
            val matched = framework?.let {
                try {
                    it.getString(XamlInterop.IFrameworkElement_get_Name) == name
                } finally {
                    it.release()
                }
            } ?: false
            if (matched) found = element.queryInterface(XamlInterop.IID_IUIElement)
            matched
        }
        return found
    }

    /** The view for [iid] of the first element having [iid] found depth-first from [root] (the caller releases it). */
    private fun firstOf(root: ComPtr, iid: String): ComPtr? {
        var found: ComPtr? = null
        walk(root) { element ->
            found = element.queryInterfaceOrNull(iid)
            found != null
        }
        return found
    }

    /** Walks [root] (borrowed) and its descendants depth-first. Stops when [visit] returns true. True if stopped. */
    private fun walk(root: ComPtr, visit: (ComPtr) -> Boolean): Boolean {
        if (visit(root)) return true
        for (index in 0 until Xaml.childCount(root)) {
            val child = Xaml.childAt(root, index) ?: continue
            try {
                if (walk(child, visit)) return true
            } finally {
                child.release()
            }
        }
        return false
    }
}
