package com.appkitbox.winui4k.sample.ribbon.excel

import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandCatalog
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualActivation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRelayCommand
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** The number format of a cell. */
enum class NumberFormat(val label: String) {
    GENERAL(Messages.get("format.general")),
    NUMBER(Messages.get("format.number")),
    CURRENCY(Messages.get("format.currency")),
    ACCOUNTING(Messages.get("format.accounting")),
    SHORT_DATE(Messages.get("format.short.date")),
    LONG_DATE(Messages.get("format.long.date")),
    TIME(Messages.get("format.time")),
    PERCENT(Messages.get("format.percentage")),
    FRACTION(Messages.get("format.fraction")),
    SCIENTIFIC(Messages.get("format.scientific")),
    TEXT(Messages.get("format.text")),
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
    val chartTools: RibbonContextualGroupModel = RibbonContextualGroupModel("chart", Messages.get("chart.tools")).also {
        it.color = RibbonColor.parse("#107C41")
        it.activation = RibbonContextualActivation.SELECT_ON_SHOW
        it.isVisible = false
    }

    /** The command that toggles selection of the sample chart ([Select Sample Chart] on [Insert]). */
    val toggleChartCommand: RibbonRelayCommand = RibbonRelayCommand.of({
        chartTools.isVisible = !chartTools.isVisible
        record(if (chartTools.isVisible) Messages.get("log.chart.selected") else Messages.get("log.chart.deselected"))
    })

    /** The last executed command (shown in the status bar). */
    var lastCommand: String = Messages.get("status.ready")
        private set

    init {
        val data = listOf(
            listOf(Messages.get("sheet.category"), "Q1", "Q2", "Q3", "Q4"),
            listOf(Messages.get("sheet.rent"), 1200, 1200, 1250, 1250),
            listOf(Messages.get("sheet.software"), 860, 910, 940, 990),
            listOf(Messages.get("sheet.travel"), 420, 380, 610, 300),
            listOf(Messages.get("sheet.marketing"), 1900, 2400, 2100, 2650),
        )
        data.forEachIndexed { r, row -> row.forEachIndexed { c, v -> values["${columns[c]}${r + 1}"] = v } }
        values["A6"] = Messages.get("sheet.total")
        for (c in 1..4) values["${columns[c]}6"] = "=SUM(${columns[c]}2:${columns[c]}5)"
        registerCommands()
    }

    private fun registerCommands() {
        // Commands without behavior only report in the status bar that they were executed (same as the RibbonSpace demo)
        for ((id, label, shortcut) in COMMANDS) {
            catalog.register(id, label, RibbonRelayCommand({ p -> record(label + (p?.let { " ($it)" } ?: "")) }), shortcut = shortcut, category = "Excel")
        }
        catalog.register("formatCellsLauncher", Messages.get("command.format.cells.dialog"), RibbonRelayCommand.of({ record(Messages.get("command.format.cells.dialog")) }))
        catalog.register("accounting", Messages.get("command.accounting"), RibbonRelayCommand.of({ setFormat(NumberFormat.ACCOUNTING, Messages.get("command.accounting")) }), category = Messages.get("format.number"))
        catalog.register("percent", Messages.get("command.percent"), RibbonRelayCommand.of({ setFormat(NumberFormat.PERCENT, Messages.get("command.percent")) }), shortcut = "Ctrl+Shift+%", category = Messages.get("format.number"))
        catalog.register("comma", Messages.get("command.comma"), RibbonRelayCommand.of({ setFormat(NumberFormat.NUMBER, Messages.get("command.comma")) }), category = Messages.get("format.number"))
        catalog.register("increaseDecimal", Messages.get("command.increase.decimal"), RibbonRelayCommand.of({ changeDecimals(1) }), category = Messages.get("format.number"))
        catalog.register("decreaseDecimal", Messages.get("command.decrease.decimal"), RibbonRelayCommand.of({ changeDecimals(-1) }), category = Messages.get("format.number"))
        catalog.register("autoSum", Messages.get("command.auto.sum"), RibbonRelayCommand.of({ autoSum() }), shortcut = "Alt+=", category = Messages.get("common.editing"))
        catalog.register("clear", Messages.get("command.clear"), RibbonRelayCommand.of({ clear() }), category = Messages.get("common.editing"))
        catalog.register(
            "numberFormat",
            Messages.get("command.number.format"),
            RibbonRelayCommand({ parameter -> (parameter as? NumberFormat)?.let { setFormat(it, it.label) } }),
            category = Messages.get("format.number"),
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
        record(if (delta > 0) Messages.get("command.increase.decimal") else Messages.get("command.decrease.decimal"))
    }

    private fun autoSum() {
        val column = selected.takeWhile { it.isLetter() }
        values[selected] = "=SUM(${column}2:${column}5)"
        record(Messages.get("command.auto.sum"))
    }

    private fun clear() {
        values.remove(selected)
        formats.remove(selected)
        record(Messages.get("command.clear"))
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
        val format = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Messages.locale))
        return when (formats[cell] ?: NumberFormat.GENERAL) {
            NumberFormat.CURRENCY, NumberFormat.ACCOUNTING -> Messages.get("number.currency.symbol") + format.format(value.toDouble())
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
            Triple("paste", Messages.get("common.paste"), "Ctrl+V"),
            Triple("cut", Messages.get("common.cut"), "Ctrl+X"),
            Triple("copy", Messages.get("common.copy"), "Ctrl+C"),
            Triple("formatPainter", Messages.get("common.format.painter"), null),
            Triple("insertCells", Messages.get("common.insert"), null),
            Triple("deleteCells", Messages.get("common.delete"), null),
            Triple("formatCells", Messages.get("command.format.cells"), "Ctrl+1"),
            Triple("fill", Messages.get("command.fill"), null),
            Triple("sortFilter", Messages.get("command.sort.filter"), null),
            Triple("findSelect", Messages.get("command.find.select"), "Ctrl+F"),
            Triple("pivotTable", Messages.get("command.pivot.table"), null),
            Triple("table", Messages.get("command.table"), "Ctrl+T"),
            Triple("recommendedCharts", Messages.get("command.recommended.charts"), null),
            Triple("insertFunction", Messages.get("command.insert.function"), "Shift+F3"),
            Triple("calculateNow", Messages.get("command.calculate.now"), "F9"),
            Triple("refreshAll", Messages.get("command.refresh.all"), "Ctrl+Alt+F5"),
            Triple("freezePanes", Messages.get("command.freeze.panes"), null),
            Triple("newWindow", Messages.get("common.new.window"), null),
            Triple("wrapText", Messages.get("command.wrap.text"), null),
            Triple("mergeCenter", Messages.get("command.merge.center"), null),
            Triple("chartStyles", Messages.get("command.chart.colors"), null),
            Triple("switchRowColumn", Messages.get("command.switch.row.column"), null),
            Triple("selectData", Messages.get("command.select.data"), null),
            Triple("changeChartType", Messages.get("command.change.chart.type"), null),
        )
    }
}
