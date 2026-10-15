package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.table.DefaultTableModel
import java.util.concurrent.TimeUnit

/** The model and the visual tree helpers shared by the WTable tests. */
internal object TableTestSupport {
    /** A model of 3 columns (product name / price / in stock) × 4 rows. */
    fun productModel(): DefaultTableModel = object : DefaultTableModel(
        listOf(
            listOf("Apple", 150, true),
            listOf("Orange", 80, false),
            listOf("Grape", 480, true),
            listOf("Peach", 320, false),
        ),
        listOf("Product", "Price", "In Stock"),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            1 -> Integer::class.java
            2 -> java.lang.Boolean::class.java
            else -> String::class.java
        }
    }

    /** Shows [table], waits for its template to be applied, runs [block] on the UI thread, and then removes it. */
    fun <T> withShownTable(table: WTable, block: (WTable) -> T): T {
        UiTestHarness.attachAndAwaitLoaded(table)
        try {
            return onUiThreadGet {
                table.updateLayout()
                block(table)
            }
        } finally {
            UiTestHarness.detach(table)
        }
    }

    /** The runtime class name of [node] (a DependencyObject) (IInspectable.GetRuntimeClassName). */
    fun runtimeClassName(node: ComPtr): String = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        node.call(4, out) // IInspectable::GetRuntimeClassName
        val hstring = Ffi.backend.memory.getPtr(out, 0)
        try {
            Hstring.read(hstring)
        } finally {
            Hstring.free(hstring)
        }
    }

    /** The children of [node] (VisualTreeHelper.GetChild). The caller releases them. */
    fun children(node: ComPtr): List<ComPtr> {
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        try {
            val count = Ffi.backend.withScope { scope ->
                val out = scope.allocate(4)
                statics.call(XamlInterop.IVisualTreeHelperStatics_GetChildrenCount, node.ptr, out)
                Ffi.backend.memory.getInt(out, 0)
            }
            return (0 until count).mapNotNull { statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetChild, node.ptr, it) }
        } finally {
            statics.release()
        }
    }

    /** The non-empty strings of the TextBlocks under [node] (in display order). */
    fun texts(node: ComPtr): List<String> {
        val own = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
            textBlock.getString(XamlInterop.ITextBlock_get_Text).also { textBlock.release() }
        }
        return listOfNotNull(own?.takeIf { it.isNotEmpty() }) + children(node).flatMap { child -> texts(child).also { child.release() } }
    }

    /** The first TextBlock under [node] whose text is [text] (an owned reference). Null if none is found. */
    fun findTextBlock(node: ComPtr, text: String): ComPtr? {
        val matched = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
            try {
                textBlock.getString(XamlInterop.ITextBlock_get_Text) == text
            } finally {
                textBlock.release()
            }
        } == true
        if (matched) return node.also { it.addRef() }
        val children = children(node)
        try {
            return children.firstNotNullOfOrNull { findTextBlock(it, text) }
        } finally {
            children.forEach { it.release() }
        }
    }

    /** The row of the TextBlock whose text is [text] in [table] (TableViewRowInvoker.viewRowOf; identifies the row on a double-click). */
    fun viewRowOfText(table: WTable, text: String): Int {
        val textBlock = checkNotNull(findTextBlock(table.dependencyObject, text)) { "TextBlock not found: $text" }
        return try {
            table.rowInvoker.viewRowOf(textBlock)
        } finally {
            textBlock.release()
        }
    }

    /**
     * The non-empty strings of the TextBlocks under [node], grouped by the view row they are shown in
     * (TableViewRowInvoker.viewRowOf). Elements outside rows (column headers and recycled elements) are left out.
     */
    fun rowTexts(table: WTable, node: ComPtr = table.dependencyObject): Map<Int, List<String>> {
        val own = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
            textBlock.getString(XamlInterop.ITextBlock_get_Text).also { textBlock.release() }
        }
        val result = sortedMapOf<Int, MutableList<String>>()
        if (!own.isNullOrEmpty()) table.rowInvoker.viewRowOf(node).takeIf { it >= 0 }?.let { result[it] = mutableListOf(own) }
        for (child in children(node)) {
            rowTexts(table, child).forEach { (row, texts) -> result.getOrPut(row) { mutableListOf() }.addAll(texts) }
            child.release()
        }
        return result
    }

    /** The widths of the cells in each TableViewCellsPanel under [node] (the column headers and every shown row), rounded. */
    fun cellWidths(node: ComPtr): List<List<Long>> {
        if (runtimeClassName(node) == "Microsoft.UI.Xaml.Controls.Tabular.TableViewCellsPanel") {
            return listOf(
                children(node).map { cell ->
                    val element = cell.queryInterface(XamlInterop.IID_IFrameworkElement)
                    Math.round(element.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth)).also {
                        element.release()
                        cell.release()
                    }
                },
            )
        }
        return children(node).flatMap { child -> cellWidths(child).also { child.release() } }
    }

    /** Reads [notifications] on the UI thread until it is not empty (or a few seconds have passed) and returns it. */
    fun <T> awaitNotifications(notifications: () -> List<T>): List<T> {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (true) {
            val current = onUiThreadGet(notifications)
            if (current.isNotEmpty() || System.nanoTime() > deadline) return current
            Thread.sleep(50)
        }
    }
}
