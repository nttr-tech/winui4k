package com.appkitbox.winui4k.extension.ribbon.model

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Per-language strings used by the built-in UI of the ribbon (menus, tooltips, and dialogs).
 *
 * English, German, French, Spanish, Polish (exactly as defined by RibbonSpace), and Japanese are built in.
 * Other languages can be added with [register], and a single key can be replaced with [override].
 * Every ribbon uses [current], and when [current] changes, the listeners of [addCurrentChangedListener] are notified
 * (views update the strings they show).
 */
class RibbonStrings private constructor(
    private val values: MutableMap<String, String>,
    /** The language (locale) of these strings. */
    val locale: Locale,
) {
    /** The keys set on this instance with [override] (carried over when re-registered). */
    private val overrides = LinkedHashMap<String, String>()

    /**
     * The string for a key. Falls back to English if this instance has none, and to the key name itself if English has
     * none either.
     */
    operator fun get(key: String): String = values[key] ?: englishValue(key) ?: key

    /**
     * Replaces `{0}`, `{1}`, ... in the string for a key with [args] (the same syntax as .NET's string.Format; `{{` /
     * `}}` are braces).
     */
    fun format(key: String, vararg args: Any?): String = formatComposite(this[key], args)

    /**
     * Replaces a key of this instance (for example, changing the application button's "File" to "Home").
     * Notifies the change if this is the [current] instance. Returns this instance.
     */
    fun override(key: String, value: String): RibbonStrings {
        require(key.isNotEmpty()) { "key is empty" }
        values[key] = value
        overrides[key] = value
        if (this === currentInstance) fireCurrentChanged()
        return this
    }

    private fun formatComposite(pattern: String, args: Array<out Any?>): String {
        val builder = StringBuilder(pattern.length + 16)
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            when {
                c == '{' && i + 1 < pattern.length && pattern[i + 1] == '{' -> {
                    builder.append('{')
                    i += 2
                }
                c == '}' && i + 1 < pattern.length && pattern[i + 1] == '}' -> {
                    builder.append('}')
                    i += 2
                }
                c == '{' -> {
                    val end = pattern.indexOf('}', i)
                    val index = if (end > i) pattern.substring(i + 1, end).substringBefore(',').substringBefore(':').trim().toIntOrNull() else null
                    if (index == null || end < 0) {
                        builder.append(c)
                        i++
                    } else {
                        builder.append(formatArgument(args.getOrNull(index)))
                        i = end + 1
                    }
                }
                else -> {
                    builder.append(c)
                    i++
                }
            }
        }
        return builder.toString()
    }

    /**
     * Converts an argument to a string with .NET's default format (no digit grouping; the decimal point follows the
     * language).
     */
    private fun formatArgument(value: Any?): String = when (value) {
        null -> ""
        is Double, is Float -> {
            val text = BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
            text.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        }
        else -> value.toString()
    }

    /** "File" */
    val file: String get() = this["File"]

    /** "Collapse the Ribbon" */
    val collapseRibbon: String get() = this["CollapseRibbon"]

    /** "Pin the Ribbon" */
    val pinRibbon: String get() = this["PinRibbon"]

    /** "Ribbon display options" */
    val ribbonDisplayOptions: String get() = this["RibbonDisplayOptions"]

    /** "Full-screen mode" */
    val fullScreenMode: String get() = this["FullScreenMode"]

    /** "Show tabs only" */
    val showTabsOnly: String get() = this["ShowTabsOnly"]

    /** "Always show Ribbon" */
    val alwaysShowRibbon: String get() = this["AlwaysShowRibbon"]

    /** "Show panel buttons" */
    val showPanelButtons: String get() = this["ShowPanelButtons"]

    /** "Show panel titles" */
    val showPanelTitles: String get() = this["ShowPanelTitles"]

    /** "Minimize to Tabs" */
    val minimizeToTabs: String get() = this["MinimizeToTabs"]

    /** "Minimize to Panel Titles" */
    val minimizeToPanelTitles: String get() = this["MinimizeToPanelTitles"]

    /** "Minimize to Panel Buttons" */
    val minimizeToPanelButtons: String get() = this["MinimizeToPanelButtons"]

    /** "Cycle through All" */
    val cycleThroughAll: String get() = this["CycleThroughAll"]

    /** "Minimize the Ribbon" */
    val minimizeRibbon: String get() = this["MinimizeRibbon"]

    /** "Show Full Ribbon" */
    val showFullRibbon: String get() = this["ShowFullRibbon"]

    /** "Show Tabs" */
    val showTabs: String get() = this["ShowTabs"]

    /** "Show Panels" */
    val showPanels: String get() = this["ShowPanels"]

    /** "Show Panel Titles" */
    val showGroupTitles: String get() = this["ShowGroupTitles"]

    /** "Float Panel" */
    val floatPanel: String get() = this["FloatPanel"]

    /** "Return Panel to Ribbon" */
    val returnPanelToRibbon: String get() = this["ReturnPanelToRibbon"]

    /** "Return Panels to Ribbon" */
    val returnPanelsToRibbon: String get() = this["ReturnPanelsToRibbon"]

    /** "Keep panel open" */
    val pinPanel: String get() = this["PinPanel"]

    /** "Unpin panel" */
    val unpinPanel: String get() = this["UnpinPanel"]

    /** "More {0} commands" */
    val expandPanel: String get() = this["ExpandPanel"]

    /** [expandPanel] with panelName filled in. */
    fun expandPanel(panelName: String): String = format("ExpandPanel", panelName)

    /** "Recent Documents" */
    val recentDocuments: String get() = this["RecentDocuments"]

    /** "Search commands" */
    val searchCommands: String get() = this["SearchCommands"]

    /** "Simplified Ribbon" */
    val useSimplifiedRibbon: String get() = this["UseSimplifiedRibbon"]

    /** "Classic Ribbon" */
    val useClassicRibbon: String get() = this["UseClassicRibbon"]

    /** "Show Quick Access Toolbar" */
    val showQuickAccessToolbar: String get() = this["ShowQuickAccessToolbar"]

    /** "Hide Quick Access Toolbar" */
    val hideQuickAccessToolbar: String get() = this["HideQuickAccessToolbar"]

    /** "Show Above the Ribbon" */
    val showAboveRibbon: String get() = this["ShowAboveRibbon"]

    /** "Show Below the Ribbon" */
    val showBelowRibbon: String get() = this["ShowBelowRibbon"]

    /** "Show Command Labels" */
    val showCommandLabels: String get() = this["ShowCommandLabels"]

    /** "Customize Quick Access Toolbar" */
    val customizeQuickAccessToolbar: String get() = this["CustomizeQuickAccessToolbar"]

    /** "More Commands..." */
    val moreCommands: String get() = this["MoreCommands"]

    /** "Add to Quick Access Toolbar" */
    val addToQuickAccessToolbar: String get() = this["AddToQuickAccessToolbar"]

    /** "Remove from Quick Access Toolbar" */
    val removeFromQuickAccessToolbar: String get() = this["RemoveFromQuickAccessToolbar"]

    /** "Customize the Ribbon..." */
    val customizeRibbon: String get() = this["CustomizeRibbon"]

    /** "More options" */
    val moreOptions: String get() = this["MoreOptions"]

    /** "{0} options" */
    val splitButtonOptions: String get() = this["SplitButtonOptions"]

    /** [splitButtonOptions] with label filled in. */
    fun splitButtonOptions(label: String): String = format("SplitButtonOptions", label)

    /** "Zoom in" */
    val zoomIn: String get() = this["ZoomIn"]

    /** "Zoom out" */
    val zoomOut: String get() = this["ZoomOut"]

    /** "Zoom" */
    val zoom: String get() = this["Zoom"]

    /** "The text is not a valid ribbon customization (JSON). Nothing was imported." */
    val invalidImport: String get() = this["InvalidImport"]

    /** "Search" */
    val search: String get() = this["Search"]

    /** "Search (Alt+Q)" */
    val searchPlaceholder: String get() = this["SearchPlaceholder"]

    /** "No results" */
    val searchNoResults: String get() = this["SearchNoResults"]

    /** "Recently used" */
    val searchRecent: String get() = this["SearchRecent"]

    /** "Actions" */
    val searchActions: String get() = this["SearchActions"]

    /** "Back" */
    val back: String get() = this["Back"]

    /** "Automatic" */
    val automatic: String get() = this["Automatic"]

    /** "No Color" */
    val noColor: String get() = this["NoColor"]

    /** "More Colors..." */
    val moreColors: String get() = this["MoreColors"]

    /** "Theme Colors" */
    val themeColors: String get() = this["ThemeColors"]

    /** "Standard Colors" */
    val standardColors: String get() = this["StandardColors"]

    /** "Recent Colors" */
    val recentColors: String get() = this["RecentColors"]

    /** "Scroll left" */
    val scrollLeft: String get() = this["ScrollLeft"]

    /** "Scroll right" */
    val scrollRight: String get() = this["ScrollRight"]

    /** "Row up" */
    val galleryUp: String get() = this["GalleryUp"]

    /** "Row down" */
    val galleryDown: String get() = this["GalleryDown"]

    /** "More" */
    val galleryMore: String get() = this["GalleryMore"]

    /** "Filter" */
    val galleryFilter: String get() = this["GalleryFilter"]

    /** "{0} Settings" */
    val dialogLauncher: String get() = this["DialogLauncher"]

    /** [dialogLauncher] with groupName filled in. */
    fun dialogLauncher(groupName: String): String = format("DialogLauncher", groupName)

    /** "{0}x{1} Table" */
    val tablePickerFormat: String get() = this["TablePickerFormat"]

    /** [tablePickerFormat] with columns and rows filled in. */
    fun tablePickerFormat(columns: Int, rows: Int): String = format("TablePickerFormat", columns, rows)

    /** "Insert Table" */
    val insertTable: String get() = this["InsertTable"]

    /** "OK" */
    val ok: String get() = this["Ok"]

    /** "Cancel" */
    val cancel: String get() = this["Cancel"]

    /** "Reset" */
    val reset: String get() = this["Reset"]

    /** "New Tab" */
    val newTab: String get() = this["NewTab"]

    /** "New Group" */
    val newGroup: String get() = this["NewGroup"]

    /** "Rename..." */
    val rename: String get() = this["Rename"]

    /** "Add >>" */
    val add: String get() = this["Add"]

    /** "<< Remove" */
    val remove: String get() = this["Remove"]

    /** "Move Up" */
    val moveUp: String get() = this["MoveUp"]

    /** "Move Down" */
    val moveDown: String get() = this["MoveDown"]

    /** "Import/Export" */
    val importExport: String get() = this["ImportExport"]

    /** "Choose commands" */
    val chooseCommands: String get() = this["ChooseCommands"]

    /** "Main Tabs" */
    val mainTabs: String get() = this["MainTabs"]

    /** "Quick Access Toolbar" */
    val quickAccessToolbar: String get() = this["QuickAccessToolbar"]

    /** "(Custom)" */
    val customGroupSuffix: String get() = this["CustomGroupSuffix"]

    /** "Press a key to choose a command. Press Esc to go back." */
    val keyTipsHint: String get() = this["KeyTipsHint"]

    /** "Close" */
    val close: String get() = this["Close"]

    /** "Account" */
    val account: String get() = this["Account"]

    /** "Options" */
    val options: String get() = this["Options"]

    /** "Type a command" */
    val commandPalette: String get() = this["CommandPalette"]

    companion object {
        private val lock = Any()

        /** Language name ("en", "de-AT", and so on; case-insensitive) → key → string. */
        private val cultures: MutableMap<String, MutableMap<String, String>> = TreeMap<String, MutableMap<String, String>>(
            String.CASE_INSENSITIVE_ORDER,
        ).apply {
            put("en", LinkedHashMap(RibbonStringsEn.VALUES))
            put("de", LinkedHashMap(RibbonStringsDe.VALUES))
            put("fr", LinkedHashMap(RibbonStringsFr.VALUES))
            put("es", LinkedHashMap(RibbonStringsEs.VALUES))
            put("pl", LinkedHashMap(RibbonStringsPl.VALUES))
            put("ja", LinkedHashMap(RibbonStringsJa.VALUES))
        }

        private val listeners = CopyOnWriteArrayList<Runnable>()

        @Volatile
        private var currentInstance: RibbonStrings = forCulture(Locale.getDefault(Locale.Category.DISPLAY))

        /**
         * The strings used by every ribbon. The default is those of the OS display language
         * (Locale.getDefault(Locale.Category.DISPLAY)). Setting it notifies the listeners of [addCurrentChangedListener].
         */
        @JvmStatic
        var current: RibbonStrings
            get() = currentInstance
            set(value) {
                currentInstance = value
                fireCurrentChanged()
            }

        /** The names of the built-in languages (and those added with [register]). */
        @JvmStatic
        val builtInCultures: Set<String>
            get() = synchronized(lock) { LinkedHashSet(cultures.keys) }

        /** All keys (in the English definition order). */
        @JvmStatic
        val keys: Set<String>
            get() = synchronized(lock) { LinkedHashSet(cultures.getValue("en").keys) }

        /**
         * Creates the strings for [locale]. Looks them up in the order language tag ("de-AT") → language ("de") → English.
         * Returns a new instance ([override] affects only that instance).
         */
        @JvmStatic
        fun forCulture(locale: Locale): RibbonStrings = synchronized(lock) {
            val values = cultures[locale.toLanguageTag()]
                ?: locale.language.takeIf { it.isNotEmpty() }?.let { cultures[it] }
                ?: cultures.getValue("en")
            RibbonStrings(LinkedHashMap(values), locale)
        }

        /** Creates the strings for a language tag ("ja-JP", "de", and so on), with the same lookup as [forCulture]. */
        @JvmStatic
        fun forCulture(languageTag: String): RibbonStrings = forCulture(Locale.forLanguageTag(languageTag))

        /**
         * Registers a language (adding or overwriting keys if it already exists). Missing keys fall back to English.
         * If the registration affects the language of [current], recreates [current] keeping the [override]
         * replacements and notifies the change.
         */
        @JvmStatic
        fun register(cultureName: String, values: Map<String, String>) {
            require(cultureName.isNotEmpty()) { "cultureName is empty" }
            synchronized(lock) {
                cultures.getOrPut(cultureName) { LinkedHashMap() }.putAll(values)
            }
            val current = currentInstance
            if (current.locale.toLanguageTag().equals(cultureName, ignoreCase = true) ||
                current.locale.language.equals(cultureName, ignoreCase = true)
            ) {
                val refreshed = forCulture(current.locale)
                for ((key, value) in current.overrides) refreshed.override(key, value)
                this.current = refreshed
            }
        }

        /**
         * Subscribes to changes of [current] (setting [current], [override] on the [current] instance, and [register]
         * for the language of [current]). This is a static notification, so always remove the listener when it is no
         * longer needed.
         */
        @JvmStatic
        fun addCurrentChangedListener(listener: Runnable) {
            listeners += listener
        }

        /** Removes a listener registered with [addCurrentChangedListener]. */
        @JvmStatic
        fun removeCurrentChangedListener(listener: Runnable) {
            listeners -= listener
        }

        private fun fireCurrentChanged() {
            for (listener in listeners) listener.run()
        }

        private fun englishValue(key: String): String? = synchronized(lock) { cultures.getValue("en")[key] }
    }
}
