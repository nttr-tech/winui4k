package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.ribbon.RibbonButtonModel
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonDensity
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonGroupState
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonQuickAccessPosition
import com.appkitbox.winui4k.ribbon.RibbonSimplifiedVisibility
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel
import com.appkitbox.winui4k.ribbon.RibbonVisibilityMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * E2E test that places a [WRibbon] in an actual WinUI window and verifies synchronization with the model, item
 * invocation, adaptive layout, contextual tabs, the QAT, KeyTips, and state saving.
 */
class WRibbonTest : FunSpec() {
    private lateinit var model: RibbonModel
    private lateinit var ribbon: WRibbon

    /** Flushes the deferred layouts (invokeLater). */
    private fun settle() {
        repeat(SETTLE_ROUNDS) { onUiThread { ribbon.updateLayout() } }
    }

    private fun button(id: String, label: String, size: RibbonItemSize = RibbonItemSize.MEDIUM) =
        RibbonButtonModel(id, label, RibbonIcons.COPY).also { it.size = size }

    private fun createModel(): RibbonModel = RibbonModel().apply {
        // Give an explicit KeyTip, as Office does for localized labels that cannot yield a Latin KeyTip
        val home = RibbonTabModel("home", "Home").also { it.keyTip = "H" }
        home.groups.add(
            RibbonGroupModel("clipboard", "Clipboard").also { g ->
                g.items.add(
                    RibbonSplitButtonModel("paste", "Paste").also {
                        it.size = RibbonItemSize.LARGE
                        it.icon = RibbonIcons.PASTE
                    },
                )
                g.items.add(button("cut", "Cut", RibbonItemSize.SMALL).also { it.shortcut = "Ctrl+X" })
                g.items.add(button("copy", "Copy", RibbonItemSize.SMALL))
            },
        )
        home.groups.add(
            RibbonGroupModel("font", "Font").also { g ->
                g.items.add(
                    RibbonToggleButtonModel("bold", "Bold").also {
                        it.size = RibbonItemSize.SMALL
                        it.groupName = "weight"
                    },
                )
                g.items.add(
                    RibbonToggleButtonModel("light", "Light").also {
                        it.size = RibbonItemSize.SMALL
                        it.groupName = "weight"
                    },
                )
            },
        )
        repeat(EXTRA_GROUPS) { index ->
            home.groups.add(
                RibbonGroupModel("extra$index", "Extra Group $index").also { g ->
                    repeat(EXTRA_ITEMS) { g.items.add(button("extra$index.$it", "Large Command $it", RibbonItemSize.LARGE)) }
                },
            )
        }
        tabs.add(home)
        tabs.add(RibbonTabModel("insert", "Insert").also { tab -> tab.groups.add(RibbonGroupModel("tables", "Tables").also { it.items.add(button("table", "Table")) }) })
        val tableTools = RibbonContextualGroupModel("tableTools", "Table Tools").also { it.activation = RibbonContextualActivation.SELECT_ON_SHOW }
        contextualGroups.add(tableTools)
        tabs.add(RibbonTabModel("tableDesign", "Table Design").also { it.contextualGroupId = "tableTools" })
        quickAccessCandidates.add(findItem("copy")!!)
    }

    init {
        beforeTest {
            model = onUiThreadGet { createModel() }
            ribbon = onUiThreadGet { WRibbon(model) }
            UiTestHarness.attachAndAwaitLoaded(ribbon)
            settle()
        }

        afterTest {
            onUiThread { ribbon.suspendPopups() }
            UiTestHarness.detach(ribbon)
        }

        test("the headers of the normal tabs are listed and the first tab is selected (hidden contextual tabs do not appear)") {
            val (tabs, selected) = onUiThreadGet { ribbon.visibleTabs.map { it.id } to ribbon.selectedTab?.id }
            tabs shouldBe listOf("home", "insert")
            selected shouldBe "home"
            onUiThreadGet { ribbon.tabStrip.headers.size } shouldBe 2
        }

        test("changing the model's selection switches the tab content and notifies the selection change") {
            val changes = mutableListOf<String?>()
            onUiThread {
                ribbon.addTabChangeListener { changes += it.newTab?.id }
                model.selectedTabId = "insert"
            }
            settle()
            changes shouldBe listOf("insert")
            onUiThreadGet { ribbon.tabViews[model.findTab("insert")]!!.element.isVisible to ribbon.tabViews[model.findTab("home")]!!.element.isVisible } shouldBe
                (true to false)
        }

        test("invoking an item delivers the command id and arguments to the item-invoked listeners") {
            val invoked = mutableListOf<String?>()
            onUiThread {
                ribbon.addItemInvokedListener { invoked += it.item.id }
                ribbon.host.findView(model.findItem("copy")!!)!!.invoke()
            }
            invoked shouldBe listOf("copy")
        }

        test("toggles in the same group are checked one at a time, like radio buttons") {
            onUiThread {
                ribbon.host.findView(model.findItem("bold")!!)!!.invoke()
                ribbon.host.findView(model.findItem("light")!!)!!.invoke()
            }
            val bold = model.findItem("bold") as RibbonToggleButtonModel
            val light = model.findItem("light") as RibbonToggleButtonModel
            (bold.isChecked to light.isChecked) shouldBe (false to true)
        }

        test("groups shrink when the width is narrow and return to the large display when widened") {
            onUiThread { ribbon.width = NARROW }
            settle()
            val narrow = onUiThreadGet { ribbon.tabViews[model.findTab("home")]!!.groupViews().map { it.state } }
            narrow shouldContain RibbonGroupState.COLLAPSED
            onUiThread { ribbon.width = WIDE }
            settle()
            val wide = onUiThreadGet { ribbon.tabViews[model.findTab("home")]!!.groupViews().map { it.state } }
            wide.toSet() shouldBe setOf(RibbonGroupState.LARGE)
        }

        test("the popup of a collapsed group can be opened and closed from code, and the group's size can be obtained") {
            onUiThread { ribbon.width = NARROW }
            settle()
            val collapsed = onUiThreadGet { model.findTab("home")!!.groups.first { ribbon.groupState(it) == RibbonGroupState.COLLAPSED } }
            onUiThreadGet { ribbon.openGroupPopup(collapsed) } shouldBe true
            settle()
            onUiThreadGet { ribbon.isGroupPopupOpen(collapsed) } shouldBe true
            onUiThread { ribbon.closeGroupPopup(collapsed) }
            onUiThreadGet { ribbon.isGroupPopupOpen(collapsed) } shouldBe false
            onUiThread { ribbon.width = WIDE }
            settle()
            onUiThreadGet { ribbon.openGroupPopup(collapsed) } shouldBe false
        }

        test("the expanded panel can be opened and pinned from code, and the dialog launcher can be invoked") {
            val clipboard = onUiThreadGet { model.findGroup("clipboard")!! }
            val invoked = mutableListOf<String?>()
            onUiThread {
                clipboard.slideOutItems.add(button("pasteSpecial", "Paste Special"))
                clipboard.isDialogLauncherVisible = true
                ribbon.addItemInvokedListener { invoked += it.item.id }
            }
            settle()
            onUiThreadGet { ribbon.openSlideOut(clipboard) } shouldBe true
            settle()
            onUiThreadGet { ribbon.isSlideOutOpen(clipboard) } shouldBe true
            onUiThread { ribbon.setSlideOutPinned(clipboard, true) }
            onUiThreadGet { ribbon.isSlideOutPinned(clipboard) } shouldBe true
            onUiThread { ribbon.closeSlideOut(clipboard) }
            onUiThreadGet { ribbon.isSlideOutOpen(clipboard) } shouldBe false
            onUiThreadGet { ribbon.openDialogLauncher(clipboard) } shouldBe true
            invoked shouldBe listOf("clipboard")
        }

        test("items can be clicked and split button drop-downs opened and closed from code") {
            val invoked = mutableListOf<String?>()
            onUiThread { ribbon.addItemInvokedListener { invoked += it.item.id } }
            onUiThreadGet { ribbon.performClick(model.findItem("cut")!!) } shouldBe true
            invoked shouldBe listOf("cut")
            val paste = onUiThreadGet { model.findItem("paste")!! }
            onUiThreadGet { ribbon.openDropDown(paste) } shouldBe true
            settle()
            onUiThreadGet { ribbon.isDropDownOpen(paste) } shouldBe true
            onUiThread { ribbon.closeDropDown(paste) }
            settle()
            onUiThreadGet { ribbon.isDropDownOpen(paste) } shouldBe false
            onUiThreadGet { ribbon.openDropDown(model.findItem("cut")!!) } shouldBe false
        }

        test("showing a contextual group that is selected on show makes its tab appear and be selected") {
            onUiThread { model.setActiveContextualGroups("tableTools") }
            settle()
            onUiThreadGet { ribbon.visibleTabs.map { it.id } } shouldBe listOf("home", "insert", "tableDesign")
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "tableDesign"
            onUiThread { model.setActiveContextualGroups() }
            settle()
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "home"
        }

        test("adding an item to the QAT adds a view and removing it removes the view (the item's state shares the same model as in the ribbon)") {
            onUiThread { ribbon.addToQuickAccess(model.findItem("copy")!!) }
            onUiThreadGet { ribbon.quickAccessViews().map { it.model.id } } shouldBe listOf("copy")
            onUiThreadGet { ribbon.addToQuickAccess(model.findItem("copy")!!) } shouldBe false
            onUiThread { ribbon.removeFromQuickAccess(model.findItem("copy")!!) }
            onUiThreadGet { ribbon.quickAccessViews().size } shouldBe 0
        }

        test("showing KeyTips lists the application button and tab KeyTips, and a tab's KeyTip advances to that tab's command level") {
            onUiThread { ribbon.showKeyTips() }
            settle()
            onUiThreadGet { ribbon.activeKeyTips } shouldContainAll listOf("F", "H", "I")
            onUiThread { ribbon.processKeyTipInput('I') }
            settle()
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "insert"
            onUiThreadGet { ribbon.isKeyTipMode } shouldBe true
            onUiThreadGet { ribbon.popKeyTipLevel() } shouldBe true
            onUiThread { ribbon.cancelKeyTips() }
            onUiThreadGet { ribbon.isKeyTipMode } shouldBe false
        }

        test("a contextual tab's KeyTip is the group's KeyTip followed by the first character of the tab header") {
            onUiThread {
                model.contextualGroups.add(RibbonContextualGroupModel("chartTools", "Chart Tools").also { it.keyTip = "J" })
                model.tabs.add(RibbonTabModel("chartDesign", "Chart Design").also { it.contextualGroupId = "chartTools" })
                model.setContextualGroupVisible("chartTools", true)
            }
            settle()
            onUiThread { ribbon.showKeyTips() }
            settle()
            val chartTip = onUiThreadGet { ribbon.currentKeyTips.firstOrNull { it.model?.id == "chartDesign" }?.keyTip }
            chartTip shouldBe "JC"
            onUiThread { ribbon.cancelKeyTips() }
        }

        test("opening a drop-down menu with a KeyTip advances to the menu items' KeyTips, and an item can be invoked") {
            val invoked = mutableListOf<String?>()
            onUiThread {
                val insert = model.tabs.first { it.id == "insert" }
                insert.groups.first().items.add(
                    RibbonDropDownButtonModel("shapes", "Shapes").also { drop ->
                        drop.keyTip = "SH"
                        drop.menuItems.add(RibbonMenuItemModel("line", "Line").also { it.keyTip = "L" })
                        drop.menuItems.add(RibbonMenuItemModel("arrow", "Arrow"))
                    },
                )
                ribbon.addItemInvokedListener { invoked += it.item.id }
                ribbon.showKeyTips()
            }
            settle()
            onUiThread { ribbon.processKeyTipInput('I') }
            settle()
            onUiThread {
                ribbon.processKeyTipInput('S')
                ribbon.processKeyTipInput('H')
            }
            settle()
            onUiThreadGet { ribbon.currentKeyTips.map { it.keyTip to it.model?.id } } shouldContainAll listOf("L" to "line")
            onUiThread { ribbon.processKeyTipInput('L') }
            settle()
            invoked shouldBe listOf("line")
            onUiThreadGet { ribbon.isKeyTipMode } shouldBe false
        }

        test("the simplified ribbon makes the command area one row high") {
            onUiThread { model.displayMode = RibbonDisplayMode.SIMPLIFIED }
            settle()
            val height = onUiThreadGet { ribbon.tabViews[model.findTab("home")]!!.contentHeight }
            height shouldBe ribbon.metrics.simplifiedHeight
        }

        test("in the simplified ribbon, setting a group's simplifiedVisibility to OVERFLOW moves all of its items to the overflow") {
            onUiThread { model.displayMode = RibbonDisplayMode.SIMPLIFIED }
            settle()
            val fontGroup = { ribbon.tabViews[model.findTab("home")]!!.groupViews().first { it.model.id == "font" } }
            onUiThreadGet { fontGroup().overflowViews().map { it.model.id } } shouldBe emptyList()
            onUiThread { model.findGroup("font")!!.simplifiedVisibility = RibbonSimplifiedVisibility.OVERFLOW }
            settle()
            onUiThreadGet { fontGroup().overflowViews().map { it.model.id } } shouldBe listOf("bold", "light")
        }

        test("a tab header widens by the width of its icon, and its description becomes the automation help text") {
            val home = onUiThreadGet { model.findTab("home")!! }
            val before = onUiThreadGet { ribbon.tabStrip.headerOf(home)!!.headerWidth }
            onUiThread {
                home.icon = RibbonIcons.HOME
                home.description = "Frequently used commands"
            }
            settle()
            onUiThreadGet { ribbon.tabStrip.headerOf(home)!!.headerWidth } shouldBeGreaterThan before
            onUiThreadGet { ribbon.tabStrip.headerOf(home)!!.element.automationHelpText } shouldBe "Frequently used commands"
        }

        test("UI Automation: tabs use RibbonTab_{id} and items use their id as the AutomationId, with the KeyTip as the access key") {
            onUiThread { model.findItem("paste")!!.keyTip = "V" }
            settle()
            val home = onUiThreadGet { model.findTab("home")!! }
            onUiThreadGet { ribbon.tabStrip.headerOf(home)!!.element.automationId } shouldBe "RibbonTab_home"
            onUiThreadGet { ribbon.tabStrip.headerOf(home)!!.element.automationAccessKey } shouldBe "H"
            val paste = {
                ribbon.tabViews[home]!!.groupViews().flatMap { it.itemViews() }.first { it.model.id == "paste" }.element
            }
            onUiThreadGet { paste().automationId to paste().automationAccessKey } shouldBe ("paste" to "V")
            onUiThread { model.findItem("paste")!!.automationId = "PasteButton" }
            settle()
            onUiThreadGet { paste().automationId } shouldBe "PasteButton"
        }

        test("in right-to-left layout, the ← key on a tab header moves to the next tab (visually to the left)") {
            val home = onUiThreadGet { model.findTab("home")!! }
            onUiThread { ribbon.flowDirection = FlowDirection.RIGHT_TO_LEFT }
            settle()
            onUiThread { ribbon.tabStrip.headerKeyDown(ribbon.tabStrip.headerOf(home)!!, RibbonInputViews.VK_LEFT) }
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "insert"
            onUiThread { ribbon.flowDirection = FlowDirection.LEFT_TO_RIGHT }
            settle()
            onUiThread { ribbon.tabStrip.headerKeyDown(ribbon.tabStrip.headerOf(model.findTab("insert"))!!, RibbonInputViews.VK_LEFT) }
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "home"
        }

        test("changing the model on a background thread delivers the change notifications on the UI thread and updates the display") {
            val notifiedOnUiThread = mutableListOf<Boolean>()
            val label = onUiThreadGet { model.findItem("copy")!! }
            onUiThread { label.addPropertyChangeListener { notifiedOnUiThread += WinUiUtilities.isDispatchThread } }
            val worker = Thread { label.label = "Duplicate" }
            worker.start()
            worker.join()
            settle()
            notifiedOnUiThread shouldBe listOf(true)
            val copyView = { ribbon.tabViews[model.findTab("home")]!!.groupViews().flatMap { it.itemViews() }.first { it.model.id == "copy" } }
            onUiThreadGet { copyView().element.automationName } shouldBe "Duplicate"
        }

        test("items with isSearchable set to false do not appear in command search results") {
            onUiThreadGet { ribbon.search("Copy").map { it.entry.id } } shouldContain "copy"
            onUiThread { model.findItem("copy")!!.isSearchable = false }
            onUiThreadGet { ribbon.search("Copy").map { it.entry.id } } shouldNotContain "copy"
        }

        test("the default QAT position is the one at the first load and does not change when the position is changed later") {
            onUiThread { model.quickAccessPosition = RibbonQuickAccessPosition.BELOW_RIBBON }
            onUiThreadGet { ribbon.defaultQuickAccessPosition } shouldBe RibbonQuickAccessPosition.ABOVE_RIBBON
        }

        test("an item for which itemFactory returns a component displays it, and setting it back to null restores the default display") {
            val copyView = { ribbon.tabViews[model.findTab("home")]!!.groupViews().flatMap { it.itemViews() }.first { it.model.id == "copy" } }
            val custom = mutableListOf<WButton>()
            onUiThread { ribbon.itemFactory = RibbonItemFactory { item -> if (item.id == "copy") WButton("Custom Copy").also { custom += it } else null } }
            settle()
            onUiThreadGet { copyView() is RibbonFactoryItemView } shouldBe true
            custom.size shouldBe 1
            onUiThreadGet { ribbon.tabViews[model.findTab("home")]!!.groupViews().flatMap { it.itemViews() }.count { it is RibbonFactoryItemView } } shouldBe 1
            onUiThread { ribbon.itemFactory = null }
            settle()
            onUiThreadGet { copyView() is RibbonButtonView } shouldBe true
        }

        test("replacing the model displays the new model's tabs and QAT and no longer reacts to changes to the old model") {
            val old = model
            val replacement = onUiThreadGet {
                RibbonModel().also { m ->
                    m.tabs.add(RibbonTabModel("review", "Review").also { tab -> tab.groups.add(RibbonGroupModel("proofing", "Proofing").also { it.items.add(button("spell", "Spelling")) }) })
                    m.quickAccessItems.add(button("save", "Save"))
                }
            }
            onUiThread { ribbon.model = replacement }
            settle()
            onUiThreadGet { ribbon.visibleTabs.map { it.id } } shouldBe listOf("review")
            onUiThreadGet { ribbon.selectedTab?.id } shouldBe "review"
            onUiThreadGet { ribbon.quickAccessViews().map { it.model.id } } shouldBe listOf("save")
            onUiThread { old.tabs.add(RibbonTabModel("stale", "Old Tab")) }
            settle()
            onUiThreadGet { ribbon.visibleTabs.map { it.id } } shouldBe listOf("review")
        }

        test("a tab selected in the model beforehand has its content shown when displayed") {
            val preset = onUiThreadGet { createModel().also { it.selectedTabId = "insert" } }
            val presetRibbon = onUiThreadGet { WRibbon(preset) }
            UiTestHarness.attachAndAwaitLoaded(presetRibbon)
            try {
                onUiThreadGet { presetRibbon.tabViews[preset.findTab("insert")]!!.element.isVisible } shouldBe true
                onUiThread { presetRibbon.itemFactory = RibbonItemFactory { null } }
                onUiThreadGet { presetRibbon.tabViews[preset.findTab("insert")]!!.element.isVisible } shouldBe true
            } finally {
                UiTestHarness.detach(presetRibbon)
            }
        }

        test("tab headers are displayed at the font size of the same density their width was measured with (long headers are not clipped even in compact)") {
            val home = onUiThreadGet { model.findTab("home")!! }
            val fontSize = {
                ribbon.tabStrip.headerOf(home)!!.element.view(com.appkitbox.winui4k.internal.winui.XamlInterop.IID_IControl)
                    .getDouble(com.appkitbox.winui4k.internal.winui.XamlInterop.IControl_get_FontSize)
            }
            onUiThreadGet { fontSize() } shouldBe ribbon.metrics.fontSize
            onUiThread { model.density = RibbonDensity.COMPACT }
            settle()
            onUiThreadGet { fontSize() } shouldBe RibbonMetrics.COMPACT.fontSize
        }

        test("in tabs-only mode the command area is hidden and can be shown temporarily in a popup") {
            onUiThread { model.visibilityMode = RibbonVisibilityMode.TABS_ONLY }
            settle()
            onUiThreadGet { ribbon.commandBar.isVisible } shouldBe false
            onUiThread { ribbon.openMinimizedPopup() }
            settle()
            onUiThreadGet { ribbon.isMinimizedPopupOpen } shouldBe true
            onUiThread { ribbon.closeMinimizedPopup() }
            onUiThreadGet { ribbon.isMinimizedPopupOpen } shouldBe false
        }

        test("toggling minimization follows the minimize behavior, and the AutoCAD cycle goes full → panel buttons → panel titles → tabs") {
            onUiThread { model.minimizeBehavior = com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior.CYCLE_ALL }
            val visited = onUiThreadGet {
                (1..4).map {
                    ribbon.toggleMinimized()
                    model.visibilityMode
                }
            }
            visited shouldBe listOf(
                RibbonVisibilityMode.PANEL_BUTTONS,
                RibbonVisibilityMode.PANEL_TITLES,
                RibbonVisibilityMode.TABS_ONLY,
                RibbonVisibilityMode.ALWAYS_SHOW,
            )
        }

        test("saving the state to JSON and restoring it brings back the display mode, the QAT, and the selected tab") {
            val json = onUiThreadGet {
                model.displayMode = RibbonDisplayMode.SIMPLIFIED
                ribbon.addToQuickAccess(model.findItem("cut")!!)
                ribbon.selectTab("insert")
                ribbon.saveStateToJson()
            }
            onUiThread {
                model.displayMode = RibbonDisplayMode.CLASSIC
                ribbon.removeFromQuickAccess(model.findItem("cut")!!)
                ribbon.selectTab("home")
            }
            onUiThreadGet { ribbon.loadStateFromJson(json) } shouldBe true
            onUiThreadGet { Triple(model.displayMode, ribbon.quickAccessItemIds(), ribbon.selectedTab?.id) } shouldBe
                Triple(RibbonDisplayMode.SIMPLIFIED, listOf("cut"), "insert")
            onUiThreadGet { ribbon.loadStateFromJson("{ invalid") } shouldBe false
        }

        test("hiding and renaming tabs through customization changes only the display, not the model") {
            onUiThread {
                ribbon.applyCustomization(
                    com.appkitbox.winui4k.ribbon.RibbonCustomization().also {
                        it.hiddenTabIds += "insert"
                        it.labels["home"] = "Start"
                    },
                )
            }
            settle()
            onUiThreadGet { ribbon.visibleTabs.map { it.id } } shouldBe listOf("home")
            onUiThreadGet { ribbon.tabLabel(model.findTab("home")!!) } shouldBe "Start"
            model.findTab("home")!!.label shouldBe "Home"
            onUiThread { ribbon.resetCustomization() }
            onUiThreadGet { ribbon.visibleTabs.map { it.id } } shouldBe listOf("home", "insert")
        }

        test("command search finds items in the visible tabs along with their location") {
            val results = onUiThreadGet { ribbon.search("Copy") }
            results.first().entry.id shouldBe "copy"
            results.first().entry.path shouldBe "Home › Clipboard"
        }

        test("floating a group removes it from the ribbon, and returning it puts it back in its original place") {
            onUiThread { ribbon.canFloatGroups = true }
            val clipboard = model.findGroup("clipboard")!!
            onUiThreadGet { ribbon.floatGroup(clipboard, FLOAT_X, FLOAT_Y) } shouldBe true
            settle()
            onUiThreadGet { ribbon.floatingGroups() } shouldBe listOf(clipboard)
            onUiThreadGet { ribbon.host.allGroupViews().first { it.model === clipboard }.isFloatingPanelOpen } shouldBe true
            onUiThread { ribbon.returnAllPanelsToRibbon() }
            onUiThreadGet { ribbon.floatingGroups() } shouldBe emptyList()
        }

        test("a large color picker splits vertically like a split button and is the same size as a large split button with the same label") {
            val (picker, split) = onUiThreadGet {
                val group = model.findGroup("tables")!!
                val picker = RibbonColorPickerModel("pageColor", "Page Color", RibbonIcons.SHADING).also { it.size = RibbonItemSize.LARGE }
                val split = RibbonSplitButtonModel("borders", "Page Color").also {
                    it.icon = RibbonIcons.SHADING
                    it.size = RibbonItemSize.LARGE
                }
                group.items.add(picker)
                group.items.add(split)
                model.selectedTabId = "insert"
                ribbon.width = WIDE
                picker to split
            }
            settle()
            val sizes = onUiThreadGet {
                listOf(picker, split).map { item ->
                    val view = ribbon.host.findView(item)!!
                    view.layout.size to view.measure(view.layout)
                }
            }
            sizes.map { it.first } shouldBe listOf(RibbonItemSize.LARGE, RibbonItemSize.LARGE)
            sizes[0].second shouldBe sizes[1].second
        }

        test("item size calculation follows model changes (changing the label changes the width of a large item)") {
            val view = onUiThreadGet { ribbon.host.findView(model.findItem("extra0.0")!!)!! }
            val before = onUiThreadGet { view.measure(view.layout).width }
            onUiThread { model.findItem("extra0.0")!!.label = "This is a command with a very long label" }
            val after = onUiThreadGet { view.measure(view.layout).width }
            after shouldBeGreaterThan before
            onUiThreadGet { view.element.actualWidth } shouldNotBe 0.0
        }
    }

    private companion object {
        const val SETTLE_ROUNDS = 4
        const val EXTRA_GROUPS = 4
        const val EXTRA_ITEMS = 4
        const val NARROW = 360.0
        const val WIDE = 4000.0
        const val FLOAT_X = 100.0
        const val FLOAT_Y = 200.0
    }
}
