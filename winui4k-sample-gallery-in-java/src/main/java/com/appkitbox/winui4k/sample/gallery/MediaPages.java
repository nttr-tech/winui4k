package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WWebView;

import kotlin.Unit;

/*
 * Media category: the WebView2 demo page.
 */
final class MediaPages {
    private MediaPages() {
    }

    // region WebView2 page

    /** The WebView2 page: lines up demos for trying out WWebView's various features. */
    static WComponent buildWebView2Page() {
        WPanel page = GalleryScaffold.buildPage(
            "WebView2",
            "A Microsoft Edge-based web browser control. Try out WWebView's various features."
                + " (Displaying content requires the WebView2 Runtime.)");

        page.add(buildBrowserWebViewExample());
        page.add(buildExecuteScriptExample());
        page.add(buildWebMessageExample());
        return page;
    }

    /** A wrapped, muted-color purpose label to put at the top of a demo. */
    private static WLabel purposeLabel(String text) {
        WLabel label = new WLabel(text);
        label.setForeground(GalleryTheme.TEXT_SECONDARY());
        label.setTextWrapping(TextWrapping.WRAP);
        return label;
    }

    /** Wraps a WWebView in a border so the browser area's bounds are visible. */
    private static WComponent framedWebView(WWebView webView) {
        WBorder frame = new WBorder(webView);
        frame.setBorderColor(GalleryTheme.CARD_BORDER());
        frame.setBorderThickness(1.0);
        frame.setCornerRadius(4.0);
        return frame;
    }

    /** One feature example within the Options panel (bold title + content). */
    private static WComponent optionsSection(String title, WComponent... contents) {
        WPanel section = new WPanel(8.0, Orientation.VERTICAL);
        WLabel titleLabel = new WLabel(title);
        titleLabel.setFontWeight(600);
        titleLabel.setTextWrapping(TextWrapping.WRAP);
        section.add(titleLabel);
        for (WComponent content : contents) {
            section.add(content);
        }
        return section;
    }

    /** A mini browser: Source / GoBack / GoForward / Reload / NavigationStarting / NavigationCompleted. */
    private static WComponent buildBrowserWebViewExample() {
        String homeUrl = "https://learn.microsoft.com/ja-jp/windows/apps/winui/";
        WWebView webView = new WWebView(homeUrl);
        webView.setWidth(720.0);
        webView.setHeight(400.0);

        WLabel status = new WLabel("Loading...");
        status.setForeground(GalleryTheme.TEXT_SECONDARY());

        WTextField addressBar = new WTextField("");
        addressBar.setText(homeUrl);
        addressBar.setWidth(460.0);
        WButton backButton = new WButton("←");
        backButton.setEnabled(false);
        WButton forwardButton = new WButton("→");
        forwardButton.setEnabled(false);
        WButton reloadButton = new WButton("Reload");
        WButton goButton = new WButton("Go");

        backButton.addActionListener(() -> {
            webView.goBack();
            return Unit.INSTANCE;
        });
        forwardButton.addActionListener(() -> {
            webView.goForward();
            return Unit.INSTANCE;
        });
        reloadButton.addActionListener(() -> {
            webView.reload();
            return Unit.INSTANCE;
        });
        goButton.addActionListener(() -> {
            // An invalid URI (e.g. missing a scheme) makes CreateUri throw, so catch it instead of crashing
            try {
                webView.setSource(addressBar.getText());
            } catch (Exception e) {
                status.setText("Invalid URL: " + addressBar.getText());
            }
            return Unit.INSTANCE;
        });

        webView.addCoreWebView2InitializedListener(exceptionHresult -> {
            if (exceptionHresult != 0) {
                status.setText(String.format("CoreWebView2 initialization failed: HRESULT=0x%08x", exceptionHresult));
            }
            return Unit.INSTANCE;
        });
        webView.addNavigationStartingListener(uri -> {
            status.setText("Navigating: " + uri);
            return true; // returning false cancels the navigation
        });
        webView.addNavigationCompletedListener((isSuccess, errorStatus) -> {
            backButton.setEnabled(webView.getCanGoBack());
            forwardButton.setEnabled(webView.getCanGoForward());
            addressBar.setText(webView.getSource());
            status.setText(isSuccess ? "Done: " + webView.getDocumentTitle() : "Failed: " + errorStatus);
            return Unit.INSTANCE;
        });

        WPanel toolBar = new WPanel(8.0, Orientation.HORIZONTAL);
        toolBar.add(backButton);
        toolBar.add(forwardButton);
        toolBar.add(reloadButton);
        toolBar.add(addressBar);
        toolBar.add(goButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(toolBar);
        body.add(framedWebView(webView));
        body.add(status);
        return GalleryScaffold.buildExample("A mini browser (Source / GoBack / GoForward / Reload)", body);
    }

    /** ExecuteScript: run the page's JavaScript from Kotlin and receive the result as JSON. */
    private static WComponent buildExecuteScriptExample() {
        WWebView webView = new WWebView("");
        webView.setWidth(560.0);
        webView.setHeight(240.0);
        webView.navigateToString(
            "<!doctype html>\n"
                + "<html><body style=\"font-family: sans-serif; margin: 16px\">\n"
                + "<h3 style=\"margin: 0 0 8px\">Laptop (mock page)</h3>\n"
                + "<p style=\"margin: 4px 0\">Price: <span id=\"price\">$899</span></p>\n"
                + "<p style=\"margin: 4px 0\" id=\"stock\">Stock: In stock</p>\n"
                + "</body></html>");

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        WLabel purpose = purposeLabel(
            "ExecuteScript runs a page's JavaScript from Kotlin and receives the value of the "
                + "last expression as JSON. It's useful for reading values from a page (a price, input "
                + "contents) and for rewriting what it shows through DOM manipulation.");
        purpose.setWidth(560.0);
        body.add(purpose);
        body.add(framedWebView(webView));

        WTextField scriptField = new WTextField("");
        scriptField.setText("document.getElementById('price').textContent");
        WLabel result = purposeLabel("The result (JSON) will show up here after running");

        WButton runButton = new WButton("Run");
        Runnable runScript = () -> webView.executeScript(scriptField.getText(), json -> {
            result.setText("Result (JSON): " + json);
            return Unit.INSTANCE;
        });
        runButton.addActionListener(() -> {
            runScript.run();
            return Unit.INSTANCE;
        });

        // Presets: swap in a script and run it right away
        WButton readPresetButton = new WButton("Read a value (price)");
        readPresetButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('price').textContent");
            runScript.run();
            return Unit.INSTANCE;
        });
        // Let the stock display toggle between "In stock" / "Out of stock" via separate buttons
        WButton inStockButton = new WButton("In stock");
        inStockButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('stock').textContent = 'Stock: In stock'");
            runScript.run();
            return Unit.INSTANCE;
        });
        WButton outOfStockButton = new WButton("Out of stock");
        outOfStockButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('stock').textContent = 'Stock: Out of stock'");
            runScript.run();
            return Unit.INSTANCE;
        });
        WPanel stockButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        stockButtons.add(inStockButton);
        stockButtons.add(outOfStockButton);

        WPanel options = new WPanel(16.0, Orientation.VERTICAL);
        options.add(
            optionsSection(
                "Run a script directly",
                GalleryScaffold.optionsLabel("The script to run (editable)"),
                scriptField,
                runButton));
        options.add(
            optionsSection(
                "Presets",
                GalleryScaffold.optionsLabel("Try common use cases with one click"),
                readPresetButton,
                GalleryScaffold.optionsLabel("Rewrite the display (stock)"),
                stockButtons));
        options.add(optionsSection("Result", result));
        return GalleryScaffold.buildExample("ExecuteScript (run the page's JavaScript from Kotlin)", body, options);
    }

    /** WebMessage: two-way messaging between Kotlin and the page's JavaScript. */
    private static WComponent buildWebMessageExample() {
        WWebView webView = new WWebView("");
        webView.setWidth(560.0);
        webView.setHeight(280.0);
        // Page side: a chat-like page that shows a send/receive log and can send the input field's text to Kotlin
        webView.navigateToString(
            "<!doctype html>\n"
                + "<html><body style=\"font-family: sans-serif; margin: 12px\">\n"
                + "<div style=\"font-weight: bold; margin-bottom: 4px\">The page side (JavaScript)</div>\n"
                + "<ul id=\"log\" style=\"height: 140px; overflow-y: auto; margin: 4px 0; padding-left: 20px;\n"
                + "                    border: 1px solid #ccc; list-style: none\"></ul>\n"
                + "<input id=\"input\" value=\"A message from the page\" style=\"width: 220px\">\n"
                + "<button onclick=\"send()\">Send to Kotlin</button>\n"
                + "<script>\n"
                + "function log(text) {\n"
                + "    const item = document.createElement(\"li\");\n"
                + "    item.textContent = text;\n"
                + "    const logList = document.getElementById(\"log\");\n"
                + "    logList.appendChild(item);\n"
                + "    logList.scrollTop = logList.scrollHeight;\n"
                + "}\n"
                + "function send() {\n"
                + "    const text = document.getElementById(\"input\").value;\n"
                + "    window.chrome.webview.postMessage(text);\n"
                + "    log(\"Sent to Kotlin: \" + text);\n"
                + "}\n"
                + "window.chrome.webview.addEventListener(\"message\", (e) => {\n"
                + "    log(\"Received from Kotlin: \" + e.data);\n"
                + "});\n"
                + "</script>\n"
                + "</body></html>");

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        WLabel purpose = purposeLabel(
            "WebMessage is a mechanism for Kotlin and the page's JavaScript to send messages to "
                + "each other. In a layout that embeds a web page as part of the screen, it's useful "
                + "for calling Kotlin-side processing (showing a notification, saving a file, etc.) "
                + "from an action on the page, or updating the page's display from Kotlin-side processing.");
        purpose.setWidth(560.0);
        body.add(purpose);
        body.add(framedWebView(webView));

        WTextField messageField = new WTextField("");
        messageField.setText("An update notice from Kotlin");
        WButton sendButton = new WButton("Send to page");
        sendButton.addActionListener(() -> {
            webView.postWebMessageAsString(messageField.getText());
            return Unit.INSTANCE;
        });

        WLabel received = purposeLabel("Pressing \"Send to Kotlin\" on the page will show it here");
        webView.addWebMessageReceivedListener(messageAsJson -> {
            received.setText("Received from page: " + unquoteJsonString(messageAsJson));
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(16.0, Orientation.VERTICAL);
        options.add(
            optionsSection(
                "Send from Kotlin processing to the page",
                GalleryScaffold.optionsLabel("The message to send (editable)"),
                messageField,
                sendButton));
        options.add(
            optionsSection(
                "Receive from the page in Kotlin processing",
                received));
        return GalleryScaffold.buildExample("WebMessage (two-way messaging with the page)", body, options);
    }

    /**
     * Extracts a plain string from a WebMessageReceived JSON representation
     * (string messages arrive quoted, so strip that for display in the demo).
     */
    private static String unquoteJsonString(String json) {
        if (json.length() >= 2 && json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        } else {
            return json;
        }
    }

    // endregion
}
