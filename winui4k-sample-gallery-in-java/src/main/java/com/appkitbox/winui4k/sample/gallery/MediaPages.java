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
            Messages.get("webview2.description"));

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
        WPanel section = new WPanel(8.0);
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
        String homeUrl = Messages.get("webview2.browser.homeurl");
        WWebView webView = new WWebView(homeUrl);
        webView.setWidth(720.0);
        webView.setHeight(400.0);

        WLabel status = new WLabel(Messages.get("webview2.browser.loading"));
        status.setForeground(GalleryTheme.TEXT_SECONDARY());

        WTextField addressBar = new WTextField();
        addressBar.setText(homeUrl);
        addressBar.setWidth(460.0);
        WButton backButton = new WButton("←");
        backButton.setEnabled(false);
        WButton forwardButton = new WButton("→");
        forwardButton.setEnabled(false);
        WButton reloadButton = new WButton(Messages.get("webview2.browser.reload"));
        WButton goButton = new WButton(Messages.get("webview2.browser.go"));

        backButton.addActionListener(() -> {
            webView.goBack();
        });
        forwardButton.addActionListener(() -> {
            webView.goForward();
        });
        reloadButton.addActionListener(() -> {
            webView.reload();
        });
        goButton.addActionListener(() -> {
            // An invalid URI (e.g. missing a scheme) makes CreateUri throw, so catch it instead of crashing
            try {
                webView.setSource(addressBar.getText());
            } catch (Exception e) {
                status.setText(Messages.get("webview2.browser.invalidurl", addressBar.getText()));
            }
        });

        webView.addCoreWebView2InitializedListener(exceptionHresult -> {
            if (exceptionHresult != 0) {
                status.setText(Messages.get("webview2.browser.initfailed", String.format("0x%08x", exceptionHresult)));
            }
        });
        webView.addNavigationStartingListener(uri -> {
            status.setText(Messages.get("webview2.browser.navigating", uri));
            return true; // returning false cancels the navigation
        });
        webView.addNavigationCompletedListener((isSuccess, errorStatus) -> {
            backButton.setEnabled(webView.getCanGoBack());
            forwardButton.setEnabled(webView.getCanGoForward());
            addressBar.setText(webView.getSource());
            status.setText(isSuccess
                ? Messages.get("webview2.browser.completed", webView.getDocumentTitle())
                : Messages.get("webview2.browser.failed", String.valueOf(errorStatus)));
        });

        WPanel toolBar = new WPanel(8.0, Orientation.HORIZONTAL);
        toolBar.add(backButton);
        toolBar.add(forwardButton);
        toolBar.add(reloadButton);
        toolBar.add(addressBar);
        toolBar.add(goButton);

        WPanel body = new WPanel(8.0);
        body.add(toolBar);
        body.add(framedWebView(webView));
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("webview2.browser.title"), body);
    }

    /** ExecuteScript: run the page's JavaScript from Kotlin and receive the result as JSON. */
    private static WComponent buildExecuteScriptExample() {
        WWebView webView = new WWebView("");
        webView.setWidth(560.0);
        webView.setHeight(240.0);
        webView.navigateToString(
            "<!doctype html>\n"
                + "<html><body style=\"font-family: sans-serif; margin: 16px\">\n"
                + "<h3 style=\"margin: 0 0 8px\">" + Messages.get("webview2.executescript.html.heading") + "</h3>\n"
                + "<p style=\"margin: 4px 0\">" + Messages.get("webview2.executescript.html.price")
                + " <span id=\"price\">" + Messages.get("webview2.executescript.html.pricevalue") + "</span></p>\n"
                + "<p style=\"margin: 4px 0\" id=\"stock\">" + Messages.get("webview2.executescript.html.instock") + "</p>\n"
                + "</body></html>");

        WPanel body = new WPanel(8.0);
        WLabel purpose = purposeLabel(
            Messages.get("webview2.executescript.purpose"));
        purpose.setWidth(560.0);
        body.add(purpose);
        body.add(framedWebView(webView));

        WTextField scriptField = new WTextField();
        scriptField.setText("document.getElementById('price').textContent");
        WLabel result = purposeLabel(Messages.get("webview2.executescript.result.placeholder"));

        WButton runButton = new WButton(Messages.get("webview2.executescript.run"));
        Runnable runScript = () -> webView.executeScript(scriptField.getText(), json -> {
            result.setText(Messages.get("webview2.executescript.result", json));
        });
        runButton.addActionListener(() -> {
            runScript.run();
        });

        // Presets: swap in a script and run it right away
        WButton readPresetButton = new WButton(Messages.get("webview2.executescript.readprice"));
        readPresetButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('price').textContent");
            runScript.run();
        });
        // Let the stock display toggle between "In stock" / "Out of stock" via separate buttons
        WButton inStockButton = new WButton(Messages.get("webview2.executescript.instock"));
        inStockButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('stock').textContent = '"
                + Messages.get("webview2.executescript.html.instock") + "'");
            runScript.run();
        });
        WButton outOfStockButton = new WButton(Messages.get("webview2.executescript.outofstock"));
        outOfStockButton.addActionListener(() -> {
            scriptField.setText("document.getElementById('stock').textContent = '"
                + Messages.get("webview2.executescript.html.outofstock") + "'");
            runScript.run();
        });
        WPanel stockButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        stockButtons.add(inStockButton);
        stockButtons.add(outOfStockButton);

        WPanel options = new WPanel(16.0);
        options.add(
            optionsSection(
                Messages.get("webview2.executescript.direct"),
                GalleryScaffold.optionsLabel(Messages.get("webview2.executescript.direct.label")),
                scriptField,
                runButton));
        options.add(
            optionsSection(
                Messages.get("webview2.executescript.presets"),
                GalleryScaffold.optionsLabel(Messages.get("webview2.executescript.presets.label")),
                readPresetButton,
                GalleryScaffold.optionsLabel(Messages.get("webview2.executescript.presets.stock")),
                stockButtons));
        options.add(optionsSection(Messages.get("webview2.executescript.output"), result));
        return GalleryScaffold.buildExample(Messages.get("webview2.executescript.title"), body, options);
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
                + "<div style=\"font-weight: bold; margin-bottom: 4px\">" + Messages.get("webview2.webmessage.html.heading") + "</div>\n"
                + "<ul id=\"log\" style=\"height: 140px; overflow-y: auto; margin: 4px 0; padding-left: 20px;\n"
                + "                    border: 1px solid #ccc; list-style: none\"></ul>\n"
                + "<input id=\"input\" value=\"" + Messages.get("webview2.webmessage.html.input") + "\" style=\"width: 220px\">\n"
                + "<button onclick=\"send()\">" + Messages.get("webview2.webmessage.html.send") + "</button>\n"
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
                + "    log(\"" + Messages.get("webview2.webmessage.html.sent") + " \" + text);\n"
                + "}\n"
                + "window.chrome.webview.addEventListener(\"message\", (e) => {\n"
                + "    log(\"" + Messages.get("webview2.webmessage.html.received") + " \" + e.data);\n"
                + "});\n"
                + "</script>\n"
                + "</body></html>");

        WPanel body = new WPanel(8.0);
        WLabel purpose = purposeLabel(
            Messages.get("webview2.webmessage.purpose"));
        purpose.setWidth(560.0);
        body.add(purpose);
        body.add(framedWebView(webView));

        WTextField messageField = new WTextField();
        messageField.setText(Messages.get("webview2.webmessage.message.default"));
        WButton sendButton = new WButton(Messages.get("webview2.webmessage.send"));
        sendButton.addActionListener(() -> {
            webView.postWebMessageAsString(messageField.getText());
        });

        WLabel received = purposeLabel(Messages.get("webview2.webmessage.received.placeholder"));
        webView.addWebMessageReceivedListener(messageAsJson -> {
            received.setText(Messages.get("webview2.webmessage.received", unquoteJsonString(messageAsJson)));
        });

        WPanel options = new WPanel(16.0);
        options.add(
            optionsSection(
                Messages.get("webview2.webmessage.tosection"),
                GalleryScaffold.optionsLabel(Messages.get("webview2.webmessage.tosection.label")),
                messageField,
                sendButton));
        options.add(
            optionsSection(
                Messages.get("webview2.webmessage.fromsection"),
                received));
        return GalleryScaffold.buildExample(Messages.get("webview2.webmessage.title"), body, options);
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
