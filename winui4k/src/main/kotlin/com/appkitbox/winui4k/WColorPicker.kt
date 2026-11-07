package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.ColorSpectrumShape (the spectrum's shape).
 * Values extracted from the winmd (Box=0, Ring=1).
 */
enum class ColorSpectrumShape(internal val native: Int) {
    /** A rectangle (default). */
    BOX(0),

    /** A ring. */
    RING(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ColorSpectrumShape = entries.first { it.native == native }
    }
}

/**
 * JColorChooser-like: WinUI 3's ColorPicker.
 *
 * Has [color] / [isAlphaEnabled] / [isMoreButtonVisible] / [isHexInputVisible] / [spectrumShape]. Color changes can be
 * received, as in Swing, with a ChangeListener on [selectionModel] ([WColorSelectionModel]) (the source is the model). Sharing the model
 * among several pickers links their colors. The existing [addChangeListener] / [removeChangeListener] (which subscribe to ColorChanged directly
 * and pass the new color) can still be used. JColorChooser has no `addChangeListener` on the picker itself, so a [WChangeListener]
 * form is not added (adding it would make Java's `value -> ...` lambdas ambiguous).
 *
 * To let the user choose a color in a dialog, use [showDialog] (waits until closed; the same shape as JColorChooser.showDialog) or
 * the callback form of [showDialog].
 */
class WColorPicker() : WControl(
    Activation.composeDefault(XamlInterop.CLS_ColorPicker, XamlInterop.IID_IColorPickerFactory),
) {
    /** JColorChooser(Color)-like: creates it with the color [initial] selected. */
    constructor(initial: WColor) : this() {
        color = initial
    }

    /** JColorChooser(ColorSelectionModel)-like: creates it with [model] as the color model (the picker's color becomes the model's color). */
    constructor(model: WColorSelectionModel) : this() {
        selectionModel = model
    }

    /** ColorChanged event tokens registered via addChangeListener. */
    private val changeTokens = ListenerTokens<Consumer<WColor>>()

    /** The model once [selectionModel] has been used (not created until used). */
    private var model: WColorSelectionModel? = null

    /**
     * A listener that copies model changes to native. The model references the picker strongly through it, but the native side references
     * the picker only weakly ([addModelHandler]), so no cycle forms across the boundary (same as WSlider's model).
     */
    private val modelListener = WChangeListener { pushToNative() }

    /** Whether the model's color is being copied to native (native ColorChanged during that time is not written back to the model). */
    private var pushing = false

    /** The selected color (ColorPicker.Color). A Windows.UI.Color struct is passed by value. */
    var color: WColor
        get() {
            val (a, r, g, b) = XamlStructs.getColor(inspectable, XamlInterop.IColorPicker_get_Color)
            return WColor(r, g, b, a)
        }
        set(value) = XamlStructs.putColor(
            inspectable,
            XamlInterop.IColorPicker_put_Color,
            value.alpha,
            value.red,
            value.green,
            value.blue,
        )

    /**
     * Like JColorChooser.setColor(int, int, int): selects an opaque color from red, green and blue components (0..255) (like setting [color],
     * [selectionModel] also changes).
     *
     * @throws IllegalArgumentException when a component is outside 0..255 (as in the java.awt.Color constructor). The color does not change
     */
    fun setColor(red: Int, green: Int, blue: Int) {
        require(red in 0..MAX_COMPONENT && green in 0..MAX_COMPONENT && blue in 0..MAX_COMPONENT) {
            "Color parameter outside of expected range: red=$red, green=$green, blue=$blue"
        }
        color = WColor(red, green, blue)
    }

    /**
     * Like JColorChooser.setColor(int): selects an opaque color from a `0xRRGGBB` RGB value. As in java.awt.Color(int), only the lower 24 bits are used and
     * the upper 8 bits (the alpha position) are ignored.
     */
    fun setColor(rgb: Int) {
        color = WColor((rgb shr RED_SHIFT) and MAX_COMPONENT, (rgb shr GREEN_SHIFT) and MAX_COMPONENT, rgb and MAX_COMPONENT)
    }

    /** Whether alpha (opacity) can be adjusted (ColorPicker.IsAlphaEnabled). */
    var isAlphaEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IColorPicker_get_IsAlphaEnabled)
        set(value) = inspectable.putBool(XamlInterop.IColorPicker_put_IsAlphaEnabled, value)

    /** Whether to show the "more" button that expands the text input fields (ColorPicker.IsMoreButtonVisible). */
    var isMoreButtonVisible: Boolean
        get() = inspectable.getBool(XamlInterop.IColorPicker_get_IsMoreButtonVisible)
        set(value) = inspectable.putBool(XamlInterop.IColorPicker_put_IsMoreButtonVisible, value)

    /** Whether to show the hex input field (ColorPicker.IsHexInputVisible). */
    var isHexInputVisible: Boolean
        get() = inspectable.getBool(XamlInterop.IColorPicker_get_IsHexInputVisible)
        set(value) = inspectable.putBool(XamlInterop.IColorPicker_put_IsHexInputVisible, value)

    /**
     * JColorChooser.getSelectionModel / setSelectionModel-like: the model of the selected color. When first read, creates a [WDefaultColorSelectionModel]
     * holding the picker's current color. When the picker's color changes (by user operation or by setting [color]), the model's
     * [WColorSelectionModel.selectedColor] changes, and when the model's color changes, the picker's color changes.
     * When replaced, the picker's color is matched to the new model's color (as in JColorChooser, the old model's listeners are not called then).
     * The model references the picker through its listener, so holding on to the model keeps the picker from being collected too (as in Swing).
     */
    var selectionModel: WColorSelectionModel
        get() = model ?: WDefaultColorSelectionModel(color).also { attach(it) }
        set(newModel) {
            val old = model
            if (newModel === old) return
            old?.removeChangeListener(modelListener)
            attach(newModel)
            pushToNative()
        }

    /** The spectrum's shape (ColorPicker.ColorSpectrumShape). */
    var spectrumShape: ColorSpectrumShape
        get() = ColorSpectrumShape.of(inspectable.getInt(XamlInterop.IColorPicker_get_ColorSpectrumShape))
        set(value) = inspectable.call(XamlInterop.IColorPicker_put_ColorSpectrumShape, value.native)

    /**
     * ChangeListener-like: subscribes to color changes. The listener receives the new color.
     * Subscribes to ColorPicker.ColorChanged (TypedEventHandler<ColorPicker, ColorChangedEventArgs>) under the hood.
     */
    @JvmSynthetic
    fun addChangeListener(listener: (WColor) -> Unit) {
        val adapter = Consumer<WColor> { listener(it) }
        addChangeListenerForJava(adapter)
        changeTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addChangeListener")
    fun addChangeListenerForJava(listener: Consumer<WColor>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ColorChangedHandler",
            XamlInterop.IID_ColorPickerColorChangedHandler,
            XamlInterop.IColorPicker_add_ColorChanged,
        ) { _, args ->
            // args is a ColorChangedEventArgs; read NewColor (an out struct) and pass it along
            val (a, r, g, b) = XamlStructs.getColor(ComPtr(args), XamlInterop.IColorChangedEventArgs_get_NewColor)
            listener.accept(WColor(r, g, b, a))
        }
        changeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    @JvmSynthetic
    fun removeChangeListener(listener: (WColor) -> Unit) {
        val adapter = changeTokens.removeKotlinAdapter(listener) ?: return
        removeChangeListenerForJava(adapter)
    }

    @JvmName("removeChangeListener")
    fun removeChangeListenerForJava(listener: Consumer<WColor>) {
        val token = changeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IColorPicker_remove_ColorChanged, token)
    }

    /** Starts using [newModel]. The first time, subscribes to native ColorChanged, and holds the picker strongly with ListenerAnchor while shown. */
    private fun attach(newModel: WColorSelectionModel) {
        if (model == null) {
            listenerAnchor.listenerAdded()
            addModelHandler()
        }
        model = newModel
        newModel.addChangeListener(modelListener)
    }

    /** Copies the model's color to native (does nothing if the color is the same). */
    private fun pushToNative() {
        val selected = model?.selectedColor ?: return
        if (selected == color) return
        val wasPushing = pushing
        pushing = true
        try {
            color = selected
        } finally {
            pushing = wasPushing
        }
    }

    /** Writes the native color changed by user operations etc. back to the model. */
    private fun onNativeColorChanged(newColor: WColor) {
        if (pushing) return
        model?.selectedColor = newColor
    }

    private fun addModelHandler() {
        // The handler stays on the native side, so it references the picker weakly (ListenerAnchor holds it strongly while shown)
        val picker = WeakReference(this)
        inspectable.addEventHandler(
            "WinUI4K.ColorPickerModelHandler",
            XamlInterop.IID_ColorPickerColorChangedHandler,
            XamlInterop.IColorPicker_add_ColorChanged,
        ) { _, args ->
            val (a, r, g, b) = XamlStructs.getColor(ComPtr(args), XamlInterop.IColorChangedEventArgs_get_NewColor)
            picker.get()?.onNativeColorChanged(WColor(r, g, b, a))
        }
    }

    companion object {
        private const val MAX_COMPONENT = 0xFF
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8

        /**
         * JColorChooser.showDialog-like: shows a color picker in a dialog (ContentDialog), blocks the caller until it is closed, and
         * returns the selected color for OK, or null for Cancel, Esc or when the owner window is closed.
         * Starts from white if [initial] is null. How it blocks, where it can be used and the exceptions are the same as [WContentDialog.showAndWait]
         * (call it on the UI thread, outside layout notifications; [IllegalStateException] if another dialog is open).
         * The button texts are [WOptionPane.okButtonText] / [WOptionPane.cancelButtonText]. To receive the result without blocking, use
         * The following [showDialog]. The dialog appears inside the owner window, so when the window is too small for it, the content scrolls vertically.
         */
        @JvmStatic
        fun showDialog(owner: WComponent, title: String, initial: WColor?): WColor? {
            val (dialog, picker) = chooserDialog(title, initial)
            return pickedColor(dialog.showAndWait(owner), picker)
        }

        /**
         * Opens the same dialog as [showDialog] without blocking, and passes the selected color (null for Cancel etc.) to [onClosed] when it is closed
         * (not called when the owner window is closed).
         */
        @JvmStatic
        fun showDialog(owner: WComponent, title: String, initial: WColor?, onClosed: Consumer<WColor?>) {
            val (dialog, picker) = chooserDialog(title, initial)
            dialog.show(owner) { onClosed.accept(pickedColor(it, picker)) }
        }

        private fun chooserDialog(title: String, initial: WColor?): Pair<WContentDialog, WColorPicker> {
            val picker = WColorPicker(initial ?: WColor.WHITE)
            // A ContentDialog appears inside the window, and the template's ContentScrollViewer does not scroll vertically. Make the content scroll vertically
            // so that even in a small window the brightness slider and the RGB fields can be reached (Swing uses a separate window, so it is not cut off)
            val scroller = WScrollPane(picker).also { it.verticalScrollBarVisibility = ScrollBarVisibility.AUTO }
            val dialog = WContentDialog(title, scroller)
            dialog.primaryButtonText = WOptionPane.okButtonText
            dialog.closeButtonText = WOptionPane.cancelButtonText
            dialog.defaultButton = ContentDialogButton.PRIMARY
            return dialog to picker
        }

        private fun pickedColor(result: ContentDialogResult, picker: WColorPicker): WColor? =
            if (result == ContentDialogResult.PRIMARY) picker.color else null
    }
}
