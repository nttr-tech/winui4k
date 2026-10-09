package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

/**
 * Tests for JSON conversion and file storage of ribbon state (RibbonStateSerializer / RibbonState).
 * Ports RibbonSpace's StateTests and RegressionTests (the state-saving part), and reads JSON written by RibbonSpace.
 */
class RibbonStateSerializerTest : FunSpec() {
    private fun sampleState(): RibbonState {
        val group = RibbonCustomGroup("custom.group", "Tools").apply { itemIds += "bold" }
        val tab = RibbonCustomTab("custom.tab", "Mine").apply { groups += group }
        val customization = RibbonCustomization().apply {
            hiddenTabIds = mutableListOf("mailings")
            labels["home"] = "Start"
            customTabs += tab
        }
        return RibbonState().apply {
            selectedTabId = "insert"
            displayMode = RibbonDisplayMode.SIMPLIFIED
            visibilityMode = RibbonVisibilityMode.TABS_ONLY
            quickAccessPosition = RibbonQuickAccessPosition.BELOW_RIBBON
            quickAccessItemIds = mutableListOf("save", "undo")
            this.customization = customization
        }
    }

    init {
        test("state round-trips through JSON unchanged, and enum values become strings with the same names as in C#") {
            val json = RibbonStateSerializer.serialize(sampleState())
            json shouldContain "\"displayMode\": \"Simplified\""
            json shouldContain "\"visibilityMode\": \"TabsOnly\""
            json shouldContain "\"quickAccessPosition\": \"BelowRibbon\""
            val restored = RibbonStateSerializer.deserialize(json).shouldNotBeNull()
            restored.selectedTabId shouldBe "insert"
            restored.displayMode shouldBe RibbonDisplayMode.SIMPLIFIED
            restored.visibilityMode shouldBe RibbonVisibilityMode.TABS_ONLY
            restored.quickAccessPosition shouldBe RibbonQuickAccessPosition.BELOW_RIBBON
            restored.quickAccessItemIds shouldBe listOf("save", "undo")
            restored.customization.labels["home"] shouldBe "Start"
            restored.customization.customTabs[0].groups[0].itemIds[0] shouldBe "bold"
            restored.customization.isEmpty shouldBe false
        }

        test("null properties are not written, and output without indentation can be read back") {
            val json = RibbonStateSerializer.serialize(RibbonState(), indented = false)
            json shouldNotContain "selectedTabId"
            json shouldNotContain "quickAccessItemIds"
            json shouldNotContain "\n"
            json shouldContain "\"schemaVersion\":1"
            RibbonStateSerializer.deserialize(json).shouldNotBeNull().selectedTabId.shouldBeNull()
        }

        test("invalid JSON, a newer schema, and an empty string yield null, and an empty object yields the default state") {
            RibbonStateSerializer.deserialize("{ not json").shouldBeNull()
            RibbonStateSerializer.deserialize("{\"schemaVersion\": 99}").shouldBeNull()
            RibbonStateSerializer.deserialize("").shouldBeNull()
            RibbonStateSerializer.deserialize(null).shouldBeNull()
            RibbonStateSerializer.deserialize("[]").shouldBeNull()
            RibbonStateSerializer.deserialize("{\"a\":1,}").shouldBeNull() // trailing comma
            val empty = RibbonStateSerializer.deserialize("{}").shouldNotBeNull()
            empty.displayMode shouldBe RibbonDisplayMode.CLASSIC
            empty.showGroupCaptions shouldBe true
        }

        test("values of the wrong type and unknown enum values are invalid and yield null") {
            RibbonStateSerializer.deserialize("{\"displayMode\": \"Huge\"}").shouldBeNull()
            RibbonStateSerializer.deserialize("{\"isQuickAccessVisible\": \"yes\"}").shouldBeNull()
            RibbonStateSerializer.deserialize("{\"isQuickAccessVisible\": null}").shouldBeNull()
            RibbonStateSerializer.deserialize("{\"quickAccessItemIds\": \"save\"}").shouldBeNull()
        }

        test("unknown properties are ignored, and enum values can be read in any case, with underscores, or as numbers") {
            val state = RibbonStateSerializer.deserialize(
                "{\"future\": {\"x\": [1, 2]}, \"displayMode\": \"simplified\", \"density\": \"COMPACT\", " +
                    "\"visibilityMode\": 2, \"minimizeBehavior\": \"CYCLE_ALL\"}",
            ).shouldNotBeNull()
            state.displayMode shouldBe RibbonDisplayMode.SIMPLIFIED
            state.density shouldBe RibbonDensity.COMPACT
            state.visibilityMode shouldBe RibbonVisibilityMode.FULL_SCREEN
            state.minimizeBehavior shouldBe RibbonMinimizeBehavior.CYCLE_ALL
        }

        test("reads JSON written by RibbonSpace") {
            // RibbonSpace's output format (System.Text.Json, camelCase, enum values as strings, nulls omitted)
            val json = """
                {
                  "schemaVersion": 1,
                  "selectedTabId": "home",
                  "displayMode": "Classic",
                  "visibilityMode": "PanelButtons",
                  "density": "Touch",
                  "quickAccessPosition": "AboveRibbon",
                  "isQuickAccessVisible": false,
                  "showQuickAccessLabels": true,
                  "minimizeBehavior": "PanelTitles",
                  "showGroupCaptions": true,
                  "floatingGroups": [
                    {
                      "groupId": "layers",
                      "x": 40.5,
                      "y": 300
                    }
                  ],
                  "quickAccessItemIds": [
                    "save",
                    "undo"
                  ],
                  "customization": {
                    "tabOrder": ["view", "home"],
                    "groupOrder": {
                      "home": ["font", "clipboard"]
                    },
                    "hiddenTabIds": [],
                    "hiddenGroupIds": ["styles"],
                    "labels": {
                      "home": "Start Ü"
                    },
                    "customTabs": [],
                    "customGroups": [
                      {
                        "id": "custom.g",
                        "label": "Mine",
                        "tabId": "home",
                        "itemIds": ["bold"]
                      }
                    ]
                  },
                  "recentSearchIds": ["bold"],
                  "recentColors": ["#FF0000", "#80112233"]
                }
            """.trimIndent()
            val state = RibbonStateSerializer.deserialize(json).shouldNotBeNull()
            state.visibilityMode shouldBe RibbonVisibilityMode.PANEL_BUTTONS
            state.density shouldBe RibbonDensity.TOUCH
            state.isQuickAccessVisible shouldBe false
            state.showQuickAccessLabels shouldBe true
            state.minimizeBehavior shouldBe RibbonMinimizeBehavior.PANEL_TITLES
            state.floatingGroups.shouldNotBeNull().single().let {
                it.groupId shouldBe "layers"
                it.x shouldBe 40.5
                it.y shouldBe 300.0
            }
            state.customization.tabOrder shouldBe listOf("view", "home")
            state.customization.groupOrder["home"] shouldBe listOf("font", "clipboard")
            state.customization.hiddenGroupIds shouldBe listOf("styles")
            state.customization.labels["home"] shouldBe "Start Ü"
            state.customization.customGroups.single().tabId shouldBe "home"
            state.recentColors shouldBe listOf("#FF0000", "#80112233")
        }

        test("the output format matches RibbonSpace (property order, two-space indentation, integral numbers)") {
            val state = RibbonState().apply {
                floatingGroups = mutableListOf(RibbonFloatingGroupState("layers", 40.0, 300.5))
            }
            RibbonStateSerializer.serialize(state) shouldBe """
                {
                  "schemaVersion": 1,
                  "displayMode": "Classic",
                  "visibilityMode": "AlwaysShow",
                  "density": "Comfortable",
                  "quickAccessPosition": "AboveRibbon",
                  "isQuickAccessVisible": true,
                  "showQuickAccessLabels": false,
                  "minimizeBehavior": "Tabs",
                  "showGroupCaptions": true,
                  "floatingGroups": [
                    {
                      "groupId": "layers",
                      "x": 40,
                      "y": 300.5
                    }
                  ],
                  "customization": {
                    "tabOrder": [],
                    "groupOrder": {},
                    "hiddenTabIds": [],
                    "hiddenGroupIds": [],
                    "labels": {},
                    "customTabs": [],
                    "customGroups": []
                  }
                }
            """.trimIndent()
        }

        test("special characters in strings are escaped and round-trip") {
            val state = RibbonState().apply { selectedTabId = "a\"b\\c\nd\te\u0001" }
            val json = RibbonStateSerializer.serialize(state)
            json shouldContain "\\u0001"
            RibbonStateSerializer.deserialize(json).shouldNotBeNull().selectedTabId shouldBe "a\"b\\c\nd\te\u0001"
        }

        test("JSON with null collections or null elements is normalized (isEmpty is not written)") {
            val state = RibbonStateSerializer.deserialize(
                """{"customization":{"customTabs":null,"labels":null,"hiddenTabIds":[null,"x"],"customGroups":[null,{"id":"g","itemIds":null}]},"quickAccessItemIds":[null,"save"]}""",
            ).shouldNotBeNull()
            state.customization.customTabs.shouldBeEmpty()
            state.customization.labels shouldBe emptyMap()
            state.customization.hiddenTabIds shouldBe listOf("x")
            state.customization.customGroups.size shouldBe 1
            state.customization.customGroups[0].itemIds.shouldBeEmpty()
            state.quickAccessItemIds shouldBe listOf("save")
            state.customization.isEmpty shouldBe false
            RibbonStateSerializer.serialize(state) shouldNotContain "isEmpty"
        }

        test("normalization removes custom tabs and groups without an id, and empty ids") {
            val state = RibbonStateSerializer.deserialize(
                """{"customization":{"customTabs":[{"label":"x"},{"id":"t","groups":[{"label":"no id"},{"id":"g"}]}],"tabOrder":["", "a"]},"floatingGroups":[{"x":1},null]}""",
            ).shouldNotBeNull()
            state.customization.customTabs.map { it.id } shouldBe listOf("t")
            state.customization.customTabs[0].groups.map { it.id } shouldBe listOf("g")
            state.customization.tabOrder shouldBe listOf("a")
            state.floatingGroups.shouldNotBeNull().shouldBeEmpty()
        }

        test("recently used search ids round-trip through the state") {
            val json = RibbonStateSerializer.serialize(RibbonState().apply { recentSearchIds = mutableListOf("bold", "paste") })
            RibbonStateSerializer.deserialize(json).shouldNotBeNull().recentSearchIds shouldBe listOf("bold", "paste")
        }

        test("CAD state (minimize behavior, panel titles, floating panels) round-trips, and panel titles are shown by default") {
            val state = RibbonState().apply {
                minimizeBehavior = RibbonMinimizeBehavior.CYCLE_ALL
                visibilityMode = RibbonVisibilityMode.PANEL_TITLES
                showGroupCaptions = false
                floatingGroups = mutableListOf(RibbonFloatingGroupState("layers", 40.0, 300.0))
            }
            val restored = RibbonStateSerializer.deserialize(RibbonStateSerializer.serialize(state)).shouldNotBeNull()
            restored.minimizeBehavior shouldBe RibbonMinimizeBehavior.CYCLE_ALL
            restored.visibilityMode shouldBe RibbonVisibilityMode.PANEL_TITLES
            restored.showGroupCaptions shouldBe false
            restored.floatingGroups.shouldNotBeNull().single().y shouldBe 300.0
            RibbonStateSerializer.deserialize("{}").shouldNotBeNull().showGroupCaptions shouldBe true
        }

        test("deepCopy creates a copy that shares nothing with the original state") {
            val original = sampleState()
            val copy = original.deepCopy()
            copy.customization.customTabs[0].groups[0].itemIds += "italic"
            copy.quickAccessItemIds!!.clear()
            copy.customization.labels["home"] = "Changed"
            original.customization.customTabs[0].groups[0].itemIds shouldBe listOf("bold")
            original.quickAccessItemIds shouldBe listOf("save", "undo")
            original.customization.labels["home"] shouldBe "Start"
            RibbonStateSerializer.serialize(sampleState()) shouldBe RibbonStateSerializer.serialize(sampleState().deepCopy())
        }

        test("state can be saved to and loaded from a file, and a missing or corrupt file yields null") {
            val dir = Files.createTempDirectory("ribbon-state-test")
            try {
                val path = dir.resolve("nested").resolve("state.json")
                RibbonStateSerializer.save(RibbonState().apply { selectedTabId = "view" }, path)
                RibbonStateSerializer.load(path).shouldNotBeNull().selectedTabId shouldBe "view"
                Files.exists(path.resolveSibling("state.json.tmp")) shouldBe false
                RibbonStateSerializer.save(RibbonState().apply { selectedTabId = "home" }, path) // overwrite
                RibbonStateSerializer.load(path).shouldNotBeNull().selectedTabId shouldBe "home"
                RibbonStateSerializer.load(dir.resolve("missing.json")).shouldBeNull()
                val broken = dir.resolve("broken.json")
                Files.write(broken, "{".toByteArray())
                RibbonStateSerializer.load(broken).shouldBeNull()
            } finally {
                dir.toFile().deleteRecursively()
            }
        }
    }
}
