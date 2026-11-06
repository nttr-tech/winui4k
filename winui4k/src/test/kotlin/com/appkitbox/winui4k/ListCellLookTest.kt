package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Verifies the JList-style item look (selectionBackground / fixedCellHeight) of WList (ListView) and WListBox (ListBox)
 * with the item containers actually shown.
 * - selectionBackground: reads SelectedBackground of the item template root (ListViewItemPresenter) for ListView,
 *   and the Fill color of the selection background (Rectangle "PressedBackground") in the selected item's template for ListBox
 * - fixedCellHeight: reads ActualHeight of the item container. -1 is the default, as in Swing (decided by the item content)
 * The form seen from Java is in WListForJavaTest / WListBoxForJavaTest.
 */
@Tags("E2E")
class ListCellLookTest : FunSpec() {
    /** A thin interface for checking both kinds of lists with the same tests. */
    private class Kind(val name: String, val create: (List<String>) -> WControl)

    private val kinds = listOf(
        Kind("WList") { WList(it) },
        Kind("WListBox") { WListBox(it) },
    )

    private var WControl.selectionColor: WColor?
        get() = if (this is WList) selectionBackground else (this as WListBox).selectionBackground
        set(value) = if (this is WList) selectionBackground = value else (this as WListBox).selectionBackground = value

    private var WControl.cellHeight: Int
        get() = if (this is WList) fixedCellHeight else (this as WListBox).fixedCellHeight
        set(value) = if (this is WList) fixedCellHeight = value else (this as WListBox).fixedCellHeight = value

    private var WControl.rows: Int
        get() = if (this is WList) visibleRowCount else (this as WListBox).visibleRowCount
        set(value) = if (this is WList) visibleRowCount = value else (this as WListBox).visibleRowCount = value

    private fun WControl.select(index: Int) = if (this is WList) selectedIndex = index else (this as WListBox).selectedIndex = index

    private val red = WColor(200, 40, 40)

    /** The item container of item [index] of [list] (an owned reference). null if not created. */
    private fun containerOf(list: WControl, index: Int): ComPtr? {
        val mapping = list.inspectable.queryInterface(XamlInterop.IID_IItemContainerMapping)
        return try {
            mapping.getPtrOrNull(XamlInterop.IItemContainerMapping_ContainerFromIndex, index)
        } finally {
            mapping.release()
        }
    }

    /** The selection background color when item [index] of [list] is selected. null if it cannot be read. */
    private fun selectedColorOf(list: WControl, index: Int): WColor? {
        val container = containerOf(list, index) ?: return null
        return try {
            if (list is WList) presenterSelectedColor(container) else listBoxSelectedColor(container)
        } finally {
            container.release()
        }
    }

    /** The SelectedBackground color of the template root (ListViewItemPresenter) of the ListViewItem [container] (borrowed). */
    private fun presenterSelectedColor(container: ComPtr): WColor? {
        val presenter = Xaml.childAt(container, 0) ?: return null
        val view = presenter.queryInterface(XamlInterop.IID_IListViewItemPresenter)
        presenter.release()
        return try {
            brushColor(view, XamlInterop.IListViewItemPresenter_get_SelectedBackground)
        } finally {
            view.release()
        }
    }

    /** The Fill color of the selection background (Rectangle "PressedBackground") in the template of the ListBoxItem [container] (borrowed). */
    private fun listBoxSelectedColor(container: ComPtr): WColor? {
        container.addRef() // findByName takes ownership
        val rectangle = TemplateParts.findByName(container, "PressedBackground") ?: return null
        val shape = rectangle.queryInterface(XamlInterop.IID_IShape)
        rectangle.release()
        return try {
            brushColor(shape, XamlInterop.IShape_get_Fill)
        } finally {
            shape.release()
        }
    }

    private fun brushColor(view: ComPtr, slot: Int): WColor? {
        val brush = view.getPtrOrNull(slot) ?: return null
        return try {
            colorOf(brush)
        } finally {
            brush.release()
        }
    }

    /** The sum of the top and bottom padding (Padding) and border (BorderThickness) of [list] (the part added to the visibleRowCount height). */
    private fun verticalPaddingAndBorder(list: WControl): Double {
        val padding = XamlStructs.getThickness(list.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(list.control, XamlInterop.IControl_get_BorderThickness)
        return padding[1] + padding[3] + border[1] + border[3]
    }

    /** The height (DIP) of the item container of item [index] of [list]. NaN if not created. */
    private fun cellHeightOf(list: WControl, index: Int): Double {
        val container = containerOf(list, index) ?: return Double.NaN
        return try {
            VisualHitTest.actualSizeOf(container).second
        } finally {
            container.release()
        }
    }

    /** Shows [list] and advances layout until [condition] holds (up to 5 seconds). true if it holds. */
    private fun awaitOnUi(list: WControl, condition: (WControl) -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet { list.updateLayout().let { _ -> condition(list) } }) return true
            Thread.sleep(50)
        }
        return false
    }

    /** Shows [list], runs [block], and then removes it. */
    private fun <T> shown(list: WControl, block: () -> T): T {
        onUiThreadGet { list.height = 300.0 }
        UiTestHarness.attachAndAwaitLoaded(list)
        try {
            check(awaitOnUi(list) { containerOf(it, 0)?.also { c -> c.release() } != null }) { "No item is created" }
            return block()
        } finally {
            UiTestHarness.detach(list)
        }
    }

    /** The color with item 0 selected when shown with the theme's selection background. */
    private fun themeSelectedColor(kind: Kind): WColor? {
        val list = onUiThreadGet { kind.create(listOf("A", "B", "C")).also { it.select(0) } }
        return shown(list) {
            var color: WColor? = null
            awaitOnUi(list) {
                color = selectedColorOf(it, 0)
                color != null
            }
            color
        }
    }

    /** The theme's default item height. */
    private fun themeCellHeight(kind: Kind): Double {
        val list = onUiThreadGet { kind.create(listOf("A", "B", "C")) }
        return shown(list) { onUiThreadGet { cellHeightOf(list, 0) } }
    }

    init {
        for (kind in kinds) {
            context("${kind.name}: Defaults") {
                test("selectionBackground is null by default, and fixedCellHeight is -1 by default, the same as Swing") {
                    onUiThreadGet { kind.create(emptyList()).let { it.selectionColor to it.cellHeight } } shouldBe (null to -1)
                }
            }

            context("${kind.name}: Reading and writing") {
                test("setting selectionBackground before showing makes the selected item's background that color") {
                    val list = onUiThreadGet {
                        kind.create(listOf("A", "B", "C")).also {
                            it.selectionColor = red
                            it.select(1)
                        }
                    }
                    shown(list) {
                        awaitOnUi(list) { selectedColorOf(it, 1) == red } shouldBe true
                        onUiThreadGet { list.selectionColor } shouldBe red
                    }
                }

                test("setting selectionBackground after showing also makes the selected item's background that color") {
                    val list = onUiThreadGet { kind.create(listOf("A", "B", "C")).also { it.select(0) } }
                    shown(list) {
                        onUiThreadGet { list.selectionColor = red }
                        awaitOnUi(list) { selectedColorOf(it, 0) == red } shouldBe true
                    }
                }

                test("resetting selectionBackground to null returns to the theme's default color") {
                    val theme = themeSelectedColor(kind)
                    theme shouldNotBe red
                    val list = onUiThreadGet {
                        kind.create(listOf("A", "B", "C")).also {
                            it.selectionColor = red
                            it.select(0)
                        }
                    }
                    shown(list) {
                        awaitOnUi(list) { selectedColorOf(it, 0) == red } shouldBe true
                        onUiThreadGet { list.selectionColor = null }
                        awaitOnUi(list) { selectedColorOf(it, 0) == theme } shouldBe true
                    }
                }

                test("setting fixedCellHeight makes the item height that value (lower or higher than the default), and resetting it to -1 returns to the default height") {
                    val theme = themeCellHeight(kind)
                    val list = onUiThreadGet { kind.create(listOf("A", "B", "C")) }
                    shown(list) {
                        val heights = listOf(24, 60, -1).map { value ->
                            onUiThreadGet { list.cellHeight = value }
                            awaitOnUi(list) { cellHeightOf(it, 0) == if (value < 0) theme else value.toDouble() }
                        }
                        heights shouldBe listOf(true, true, true)
                        onUiThreadGet { list.cellHeight } shouldBe -1
                    }
                }

                test("fixedCellHeight and selectionBackground work together") {
                    val list = onUiThreadGet {
                        kind.create(listOf("A", "B", "C")).also {
                            it.cellHeight = 30
                            it.selectionColor = red
                            it.select(2)
                        }
                    }
                    shown(list) {
                        awaitOnUi(list) { cellHeightOf(it, 2) == 30.0 && selectedColorOf(it, 2) == red } shouldBe true
                    }
                }
            }

            context("${kind.name}: Swing compatibility") {
                test("setting fixedCellHeight makes the visibleRowCount height rows × fixedCellHeight (regardless of the order of setting)") {
                    val heights = onUiThreadGet {
                        val rowsFirst = kind.create(listOf("A")).also {
                            it.rows = 5
                            it.cellHeight = 30
                        }
                        val heightFirst = kind.create(listOf("A")).also {
                            it.cellHeight = 30
                            it.rows = 5
                        }
                        val reset = kind.create(listOf("A")).also {
                            it.cellHeight = 30
                            it.rows = 5
                            it.cellHeight = -1
                        }
                        val extra = verticalPaddingAndBorder(rowsFirst)
                        listOf(rowsFirst.height - extra, heightFirst.height - extra, reset.height - extra)
                    }
                    heights shouldBe listOf(150.0, 150.0, 200.0) // The default estimated row height is 40
                }

                test("if the user specified a height, setting fixedCellHeight does not change the height") {
                    onUiThreadGet {
                        kind.create(listOf("A")).also {
                            it.rows = 5
                            it.height = 123.0
                            it.cellHeight = 30
                        }.height
                    } shouldBe 123.0
                }
            }
        }
    }
}
