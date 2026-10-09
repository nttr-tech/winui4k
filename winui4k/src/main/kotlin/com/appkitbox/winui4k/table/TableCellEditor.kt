package com.appkitbox.winui4k.table

import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WSpinner
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WToggleSwitch
import kotlin.jvm.JvmSynthetic

/**
 * Like javax.swing.table.TableCellEditor: an editor used to edit cells.
 *
 * WinUI's TableView receives the editor as a DataTemplate (TableViewColumn.CellEditingTemplate) and generates an editor
 * from the template each time a cell is edited. So unlike Swing, an editor is expressed in XAML rather than as a
 * component. For the cell value, use the `valuePath` argument of [getCellEditorXaml] and bind it with
 * `{Binding valuePath, Mode=TwoWay, UpdateSourceTrigger=Explicit}`.
 * Committing the edit (Enter or moving focus) writes the value back to the model's [TableModel.setValueAt], and
 * canceling (Esc) does not.
 *
 * The values written back on commit are those of the bindings of TextBox.Text / AutoSuggestBox.Text / ToggleSwitch.IsOn /
 * ToggleButton (CheckBox and others).IsChecked / Selector (ComboBox and others).SelectedItem, SelectedIndex, SelectedValue /
 * Slider.Value / NumberBox.Value (as specified by TableView).
 * Usually, use [DefaultCellEditor].
 */
fun interface TableCellEditor {
    /**
     * The XAML of the editor (the single root element of the DataTemplate's content). The default namespace (xmlns) is
     * added automatically. [valuePath] is the binding path of the cell value.
     */
    fun getCellEditorXaml(valuePath: String): String
}

/**
 * Like javax.swing.DefaultCellEditor: an editor modeled on a standard component.
 *
 * As in Swing, it is created by passing a component such as a text field, check box, or combo box.
 * In WinUI, editors are generated from templates, so the editor is not the passed component itself but a XAML editor
 * that copies its settings at creation time (the items of a combo box, the range of a slider, and so on).
 */
class DefaultCellEditor private constructor(private val template: (String) -> String) : TableCellEditor {
    /** Edits with a text box (inheriting placeholderText / maxLength / textAlignment from [textField]). */
    constructor(textField: WTextField) : this(textBoxTemplate(textField))

    /** Edits with a check box (using the text of [checkBox] as the label). The value is a Boolean. */
    constructor(checkBox: WCheckBox) : this(checkBoxTemplate(checkBox.text))

    /**
     * Edits by choosing from the items of a combo box (inheriting the items and isEditable of [comboBox]). The value is
     * a String.
     */
    constructor(comboBox: WComboBox) : this(comboBoxTemplate(comboBox))

    /** Edits with a toggle switch (inheriting onContent / offContent from [toggleSwitch]). The value is a Boolean. */
    constructor(toggleSwitch: WToggleSwitch) : this(toggleSwitchTemplate(toggleSwitch))

    /** Edits with a slider (inheriting the range and step of [slider]). The value is a Double. */
    constructor(slider: WSlider) : this(sliderTemplate(slider))

    /** Edits with a number box (NumberBox) (inheriting the range and increment of [spinner]). The value is a Double. */
    constructor(spinner: WSpinner) : this(numberBoxTemplate(spinner))

    override fun getCellEditorXaml(valuePath: String): String = template(valuePath)
}

private fun binding(path: String, property: String) =
    "$property=\"{Binding ${escape(path)}, Mode=TwoWay, UpdateSourceTrigger=Explicit}\""

private fun textBoxTemplate(textField: WTextField): (String) -> String {
    val placeholder = textField.placeholderText
    val maxLength = textField.maxLength
    val alignment = pascalCase(textField.textAlignment.name)
    return { path ->
        "<TextBox ${binding(path, "Text")} PlaceholderText=\"${escape(placeholder)}\" " +
            "MaxLength=\"$maxLength\" TextAlignment=\"$alignment\" MinHeight=\"0\" " +
            "VerticalAlignment=\"Center\" Padding=\"8,4,8,4\" />"
    }
}

private fun checkBoxTemplate(text: String): (String) -> String = { path ->
    "<CheckBox ${binding(path, "IsChecked")} Content=\"${escape(text)}\" MinWidth=\"0\" " +
        "Margin=\"8,0,0,0\" VerticalAlignment=\"Center\" />"
}

private fun comboBoxTemplate(comboBox: WComboBox): (String) -> String {
    val items = (0 until comboBox.itemCount).map { comboBox.getItem(it) }
    val isEditable = comboBox.isEditable
    return { path ->
        val itemsXaml = items.joinToString("") { "<x:String>${escape(it)}</x:String>" }
        "<ComboBox ${binding(path, "SelectedItem")} IsEditable=\"$isEditable\" " +
            "HorizontalAlignment=\"Stretch\" VerticalAlignment=\"Center\" " +
            "xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\">$itemsXaml</ComboBox>"
    }
}

private fun toggleSwitchTemplate(toggleSwitch: WToggleSwitch): (String) -> String {
    val onContent = toggleSwitch.onContent
    val offContent = toggleSwitch.offContent
    return { path ->
        val contents = buildString {
            if (onContent.isNotEmpty()) append(" OnContent=\"${escape(onContent)}\"")
            if (offContent.isNotEmpty()) append(" OffContent=\"${escape(offContent)}\"")
        }
        "<ToggleSwitch ${binding(path, "IsOn")}$contents MinWidth=\"0\" Margin=\"8,0,0,0\" " +
            "VerticalAlignment=\"Center\" />"
    }
}

private fun sliderTemplate(slider: WSlider): (String) -> String {
    val minimum = slider.minimum
    val maximum = slider.maximum
    val step = slider.stepFrequency
    return { path ->
        "<Slider ${binding(path, "Value")} Minimum=\"$minimum\" Maximum=\"$maximum\" " +
            "StepFrequency=\"$step\" Margin=\"8,0,8,0\" VerticalAlignment=\"Center\" />"
    }
}

private fun numberBoxTemplate(spinner: WSpinner): (String) -> String {
    val minimum = spinner.minimum
    val maximum = spinner.maximum
    val smallChange = spinner.smallChange
    val largeChange = spinner.largeChange
    val placement = pascalCase(spinner.spinButtonPlacementMode.name)
    return { path ->
        "<NumberBox ${binding(path, "Value")} Minimum=\"${number(minimum)}\" Maximum=\"${number(maximum)}\" " +
            "SmallChange=\"$smallChange\" LargeChange=\"$largeChange\" " +
            "SpinButtonPlacementMode=\"$placement\" VerticalAlignment=\"Center\" />"
    }
}

/** Converts a Kotlin enum name (DETECT_FROM_CONTENT) to the XAML enum notation (DetectFromContent). */
private fun pascalCase(name: String): String =
    name.split('_').joinToString("") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }

/** A XAML Double attribute value (infinity is written in XAML notation). */
private fun number(value: Double): String = when {
    value == Double.POSITIVE_INFINITY -> "Infinity"
    value == Double.NEGATIVE_INFINITY -> "-Infinity"
    else -> value.toString()
}

/** Escapes a string embedded in an XML attribute value or text. */
@JvmSynthetic
internal fun escape(text: String): String = buildString(text.length) {
    for (c in text) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
