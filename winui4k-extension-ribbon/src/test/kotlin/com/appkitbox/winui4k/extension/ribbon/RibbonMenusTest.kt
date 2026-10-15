package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.UiTestHarness
import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutItemBase
import com.appkitbox.winui4k.WMenuFlyoutSubItem
import com.appkitbox.winui4k.WRadioMenuFlyoutItem
import com.appkitbox.winui4k.WToggleMenuFlyoutItem
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMinimizeBehavior
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * E2E test of the ribbon's menus (RibbonMenus / RibbonMenuBuilder) in an actual WinUI window: the menus the ribbon
 * builds open, and their items carry the icons, whatever the kind of the menu item (plain, toggle, radio, or submenu).
 */
class RibbonMenusTest : FunSpec() {
    private lateinit var model: RibbonModel
    private lateinit var ribbon: WRibbon

    private fun createModel(): RibbonModel = RibbonModel().apply {
        val home = RibbonTabModel("home", "Home")
        home.groups.add(
            RibbonGroupModel("clipboard", "Clipboard").also { g ->
                g.items.add(RibbonButtonModel("copy", "Copy", RibbonIcons.COPY))
                g.items.add(RibbonToggleButtonModel("bold", "Bold").also { it.icon = RibbonIcons.COPY })
            },
        )
        tabs.add(home)
    }

    /** Whether the menu item has an icon (MenuFlyoutItem.Icon / MenuFlyoutSubItem.Icon is not null). */
    private fun hasIcon(item: WMenuFlyoutItemBase): Boolean {
        val (iid, slot) = when (item) {
            is WMenuFlyoutSubItem -> XamlInterop.IID_IMenuFlyoutSubItem to XamlInterop.IMenuFlyoutSubItem_get_Icon
            else -> XamlInterop.IID_IMenuFlyoutItem to XamlInterop.IMenuFlyoutItem_get_Icon
        }
        val view = item.inspectable.queryInterface(iid)
        try {
            val icon = view.getPtrOrNull(slot) ?: return false
            icon.release()
            return true
        } finally {
            view.release()
        }
    }

    /** Waits until [menu] opens, then closes it and waits until it has closed (so the next menu can open). */
    private suspend fun awaitOpenedThenClose(menu: WMenuFlyout) {
        try {
            eventually(POPUP_TIMEOUT) { onUiThreadGet { menu.isOpen } shouldBe true }
        } finally {
            onUiThread { menu.hide() }
            eventually(POPUP_TIMEOUT) { onUiThreadGet { menu.isOpen } shouldBe false }
        }
    }

    init {
        beforeTest {
            model = onUiThreadGet { createModel() }
            ribbon = onUiThreadGet { WRibbon(model) }
            UiTestHarness.attachAndAwaitLoaded(ribbon)
        }

        afterTest {
            onUiThread { ribbon.suspendPopups() }
            UiTestHarness.detach(ribbon)
        }

        test("the display options menu opened by its button shows an icon on each display mode, also when it is opened again") {
            val strings = RibbonStrings.current
            repeat(OPEN_TIMES) {
                val menu = onUiThreadGet { ribbon.showDisplayOptionsMenu() }
                val modes = onUiThreadGet {
                    menu.addedItems.filterIsInstance<WRadioMenuFlyoutItem>().filter { it.groupName == "visibility" }.map { it.text to hasIcon(it) }
                }
                modes shouldBe listOf(strings.fullScreenMode, strings.showTabsOnly, strings.alwaysShowRibbon).map { it to true }
                awaitOpenedThenClose(menu)
            }
        }

        test("the display options menu with the panel modes (shown while the minimize button is) opens with their icons") {
            val strings = RibbonStrings.current
            onUiThread {
                model.minimizeBehavior = RibbonMinimizeBehavior.PANEL_TITLES
                ribbon.isMinimizeButtonVisible = true
            }
            val menu = onUiThreadGet { ribbon.showDisplayOptionsMenu() }
            onUiThreadGet {
                menu.addedItems.filterIsInstance<WRadioMenuFlyoutItem>().filter { it.groupName == "visibility" }.map { it.text to hasIcon(it) }
            } shouldBe listOf(strings.fullScreenMode, strings.showPanelTitles, strings.showPanelButtons, strings.showTabsOnly, strings.alwaysShowRibbon)
                .map { it to true }
            awaitOpenedThenClose(menu)
        }

        test("the minimize behavior menu opened by its button lists the behaviors with the current one checked, also when opened again") {
            val strings = RibbonStrings.current
            onUiThread {
                model.minimizeBehavior = RibbonMinimizeBehavior.PANEL_BUTTONS
                ribbon.isMinimizeButtonVisible = true
            }
            repeat(OPEN_TIMES) {
                val menu = onUiThreadGet { ribbon.showMinimizeBehaviorMenu() }
                onUiThreadGet { menu.addedItems.filterIsInstance<WRadioMenuFlyoutItem>().map { it.text to it.isChecked } } shouldBe listOf(
                    strings.minimizeToTabs to false,
                    strings.minimizeToPanelTitles to false,
                    strings.minimizeToPanelButtons to true,
                    strings.cycleThroughAll to false,
                )
                awaitOpenedThenClose(menu)
            }
        }

        test("an item's right-click menu shows the icon of [Add to Quick Access Toolbar], also when opened again") {
            val menus = mutableListOf<WMenuFlyout>()
            onUiThread { ribbon.addContextMenuListener(RibbonContextMenuListener { menus += it.menu }) }
            repeat(OPEN_TIMES) { index ->
                onUiThreadGet { ribbon.showItemContextMenu(model.findItem("copy")!!) } shouldBe true
                val menu = menus[index]
                onUiThreadGet {
                    menu.addedItems.filterIsInstance<WMenuFlyoutItem>().filter { it.text == RibbonStrings.current.addToQuickAccessToolbar }.map { hasIcon(it) }
                } shouldBe listOf(true)
                // The menu opens at a point through a parentless popup, which the shared test window does not always show
                onUiThread { menu.hide() }
            }
        }

        test("the customize menu of the Quick Access Toolbar lists the candidates and [Show Command Labels] as check items") {
            onUiThread { model.quickAccessCandidates.add(model.findItem("copy")!!) }
            val menu = onUiThreadGet { ribbon.buildQuickAccessCustomizeMenu() }
            onUiThreadGet { menu.addedItems.filterIsInstance<WToggleMenuFlyoutItem>().map { it.text to it.isChecked } } shouldBe
                listOf("Copy" to false, RibbonStrings.current.showCommandLabels to false)
        }

        test("menu items made from the model show their icons whether they are plain, toggle, radio, or submenu items") {
            val entries = listOf(
                RibbonMenuItemModel("plain", "Plain").also { it.icon = RibbonIcons.COPY },
                RibbonMenuItemModel("check", "Check").also {
                    it.icon = RibbonIcons.COPY
                    it.isCheckable = true
                    it.isChecked = true
                },
                RibbonMenuItemModel("radio", "Radio").also {
                    it.icon = RibbonIcons.COPY
                    it.isCheckable = true
                    it.groupName = "group"
                },
                RibbonMenuItemModel("sub", "Sub").also {
                    it.icon = RibbonIcons.COPY
                    it.items.add(RibbonMenuItemModel("child", "Child"))
                },
            )
            val items = onUiThreadGet {
                RibbonMenus.createMenuItems(entries, ribbon.host).map { item ->
                    val kind = when (item) {
                        is WRadioMenuFlyoutItem -> "radio"
                        is WToggleMenuFlyoutItem -> "toggle"
                        is WMenuFlyoutSubItem -> "sub"
                        else -> "plain"
                    }
                    kind to hasIcon(item)
                }
            }
            items shouldBe listOf("plain" to true, "toggle" to true, "radio" to true, "sub" to true)
        }

        test("a toggle button in the overflow menu becomes a check item with the button's icon") {
            val item = onUiThreadGet {
                val view = ribbon.host.findView(model.findItem("bold")!!)!!
                view.overflowMenuItems().single()
            }
            onUiThreadGet { (item as WToggleMenuFlyoutItem).text to hasIcon(item) } shouldBe ("Bold" to true)
        }
    }

    private companion object {
        const val OPEN_TIMES = 3
        val POPUP_TIMEOUT = 5.seconds
    }
}
