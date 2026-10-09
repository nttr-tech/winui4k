package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests for the ribbon model (lookup, bulk command state, change notification, merging, icons) (no UI required). */
class RibbonModelTest : FunSpec() {
    /** A model with the same structure as RibbonSpace's ModelTests: a [Home] tab and a hidden contextual tab. */
    private fun createModel(): RibbonModel {
        val model = RibbonModel()
        val home = RibbonTabModel("home", "Home")
        home.groups += RibbonGroupModel("clipboard", "Clipboard").apply {
            items += RibbonSplitButtonModel("paste", "Paste", RibbonIcons.PASTE).apply { commandId = "paste" }
            items += RibbonButtonModel("cut", "Cut", RibbonIcons.CUT).apply { commandId = "cut" }
        }
        home.groups += RibbonGroupModel("font", "Font").apply {
            itemsLayout = RibbonGroupItemsLayout.ROWS
            items += RibbonRowModel(
                "font-row2",
                RibbonButtonGroupModel(
                    "bis",
                    RibbonToggleButtonModel("bold", "Bold", RibbonIcons.BOLD).apply { commandId = "bold" },
                    RibbonToggleButtonModel("italic", "Italic", RibbonIcons.ITALIC),
                ),
            )
        }
        model.tabs += home
        model.contextualGroups += RibbonContextualGroupModel("table-tools", "Table Tools")
        model.tabs += RibbonTabModel("table-design", "Table Design").apply { contextualGroupId = "table-tools" }
        return model
    }

    /** Returns a list that records the names of property change notifications from [observable]. */
    private fun recordChanges(observable: RibbonObservable): MutableList<String> {
        val changed = mutableListOf<String>()
        observable.addPropertyChangeListener { changed += it.propertyName }
        return changed
    }

    init {
        synchronousModelNotifications()

        test("items inside nested containers are found by id, and enumeration includes nested items") {
            val model = createModel()
            model.findItem("bold").shouldBeInstanceOf<RibbonToggleButtonModel>()
            model.findGroup("font")!!.id shouldBe "font"
            model.findNode("table-tools")!!.id shouldBe "table-tools"
            model.enumerateItems().map { it.id }.toList() shouldContainExactly
                listOf("paste", "cut", "font-row2", "bis", "bold", "italic")
        }

        test("a contextual tab is among the visible tabs only while its contextual group is visible") {
            val model = createModel()
            model.visibleTabs.map { it.id } shouldNotContain "table-design"
            model.setActiveContextualGroups("table-tools")
            model.visibleTabs.map { it.id } shouldContain "table-design"
            model.findTab("table-design")!!.isContextual shouldBe true
            model.setActiveContextualGroups()
            model.visibleTabs.map { it.id } shouldNotContain "table-design"
        }

        test("setContextualGroupVisible returns false for an unknown id and changes nothing") {
            val model = createModel()
            model.setContextualGroupVisible("unknown", true) shouldBe false
            model.setContextualGroupVisible("table-tools", true) shouldBe true
            model.findContextualGroup("table-tools")!!.isVisible shouldBe true
        }

        test("enabled and checked states of commands are set in bulk by commandId") {
            val model = createModel()
            model.setCommandEnabled("cut", false)
            model.setCommandChecked("bold", true)
            model.findItem("cut")!!.isEnabled shouldBe false
            (model.findItem("bold") as RibbonToggleButtonModel).isChecked shouldBe true
            // commandId is case-insensitive
            model.setCommandEnabled("CUT", true)
            model.findItem("cut")!!.isEnabled shouldBe true
        }

        test("property changes are notified only when the value actually changes") {
            val item = RibbonToggleButtonModel("x", "X")
            val changed = recordChanges(item)
            item.isChecked = true
            item.isChecked = true
            item.label = "Y"
            changed shouldContainExactly listOf("isChecked", "label")
        }

        test("the selected tab round-trips with its id, and changing selectedTabId also notifies a selectedTab change") {
            val model = createModel()
            val changed = recordChanges(model)
            model.selectedTab = model.findTab("home")
            model.selectedTabId shouldBe "home"
            model.selectedTab shouldBeSameInstanceAs model.findTab("home")
            changed shouldContain "selectedTabId"
            changed shouldContain "selectedTab"
        }

        test("changing contextualGroupId also notifies an isContextual change") {
            val tab = RibbonTabModel("t")
            val changed = recordChanges(tab)
            tab.contextualGroupId = "table"
            changed shouldContain "isContextual"
        }

        test("without an explicit size definition, changing size also notifies an effectiveSizeDefinition change") {
            val item = RibbonButtonModel("b")
            val changed = recordChanges(item)
            item.size = RibbonItemSize.SMALL
            changed shouldContain "effectiveSizeDefinition"
            item.effectiveSizeDefinition shouldBe RibbonSizeDefinition.ALWAYS_SMALL

            item.sizeDefinition = RibbonSizeDefinition.ALWAYS_LARGE
            changed.clear()
            item.size = RibbonItemSize.MEDIUM
            changed shouldContainExactly listOf("size") // the explicit size definition wins, so the effective value does not change
            item.effectiveSizeDefinition shouldBe RibbonSizeDefinition.ALWAYS_LARGE
        }

        test("properties with value coercion are clamped to their range") {
            val group = RibbonGroupModel()
            group.rowCount = 7
            group.rowCount shouldBe 3
            group.rowCount = 0
            group.rowCount shouldBe 1
            val picker = RibbonGridPickerModel()
            picker.rows = -2
            picker.rows shouldBe 1
        }

        test("omitting the id assigns a unique id") {
            val first = RibbonButtonModel()
            val second = RibbonButtonModel()
            first.id.isNotEmpty() shouldBe true
            (first.id == second.id) shouldBe false
        }

        test("command state also reaches menu items and Backstage items") {
            val model = RibbonModel()
            val paste = RibbonSplitButtonModel("paste").apply { commandId = "paste" }
            val special = RibbonMenuItemModel("paste.special").apply {
                commandId = "save"
                isCheckable = true
            }
            paste.menuItems += special
            model.tabs += RibbonTabModel("home").apply { groups += RibbonGroupModel("clipboard").apply { items += paste } }
            val save = RibbonBackstageItemModel("save").apply { commandId = "save" }
            model.backstage.items += save

            model.setCommandEnabled("save", false)
            special.isEnabled shouldBe false
            save.isEnabled shouldBe false
            model.setCommandChecked("save", true)
            special.isChecked shouldBe true
        }

        test("slide-out items are also model items that can be found and have their state set") {
            val model = RibbonModel()
            val group = RibbonGroupModel("draw")
            group.slideOutItems += RibbonButtonModel("xline").apply { commandId = "xline" }
            model.tabs += RibbonTabModel("home").apply { groups += group }
            model.findItem("xline").shouldNotBeNull()
            model.setCommandEnabled("xline", false)
            model.findItem("xline")!!.isEnabled shouldBe false
        }

        test("merging adds, merges children, and removes, and unmerge restores the original") {
            val target = createModel()
            val plugin = RibbonModel()
            plugin.tabs += RibbonTabModel("home", "Home").apply {
                groups += RibbonGroupModel("clipboard").apply {
                    items += RibbonButtonModel("copy", "Copy")
                    items += RibbonButtonModel("cut").apply { mergeAction = RibbonMergeAction.REMOVE }
                }
                groups += RibbonGroupModel("addin", "Add-in").apply { items += RibbonButtonModel("addin-run", "Run") }
            }
            plugin.tabs += RibbonTabModel("addins", "Add-ins")

            val merger = RibbonModelMerger.merge(target, plugin)
            val clipboard = target.findGroup("clipboard")!!
            clipboard.items.map { it.id } shouldContain "copy"
            clipboard.items.map { it.id } shouldNotContain "cut"
            target.findGroup("addin").shouldNotBeNull()
            target.findTab("addins").shouldNotBeNull()

            merger.unmerge()
            clipboard.items.map { it.id } shouldContainExactly listOf("paste", "cut")
            target.findGroup("addin").shouldBeNull()
            target.findTab("addins").shouldBeNull()
        }

        test("a replacing merge swaps in the node with the same id, and unmerge restores the original node") {
            val target = createModel()
            val original = target.findItem("cut")!!
            val plugin = RibbonModel()
            val replacement = RibbonButtonModel("cut", "Cut (plugin)").apply { mergeAction = RibbonMergeAction.REPLACE }
            plugin.tabs += RibbonTabModel("home").apply {
                groups += RibbonGroupModel("clipboard").apply { items += replacement }
            }
            val merger = RibbonModelMerger.merge(target, plugin)
            target.findItem("cut") shouldBeSameInstanceAs replacement
            merger.unmerge()
            target.findItem("cut") shouldBeSameInstanceAs original
        }

        test("merging inserts according to order") {
            val target = RibbonModel()
            target.tabs += RibbonTabModel("a").apply { order = 10 }
            target.tabs += RibbonTabModel("c").apply { order = 30 }
            val source = RibbonModel()
            source.tabs += RibbonTabModel("b").apply { order = 20 }
            RibbonModelMerger.merge(target, source)
            target.tabs.map { it.id } shouldContainExactly listOf("a", "b", "c")
        }

        test("merging handles the command catalog, nested items, and QAT removals, and unmerge restores the original") {
            val host = RibbonModel().apply { commandCatalog = RibbonCommandCatalog() }
            val row = RibbonButtonGroupModel("format").apply { items += RibbonToggleButtonModel("bold") }
            host.tabs += RibbonTabModel("home").apply { groups += RibbonGroupModel("font").apply { items += row } }
            host.quickAccessItems += RibbonButtonModel("undo")

            val plugin = RibbonModel().apply { commandCatalog = RibbonCommandCatalog() }
            plugin.commandCatalog!!.register("plugin.run", "Run", RibbonCommand { })
            val pluginRow = RibbonButtonGroupModel("format").apply { items += RibbonToggleButtonModel("strike") }
            plugin.tabs += RibbonTabModel("home").apply { groups += RibbonGroupModel("font").apply { items += pluginRow } }
            plugin.quickAccessItems += RibbonButtonModel("undo").apply { mergeAction = RibbonMergeAction.REMOVE }

            val merger = RibbonModelMerger.merge(host, plugin)
            host.commandCatalog!!.find("plugin.run").shouldNotBeNull()
            row.items.map { it.id } shouldContainExactly listOf("bold", "strike")
            host.quickAccessItems.shouldBeEmpty()

            merger.unmerge()
            host.commandCatalog!!.find("plugin.run").shouldBeNull()
            row.items.map { it.id } shouldContainExactly listOf("bold")
            host.quickAccessItems.map { it.id } shouldContainExactly listOf("undo")
        }

        test("if the host has no command catalog it takes over the plugin's catalog, and unmerge removes it") {
            val host = RibbonModel()
            val catalog = RibbonCommandCatalog()
            val plugin = RibbonModel().apply { commandCatalog = catalog }
            val merger = RibbonModelMerger.merge(host, plugin)
            host.commandCatalog shouldBeSameInstanceAs catalog
            merger.unmerge()
            host.commandCatalog.shouldBeNull()
        }

        test("each icon factory sets the kind and value correctly") {
            RibbonIcons.BOLD.kind shouldBe RibbonIconKind.GLYPH
            RibbonIcons.BOLD.value shouldBe ""
            RibbonIcons.SUBSCRIPT.kind shouldBe RibbonIconKind.PATH
            RibbonIcons.SHOW_FORMATTING.kind shouldBe RibbonIconKind.TEXT
            RibbonIcon.path("M0,0 L1,1").kind shouldBe RibbonIconKind.PATH
            RibbonIcon.glyph("x").withForeground("#FF0000").foreground shouldBe "#FF0000"
            RibbonIcon.image("ms-appx:///a.png").kind shouldBe RibbonIconKind.IMAGE
        }

        test("icon layers round-trip through strings, and the header settings and viewbox can be read") {
            val icon = RibbonIcon.layers(
                32.0,
                RibbonIconLayer("M4,28 L28,4", 1.8),
                RibbonIconLayer("M2,26 h4 v4 h-4 Z", 0.0, "#3DA9F5", 0.8),
            )
            icon.kind shouldBe RibbonIconKind.PATH
            icon.viewBoxSize shouldBe 32.0
            val layers = RibbonIconLayer.parse(icon.value)
            layers.size shouldBe 2
            layers[0].strokeThickness shouldBe 1.8
            layers[0].color.shouldBeNull()
            layers[1].color shouldBe "#3DA9F5"
            layers[1].opacity shouldBe 0.8
            layers[1].data shouldBe "M2,26 h4 v4 h-4 Z"
            RibbonIconLayer.parse(RibbonIcon.stroke("M0,0 L10,10").value)[0].strokeThickness shouldBe 1.5
            RibbonIconLayer.parse("M1,1 L2,2").size shouldBe 1
            icon.value.startsWith("[stroke=1.8]") shouldBe true
            val xaml = RibbonIconLayer.parse("[viewbox=32;stroke=2]M0,0 L4,4|{color=#FF0000}M1,1 h2 v2 Z")
            xaml[0].strokeThickness shouldBe 2.0
            xaml[1].color shouldBe "#FF0000"
            RibbonIconLayer.parseViewBox("[viewbox=32;stroke=2]M0,0 L4,4") shouldBe 32.0
            RibbonIconLayer.parseViewBox("M0,0 L4,4").shouldBeNull()
            RibbonIconLayer.parse("  ").shouldBeEmpty()
        }
    }
}
