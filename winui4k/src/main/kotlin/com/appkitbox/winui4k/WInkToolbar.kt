package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.InkInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.EventObject
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarInitialControls (the buttons placed on the toolbar initially).
 * Values extracted from Microsoft.UI.Xaml.winmd (All=0, None=1, PensOnly=2, AllExceptPens=3).
 */
enum class InkToolbarInitialControls(internal val native: Int) {
    /** All built-in buttons (the default). */
    ALL(0),

    /** No built-in buttons (only the buttons that are added). */
    NONE(1),

    /** Pens only (ballpoint pen, pencil, highlighter). */
    PENS_ONLY(2),

    /** Everything except pens (eraser, stencils, and so on) only. */
    ALL_EXCEPT_PENS(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarInitialControls = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarButtonFlyoutPlacement (the direction in which a button's flyout opens).
 * Values extracted from Microsoft.UI.Xaml.winmd (Auto=0, Top=1, Bottom=2, Left=3, Right=4).
 */
enum class InkToolbarButtonFlyoutPlacement(internal val native: Int) {
    /** Opens automatically in a direction with free space (the default). */
    AUTO(0),

    /** Top. */
    TOP(1),

    /** Bottom. */
    BOTTOM(2),

    /** Left. */
    LEFT(3),

    /** Right. */
    RIGHT(4),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarButtonFlyoutPlacement = entries.first { it.native == native }
    }
}

/**
 * The argument of InkToolbar.IsStencilButtonCheckedChanged: the stencil button that was toggled on or off, and its
 * kind.
 */
class InkToolbarStencilCheckedEvent internal constructor(
    source: WInkToolbar,
    /** The stencil button that was toggled on or off (StencilButton). */
    val stencilButton: WInkToolbarStencilButton?,
    /** The stencil that was shown or hidden (StencilKind). */
    val stencilKind: InkToolbarStencilKind,
) : EventObject(source) {
    /** The toolbar that is the source of the event. */
    override fun getSource(): WInkToolbar = super.getSource() as WInkToolbar
}

/**
 * Like JToolBar: the WinUI 3 InkToolbar (experimental in Windows App SDK 2.5). A toolbar for choosing the pen, pencil,
 * highlighter, eraser, ruler, and protractor used to draw on a [WInkCanvas]. When attached to [targetInkCanvas], the
 * selected tool, color, and size are reflected in the canvas's input processing mode and default drawing attributes
 * ([WInkPresenter.defaultDrawingAttributes]).
 *
 * - Built-in buttons: [initialControls]. Obtained after loading (Loaded) with [getToolButton] / [getToggleButton] /
 *   [getMenuButton]
 * - Adding buttons: [add] (custom pens, tools, toggles, and so on; [WInkToolbarItem])
 * - State: [activeTool] / [inkDrawingAttributes] / [isRulerButtonChecked] / [isStencilButtonChecked]
 * - Appearance: [orientation] / [buttonFlyoutPlacement]
 * - Events: [addActiveToolChangedListener] / [addInkDrawingAttributesChangedListener] /
 *   [addEraseAllClickedListener] / [addStencilCheckedListener]
 *
 * When the eraser's "Erase all ink" is pressed, the model of the target canvas is emptied after listeners are notified
 * (as in WinUI, listeners can still read the lines before they are erased).
 * When a custom pen ([WInkToolbarCustomPenButton.customPen]) is selected, the drawing attributes that the pen creates
 * are applied to [inkDrawingAttributes] and the canvas (a workaround for a bug in the experimental Windows App SDK
 * 2.5.4 where InkToolbar does not call the custom pen).
 */
class WInkToolbar : WControl(
    Activation.composeDefault(InkInterop.CLS_InkToolbar, InkInterop.IID_IInkToolbarFactory), // Default interface = IInkToolbar
) {
    /** The IVector<DependencyObject> view of Children (DependencyObjectCollection). */
    private val children: ComPtr by lazy {
        val collection = inspectable.getPtr(InkInterop.IInkToolbar_get_Children)
        try {
            own(collection.queryInterface(InkInterop.IID_IVector_DependencyObject))
        } finally {
            collection.release()
        }
    }

    /** The buttons added with [add] (in Children order). */
    private val items = mutableListOf<WInkToolbarItem>()

    /** Native button (COM identity) → wrapper. Used to return the same wrapper for the same button. */
    private val wrappers = HashMap<Long, WInkToolbarItem>()

    private val activeToolChangedListeners = mutableListOf<Runnable>()
    private val activeToolChangedAdapters = KotlinListenerAdapters<Runnable>()
    private val inkDrawingAttributesChangedListeners = mutableListOf<Runnable>()
    private val inkDrawingAttributesChangedAdapters = KotlinListenerAdapters<Runnable>()
    private val eraseAllClickedListeners = mutableListOf<Runnable>()
    private val eraseAllClickedAdapters = KotlinListenerAdapters<Runnable>()
    private val stencilCheckedListeners = mutableListOf<Consumer<InkToolbarStencilCheckedEvent>>()
    private val stencilCheckedAdapters = KotlinListenerAdapters<Consumer<InkToolbarStencilCheckedEvent>>()

    init {
        // "Erase all" needs to empty the model after the listeners run, so for all four events
        // there is a single native subscription that dispatches to the Kotlin-side listeners in order
        inspectable.addEventHandler(
            "WinUI4K.InkToolbarActiveToolChangedHandler",
            InkInterop.IID_InkToolbarObjectHandler,
            InkInterop.IInkToolbar_add_ActiveToolChanged,
        ) { _, _ -> onActiveToolChanged() }
        inspectable.addEventHandler(
            "WinUI4K.InkToolbarInkDrawingAttributesChangedHandler",
            InkInterop.IID_InkToolbarObjectHandler,
            InkInterop.IInkToolbar_add_InkDrawingAttributesChanged,
        ) { _, _ -> onInkDrawingAttributesChanged() }
        inspectable.addEventHandler(
            "WinUI4K.InkToolbarEraseAllClickedHandler",
            InkInterop.IID_InkToolbarObjectHandler,
            InkInterop.IInkToolbar_add_EraseAllClicked,
        ) { _, _ -> onEraseAllClicked() }
        inspectable.addEventHandler(
            "WinUI4K.InkToolbarIsStencilButtonCheckedChangedHandler",
            InkInterop.IID_InkToolbarIsStencilButtonCheckedChangedHandler,
            InkInterop.IInkToolbar_add_IsStencilButtonCheckedChanged,
        ) { _, args -> onStencilCheckedChanged(ComPtr(args)) }
    }

    /** The built-in buttons placed initially (InitialControls). */
    var initialControls: InkToolbarInitialControls
        get() = InkToolbarInitialControls.of(inspectable.getInt(InkInterop.IInkToolbar_get_InitialControls))
        set(value) = inspectable.call(InkInterop.IInkToolbar_put_InitialControls, value.native)

    // ------------------------------------------------------------------
    // Children
    // ------------------------------------------------------------------

    /** Adds a button at the end (Children.Append). It is placed alongside the built-in buttons. */
    fun add(item: WInkToolbarItem) {
        children.call(FoundationInterop.IVector_Append, componentOf(item).dependencyObject)
        items += item
        register(item)
    }

    /** Inserts a button at [index] (Children.InsertAt). */
    fun add(item: WInkToolbarItem, index: Int) {
        if (index !in 0..items.size) throw IndexOutOfBoundsException("index=$index, size=${items.size}")
        children.call(FoundationInterop.IVector_InsertAt, index, componentOf(item).dependencyObject)
        items.add(index, item)
        register(item)
    }

    /** Removes a button added with [add]. */
    fun remove(item: WInkToolbarItem) {
        val index = items.indexOf(item)
        if (index < 0) return
        children.call(FoundationInterop.IVector_RemoveAt, index)
        items.removeAt(index)
    }

    /** Removes all buttons added with [add] (Children.Clear). */
    fun removeAll() {
        children.call(FoundationInterop.IVector_Clear)
        items.clear()
    }

    /** The list of buttons added with [add] (Children). */
    fun getItems(): List<WInkToolbarItem> = items.toList()

    private fun componentOf(item: WInkToolbarItem): WComponent = item as WComponent

    private fun register(item: WInkToolbarItem) {
        wrappers[InkNative.identityOf(componentOf(item).inspectable)] = item
    }

    /**
     * The wrapper for the native button [native] (an owned reference). Returns the same one if it was already created.
     */
    private fun <T : WInkToolbarItem> wrap(native: ComPtr, create: (ComPtr) -> T, type: Class<T>): T {
        val identity = InkNative.identityOf(native)
        wrappers[identity]?.let { existing ->
            if (type.isInstance(existing)) {
                native.release()
                return type.cast(existing)
            }
        }
        return create(native).also { wrappers[identity] = it }
    }

    // ------------------------------------------------------------------
    // Built-in buttons
    // ------------------------------------------------------------------

    /**
     * The tool button of kind [tool] (GetToolButton), or null if there is none or it is not loaded yet.
     * Always null for [InkToolbarTool.CUSTOM_PEN] / [InkToolbarTool.CUSTOM_TOOL], since several of them can be placed
     * (by WinUI design; obtain them with [getItems]).
     */
    fun getToolButton(tool: InkToolbarTool): WInkToolbarToolButton? {
        val native = inspectable.getPtrOrNull(InkInterop.IInkToolbar_GetToolButton, tool.native) ?: return null
        return wrap(native, InkToolbarItems::wrapToolButton, WInkToolbarToolButton::class.java)
    }

    /**
     * The toggle button of kind [toggle] (GetToggleButton), or null if there is none or it is not loaded yet.
     * Always null for [InkToolbarToggle.CUSTOM], since several of them can be placed (by WinUI design; obtain them
     * with [getItems]).
     */
    fun getToggleButton(toggle: InkToolbarToggle): WInkToolbarToggleButton? {
        val native = inspectable.getPtrOrNull(InkInterop.IInkToolbar_GetToggleButton, toggle.native) ?: return null
        return wrap(native, InkToolbarItems::wrapToggleButton, WInkToolbarToggleButton::class.java)
    }

    /** The menu button of kind [menu] (GetMenuButton), or null if there is none or it is not loaded yet. */
    fun getMenuButton(menu: InkToolbarMenuKind): WInkToolbarMenuButton? {
        val native = inspectable.getPtrOrNull(InkInterop.IInkToolbar_GetMenuButton, menu.native) ?: return null
        return wrap(native, InkToolbarItems::wrapMenuButton, WInkToolbarMenuButton::class.java)
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** The selected tool (ActiveTool). Null before loading. */
    var activeTool: WInkToolbarToolButton?
        get() {
            val native = inspectable.getPtrOrNull(InkInterop.IInkToolbar_get_ActiveTool) ?: return null
            return wrap(native, InkToolbarItems::wrapToolButton, WInkToolbarToolButton::class.java)
        }
        set(value) {
            if (value == null) {
                inspectable.call(InkInterop.IInkToolbar_put_ActiveTool, null)
                return
            }
            val toolButton = value.inspectable.queryInterface(InkInterop.IID_IInkToolbarToolButton)
            try {
                inspectable.call(InkInterop.IInkToolbar_put_ActiveTool, toolButton)
            } finally {
                toolButton.release()
            }
        }

    /** The drawing attributes of the selected tool, color, and size (InkDrawingAttributes). Null before loading. */
    val inkDrawingAttributes: InkDrawingAttributes?
        get() {
            val native = inspectable.getPtrOrNull(InkInterop.IInkToolbar_get_InkDrawingAttributes) ?: return null
            return try {
                InkNative.readDrawingAttributes(native)
            } finally {
                native.release()
            }
        }

    /** Whether the ruler button is on (IsRulerButtonChecked). */
    var isRulerButtonChecked: Boolean
        get() = inspectable.getBool(InkInterop.IInkToolbar_get_IsRulerButtonChecked)
        set(value) = inspectable.putBool(InkInterop.IInkToolbar_put_IsRulerButtonChecked, value)

    /**
     * Whether the stencil button is on (IsStencilButtonChecked). Turning it on shows the selected stencil on the
     * canvas.
     */
    var isStencilButtonChecked: Boolean
        get() = inspectable.getBool(InkInterop.IInkToolbar_get_IsStencilButtonChecked)
        set(value) = inspectable.putBool(InkInterop.IInkToolbar_put_IsStencilButtonChecked, value)

    /** The direction in which button flyouts open (ButtonFlyoutPlacement). */
    var buttonFlyoutPlacement: InkToolbarButtonFlyoutPlacement
        get() = InkToolbarButtonFlyoutPlacement.of(inspectable.getInt(InkInterop.IInkToolbar_get_ButtonFlyoutPlacement))
        set(value) = inspectable.call(InkInterop.IInkToolbar_put_ButtonFlyoutPlacement, value.native)

    /** The direction in which buttons are laid out (Orientation; horizontal by default). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(InkInterop.IInkToolbar_get_Orientation))
        set(value) = inspectable.call(InkInterop.IInkToolbar_put_Orientation, value.native)

    /** The target canvas (TargetInkCanvas). */
    var targetInkCanvas: WInkCanvas? = null
        set(value) {
            field = value
            inspectable.call(InkInterop.IInkToolbar_put_TargetInkCanvas, value?.inspectable)
        }

    /** The target InkPresenter (TargetInkPresenter). Takes precedence over [targetInkCanvas]. */
    var targetInkPresenter: WInkPresenter? = null
        set(value) {
            field = value
            inspectable.call(InkInterop.IInkToolbar_put_TargetInkPresenter, value?.presenter)
        }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    /** Subscribes to changes of the selected tool (ActiveToolChanged). */
    @JvmSynthetic
    fun addActiveToolChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        activeToolChangedListeners += adapter
        activeToolChangedAdapters.add(listener, adapter)
    }

    @JvmName("addActiveToolChangedListener")
    fun addActiveToolChangedListenerForJava(listener: Runnable) {
        activeToolChangedListeners += listener
    }

    /** Removes a listener registered with [addActiveToolChangedListener]. */
    @JvmSynthetic
    fun removeActiveToolChangedListener(listener: () -> Unit) {
        activeToolChangedAdapters.remove(listener)?.let { activeToolChangedListeners -= it }
    }

    @JvmName("removeActiveToolChangedListener")
    fun removeActiveToolChangedListenerForJava(listener: Runnable) {
        activeToolChangedListeners -= listener
    }

    /** Subscribes to changes of the drawing attributes (color, size, tool) (InkDrawingAttributesChanged). */
    @JvmSynthetic
    fun addInkDrawingAttributesChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        inkDrawingAttributesChangedListeners += adapter
        inkDrawingAttributesChangedAdapters.add(listener, adapter)
    }

    @JvmName("addInkDrawingAttributesChangedListener")
    fun addInkDrawingAttributesChangedListenerForJava(listener: Runnable) {
        inkDrawingAttributesChangedListeners += listener
    }

    /** Removes a listener registered with [addInkDrawingAttributesChangedListener]. */
    @JvmSynthetic
    fun removeInkDrawingAttributesChangedListener(listener: () -> Unit) {
        inkDrawingAttributesChangedAdapters.remove(listener)?.let { inkDrawingAttributesChangedListeners -= it }
    }

    @JvmName("removeInkDrawingAttributesChangedListener")
    fun removeInkDrawingAttributesChangedListenerForJava(listener: Runnable) {
        inkDrawingAttributesChangedListeners -= listener
    }

    /**
     * Subscribes to presses of the eraser's "Erase all ink" (EraseAllClicked).
     * Listeners are called before the lines on the target canvas are erased (so the lines being erased can be
     * recorded for undo).
     */
    @JvmSynthetic
    fun addEraseAllClickedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        eraseAllClickedListeners += adapter
        eraseAllClickedAdapters.add(listener, adapter)
    }

    @JvmName("addEraseAllClickedListener")
    fun addEraseAllClickedListenerForJava(listener: Runnable) {
        eraseAllClickedListeners += listener
    }

    /** Removes a listener registered with [addEraseAllClickedListener]. */
    @JvmSynthetic
    fun removeEraseAllClickedListener(listener: () -> Unit) {
        eraseAllClickedAdapters.remove(listener)?.let { eraseAllClickedListeners -= it }
    }

    @JvmName("removeEraseAllClickedListener")
    fun removeEraseAllClickedListenerForJava(listener: Runnable) {
        eraseAllClickedListeners -= listener
    }

    /** Subscribes to the stencil button being toggled on or off (IsStencilButtonCheckedChanged). */
    @JvmSynthetic
    fun addStencilCheckedListener(listener: (InkToolbarStencilCheckedEvent) -> Unit) {
        val adapter = Consumer<InkToolbarStencilCheckedEvent> { listener(it) }
        stencilCheckedListeners += adapter
        stencilCheckedAdapters.add(listener, adapter)
    }

    @JvmName("addStencilCheckedListener")
    fun addStencilCheckedListenerForJava(listener: Consumer<InkToolbarStencilCheckedEvent>) {
        stencilCheckedListeners += listener
    }

    /** Removes a listener registered with [addStencilCheckedListener]. */
    @JvmSynthetic
    fun removeStencilCheckedListener(listener: (InkToolbarStencilCheckedEvent) -> Unit) {
        stencilCheckedAdapters.remove(listener)?.let { stencilCheckedListeners -= it }
    }

    @JvmName("removeStencilCheckedListener")
    fun removeStencilCheckedListenerForJava(listener: Consumer<InkToolbarStencilCheckedEvent>) {
        stencilCheckedListeners -= listener
    }

    private fun onActiveToolChanged() {
        applyCustomPen()
        activeToolChangedListeners.toList().forEach { it.run() }
    }

    private fun onInkDrawingAttributesChanged() {
        // If replaced, listeners are notified in the InkDrawingAttributesChanged that arrives again because of the
        // replacement
        if (applyCustomPen()) return
        inkDrawingAttributesChangedListeners.toList().forEach { it.run() }
    }

    /**
     * Whether [applyCustomPen] is in the middle of replacing InkDrawingAttributes (prevents reentry from the
     * notification caused by the replacement).
     */
    private var applyingCustomPen = false

    /**
     * If the selected tool is a [WInkToolbarCustomPenButton] with a [WInkToolbarCustomPen], replaces
     * InkToolbar.InkDrawingAttributes with that pen's drawing attributes (once replaced, the toolbar applies them to
     * the target canvas). Returns true if replaced.
     *
     * In the experimental Windows App SDK 2.5.4, even when a custom pen button is selected, InkToolbar creates the
     * drawing attributes from only the tool kind, color, and size, and does not call InkToolbarCustomPen (a WinUI bug),
     * so this compensates for it.
     */
    private fun applyCustomPen(): Boolean {
        if (applyingCustomPen) return false
        val button = activeTool as? WInkToolbarCustomPenButton ?: return false
        val pen = button.customPen ?: return false
        val attributes = pen.createInkDrawingAttributes(button.selectedColor, button.selectedStrokeWidth)
        if (inkDrawingAttributes == attributes) return false
        val native = InkNative.createDrawingAttributes(attributes)
        applyingCustomPen = true
        try {
            dependencyObject.call(XamlInterop.IDependencyObject_SetValue, inkDrawingAttributesProperty, native)
        } finally {
            applyingCustomPen = false
            native.release()
        }
        return true
    }

    /** EraseAllClicked: notifies listeners and then empties the model of the target canvas. */
    internal fun onEraseAllClicked() {
        eraseAllClickedListeners.toList().forEach { it.run() }
        val canvas = targetInkPresenter?.inkCanvas ?: targetInkCanvas ?: return
        if (!canvas.inkPresenter.isCustomDryingActive) canvas.model.clear()
    }

    private fun onStencilCheckedChanged(args: ComPtr) {
        val button = args.getPtrOrNull(InkInterop.IInkToolbarIsStencilButtonCheckedChangedEventArgs_get_StencilButton)
        val stencilButton = button?.let {
            wrap(it, InkToolbarItems::wrapMenuButton, WInkToolbarMenuButton::class.java) as? WInkToolbarStencilButton
        }
        val kind = InkToolbarStencilKind.of(
            args.getInt(InkInterop.IInkToolbarIsStencilButtonCheckedChangedEventArgs_get_StencilKind),
        )
        val event = InkToolbarStencilCheckedEvent(this, stencilButton, kind)
        stencilCheckedListeners.toList().forEach { it.accept(event) }
    }

    private companion object {
        /**
         * InkToolbar.InkDrawingAttributesProperty (the dependency property identifier; kept for the lifetime of the
         * process).
         */
        val inkDrawingAttributesProperty: ComPtr by lazy {
            val statics = Activation.factory(InkInterop.CLS_InkToolbar, InkInterop.IID_IInkToolbarStatics)
            try {
                statics.getPtr(InkInterop.IInkToolbarStatics_get_InkDrawingAttributesProperty)
            } finally {
                statics.release()
            }
        }
    }
}
