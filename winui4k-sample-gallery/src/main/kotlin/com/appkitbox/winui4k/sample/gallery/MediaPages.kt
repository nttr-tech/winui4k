package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WWebView

/*
 * Media category: the WebView2 demo page.
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
        } catch (e: Exception) {
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
