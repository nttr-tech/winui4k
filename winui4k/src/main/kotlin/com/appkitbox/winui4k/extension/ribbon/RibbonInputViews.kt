package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.FlyoutPlacement
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFlyout
import com.appkitbox.winui4k.WFlyoutBase
import com.appkitbox.winui4k.WMenuFlyoutItemBase
import com.appkitbox.winui4k.WSize
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.XamlKeyEvent
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNumberFormat
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSegmentedModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSliderModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTextBoxModel
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.math.max
import kotlin.math.roundToInt

/** Creates views for input items (a continuation of RibbonItemViews.create). */
internal object RibbonInputViews {
    /** Creates the view for [model]. Returns null for an unsupported model. */
    fun create(model: RibbonItemModel, host: RibbonItemHost, embedded: Boolean): RibbonItemView? = when (model) {
        is RibbonComboBoxModel -> RibbonComboBoxView(model, host)
        is RibbonSpinnerModel -> RibbonSpinnerView(model, host)
        is RibbonTextBoxModel -> RibbonTextBoxView(model, host)
        is RibbonSliderModel -> RibbonSliderView(model, host)
        is RibbonSegmentedModel -> RibbonSegmentedView(model, host)
        is RibbonGridPickerModel -> RibbonGridPickerView(model, host, embedded)
        is RibbonColorPickerModel -> RibbonColorPickerView(model, host, embedded)
        is RibbonGalleryModel -> RibbonGalleryView(model, host, embedded)
        is com.appkitbox.winui4k.extension.ribbon.model.RibbonZoomModel -> RibbonZoomView(model, host)
        else -> null
    }

    /** Virtual-key codes. */
    const val VK_ENTER = 13
    const val VK_ESCAPE = 27
    const val VK_SPACE = 32
    const val VK_PAGE_UP = 33
    const val VK_PAGE_DOWN = 34
    const val VK_END = 35
    const val VK_HOME = 36
    const val VK_LEFT = 37
    const val VK_UP = 38
    const val VK_RIGHT = 39
    const val VK_DOWN = 40
    const val VK_MENU = 18
    const val VK_F4 = 115

    /** Whether the key opens a drop-down (F4 or Alt+Down). */
    fun isDropDownKey(key: Int): Boolean = key == VK_F4 || (key == VK_DOWN && Xaml.isKeyDown(VK_MENU))
}

/**
 * The common base for items that show a label next to an input box (RibbonInputBase in RibbonSpace).
 * Its look is the template of the [styleKey] ContentControl (icon, label, and input box frame), and it switches the
 * state of the input box frame (normal, pointer over, focused, disabled).
 */
internal abstract class RibbonInputView(model: RibbonItemModel, host: RibbonItemHost, styleKey: String) : RibbonItemView(model, host) {
    override val element: XamlElement = XamlElement.load("<ContentControl Style=\"{StaticResource $styleKey}\" />")
    protected val iconPart: XamlElement by lazy { element.templatePart("PART_Icon") }
    protected val labelPart: XamlElement by lazy { element.templatePart("PART_Label") }
    protected val inputBorder: XamlElement by lazy { element.templatePart("PART_InputBorder") }
    private var isPointerOver = false
    private var isFocused = false

    init {
        element.onPointer(XamlInterop.IUIElement_add_PointerEntered) {
            isPointerOver = true
            updateInputState()
        }
        element.onPointer(XamlInterop.IUIElement_add_PointerExited) {
            isPointerOver = false
            updateInputState()
        }
        element.onFocus(true) {
            isFocused = true
            updateInputState()
        }
        element.onFocus(false) {
            isFocused = false
            updateInputState()
        }
    }

    /** Width of the input box. */
    protected abstract val inputWidth: Double

    /** Whether the label is shown to the left of the input box by default (for combo boxes, only when the model specifies it). */
    protected open val showsLabelByDefault: Boolean get() = true

    /** The height of the input box. */
    protected fun inputHeight(layout: RibbonItemLayout): Double =
        if (layout.isSimplified) max(24.0, layout.metrics.simplifiedItemHeight - 4) else layout.metrics.rowHeight

    /** Whether to show the label (LabelVisibility in RibbonSpace). */
    protected fun labelVisible(layout: RibbonItemLayout): Boolean = !model.label.isNullOrEmpty() && showsLabelByDefault &&
        (layout.isSimplified || (showsLabel(layout) && layout.size != RibbonItemSize.SMALL))

    /** Whether to show the icon (IconVisibility in RibbonSpace). */
    protected fun iconVisible(layout: RibbonItemLayout): Boolean = model.icon != null && (layout.size != RibbonItemSize.SMALL || !labelVisible(layout))

    override fun measure(layout: RibbonItemLayout): WSize {
        val icon = if (iconVisible(layout)) ICON_MARGIN + layout.metrics.smallIconSize else 0.0
        val label = if (labelVisible(layout)) {
            host.textWidths.widths(listOf(model.label.orEmpty()), layout.metrics.fontSize, false)[0] + LABEL_MARGIN
        } else {
            0.0
        }
        return WSize(icon + label + inputWidth, inputHeight(layout))
    }

    override fun applyLayoutCore() {
        val showIcon = iconVisible(layout)
        iconPart.isVisible = showIcon
        iconPart.setChild(
            if (showIcon) RibbonIconXaml.build(model.icon, layout.metrics.smallIconSize)?.let { XamlElement.load(it) } else null,
        )
        val showLabel = labelVisible(layout)
        labelPart.isVisible = showLabel
        labelPart.setText(model.label)
        labelPart.view(XamlInterop.IID_ITextBlock).call(XamlInterop.ITextBlock_put_FontSize, layout.metrics.fontSize)
        element.setFontSize(layout.metrics.fontSize)
        inputBorder.setSize(inputWidth, inputHeight(layout))
        val size = measure(layout)
        element.setSize(size.width, size.height)
        updateInputState()
    }

    override fun refreshEnabled() {
        super.refreshEnabled()
        updateInputState()
    }

    /** Switches the state of the input box frame (in priority order: disabled → focused → pointer over → normal). */
    protected fun updateInputState() {
        val state = when {
            !isEffectivelyEnabled -> "InputDisabled"
            isFocused -> "InputFocused"
            isPointerOver -> "InputPointerOver"
            else -> "InputNormal"
        }
        element.goToState(state)
    }

    override fun overflowMenuItems(): List<WMenuFlyoutItemBase> = emptyList()

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        if (name == "inputWidth") {
            host.invalidateItemsLayout()
            applyLayoutCore()
        }
    }

    private companion object {
        /** The left and right margins of the icon (Margin="3,0,4,0" in the template). */
        const val ICON_MARGIN = 7.0

        /** The right margin of the label (Margin="0,0,6,0" in the template). */
        const val LABEL_MARGIN = 6.0
    }
}

/**
 * A combo box (RibbonComboBox / RibbonFontComboBox / RibbonFontSizeComboBox in RibbonSpace).
 * When editable, typed text is committed with Enter or by moving the focus, and a matching item is selected if there is one.
 * The drop-down is rebuilt from the model's items each time it opens.
 */
internal class RibbonComboBoxView(override val model: RibbonComboBoxModel, host: RibbonItemHost) :
    RibbonInputView(model, host, "RibbonComboBoxHostStyle") {
    private val textBox: XamlElement by lazy { element.templatePart("PART_TextBox") }
    private val selectionBox: XamlElement by lazy { element.templatePart("PART_SelectionBox") }
    private val dropDownButton: XamlElement by lazy { element.templatePart("PART_DropDownButton") }
    private val flyout = WFlyout()
    override val dropDown: WFlyoutBase get() = flyout
    private var listButtons: List<Pair<Any, XamlElement>> = emptyList()
    private var syncing = false

    override val inputWidth: Double get() = model.inputWidth

    override val showsLabelByDefault: Boolean get() = model.showLabel

    init {
        flyout.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_LEFT
        RibbonMenus.applyFlyoutStyle(flyout, bare = false)
        RibbonMenus.onClosed(flyout) { listButtons = emptyList() }
    }

    override fun attach() {
        textBox.onPreviewKeyDown { e -> onTextKeyDown(e) }
        textBox.onFocus(true) { if (model.isEditable) textBox.selectAllText() }
        textBox.onFocus(false) {
            val text = textBox.textBoxText
            if (model.isEditable && text != model.text.orEmpty() && !flyout.isOpen) commitText(text)
        }
        dropDownButton.onClick { if (flyout.isOpen) flyout.hide() else openDropDown() }
        inputBorder.onTapped { if (!model.isEditable) openDropDown() }
        super.attach()
        syncFromModel()
    }

    override fun applyLayoutCore() {
        super.applyLayoutCore()
        textBox.view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_FontSize, layout.metrics.fontSize)
        updateEditable()
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "selectedItem", "text", "itemTextProvider", "itemRenderer" -> syncFromModel()
            "isEditable", "placeholder" -> updateEditable()
        }
    }

    /** Reflects the model's selection and text in the input box (text being typed is not overwritten). */
    private fun syncFromModel() {
        if (syncing) return
        val text = model.text ?: model.selectedItem?.let { model.getItemText(it) }.orEmpty()
        if (!textBox.hasFocus && textBox.textBoxText != text) textBox.textBoxText = text
        updateSelectionBox()
    }

    private fun updateEditable() {
        textBox.view(XamlInterop.IID_ITextBox).putBool(XamlInterop.ITextBox_put_IsReadOnly, !model.isEditable)
        textBox.setHitTestVisible(model.isEditable)
        textBox.setPlaceholderText(model.placeholder)
        updateSelectionBox()
    }

    /**
     * When read-only with a rendering specification, shows a rendering of the selected item (SelectionBoxTemplate in
     * RibbonSpace).
     */
    private fun updateSelectionBox() {
        val renderer = model.itemRenderer
        val selected = model.selectedItem
        val templated = renderer != null && !model.isEditable && selected != null
        selectionBox.setChild(if (templated) renderer!!.createComponent(selected) else null)
        selectionBox.isVisible = templated
        textBox.opacity = if (templated) 0.0 else 1.0
    }

    private fun onTextKeyDown(e: XamlKeyEvent) {
        when (e.key) {
            RibbonInputViews.VK_ENTER -> {
                if (model.isEditable) commitText(textBox.textBoxText) else openDropDown()
                e.handled = true
            }
            RibbonInputViews.VK_ESCAPE -> textBox.textBoxText = model.text ?: model.selectedItem?.let { model.getItemText(it) }.orEmpty()
            RibbonInputViews.VK_F4 -> {
                openDropDown()
                e.handled = true
            }
            RibbonInputViews.VK_UP, RibbonInputViews.VK_DOWN -> {
                e.handled = stepSelection(e.key == RibbonInputViews.VK_DOWN)
            }
        }
    }

    /** Up and Down keys: with Alt, opens the drop-down; otherwise commits the adjacent item. */
    private fun stepSelection(down: Boolean): Boolean {
        val items = model.items.toList()
        if (items.isEmpty()) return false
        if (Xaml.isKeyDown(RibbonInputViews.VK_MENU)) {
            openDropDown()
            return true
        }
        val current = items.indexOf(model.selectedItem)
        val next = (current + if (down) 1 else -1).coerceIn(0, items.size - 1)
        commit(items[next])
        return true
    }

    /** Commits an item: selects it, updates the text to match, and executes with the item as the argument. */
    fun commit(item: Any?) {
        flyout.hide()
        syncing = true
        try {
            model.selectedItem = item
            model.text = model.getItemText(item)
        } finally {
            syncing = false
        }
        textBox.textBoxText = model.text.orEmpty()
        updateSelectionBox()
        model.fireCommitted(item, model.text.orEmpty())
        execute(item)
    }

    /** Commits text: selects a matching item if there is one; otherwise (if editable) executes with the text as the argument. */
    fun commitText(text: String) {
        val match = model.items.firstOrNull { model.getItemText(it).equals(text, ignoreCase = true) }
        if (match != null) {
            commit(match)
            return
        }
        syncing = true
        try {
            model.text = text
            model.selectedItem = null
        } finally {
            syncing = false
        }
        updateSelectionBox()
        model.fireCommitted(null, text)
        execute(text)
    }

    /** Opens the drop-down (stacks the item buttons vertically and puts the focus on the selected item). */
    override fun openDropDown() {
        if (!isEffectivelyEnabled) return
        val list = XamlElement.load("<StackPanel Padding=\"2\" />")
        val buttons = mutableListOf<Pair<Any, XamlElement>>()
        for (item in model.items) {
            val button = XamlElement.load(
                "<Button Style=\"{StaticResource RibbonMenuItemButtonStyle}\" HorizontalAlignment=\"Stretch\" HorizontalContentAlignment=\"Left\" />",
            )
            button.setContent(itemContent(item))
            button.setAutomationName(model.getItemText(item))
            button.onClick { commit(item) }
            button.onKeyDown { e -> onListKeyDown(e) }
            list.addChild(button)
            buttons += item to button
        }
        listButtons = buttons
        val scroll = XamlElement.load(
            "<ScrollViewer VerticalScrollBarVisibility=\"Auto\" HorizontalScrollBarVisibility=\"Disabled\" " +
                "MinWidth=\"${Xaml.num(inputWidth)}\" MaxHeight=\"${Xaml.num(model.maxDropDownHeight)}\" />",
        )
        scroll.setContent(list)
        flyout.content = scroll
        flyout.showAt(inputBorder)
        WinUiUtilities.invokeLater {
            val selected = buttons.firstOrNull { it.first == model.selectedItem } ?: buttons.firstOrNull()
            selected?.second?.focus()
        }
    }

    private fun itemContent(item: Any): WComponent {
        model.itemRenderer?.let { return it.createComponent(item) }
        val text = model.getItemText(item)
        val font = if (model.previewFontFamily && text.isNotEmpty()) " FontFamily=\"${Xaml.escape(text)}\" FontSize=\"14\"" else ""
        return XamlElement.load("<TextBlock Text=\"${Xaml.escape(text)}\" VerticalAlignment=\"Center\"$font />")
    }

    private fun onListKeyDown(e: XamlKeyEvent) {
        when (e.key) {
            RibbonInputViews.VK_DOWN -> e.handled = Xaml.moveFocus(true)
            RibbonInputViews.VK_UP -> e.handled = Xaml.moveFocus(false)
        }
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        if (model.isEditable) {
            textBox.focus()
            textBox.selectAllText()
        } else {
            openDropDown()
        }
        return RibbonKeyTipResult.Close
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        textBox.focus()
        return true
    }
}

/**
 * A numeric spinner with a unit (RibbonSpinner in RibbonSpace). The value changes with the arrow buttons, the Up and Down
 * keys (PageUp / PageDown step by 10x), the mouse wheel, and dragging the label horizontally; the typed text is committed
 * with Enter or by moving the focus.
 */
internal class RibbonSpinnerView(override val model: RibbonSpinnerModel, host: RibbonItemHost) :
    RibbonInputView(model, host, "RibbonSpinnerHostStyle") {
    private val textBox: XamlElement by lazy { element.templatePart("PART_TextBox") }
    private val upButton: XamlElement by lazy { element.templatePart("PART_UpButton") }
    private val downButton: XamlElement by lazy { element.templatePart("PART_DownButton") }
    private var scrubStartX: Double? = null
    private var scrubStartValue = 0.0

    override val inputWidth: Double get() = model.inputWidth

    override fun attach() {
        upButton.onClick { step(model.increment) }
        downButton.onClick { step(-model.increment) }
        textBox.onPreviewKeyDown { e -> onTextKeyDown(e) }
        textBox.onFocus(false) { commitText() }
        textBox.onPointer(XamlInterop.IUIElement_add_PointerWheelChanged) { e ->
            step(if (e.wheelDelta > 0) model.increment else -model.increment)
            e.handled = true
        }
        attachScrub()
        super.attach()
        updateText(force = true)
    }

    /** Changes the value by dragging the label horizontally (one increment per 4px). */
    private fun attachScrub() {
        labelPart.onPointer(XamlInterop.IUIElement_add_PointerPressed) { e ->
            if (model.isScrubEnabled && isEffectivelyEnabled) {
                scrubStartX = e.position(null)[0]
                scrubStartValue = model.value
                e.capture(labelPart)
                e.handled = true
            }
        }
        labelPart.onPointer(XamlInterop.IUIElement_add_PointerMoved) { e ->
            val start = scrubStartX ?: return@onPointer
            val dx = e.position(null)[0] - start
            model.value = coerce(scrubStartValue + (dx / SCRUB_PIXELS).roundToInt() * model.increment)
        }
        labelPart.onPointer(XamlInterop.IUIElement_add_PointerReleased) { e ->
            if (scrubStartX != null) {
                scrubStartX = null
                e.release(labelPart)
                execute(model.value)
            }
        }
    }

    override fun applyLayoutCore() {
        super.applyLayoutCore()
        textBox.view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_FontSize, layout.metrics.fontSize)
        textBox.setAutomationName(model.label)
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "value" -> {
                val coerced = coerce(model.value)
                if (coerced != model.value) model.value = coerced else updateText(force = false)
            }
            "minimum", "maximum" -> model.value = coerce(model.value)
            "format", "unit" -> updateText(force = true)
        }
    }

    private fun coerce(value: Double): Double {
        val min = model.minimum
        val max = max(min, model.maximum)
        return if (value.isNaN()) min else value.coerceIn(min, max)
    }

    /** Shows the value in the input box (not overwritten while typing). */
    private fun updateText(force: Boolean) {
        if (force || !textBox.hasFocus) textBox.textBoxText = RibbonNumberFormat.format(model.value, model.format, model.unit)
    }

    private fun step(delta: Double) {
        if (!isEffectivelyEnabled) return
        model.value = coerce(model.value + delta)
        updateText(force = true)
        execute(model.value)
    }

    private fun onTextKeyDown(e: XamlKeyEvent) {
        val delta = when (e.key) {
            RibbonInputViews.VK_UP -> model.increment
            RibbonInputViews.VK_DOWN -> -model.increment
            RibbonInputViews.VK_PAGE_UP -> model.increment * PAGE_FACTOR
            RibbonInputViews.VK_PAGE_DOWN -> -model.increment * PAGE_FACTOR
            else -> null
        }
        if (delta != null) {
            step(delta)
            e.handled = true
            return
        }
        when (e.key) {
            RibbonInputViews.VK_ENTER -> {
                commitText()
                e.handled = true
            }
            RibbonInputViews.VK_ESCAPE -> updateText(force = true)
        }
    }

    /**
     * Commits the typed text (if it can be parsed, changes the value and executes; the input box is restored to the
     * formatted value).
     */
    fun commitText() {
        val parsed = RibbonNumberFormat.parse(textBox.textBoxText, model.unit)
        if (parsed != null) {
            val old = model.value
            model.value = coerce(parsed)
            if (Math.abs(model.value - old) > 0.0) execute(model.value)
        }
        updateText(force = true)
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        textBox.focus()
        textBox.selectAllText()
        return RibbonKeyTipResult.Close
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        textBox.focus()
        return true
    }

    private companion object {
        const val PAGE_FACTOR = 10
        const val SCRUB_PIXELS = 4.0
    }
}

/** A single-line text input (RibbonTextBox in RibbonSpace). Enter executes with the typed text as the argument. */
internal class RibbonTextBoxView(override val model: RibbonTextBoxModel, host: RibbonItemHost) :
    RibbonInputView(model, host, "RibbonTextBoxHostStyle") {
    private val textBox: XamlElement by lazy { element.templatePart("PART_TextBox") }
    private var syncing = false

    override val inputWidth: Double get() = model.inputWidth

    override fun attach() {
        textBox.onTextChanged {
            if (!syncing) {
                syncing = true
                try {
                    model.text = textBox.textBoxText
                } finally {
                    syncing = false
                }
            }
        }
        textBox.onPreviewKeyDown { e ->
            if (e.key == RibbonInputViews.VK_ENTER) {
                execute(textBox.textBoxText)
                e.handled = true
            }
        }
        super.attach()
        syncText()
    }

    override fun applyLayoutCore() {
        super.applyLayoutCore()
        textBox.view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_FontSize, layout.metrics.fontSize)
        textBox.setPlaceholderText(model.placeholder)
        textBox.setAutomationName(model.label)
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "text" -> syncText()
            "placeholder" -> textBox.setPlaceholderText(model.placeholder)
        }
    }

    private fun syncText() {
        if (syncing) return
        val text = model.text.orEmpty()
        if (textBox.textBoxText != text) {
            syncing = true
            try {
                textBox.textBoxText = text
            } finally {
                syncing = false
            }
        }
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        textBox.focus()
        textBox.selectAllText()
        return RibbonKeyTipResult.Close
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        textBox.focus()
        return true
    }
}

/** A slider (RibbonSlider in RibbonSpace). User interaction executes with the value as the argument. */
internal class RibbonSliderView(override val model: RibbonSliderModel, host: RibbonItemHost) :
    RibbonInputView(model, host, "RibbonSliderHostStyle") {
    private val slider: XamlElement by lazy { element.templatePart("PART_Slider") }
    private var syncing = false

    override val inputWidth: Double get() = model.sliderWidth

    override fun attach() {
        slider.onValueChanged { value ->
            if (!syncing && value != model.value) {
                model.value = value
                execute(value)
            }
        }
        super.attach()
        syncSlider()
    }

    override fun applyLayoutCore() {
        super.applyLayoutCore()
        slider.setAutomationName(model.label)
    }

    override fun onModelChanged(name: String) {
        super.onModelChanged(name)
        when (name) {
            "value", "minimum", "maximum", "stepFrequency" -> syncSlider()
            "sliderWidth" -> {
                host.invalidateItemsLayout()
                applyLayoutCore()
            }
        }
    }

    private fun syncSlider() {
        syncing = true
        try {
            slider.setRange(model.minimum, max(model.minimum, model.maximum))
            slider.view(XamlInterop.IID_ISlider).call(XamlInterop.ISlider_put_StepFrequency, model.stepFrequency)
            if (slider.rangeValue != model.value) slider.rangeValue = model.value
        } finally {
            syncing = false
        }
    }

    override fun onKeyTip(): RibbonKeyTipResult {
        slider.focus()
        return RibbonKeyTipResult.Close
    }

    override fun invoke(): Boolean {
        if (!isEffectivelyEnabled) return false
        slider.focus()
        return true
    }
}
