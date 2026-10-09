package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.UiTestHarness
import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.extension.ribbon.model.RibbonApplicationMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonApplicationMenuModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonComboBoxModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuBarItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRecentItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRelayCommand
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStatusBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToolBarModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToolBarOrientation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonZoomModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/** E2E tests for the components outside the ribbon (toolbar, status bar, zoom, menu bar, application menu, search, title bar). */
class WRibbonBarsTest : FunSpec() {
    private val attached = mutableListOf<WComponent>()

    private fun settle(component: WComponent) {
        repeat(SETTLE_ROUNDS) { onUiThread { component.updateLayout() } }
    }

    private fun show(component: WComponent): WComponent {
        UiTestHarness.attachAndAwaitLoaded(component)
        attached += component
        settle(component)
        return component
    }

    init {
        afterTest {
            attached.forEach { UiTestHarness.detach(it) }
            attached.clear()
        }

        test("the toolbar moves items that do not fit its width to the overflow menu and brings them back when widened") {
            val model = onUiThreadGet {
                RibbonToolBarModel().also { m ->
                    m.showLabels = true
                    repeat(ITEMS) { m.items.add(RibbonButtonModel("tool$it", "Tool $it", RibbonIcons.PEN)) }
                }
            }
            val toolBar = onUiThreadGet { WRibbonToolBar(model).also { it.width = NARROW } }
            show(toolBar)
            val narrow = onUiThreadGet { toolBar.overflowItems.size }
            narrow shouldBeGreaterThan 0
            onUiThread { toolBar.width = WIDE }
            settle(toolBar)
            onUiThreadGet { toolBar.overflowItems.size } shouldBe 0
        }

        test("a vertical toolbar whose height fits its contents does not move items to overflow when all items fit") {
            val model = onUiThreadGet {
                RibbonToolBarModel().also { m ->
                    m.orientation = RibbonToolBarOrientation.VERTICAL
                    m.columns = 2
                    repeat(PALETTE_ITEMS) { m.items.add(RibbonButtonModel("tool$it", "Tool $it", RibbonIcons.PEN)) }
                }
            }
            // Because it is top-aligned, the toolbar's height is the content height it decides itself (no height constraint comes from outside)
            val toolBar = onUiThreadGet { WRibbonToolBar(model).also { it.verticalAlignment = VerticalAlignment.TOP } }
            show(toolBar)
            onUiThreadGet { toolBar.overflowItems.map { it.id } } shouldBe emptyList()
        }

        test("invoking a toolbar item is notified as a ribbon item invocation") {
            val model = RibbonModel()
            val ribbon = onUiThreadGet { WRibbon(model) }
            val toolModel = onUiThreadGet { RibbonToolBarModel().also { it.items.add(RibbonButtonModel("draw", "Draw", RibbonIcons.PEN)) } }
            val toolBar = onUiThreadGet { WRibbonToolBar(toolModel).also { it.ribbon = ribbon } }
            show(toolBar)
            val invoked = mutableListOf<String?>()
            onUiThread {
                ribbon.addItemInvokedListener { invoked += it.item.id }
                toolBar.itemViews().first().invoke()
            }
            invoked shouldBe listOf("draw")
        }

        test("a toolbar not attached to a ribbon also notifies item invocations, with the bar itself as the source") {
            val toolModel = onUiThreadGet { RibbonToolBarModel().also { it.items.add(RibbonButtonModel("draw", "Draw", RibbonIcons.PEN)) } }
            val toolBar = onUiThreadGet { WRibbonToolBar(toolModel) }
            show(toolBar)
            val invoked = mutableListOf<Triple<String?, Any, WRibbon?>>()
            onUiThread {
                toolBar.addItemInvokedListener { invoked += Triple(it.item.id, it.source, it.ribbon) }
                toolBar.itemViews().first().invoke()
            }
            invoked shouldBe listOf(Triple("draw", toolBar, null))
        }

        test("the toolbar shows no labels by default, and showLabels shows the item labels (showLabelInSimplified)") {
            val toolModel = onUiThreadGet {
                RibbonToolBarModel().also { m -> m.items.add(RibbonButtonModel("draw", "Draw", RibbonIcons.PEN).also { it.showLabelInSimplified = true }) }
            }
            toolModel.showLabels shouldBe false
            val toolBar = onUiThreadGet { WRibbonToolBar(toolModel) }
            show(toolBar)
            val iconOnly = onUiThreadGet { toolBar.itemViews().first().element.desiredSize()[0] }
            onUiThread { toolModel.showLabels = true }
            settle(toolBar)
            onUiThreadGet { toolBar.itemViews().first().element.desiredSize()[0] } shouldBeGreaterThan iconOnly
        }

        test("zoom fires only for user operations on the buttons and slider, not for changes made in code (values are clamped to the range)") {
            val executed = mutableListOf<Any?>()
            val zoom = onUiThreadGet {
                RibbonZoomModel("zoom", 100.0).also { it.command = RibbonRelayCommand({ p -> executed += p }) }
            }
            val statusModel = onUiThreadGet { RibbonStatusBarModel().also { it.endItems.add(zoom) } }
            val statusBar = onUiThreadGet { WRibbonStatusBar(statusModel) }
            show(statusBar)
            val view = onUiThreadGet { statusBar.itemViews().single() as RibbonZoomView }
            onUiThread { zoom.value = 150.0 }
            executed shouldBe emptyList()
            onUiThread { view.zoomIn() }
            (zoom.value to executed) shouldBe (160.0 to listOf<Any?>(160.0))
            onUiThread { view.zoomOut() }
            (zoom.value to executed.last()) shouldBe (150.0 to 150.0)
            onUiThread { zoom.value = 9999.0 }
            zoom.value shouldBe 500.0
            executed.size shouldBe 2
        }

        test("the menu bar opens a menu from its heading and notifies just before it opens") {
            var opening = 0
            val model = onUiThreadGet {
                RibbonMenuBarModel().also { m ->
                    m.items.add(
                        RibbonMenuBarItemModel("file", "File").also { item ->
                            item.items.add(RibbonMenuItemModel("open", "Open"))
                            item.addOpeningListener { opening++ }
                        },
                    )
                }
            }
            val menuBar = onUiThreadGet { WRibbonMenuBar(model) }
            show(menuBar)
            onUiThreadGet { menuBar.openMenu("File") } shouldBe true
            settle(menuBar)
            opening shouldBe 1
            onUiThreadGet { menuBar.openMenu("Edit") } shouldBe false
        }

        test("the application menu switches the right pane for items with subcommands, invokes commands, and finds them by search") {
            val executed = mutableListOf<String>()
            val model = onUiThreadGet {
                RibbonApplicationMenuModel().also { m ->
                    m.items.add(
                        RibbonApplicationMenuItemModel("saveAs", "Save As", RibbonIcons.SAVE_AS).also { item ->
                            item.items.add(
                                RibbonApplicationMenuItemModel("saveDrawing", "Drawing").also { sub ->
                                    sub.command = RibbonRelayCommand.of({ executed += "drawing" }, null)
                                },
                            )
                        },
                    )
                    m.recentItems.add(RibbonRecentItemModel("r1", "Plan.dwg", "C:\\drawings"))
                }
            }
            val menu = onUiThreadGet { WRibbonApplicationMenu(model) }
            show(menu)
            onUiThreadGet { menu.shownItem } shouldBe null
            onUiThread { menu.invoke(model.items[0]) }
            onUiThreadGet { menu.shownItem?.id } shouldBe "saveAs"
            onUiThread { menu.invoke(model.items[0].items[0]) }
            executed shouldBe listOf("drawing")
            onUiThreadGet { menu.searchMenu("Drawing").map { it.entry.label } } shouldBe listOf("Drawing")
        }

        test("the application menu shows the subcommands of the command under the pointer and reverts to the default view for a command without subcommands") {
            val model = onUiThreadGet {
                RibbonApplicationMenuModel().also { m ->
                    m.items.add(
                        RibbonApplicationMenuItemModel("saveAs", "Save As", RibbonIcons.SAVE_AS).also {
                            it.items.add(RibbonApplicationMenuItemModel("saveDrawing", "Drawing"))
                        },
                    )
                    m.items.add(RibbonApplicationMenuItemModel("print", "Print", RibbonIcons.PRINT))
                }
            }
            val menu = onUiThreadGet { WRibbonApplicationMenu(model) }
            show(menu)
            // What happens when the pointer enters or focus arrives
            onUiThread { menu.onCommandHovered(model.items[0]) }
            onUiThreadGet { menu.shownItem?.id } shouldBe "saveAs"
            onUiThread { menu.onCommandHovered(model.items[1]) }
            onUiThreadGet { menu.shownItem } shouldBe null
        }

        test("the color palette can be placed on its own, and setting showThemeColors to false hides the theme colors section") {
            val model = onUiThreadGet { RibbonColorPickerModel("fill", "Fill Color") }
            val palette = onUiThreadGet { WRibbonColorPalette(model) }
            show(palette)
            val withTheme = onUiThreadGet { palette.swatchCount }
            onUiThread { model.showThemeColors = false }
            onUiThreadGet { palette.swatchCount } shouldBe withTheme - THEME_SWATCHES
        }

        test("committing a combo box reports both the item and the text (if it is editable and nothing matches, the item is null)") {
            val combo = onUiThreadGet { RibbonComboBoxModel("size", "Size", listOf("10", "12")).also { it.isEditable = true } }
            val toolBar = onUiThreadGet { WRibbonToolBar(RibbonToolBarModel().also { it.items.add(combo) }) }
            show(toolBar)
            val commits = mutableListOf<Pair<Any?, String>>()
            onUiThread {
                combo.addCommitListener { commits += it.item to it.text }
                val view = toolBar.itemViews().first() as RibbonComboBoxView
                view.commit("12")
                view.commitText("14")
            }
            commits shouldBe listOf<Pair<Any?, String>>("12" to "12", null to "14")
        }

        test("WRibbonIcon draws a layered line-art icon at the given size and redraws it when the size changes") {
            val icon = onUiThreadGet { WRibbonIcon(RibbonIcon.path("[viewbox=32;stroke=1.8]M6,26 L26,6|[color=#3DA9F5]M4,24 L8,24 L8,28 L4,28 Z", 32.0), 24.0) }
            show(icon)
            onUiThreadGet { icon.preferredSize().width } shouldBe 24.0
            onUiThread { icon.iconSize = 40.0 }
            settle(icon)
            onUiThreadGet { icon.preferredSize().width } shouldBe 40.0
        }

        test("the search box searches the ribbon, invokes a result, and clears the search text") {
            val model = onUiThreadGet {
                RibbonModel().also { m ->
                    m.tabs.add(RibbonTabModel("home", "Home").also { t -> t.groups.add(RibbonGroupModel("g", "Editing").also { it.items.add(RibbonButtonModel("find", "Find and Replace", RibbonIcons.FIND)) }) })
                }
            }
            val ribbon = onUiThreadGet { WRibbon(model) }
            show(ribbon)
            val box = onUiThreadGet { WRibbonSearchBox(ribbon).also { it.width = BOX_WIDTH } }
            show(box)
            val invoked = mutableListOf<String?>()
            onUiThread {
                ribbon.addItemInvokedListener { invoked += it.item.id }
                box.text = "Replace"
            }
            val results = onUiThreadGet { ribbon.search("Replace") }
            results.shouldNotBeEmpty()
            onUiThread { ribbon.executeSearchEntry(results.first().entry) }
            invoked shouldBe listOf("find")
        }

        test("the search box shows no results right after it appears, opening the results also opens the layer covering the outside, and closing closes both") {
            val ribbon = onUiThreadGet { WRibbon(RibbonModel()) }
            show(ribbon)
            val box = onUiThreadGet { WRibbonSearchBox(ribbon).also { it.width = BOX_WIDTH } }
            show(box)
            onUiThreadGet { box.isResultsOpen to box.isDismissLayerOpen } shouldBe (false to false)
            onUiThread { box.focusSearch() }
            onUiThreadGet { box.isResultsOpen to box.isDismissLayerOpen } shouldBe (true to true)
            // The screen reader notification for the highlighted result (AutomationPeer.RaiseNotificationEvent) can be raised without throwing
            onUiThread { Xaml.announce(box.inspectable, "Test", "RibbonSearchHighlight") }
            onUiThread { box.close() }
            onUiThreadGet { box.isResultsOpen to box.isDismissLayerOpen } shouldBe (false to false)
        }

        test("the title bar hosts the QAT while it is above the ribbon and returns it to the ribbon when it moves below") {
            val model = onUiThreadGet { RibbonModel().also { it.quickAccessItems.add(RibbonButtonModel("save", "Save", RibbonIcons.SAVE)) } }
            val ribbon = onUiThreadGet { WRibbon(model) }
            val titleBar = onUiThreadGet { WRibbonTitleBar(ribbon) }
            val panel = onUiThreadGet {
                WPanel().also {
                    it.add(titleBar)
                    it.add(ribbon)
                }
            }
            show(panel)
            onUiThreadGet { titleBar.isHostingQuickAccess to ribbon.isQuickAccessHostedExternally } shouldBe (true to true)
            onUiThread { model.quickAccessPosition = RibbonQuickAccessPosition.BELOW_RIBBON }
            settle(panel)
            onUiThreadGet { titleBar.isHostingQuickAccess } shouldBe false
            onUiThreadGet { Xaml.parentOf(ribbon.quickAccessElement.inspectable) != null && ribbon.quickAccessViews().size == 1 } shouldBe true
        }
    }

    private companion object {
        const val SETTLE_ROUNDS = 4
        const val ITEMS = 12
        const val PALETTE_ITEMS = 6
        const val NARROW = 200.0
        const val WIDE = 3000.0
        const val BOX_WIDTH = 300.0

        /** The 10 theme colors and their 5 shades. */
        const val THEME_SWATCHES = 60
    }
}
