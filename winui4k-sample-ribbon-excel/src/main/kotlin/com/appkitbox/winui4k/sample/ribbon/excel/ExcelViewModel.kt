package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.ribbon.RibbonCommandCatalog
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonRelayCommand
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** The number format of a cell. */
enum class NumberFormat(val label: String) {
    GENERAL("General"),
    NUMBER("Number"),
    CURRENCY("Currency"),
    PERCENT("Percentage"),
}

/**
 * The spreadsheet view model (the ViewModel in MVVM). It holds the sheet data, the selected cell and the number formats,
 * and registers the ribbon commands in the command catalog ([catalog]) under string ids. Ribbon items only reference
 * these commands by commandId; the view code never manipulates the ribbon directly (100% MVVM, same as the RibbonSpace
 * Excel demo).
 */
class ExcelViewModel {
    /** The column headers (A..L). */
    val columns: List<String> = ('A'..'L').map { it.toString() }

    /** The number of rows. */
    val rowCount: Int = ROWS

    private val values = HashMap<String, Any>()
    private val formats = HashMap<String, NumberFormat>()
    private val decimals = HashMap<String, Int>()
    private val listeners = mutableListOf<Runnable>()

    /** The selected cell ("A1"). */
    var selected: String = "A1"
        set(value) {
            field = value
            changed()
        }

    /** The command catalog (referenced by the ribbon's commandId). */
    val catalog = RibbonCommandCatalog()

    /** The last executed command. */
    var lastCommand: String = "Ready"
        private set

    init {
        val data = listOf(
            listOf("Category", "Q1", "Q2", "Q3", "Q4"),
            listOf("Rent", 1200, 1200, 1250, 1250),
            listOf("Software", 860, 910, 940, 990),
            listOf("Travel", 420, 380, 610, 300),
            listOf("Marketing", 1900, 2400, 2100, 2650),
        )
        data.forEachIndexed { r, row -> row.forEachIndexed { c, v -> values["${columns[c]}${r + 1}"] = v } }
        values["A6"] = "Total"
        for (c in 1..4) values["${columns[c]}6"] = "=SUM(${columns[c]}2:${columns[c]}5)"
        registerCommands()
    }

    private fun registerCommands() {
        fun format(id: String, label: String, format: NumberFormat, icon: com.appkitbox.winui4k.ribbon.RibbonIcon) =
            catalog.register(id, label, RibbonRelayCommand.of({ setFormat(format, label) }), icon, category = "Number")
        format("number.currency", "Accounting Number Format", NumberFormat.CURRENCY, RibbonIcons.SUM)
        format("number.percent", "Percent Style", NumberFormat.PERCENT, RibbonIcons.FUNCTION)
        format("number.comma", "Comma Style", NumberFormat.NUMBER, RibbonIcons.CALCULATOR)
        catalog.register("number.increase", "Increase Decimal", RibbonRelayCommand.of({ changeDecimals(1) }), RibbonIcons.ADD, category = "Number")
        catalog.register("number.decrease", "Decrease Decimal", RibbonRelayCommand.of({ changeDecimals(-1) }), RibbonIcons.REMOVE, category = "Number")
        catalog.register("editing.autoSum", "AutoSum", RibbonRelayCommand.of({ autoSum() }), RibbonIcons.SUM, "Alt+=", category = "Editing")
        catalog.register("editing.clear", "Clear All", RibbonRelayCommand.of({ clear() }), RibbonIcons.CLEAR, category = "Editing")
        catalog.register(
            "number.formatList",
            "Number Format",
            RibbonRelayCommand({ parameter -> (parameter as? NumberFormat)?.let { setFormat(it, it.label) } }),
            category = "Number",
        )
    }

    /** Subscribes to changes (the view refreshes its display). */
    fun addChangeListener(listener: Runnable) {
        listeners += listener
    }

    private fun changed() = listeners.toList().forEach { it.run() }

    /** Records an executed command (shown in the status bar). */
    fun record(command: String) {
        lastCommand = command
        changed()
    }

    /** The number format of the selected cell. */
    val selectedFormat: NumberFormat get() = formats[selected] ?: NumberFormat.GENERAL

    private fun setFormat(format: NumberFormat, label: String) {
        formats[selected] = format
        record(label)
    }

    private fun changeDecimals(delta: Int) {
        decimals[selected] = ((decimals[selected] ?: 0) + delta).coerceIn(0, MAX_DECIMALS)
        record(if (delta > 0) "Increase Decimal" else "Decrease Decimal")
    }

    private fun autoSum() {
        val column = selected.takeWhile { it.isLetter() }
        values[selected] = "=SUM(${column}2:${column}5)"
        record("AutoSum")
    }

    private fun clear() {
        values.remove(selected)
        formats.remove(selected)
        record("Clear All")
    }

    /** The input of [cell] (formulas as is). */
    fun formulaOf(cell: String): String = values[cell]?.toString().orEmpty()

    /** The value of [cell] (formulas are evaluated). */
    fun valueOf(cell: String): Any? {
        val raw = values[cell] ?: return null
        val text = raw.toString()
        if (!text.startsWith("=SUM(")) return raw
        val range = text.removePrefix("=SUM(").removeSuffix(")").split(':')
        val column = range[0].takeWhile { it.isLetter() }
        val from = range[0].drop(column.length).toInt()
        val to = range[1].drop(column.length).toInt()
        return (from..to).sumOf { (valueOf("$column$it") as? Number)?.toDouble() ?: 0.0 }
    }

    /** The display string of [cell] (following its number format and number of decimal places). */
    fun displayOf(cell: String): String {
        val value = valueOf(cell) ?: return ""
        if (value !is Number) return value.toString()
        val digits = decimals[cell] ?: 0
        val pattern = "#,##0" + if (digits > 0) "." + "0".repeat(digits) else ""
        val format = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.JAPAN))
        return when (formats[cell] ?: NumberFormat.GENERAL) {
            NumberFormat.GENERAL -> if (value.toDouble() % 1.0 == 0.0 && digits == 0) format.format(value.toLong()) else format.format(value.toDouble())
            NumberFormat.NUMBER -> format.format(value.toDouble())
            NumberFormat.CURRENCY -> "¥" + format.format(value.toDouble())
            NumberFormat.PERCENT -> format.format(value.toDouble() * PERCENT) + "%"
        }
    }

    /** Statistics of the numbers in the selected cell's column (average, count, sum). */
    fun statistics(): Triple<Double, Int, Double> {
        val column = selected.takeWhile { it.isLetter() }
        val numbers = (2..5).mapNotNull { (valueOf("$column$it") as? Number)?.toDouble() }
        val sum = numbers.sum()
        return Triple(if (numbers.isEmpty()) 0.0 else sum / numbers.size, numbers.size, sum)
    }

    private companion object {
        const val ROWS = 14
        const val MAX_DECIMALS = 4
        const val PERCENT = 100.0
    }
}
