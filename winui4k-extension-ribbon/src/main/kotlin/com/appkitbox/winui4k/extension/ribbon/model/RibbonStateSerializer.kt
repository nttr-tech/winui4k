package com.appkitbox.winui4k.extension.ribbon.model

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Converts [RibbonState] to and from JSON, and saves it to and loads it from files.
 *
 * The format is compatible with RibbonSpace's JSON: property names are camelCase, enum values are strings (PascalCase
 * like "AlwaysShow"; reading ignores case and "_" and also accepts numbers), and null properties are not written.
 * Reading ignores unknown properties.
 */
object RibbonStateSerializer {
    /** Converts the state to JSON. Indents it if [indented]. */
    @JvmStatic
    @JvmOverloads
    fun serialize(state: RibbonState, indented: Boolean = true): String = RibbonJson.write(toJson(state), indented)

    /** Reads the state from JSON. Returns null for invalid JSON, values of the wrong type, or a newer schema. */
    @JvmStatic
    fun deserialize(json: String?): RibbonState? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = RibbonJson.parse(json) as? Map<*, *> ?: return null
            val state = readState(root)
            if (state.schemaVersion > RibbonState.CURRENT_SCHEMA_VERSION) return null
            state.normalize()
            state.schemaVersion = RibbonState.CURRENT_SCHEMA_VERSION
            state
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Saves the state to a file (also creating the parent directories). It writes to a temporary file and then replaces
     * the file, so the previous file is not corrupted even if writing fails midway.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun save(state: RibbonState, path: Path) {
        path.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        val temp = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.write(temp, serialize(state).toByteArray(StandardCharsets.UTF_8))
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Reads the state from a file. Returns null if the file is missing, unreadable, or invalid. */
    @JvmStatic
    fun load(path: Path): RibbonState? = try {
        if (Files.isRegularFile(path)) deserialize(String(Files.readAllBytes(path), StandardCharsets.UTF_8)) else null
    } catch (_: IOException) {
        null
    }

    // ---- Writing (in the declaration order of the C# properties) ----

    private fun toJson(state: RibbonState): Map<String, Any?> = jsonObject(
        "schemaVersion" to state.schemaVersion,
        "selectedTabId" to state.selectedTabId,
        "displayMode" to enumName(state.displayMode),
        "visibilityMode" to enumName(state.visibilityMode),
        "density" to enumName(state.density),
        "quickAccessPosition" to enumName(state.quickAccessPosition),
        "isQuickAccessVisible" to state.isQuickAccessVisible,
        "showQuickAccessLabels" to state.showQuickAccessLabels,
        "minimizeBehavior" to enumName(state.minimizeBehavior),
        "showGroupCaptions" to state.showGroupCaptions,
        "floatingGroups" to state.floatingGroups?.map { jsonObject("groupId" to it.groupId, "x" to it.x, "y" to it.y) },
        "quickAccessItemIds" to state.quickAccessItemIds,
        "customization" to toJson(state.customization),
        "recentSearchIds" to state.recentSearchIds,
        "recentColors" to state.recentColors,
    )

    private fun toJson(customization: RibbonCustomization): Map<String, Any?> = jsonObject(
        "tabOrder" to customization.tabOrder,
        "groupOrder" to customization.groupOrder,
        "hiddenTabIds" to customization.hiddenTabIds,
        "hiddenGroupIds" to customization.hiddenGroupIds,
        "labels" to customization.labels,
        "customTabs" to customization.customTabs.map { tab ->
            jsonObject("id" to tab.id, "label" to tab.label, "groups" to tab.groups.map { toJson(it) })
        },
        "customGroups" to customization.customGroups.map { toJson(it) },
    )

    private fun toJson(group: RibbonCustomGroup): Map<String, Any?> =
        jsonObject("id" to group.id, "label" to group.label, "tabId" to group.tabId, "itemIds" to group.itemIds)

    /** An object without null values (C#'s WhenWritingNull). */
    private fun jsonObject(vararg properties: Pair<String, Any?>): Map<String, Any?> =
        properties.filter { it.second != null }.toMap(LinkedHashMap())

    /** Converts an UPPER_SNAKE enum value to its C# name (PascalCase). */
    private fun enumName(value: Enum<*>): String =
        value.name.split('_').joinToString("") { part -> part.lowercase().replaceFirstChar { it.uppercase() } }

    // ---- Reading ----

    private fun readState(json: Map<*, *>): RibbonState = RibbonState().apply {
        json.int("schemaVersion")?.let { schemaVersion = it }
        selectedTabId = json.string("selectedTabId")
        json.enumValue<RibbonDisplayMode>("displayMode")?.let { displayMode = it }
        json.enumValue<RibbonVisibilityMode>("visibilityMode")?.let { visibilityMode = it }
        json.enumValue<RibbonDensity>("density")?.let { density = it }
        json.enumValue<RibbonQuickAccessPosition>("quickAccessPosition")?.let { quickAccessPosition = it }
        json.bool("isQuickAccessVisible")?.let { isQuickAccessVisible = it }
        json.bool("showQuickAccessLabels")?.let { showQuickAccessLabels = it }
        json.enumValue<RibbonMinimizeBehavior>("minimizeBehavior")?.let { minimizeBehavior = it }
        json.bool("showGroupCaptions")?.let { showGroupCaptions = it }
        floatingGroups = json.objects("floatingGroups")?.mapTo(mutableListOf()) { floating ->
            RibbonFloatingGroupState(floating.string("groupId").orEmpty(), floating.double("x") ?: 0.0, floating.double("y") ?: 0.0)
        }
        quickAccessItemIds = json.strings("quickAccessItemIds")
        json.obj("customization")?.let { customization = readCustomization(it) }
        recentSearchIds = json.strings("recentSearchIds")
        recentColors = json.strings("recentColors")
    }

    private fun readCustomization(json: Map<*, *>): RibbonCustomization = RibbonCustomization().apply {
        json.strings("tabOrder")?.let { tabOrder = it }
        json.obj("groupOrder")?.forEach { (tab, order) ->
            val ids = stringList(order, "groupOrder") ?: return@forEach
            groupOrder[tab.toString()] = ids
        }
        json.strings("hiddenTabIds")?.let { hiddenTabIds = it }
        json.strings("hiddenGroupIds")?.let { hiddenGroupIds = it }
        json.obj("labels")?.let { labels = readLabels(it) }
        json.objects("customTabs")?.forEach { customTabs += readCustomTab(it) }
        json.objects("customGroups")?.forEach { customGroups += readCustomGroup(it) }
    }

    /** Renames (id → label). Null values are removed. */
    private fun readLabels(json: Map<*, *>): MutableMap<String, String> {
        val labels = linkedMapOf<String, String>()
        for ((id, label) in json) {
            if (label == null) continue
            labels[id.toString()] = label as? String ?: throw IllegalArgumentException("a value of labels is not a string")
        }
        return labels
    }

    private fun readCustomTab(json: Map<*, *>): RibbonCustomTab =
        RibbonCustomTab(json.string("id").orEmpty(), json.string("label").orEmpty()).apply {
            json.objects("groups")?.forEach { groups += readCustomGroup(it) }
        }

    private fun readCustomGroup(json: Map<*, *>): RibbonCustomGroup =
        RibbonCustomGroup(json.string("id").orEmpty(), json.string("label").orEmpty(), json.string("tabId")).apply {
            json.strings("itemIds")?.let { itemIds = it }
        }

    private fun Map<*, *>.string(key: String): String? = when (val value = this[key]) {
        null -> null
        is String -> value
        else -> throw IllegalArgumentException("$key is not a string")
    }

    /** A Boolean. Null is invalid, like a non-null C# bool (returns null only when the key is missing). */
    private fun Map<*, *>.bool(key: String): Boolean? {
        if (!containsKey(key)) return null
        return this[key] as? Boolean ?: throw IllegalArgumentException("$key is not a Boolean")
    }

    private fun Map<*, *>.double(key: String): Double? {
        if (!containsKey(key)) return null
        return (this[key] as? Number)?.toDouble() ?: throw IllegalArgumentException("$key is not a number")
    }

    private fun Map<*, *>.int(key: String): Int? {
        if (!containsKey(key)) return null
        val value = this[key] as? Long ?: throw IllegalArgumentException("$key is not an integer")
        require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "$key is out of range" }
        return value.toInt()
    }

    private inline fun <reified E : Enum<E>> Map<*, *>.enumValue(key: String): E? {
        if (!containsKey(key)) return null
        val entries = enumValues<E>()
        return when (val value = this[key]) {
            is String -> {
                val normalized = value.replace("_", "")
                entries.firstOrNull { it.name.replace("_", "").equals(normalized, ignoreCase = true) }
            }
            is Long -> entries.getOrNull(value.toInt())
            else -> null
        } ?: throw IllegalArgumentException("invalid value of $key: ${this[key]}")
    }

    private fun Map<*, *>.obj(key: String): Map<*, *>? = when (val value = this[key]) {
        null -> null
        is Map<*, *> -> value
        else -> throw IllegalArgumentException("$key is not an object")
    }

    /** An array of objects. Null elements are removed (same as C#'s Normalize). */
    private fun Map<*, *>.objects(key: String): List<Map<*, *>>? = when (val value = this[key]) {
        null -> null
        is List<*> -> value.filterNotNull().map { it as? Map<*, *> ?: throw IllegalArgumentException("an element of $key is not an object") }
        else -> throw IllegalArgumentException("$key is not an array")
    }

    private fun Map<*, *>.strings(key: String): MutableList<String>? = stringList(this[key], key)

    /** An array of strings. Null elements are removed (same as C#'s Normalize). */
    private fun stringList(value: Any?, key: String): MutableList<String>? = when (value) {
        null -> null
        is List<*> -> value.filterNotNull().mapTo(mutableListOf()) { it as? String ?: throw IllegalArgumentException("an element of $key is not a string") }
        else -> throw IllegalArgumentException("$key is not an array")
    }
}
