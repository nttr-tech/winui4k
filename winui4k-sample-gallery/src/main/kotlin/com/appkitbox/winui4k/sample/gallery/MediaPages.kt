package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WChart
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.WWebView
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.chart.AreaSeries
import com.appkitbox.winui4k.chart.BarOrientation
import com.appkitbox.winui4k.chart.BarSeries
import com.appkitbox.winui4k.chart.CategoryAxis
import com.appkitbox.winui4k.chart.CategorySortKey
import com.appkitbox.winui4k.chart.DataLabelOverride
import com.appkitbox.winui4k.chart.DataMarkerOverride
import com.appkitbox.winui4k.chart.DateTimeAxis
import com.appkitbox.winui4k.chart.DateTimeIntervalType
import com.appkitbox.winui4k.chart.DefaultSampleModel
import com.appkitbox.winui4k.chart.GridLines
import com.appkitbox.winui4k.chart.LineSeries
import com.appkitbox.winui4k.chart.LinearAxis
import com.appkitbox.winui4k.chart.MarkerShape
import com.appkitbox.winui4k.chart.SortOrder
import com.appkitbox.winui4k.chart.StrokeDashStyle
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/*
 * Media category: demo pages for WebView2 and Chart.
 */

// region WebView2 page

/** The WebView2 page: lines up demos for trying out WWebView's various features. */
internal fun buildWebView2Page(): WComponent {
    val page = buildPage(
        "WebView2",
        "A Microsoft Edge-based web browser control. Try out WWebView's various features." +
            " (Displaying content requires the WebView2 Runtime.)",
    )

    page.add(buildBrowserWebViewExample())
    page.add(buildExecuteScriptExample())
    page.add(buildWebMessageExample())
    return page
}

/** A wrapped, muted-color purpose label to put at the top of a demo. */
private fun purposeLabel(text: String): WLabel = WLabel(text).also {
    it.foreground = TEXT_SECONDARY
    it.textWrapping = TextWrapping.WRAP
}

/** Wraps a WWebView in a border so the browser area's bounds are visible. */
private fun framedWebView(webView: WWebView): WComponent {
    val frame = WBorder(webView)
    frame.borderColor = CARD_BORDER
    frame.borderThickness = 1.0
    frame.cornerRadius = 4.0
    return frame
}

/** One feature example within the Options panel (bold title + content). */
private fun optionsSection(title: String, vararg contents: WComponent): WComponent {
    val section = WPanel(spacing = 8.0)
    section.add(
        WLabel(title).also {
            it.fontWeight = 600
            it.textWrapping = TextWrapping.WRAP
        },
    )
    contents.forEach { section.add(it) }
    return section
}

/** A mini browser: Source / GoBack / GoForward / Reload / NavigationStarting / NavigationCompleted. */
private fun buildBrowserWebViewExample(): WComponent {
    val homeUrl = "https://learn.microsoft.com/windows/apps/winui/"
    val webView = WWebView(source = homeUrl)
    webView.width = 720.0
    webView.height = 400.0

    val status = WLabel("Loading...").also { it.foreground = TEXT_SECONDARY }

    val addressBar = WTextField()
    addressBar.text = homeUrl
    addressBar.width = 460.0
    val backButton = WButton("←").also { it.isEnabled = false }
    val forwardButton = WButton("→").also { it.isEnabled = false }
    val reloadButton = WButton("Reload")
    val goButton = WButton("Go")

    backButton.addActionListener { webView.goBack() }
    forwardButton.addActionListener { webView.goForward() }
    reloadButton.addActionListener { webView.reload() }
    goButton.addActionListener {
        // An invalid URI (e.g. missing a scheme) makes CreateUri throw, so catch it instead of crashing
        try {
            webView.source = addressBar.text
        } catch (_: Exception) {
            status.text = "Invalid URL: ${addressBar.text}"
        }
    }

    webView.addCoreWebView2InitializedListener { exceptionHresult ->
        if (exceptionHresult != 0) {
            status.text = "CoreWebView2 initialization failed: HRESULT=0x%08x".format(exceptionHresult)
        }
    }
    webView.addNavigationStartingListener { uri ->
        status.text = "Navigating: $uri"
        true // returning false would cancel it
    }
    webView.addNavigationCompletedListener { isSuccess, errorStatus ->
        backButton.isEnabled = webView.canGoBack
        forwardButton.isEnabled = webView.canGoForward
        addressBar.text = webView.source
        status.text = if (isSuccess) "Done: ${webView.documentTitle}" else "Failed: $errorStatus"
    }

    val toolBar = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    toolBar.add(backButton)
    toolBar.add(forwardButton)
    toolBar.add(reloadButton)
    toolBar.add(addressBar)
    toolBar.add(goButton)

    val body = WPanel(spacing = 8.0)
    body.add(toolBar)
    body.add(framedWebView(webView))
    body.add(status)
    return buildExample("A mini browser (Source / GoBack / GoForward / Reload)", body)
}

/** ExecuteScript: run the page's JavaScript from Kotlin and receive the result as JSON. */
@Suppress("LongMethod") // Declarative UI-building sample code
private fun buildExecuteScriptExample(): WComponent {
    val webView = WWebView()
    webView.width = 560.0
    webView.height = 240.0
    webView.navigateToString(
        """
        <!doctype html>
        <html><body style="font-family: sans-serif; margin: 16px">
        <h3 style="margin: 0 0 8px">Laptop (mock page)</h3>
        <p style="margin: 4px 0">Price: <span id="price">$899</span></p>
        <p style="margin: 4px 0" id="stock">Stock: In stock</p>
        </body></html>
        """.trimIndent(),
    )

    val body = WPanel(spacing = 8.0)
    body.add(
        purposeLabel(
            "ExecuteScript runs JavaScript on the page from Kotlin and receives the value of the " +
                "last expression as JSON. It's useful for reading values from a page (a price, input " +
                "contents) or rewriting the display via DOM manipulation.",
        ).also { it.width = 560.0 },
    )
    body.add(framedWebView(webView))

    val scriptField = WTextField()
    scriptField.text = "document.getElementById('price').textContent"
    val result = purposeLabel("The result (JSON) will show up here after running")

    val runButton = WButton("Run")
    val runScript = {
        webView.executeScript(scriptField.text) { json ->
            result.text = "Result (JSON): $json"
        }
    }
    runButton.addActionListener { runScript() }

    // Presets: swap in a script and run it right away
    val readPresetButton = WButton("Read a value (price)")
    readPresetButton.addActionListener {
        scriptField.text = "document.getElementById('price').textContent"
        runScript()
    }
    // Let the stock display toggle between "In stock" / "Out of stock" via separate buttons
    val inStockButton = WButton("In stock")
    inStockButton.addActionListener {
        scriptField.text = "document.getElementById('stock').textContent = 'Stock: In stock'"
        runScript()
    }
    val outOfStockButton = WButton("Out of stock")
    outOfStockButton.addActionListener {
        scriptField.text = "document.getElementById('stock').textContent = 'Stock: Out of stock'"
        runScript()
    }
    val stockButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    stockButtons.add(inStockButton)
    stockButtons.add(outOfStockButton)

    val options = WPanel(spacing = 16.0)
    options.add(
        optionsSection(
            "Run a script directly",
            optionsLabel("The script to run (editable)"),
            scriptField,
            runButton,
        ),
    )
    options.add(
        optionsSection(
            "Presets",
            optionsLabel("Try common use cases with one click"),
            readPresetButton,
            optionsLabel("Rewrite the display (stock)"),
            stockButtons,
        ),
    )
    options.add(optionsSection("Result", result))
    return buildExample("ExecuteScript (run the page's JavaScript from Kotlin)", body, options)
}

/** WebMessage: two-way messaging between Kotlin and the page's JavaScript. */
@Suppress("LongMethod") // Declarative UI-building sample code
private fun buildWebMessageExample(): WComponent {
    val webView = WWebView()
    webView.width = 560.0
    webView.height = 280.0
    // Page side: a chat-like page that shows a send/receive log and can send the input field's text to Kotlin
    webView.navigateToString(
        """
        <!doctype html>
        <html><body style="font-family: sans-serif; margin: 12px">
        <div style="font-weight: bold; margin-bottom: 4px">Page side (JavaScript)</div>
        <ul id="log" style="height: 140px; overflow-y: auto; margin: 4px 0; padding-left: 20px;
                            border: 1px solid #ccc; list-style: none"></ul>
        <input id="input" value="A notice from the page" style="width: 220px">
        <button onclick="send()">Send to Kotlin</button>
        <script>
        function log(text) {
            const item = document.createElement("li");
            item.textContent = text;
            const logList = document.getElementById("log");
            logList.appendChild(item);
            logList.scrollTop = logList.scrollHeight;
        }
        function send() {
            const text = document.getElementById("input").value;
            window.chrome.webview.postMessage(text);
            log("Sent to Kotlin: " + text);
        }
        window.chrome.webview.addEventListener("message", (e) => {
            log("Received from Kotlin: " + e.data);
        });
        </script>
        </body></html>
        """.trimIndent(),
    )

    val body = WPanel(spacing = 8.0)
    body.add(
        purposeLabel(
            "WebMessage is a mechanism for Kotlin and the page's JavaScript to send messages to " +
                "each other. In a layout that embeds a web page as part of the screen, it's useful " +
                "for calling Kotlin-side processing (showing a notification, saving a file, etc.) " +
                "from an action on the page, or updating the page's display from Kotlin-side processing.",
        ).also { it.width = 560.0 },
    )
    body.add(framedWebView(webView))

    val messageField = WTextField()
    messageField.text = "An update notice from Kotlin"
    val sendButton = WButton("Send to page")
    sendButton.addActionListener { webView.postWebMessageAsString(messageField.text) }

    val received = purposeLabel("Pressing \"Send to Kotlin\" on the page will show it here")
    webView.addWebMessageReceivedListener { messageAsJson ->
        received.text = "Received from page: ${unquoteJsonString(messageAsJson)}"
    }

    val options = WPanel(spacing = 16.0)
    options.add(
        optionsSection(
            "Send from Kotlin processing to the page",
            optionsLabel("The message to send (editable)"),
            messageField,
            sendButton,
        ),
    )
    options.add(
        optionsSection(
            "Receive from the page in Kotlin processing",
            received,
        ),
    )
    return buildExample("WebMessage (two-way messaging with the page)", body, options)
}

/**
 * Extracts a plain string from a WebMessageReceived JSON representation
 * (string messages arrive quoted, so strip that for display in the demo).
 */
private fun unquoteJsonString(json: String): String =
    if (json.length >= 2 && json.startsWith("\"") && json.endsWith("\"")) {
        json.substring(1, json.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
    } else {
        json
    }

// endregion

// region Chart page

/** The Chart page: lines up demos for trying out the main features of WChart and the chart package (data model, series, axes). */
internal fun buildChartPage(): WComponent {
    val page = buildPage(
        "Chart",
        "A chart that draws numeric, category, and date-time data as lines, areas, and bars (Windows App SDK 2.5 experimental)." +
            " With the same MVC structure as JTable, the data is handled by SampleModel, the series by LineSeries / AreaSeries / BarSeries, " +
            "and the axes by LinearAxis / CategoryAxis / DateTimeAxis.",
    )

    page.add(buildBasicLineChartExample())
    page.add(buildBarChartExample())
    page.add(buildAxisOptionsExample())
    page.add(buildLinesAndMarkersExample())
    page.add(buildCombinedSeriesExample())
    page.add(buildTimeSeriesExample())
    page.add(buildLiveChartExample())
    page.add(buildSeriesManagementExample())
    return page
}

private val CHART_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")

/** The chart for the demos. Only the height is fixed; the width follows the card ([stretchChart]). */
private fun buildDemoChart(): WChart = WChart().also { it.height = 320.0 }

/** buildExample left-aligns the example body (to its content width), so stretch the chart back to the full width of the card. */
private fun stretchChart(chart: WChart) {
    chart.horizontalAlignment = HorizontalAlignment.STRETCH
}

/** A toggle switch for the Options panel (with a header). */
private fun chartToggle(header: String, on: Boolean, onChanged: (Boolean) -> Unit): WToggleSwitch {
    val toggle = WToggleSwitch(header)
    toggle.isOn = on
    toggle.addItemListener(onChanged)
    return toggle
}

/** A combo box for the Options panel (with a header). Passes the selected index when the selection changes. */
private fun chartComboBox(header: String, items: List<String>, selected: Int, onChanged: (Int) -> Unit): WComboBox {
    val comboBox = WComboBox(items)
    comboBox.header = header
    comboBox.selectedIndex = selected
    comboBox.width = 240.0
    comboBox.addListSelectionListener { onChanged(comboBox.selectedIndex) }
    return comboBox
}

/** A combo box for choosing any value of an enum. */
private fun <E : Enum<E>> chartEnumComboBox(header: String, values: List<E>, selected: E, onChanged: (E) -> Unit): WComboBox =
    chartComboBox(header, values.map { it.name }, values.indexOf(selected)) { onChanged(values[it]) }

/** Basics: a line chart and a legend (LineSeries / CategoryAxis / LinearAxis / ShowLegend / LegendTitle). */
private fun buildBasicLineChartExample(): WComponent {
    val months = DefaultSampleModel(CHART_MONTHS)
    val profits = DefaultSampleModel.of(18.0, 26.0, 33.0, 39.0, 47.0, 52.0)
    val chart = buildDemoChart()
    chart.showLegend = true
    chart.legendTitle = "Profit"
    chart.addSeries(
        LineSeries("2025", months, profits).apply {
            xAxis = CategoryAxis("Month")
            yAxis = LinearAxis("Profit (million yen)").apply {
                minimum = 0.0
                gridLines = GridLines.MAJOR
            }
        },
    )

    val legendTitle = WTextField()
    legendTitle.header = "Legend title (LegendTitle)"
    legendTitle.text = chart.legendTitle
    legendTitle.addTextChangedListener { chart.legendTitle = it }

    val options = WPanel(spacing = 12.0)
    options.add(chartToggle("Legend (ShowLegend)", chart.showLegend) { chart.showLegend = it })
    options.add(legendTitle)
    return buildExample("Line chart and legend (LineSeries / ShowLegend / LegendTitle)", chart, options).also { stretchChart(chart) }
}

/** Bar chart: bar orientation (Orientation) and category sorting (CategoryAxis.SortKey / SortOrder). */
private fun buildBarChartExample(): WComponent {
    val regions = DefaultSampleModel.of("Hokkaido", "Tohoku", "Kanto", "Chubu", "Kinki", "Kyushu")
    val units = DefaultSampleModel.of(42.0, 38.0, 71.0, 47.0, 55.0, 33.0)
    val regionAxis = CategoryAxis("Region")
    val series = BarSeries("Units sold", regions, units).apply {
        orientation = BarOrientation.VERTICAL
        fill = WColor(0, 120, 212)
        showDataLabels = true
        xAxis = regionAxis
        yAxis = LinearAxis("Units sold").apply {
            minimum = 0.0
            gridLines = GridLines.MAJOR
        }
    }
    val chart = buildDemoChart()
    chart.addSeries(series)

    val options = WPanel(spacing = 12.0)
    options.add(chartEnumComboBox("Bar orientation (Orientation)", BarOrientation.entries, series.orientation) { series.orientation = it })
    options.add(chartEnumComboBox("Sort key (SortKey)", CategorySortKey.entries, regionAxis.sortKey) { regionAxis.sortKey = it })
    options.add(chartEnumComboBox("Sort order (SortOrder)", SortOrder.entries, regionAxis.sortOrder) { regionAxis.sortOrder = it })
    options.add(chartToggle("Data labels (ShowDataLabels)", series.showDataLabels) { series.showDataLabels = it })
    return buildExample("Bar chart and category sorting (BarSeries / CategoryAxis)", chart, options).also { stretchChart(chart) }
}

/** Axis settings: visibility, ticks, grid lines, and range (Axis / CartesianAxis / LinearAxis). */
private fun buildAxisOptionsExample(): WComponent {
    val quarterAxis = CategoryAxis("Quarter").apply {
        axisLineColor = WColor(118, 118, 118)
        tickColor = WColor(118, 118, 118)
    }
    val visitorAxis = LinearAxis("Visitors (thousands)").apply {
        minimum = 0.0
        maximum = 100.0
        spacing = 20.0
        gridLines = GridLines.MAJOR
        axisLineColor = WColor(118, 118, 118)
        tickColor = WColor(118, 118, 118)
        gridLineMajorColor = WColor(128, 128, 128, 102)
        gridLineMinorColor = WColor(128, 128, 128, 51)
    }
    val axes = listOf(quarterAxis, visitorAxis)
    val chart = buildDemoChart()
    chart.addSeries(
        LineSeries("Visitors", DefaultSampleModel.of("Q1", "Q2", "Q3", "Q4"), DefaultSampleModel.of(35.0, 62.0, 48.0, 81.0)).apply {
            showDataMarkers = true
            xAxis = quarterAxis
            yAxis = visitorAxis
        },
    )

    // Presets for the Y axis range (null means automatic)
    val ranges = listOf(
        Triple<Double?, Double?, Double?>(0.0, 100.0, 20.0),
        Triple<Double?, Double?, Double?>(null, null, null),
        Triple<Double?, Double?, Double?>(20.0, 90.0, 10.0),
    )

    val options = WPanel(spacing = 12.0)
    options.add(chartToggle("Show axes (IsVisible)", true) { on -> axes.forEach { it.isVisible = on } })
    options.add(chartToggle("Tick labels (ShowTickLabels)", true) { on -> axes.forEach { it.showTickLabels = on } })
    options.add(chartToggle("Tick marks (ShowTickMarks)", visitorAxis.showTickMarks) { on -> axes.forEach { it.showTickMarks = on } })
    options.add(chartEnumComboBox("Y axis grid lines (GridLines)", GridLines.entries, visitorAxis.gridLines) { visitorAxis.gridLines = it })
    options.add(
        chartComboBox("Y axis range (Minimum / Maximum / Spacing)", listOf("0 - 100 (step 20)", "Automatic (null)", "20 - 90 (step 10)"), 0) {
            val (minimum, maximum, spacing) = ranges[it]
            visitorAxis.minimum = minimum
            visitorAxis.maximum = maximum
            visitorAxis.spacing = spacing
        },
    )
    return buildExample("Axis settings (IsVisible / ShowTickLabels / ShowTickMarks / GridLines / range)", chart, options).also { stretchChart(chart) }
}

/** The look of lines, markers, and data labels, and per-data-point overrides (DataLabelOverride / DataMarkerOverride). */
private fun buildLinesAndMarkersExample(): WComponent {
    val visits = listOf(120.0, 135.0, 128.0, 172.0, 150.0, 98.0, 110.0)
    val purple = WColor(135, 100, 184)
    val days = DefaultSampleModel.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val series = LineSeries("Visits", days, DefaultSampleModel(visits)).apply {
        stroke = purple
        dataMarkerColor = purple
        strokeThickness = 2.0
        showDataMarkers = true
        markerShape = MarkerShape.CIRCLE
        xAxis = CategoryAxis("Day of week")
        yAxis = LinearAxis("Visits").apply {
            minimum = 0.0
            gridLines = GridLines.MAJOR
        }
    }
    val peak = visits.indexOf(visits.max())
    val low = visits.indexOf(visits.min())
    val blue = WColor(0, 120, 212)
    fun applyOverrides(on: Boolean) {
        if (on) {
            series.setDataLabelOverride(peak, DataLabelOverride("Max"))
            series.setDataMarkerOverride(peak, DataMarkerOverride(MarkerShape.DIAMOND, WColor(209, 52, 56)))
            series.setDataLabelOverride(low, DataLabelOverride("Min", blue))
            series.setDataMarkerOverride(low, DataMarkerOverride(MarkerShape.TRIANGLE, blue))
        } else {
            series.clearDataLabelOverrides()
            series.clearDataMarkerOverrides()
        }
    }
    applyOverrides(true)
    val chart = buildDemoChart()
    chart.addSeries(series)

    val thickness = WSlider(minimum = 1.0, maximum = 8.0, value = series.strokeThickness)
    thickness.addChangeListener { series.strokeThickness = it }

    val options = WPanel(spacing = 12.0)
    options.add(chartEnumComboBox("Dashes (StrokeDashStyle)", StrokeDashStyle.entries, series.strokeDashStyle) { series.strokeDashStyle = it })
    options.add(optionsLabel("Line thickness (StrokeThickness)"))
    options.add(thickness)
    options.add(chartEnumComboBox("Marker shape (MarkerShape)", MarkerShape.entries, series.markerShape) { series.markerShape = it })
    options.add(chartToggle("Markers (ShowDataMarkers)", series.showDataMarkers) { series.showDataMarkers = it })
    options.add(chartToggle("Data labels (ShowDataLabels)", series.showDataLabels) { series.showDataLabels = it })
    options.add(chartToggle("Max / min overrides (Data*Override)", true) { applyOverrides(it) })
    return buildExample("Lines, markers, and data labels (StrokeDashStyle / MarkerShape / DataLabelOverride)", chart, options).also { stretchChart(chart) }
}

/** Combining series: area, bar, and line series share axes, and each series can be shown or hidden (IsVisible). */
private fun buildCombinedSeriesExample(): WComponent {
    val months = DefaultSampleModel(CHART_MONTHS)
    val monthAxis = CategoryAxis("Month")
    val revenueAxis = LinearAxis("Revenue (million yen)").apply {
        minimum = 0.0
        maximum = 60.0
        spacing = 10.0
        gridLines = GridLines.MAJOR
    }
    val forecast = AreaSeries("Forecast", months, DefaultSampleModel.of(16.0, 25.0, 35.0, 40.0, 48.0, 55.0)).apply {
        stroke = WColor(0, 120, 212)
        strokeThickness = 2.0
        fill = WColor(0, 120, 212, 77)
    }
    val actual = BarSeries("Actual", months, DefaultSampleModel.of(12.0, 23.0, 37.0, 31.0, 46.0, 52.0)).apply {
        orientation = BarOrientation.VERTICAL
        fill = WColor(0, 133, 117)
        showDataLabels = true
    }
    val red = WColor(209, 52, 56)
    val target = LineSeries("Target", months, DefaultSampleModel.of(20.0, 25.0, 40.0, 40.0, 50.0, 50.0)).apply {
        stroke = red
        strokeThickness = 2.0
        strokeDashStyle = StrokeDashStyle.DASH
        showDataMarkers = true
        markerShape = MarkerShape.DIAMOND
        dataMarkerColor = red
    }
    val chart = buildDemoChart()
    chart.showLegend = true
    chart.legendTitle = "Monthly results"
    for (series in listOf(forecast, actual, target)) {
        series.xAxis = monthAxis
        series.yAxis = revenueAxis
        chart.addSeries(series)
    }

    val options = WPanel(spacing = 8.0)
    options.add(optionsLabel("Series to show (IsVisible)"))
    for (series in listOf(forecast, actual, target)) {
        val checkBox = WCheckBox(series.title)
        checkBox.isChecked = true
        checkBox.addItemListener { series.isVisible = it == true }
        options.add(checkBox)
    }
    return buildExample("Combining area, bar, and line series (AreaSeries / BarSeries / LineSeries)", chart, options).also { stretchChart(chart) }
}

/** Time series: a date-time axis (DateTimeAxis range, tick interval unit, and label format). */
private fun buildTimeSeriesExample(): WComponent {
    val start = LocalDate.of(2026, 8, 3)
    val dates = DefaultSampleModel((0 until 5).map { start.plusWeeks(it.toLong()) })
    val temperatures = DefaultSampleModel.of(27.5, 30.1, 29.0, 31.6, 30.4)
    val rangeStart = LocalDateTime.of(2026, 7, 27, 0, 0)
    val rangeEnd = LocalDateTime.of(2026, 9, 7, 0, 0)
    val dateAxis = DateTimeAxis("Date").apply {
        minimum = rangeStart
        maximum = rangeEnd
        intervalType = DateTimeIntervalType.WEEK
        labelFormat = "month day"
    }
    val chart = buildDemoChart()
    chart.addSeries(
        AreaSeries("Temperature", dates, temperatures).apply {
            xAxis = dateAxis
            yAxis = LinearAxis("Temperature (°C)").apply { gridLines = GridLines.MAJOR }
        },
    )

    val formats = listOf("month day", "shortdate", "month.abbreviated day", "year month")
    val options = WPanel(spacing = 12.0)
    options.add(
        chartEnumComboBox("Tick interval unit (IntervalType)", DateTimeIntervalType.entries, dateAxis.intervalType) {
            dateAxis.intervalType = it
        },
    )
    options.add(chartComboBox("Label format (LabelFormat)", formats, 0) { dateAxis.labelFormat = formats[it] })
    options.add(
        chartToggle("Automatic range (Minimum / Maximum = null)", false) { auto ->
            dateAxis.minimum = if (auto) null else rangeStart
            dateAxis.maximum = if (auto) null else rangeEnd
        },
    )
    return buildExample("Time series (DateTimeAxis / IntervalType / LabelFormat)", chart, options).also { stretchChart(chart) }
}

/** Live updates: changing a DefaultSampleModel immediately redraws the chart that shows it. */
private fun buildLiveChartExample(): WComponent {
    val incoming = listOf(176.0, 169.0, 181.0, 165.0, 172.0, 190.0, 158.0)
    var next = 0
    val responseTimes = DefaultSampleModel.of(184.0, 179.0, 173.0, 171.0, 168.0)
    val chart = buildDemoChart()
    chart.addSeries(
        LineSeries("Response time", yValues = responseTimes).apply {
            showDataMarkers = true
            yAxis = LinearAxis("Milliseconds").apply {
                minimum = 0.0
                gridLines = GridLines.MAJOR
            }
        },
    )
    val status = optionsLabel("")
    fun updateStatus() {
        status.text = "Latest: ${responseTimes[responseTimes.size() - 1].toInt()} ms (${responseTimes.size()} values)"
    }
    updateStatus()

    // Append the new value at the end, and drop old values from the front once there are more than 8
    fun addSample() {
        responseTimes.addElement(incoming[next])
        next = (next + 1) % incoming.size
        if (responseTimes.size() > LIVE_CHART_CAPACITY) responseTimes.remove(0)
        updateStatus()
    }

    val add = WButton("Add a value (addElement)")
    add.addActionListener { addSample() }
    val change = WButton("Double the latest value (set)")
    change.addActionListener {
        val last = responseTimes.size() - 1
        responseTimes[last] = responseTimes[last] * 2
        updateStatus()
    }

    // Auto update: change the model via invokeLater from a timer off the UI thread (the same idea as Swing's Timer)
    var timer: ScheduledExecutorService? = null
    val auto = chartToggle("Auto update (every second)", false) { on ->
        timer?.shutdownNow()
        timer = if (on) {
            Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "chart-live-demo").also { it.isDaemon = true } }.also {
                it.scheduleAtFixedRate({ WinUiUtilities.invokeLater { addSample() } }, 1, 1, TimeUnit.SECONDS)
            }
        } else {
            null
        }
    }

    val options = WPanel(spacing = 12.0)
    options.add(add)
    options.add(change)
    options.add(auto)
    options.add(status)
    return buildExample("Live updates (DefaultSampleModel change notifications)", chart, options).also { stretchChart(chart) }
}

/** The maximum number of values shown in the live update demo. */
private const val LIVE_CHART_CAPACITY = 8

/** Adding and removing series: WChart.addSeries / removeSeriesAt (the axes a series uses are registered and unregistered automatically). */
private fun buildSeriesManagementExample(): WComponent {
    val months = DefaultSampleModel(CHART_MONTHS)
    val seed = listOf(18.0, 27.0, 22.0, 41.0, 36.0, 52.0)
    val palette = listOf(
        WColor(0, 120, 212),
        WColor(209, 52, 56),
        WColor(0, 133, 117),
        WColor(135, 100, 184),
        WColor(202, 80, 16),
    )
    val monthAxis = CategoryAxis("Month")
    val valueAxis = LinearAxis("Value").apply {
        minimum = 0.0
        gridLines = GridLines.MAJOR
    }
    val chart = buildDemoChart()
    chart.showLegend = true
    var number = 0
    val status = optionsLabel("")
    fun updateStatus() {
        status.text = "Series: ${chart.seriesCount} / registered axes: ${chart.getAxes().size}"
    }

    fun addSeries() {
        number++
        val values = seed.map { it * (0.6 + 0.1 * (number % palette.size)) + number * 3 }
        chart.addSeries(
            LineSeries("Series $number", months, DefaultSampleModel(values)).apply {
                stroke = palette[(number - 1) % palette.size]
                strokeThickness = 2.0
                xAxis = monthAxis
                yAxis = valueAxis
            },
        )
        updateStatus()
    }
    addSeries()
    addSeries()

    val add = WButton("Add a series (addSeries)")
    add.addActionListener { addSeries() }
    val remove = WButton("Remove the last series (removeSeriesAt)")
    remove.addActionListener {
        if (chart.seriesCount > 0) chart.removeSeriesAt(chart.seriesCount - 1)
        updateStatus()
    }

    val options = WPanel(spacing = 12.0)
    options.add(add)
    options.add(remove)
    options.add(status)
    return buildExample("Adding and removing series (addSeries / removeSeriesAt / automatic axis registration)", chart, options).also { stretchChart(chart) }
}

// endregion
