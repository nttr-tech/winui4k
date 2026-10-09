package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonCommandCatalog
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonRelayCommand
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** The number format of a cell. */
enum class NumberFormat(val label: String) {
    GENERAL("General"),
    NUMBER("Number"),
    CURRENCY("Currency"),
    ACCOUNTING("Accounting"),
    SHORT_DATE("Short Date"),
    LONG_DATE("Long Date"),
    TIME("Time"),
    PERCENT("Percentage"),
    FRACTION("Fraction"),
    SCIENTIFIC("Scientific"),
    TEXT("Text"),
    ;

    override fun toString(): String = label
}

/**
 * The spreadsheet view model (the ViewModel in MVVM, playing the same role as ExcelViewModel in the RibbonSpace Excel
 * demo). It holds the sheet data, the selected cell and the number formats, and registers the ribbon commands in the
 * command catalog ([catalog]) under string ids. Ribbon items only reference these commands by commandId; the view code
 * never manipulates the ribbon directly.
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

    /** The [Chart Tools] contextual group (shown when the sample chart is selected). */
    val chartTools: RibbonContextualGroupModel = RibbonContextualGroupModel("chart", "Chart Tools").also {
        it.color = RibbonColor.parse("#107C41")
        it.activation = RibbonContextualActivation.SELECT_ON_SHOW
        it.isVisible = false
    }

    /** The command that toggles selection of the sample chart ([Select Sample Chart] on [Insert]). */
    val toggleChartCommand: RibbonRelayCommand = RibbonRelayCommand.of({
        chartTools.isVisible = !chartTools.isVisible
        record(if (chartTools.isVisible) "Chart selected" else "Chart deselected")
    })

    /** The last executed command (shown in the status bar). */
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
        // Commands without behavior only report in the status bar that they were executed (same as the RibbonSpace demo)
        for ((id, label, shortcut) in COMMANDS) {
            catalog.register(id, label, RibbonRelayCommand({ p -> record(label + (p?.let { " ($it)" } ?: "")) }), shortcut = shortcut, category = "Excel")
        }
        catalog.register("formatCellsLauncher", "Format Cells dialog", RibbonRelayCommand.of({ record("Format Cells dialog") }))
        catalog.register("accounting", "Accounting Number Format", RibbonRelayCommand.of({ setFormat(NumberFormat.ACCOUNTING, "Accounting Number Format") }), category = "Number")
        catalog.register("percent", "Percent Style", RibbonRelayCommand.of({ setFormat(NumberFormat.PERCENT, "Percent Style") }), shortcut = "Ctrl+Shift+%", category = "Number")
        catalog.register("comma", "Comma Style", RibbonRelayCommand.of({ setFormat(NumberFormat.NUMBER, "Comma Style") }), category = "Number")
        catalog.register("increaseDecimal", "Increase Decimal", RibbonRelayCommand.of({ changeDecimals(1) }), category = "Number")
        catalog.register("decreaseDecimal", "Decrease Decimal", RibbonRelayCommand.of({ changeDecimals(-1) }), category = "Number")
        catalog.register("autoSum", "AutoSum", RibbonRelayCommand.of({ autoSum() }), shortcut = "Alt+=", category = "Editing")
        catalog.register("clear", "Clear", RibbonRelayCommand.of({ clear() }), category = "Editing")
        catalog.register(
            "numberFormat",
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
        record("Clear")
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
            NumberFormat.CURRENCY, NumberFormat.ACCOUNTING -> "¥" + format.format(value.toDouble())
            NumberFormat.PERCENT -> format.format(value.toDouble() * PERCENT) + "%"
            NumberFormat.SCIENTIFIC -> DecimalFormat("0.00E0").format(value.toDouble())
            NumberFormat.TEXT -> value.toString()
            NumberFormat.GENERAL -> if (value.toDouble() % 1.0 == 0.0 && digits == 0) format.format(value.toLong()) else format.format(value.toDouble())
            else -> format.format(value.toDouble())
        }
    }

    /** The numbers in rows 2 to 5 of [column] (a chart series). */
    fun seriesOf(column: String): List<Double> = (2..5).map { (valueOf("$column$it") as? Number)?.toDouble() ?: 0.0 }

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

        // The commands registered by ExcelViewModel in the RibbonSpace demo (id, label, shortcut)
        val COMMANDS: List<Triple<String, String, String?>> = listOf(
            Triple("paste", "Paste", "Ctrl+V"),
            Triple("cut", "Cut", "Ctrl+X"),
            Triple("copy", "Copy", "Ctrl+C"),
            Triple("formatPainter", "Format Painter", null),
            Triple("insertCells", "Insert", null),
            Triple("deleteCells", "Delete", null),
            Triple("formatCells", "Format Cells", "Ctrl+1"),
            Triple("fill", "Fill", null),
            Triple("sortFilter", "Sort & Filter", null),
            Triple("findSelect", "Find & Select", "Ctrl+F"),
            Triple("pivotTable", "PivotTable", null),
            Triple("table", "Table", "Ctrl+T"),
            Triple("recommendedCharts", "Recommended Charts", null),
            Triple("insertFunction", "Insert Function", "Shift+F3"),
            Triple("calculateNow", "Calculate Now", "F9"),
            Triple("refreshAll", "Refresh All", "Ctrl+Alt+F5"),
            Triple("freezePanes", "Freeze Panes", null),
            Triple("newWindow", "New Window", null),
            Triple("wrapText", "Wrap Text", null),
            Triple("mergeCenter", "Merge & Center", null),
            Triple("chartStyles", "Change Colors", null),
            Triple("switchRowColumn", "Switch Row/Column", null),
            Triple("selectData", "Select Data", null),
            Triple("changeChartType", "Change Chart Type", null),
        )
    }
}
