package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the cellRenderer ([WListCellRenderer]) of WList (ListView) and WListBox (ListBox) and [WDefaultListCellRenderer]
 * by actually showing them. Expected values come from how Swing's JList calls a ListCellRenderer (per shown element, with the selection state).
 */
class ListCellRendererTest : FunSpec() {
    /** One call of the renderer. */
    private data class Call(val value: Any?, val index: Int, val isSelected: Boolean)

    /** A renderer that records calls and draws elements with the text "<value>". */
    private class RecordingRenderer : WListCellRenderer<Any?> {
        val calls = ArrayList<Call>()
        val lists = ArrayList<WComponent>()

        override fun getListCellRendererComponent(list: WComponent, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): WComponent {
            calls += Call(value, index, isSelected)
            lists += list
            return WLabel("<$value>")
        }
    }

    private class Kind(val name: String, val create: (Array<out Any?>) -> WControl)

    private val kinds = listOf(
        Kind("WList") { WList(it) },
        Kind("WListBox") { WListBox(it) },
    )

    private var WControl.renderer: WListCellRenderer<Any?>?
        get() = if (this is WList) cellRenderer else (this as WListBox).cellRenderer
        set(value) = if (this is WList) cellRenderer = value else (this as WListBox).cellRenderer = value

    private var WControl.selected: Int
        get() = if (this is WList) selectedIndex else (this as WListBox).selectedIndex
        set(value) = if (this is WList) selectedIndex = value else (this as WListBox).selectedIndex = value

    @Suppress("UNCHECKED_CAST")
    private val WControl.listModel: WDefaultListModel<Any?>
        get() = (if (this is WList) model else (this as WListBox).model) as WDefaultListModel<Any?>

    private fun hasText(list: WControl, text: String): Boolean = tableTreeFindText(list.dependencyObject, text)?.also { it.release() } != null

    /** The AutomationProperties.Name of the container of the item at [index]. */
    private fun automationNameOf(list: WControl, index: Int): String {
        val mapping = list.inspectable.queryInterface(XamlInterop.IID_IItemContainerMapping)
        val container = try {
            checkNotNull(mapping.getPtrOrNull(XamlInterop.IItemContainerMapping_ContainerFromIndex, index)) { "no container: $index" }
        } finally {
            mapping.release()
        }
        return try {
            Xaml.getAutomation(container, XamlInterop.IAutomationPropertiesStatics_GetName)
        } finally {
            container.release()
        }
    }

    /**
     * Waits (up to 5 seconds) until all texts [texts] appear in the shown [list]. ListView first shows items as placeholders,
     * and the content becomes visible in later passes (the ContainerContentChanging phase processing), so one UpdateLayout is not enough.
     */
    private fun awaitTexts(list: WControl, texts: List<String>): Boolean {
        repeat(100) {
            if (onUiThreadGet { list.updateLayout().let { _ -> texts.all { text -> hasText(list, text) } } }) return true
            Thread.sleep(50)
        }
        return false
    }

    /** Shows [list] (waiting until the [waitFor] texts appear), runs [block] on the UI thread, and then removes it. */
    private fun <T> shown(list: WControl, waitFor: List<String> = emptyList(), block: (WControl) -> T): T {
        UiTestHarness.attachAndAwaitLoaded(list)
        try {
            check(awaitTexts(list, waitFor)) { "texts not shown: $waitFor" }
            return onUiThreadGet {
                list.updateLayout()
                block(list)
            }
        } finally {
            UiTestHarness.detach(list)
        }
    }

    /**
     * Runs [step] in order while advancing layout one pass at a time on the UI thread, and returns the result of each step.
     * Before a [waitFor] step, waits until that text appears.
     */
    private fun <T> steps(list: WControl, vararg step: (WControl) -> T, waitFor: Map<Int, List<String>> = emptyMap()): List<T> {
        UiTestHarness.attachAndAwaitLoaded(list)
        try {
            return step.mapIndexed { index, action ->
                waitFor[index]?.let { texts -> check(awaitTexts(list, texts)) { "texts not shown: $texts" } }
                onUiThreadGet { list.updateLayout().let { _ -> action(list) } }
            }
        } finally {
            UiTestHarness.detach(list)
        }
    }

    init {
        for (kind in kinds) {
            test("${kind.name}: cellRenderer defaults to null, and when set each element is drawn by the renderer's component and the name is the element's string") {
                val renderer = RecordingRenderer()
                val list = onUiThreadGet {
                    kind.create(arrayOf("a", "b", null)).also {
                        check(it.renderer == null)
                        it.renderer = renderer
                    }
                }
                shown(list, listOf("<a>", "<b>", "<null>")) {
                    listOf(
                        renderer.calls.filter { !it.isSelected }.map { it.index to it.value }.distinct().sortedBy { it.first },
                        renderer.lists.all { it === list },
                        hasText(list, "<a>") && hasText(list, "<b>") && hasText(list, "<null>"),
                        hasText(list, "a"),
                        listOf(automationNameOf(list, 0), automationNameOf(list, 1), automationNameOf(list, 2)),
                    )
                } shouldBe listOf(listOf(0 to "a", 1 to "b", 2 to null), true, true, false, listOf("a", "b", ""))
            }

            test("${kind.name}: when the selection changes, only the selected and deselected elements are redrawn with their selection state") {
                val renderer = RecordingRenderer()
                val list = onUiThreadGet { kind.create(arrayOf("a", "b", "c")).also { it.renderer = renderer } }
                steps(
                    list,
                    {
                        renderer.calls.clear()
                        it.selected = 1
                    },
                    { renderer.calls.toList().also { _ -> renderer.calls.clear() } },
                    { it.selected = 2 },
                    { renderer.calls.toList() },
                ).let { listOf(it[1], it[3]) } shouldBe listOf(
                    listOf(Call("b", 1, true)),
                    listOf(Call("b", 1, false), Call("c", 2, true)),
                )
            }

            test("${kind.name}: replacing an element redraws it, and the selection stays at its position") {
                val renderer = RecordingRenderer()
                val list = onUiThreadGet { kind.create(arrayOf("a", "b", "c")).also { it.renderer = renderer } }
                steps(
                    list,
                    { it.selected = 1 },
                    {
                        renderer.calls.clear()
                        it.listModel.set(1, "z")
                    },
                    { listOf(renderer.calls.any { call -> call.value == "z" && call.index == 1 }, hasText(it, "<z>"), it.selected) },
                    waitFor = mapOf(2 to listOf("<z>")),
                )[2] shouldBe listOf(true, true, 1)
            }

            test("${kind.name}: setting cellRenderer back to null returns to showing element strings, and the selection stays") {
                val list = onUiThreadGet { kind.create(arrayOf("a", "b")).also { it.renderer = RecordingRenderer() } }
                steps(
                    list,
                    { it.selected = 1 },
                    { it.renderer = null },
                    { listOf(hasText(it, "<a>"), hasText(it, "a"), hasText(it, "b"), it.selected, it.renderer) },
                    waitFor = mapOf(2 to listOf("a", "b")),
                )[2] shouldBe listOf(false, true, true, 1, null)
            }
        }

        test("WList: a renderer called from ContainerContentChanging (during layout) throws IllegalStateException when it tries to wait for a dialog to close") {
            val outcomes = ArrayList<String>()
            val renderer = object : WListCellRenderer<Any?> {
                override fun getListCellRendererComponent(list: WComponent, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): WComponent {
                    outcomes += runCatching { ModalLoop.run { true } }.fold({ "ran" }, { it.javaClass.simpleName })
                    return WLabel("<$value>")
                }
            }
            val list = onUiThreadGet { WList(arrayOf<Any?>("a", "b")).also { it.cellRenderer = renderer } }
            shown(list, listOf("<a>", "<b>")) { outcomes.isNotEmpty() to outcomes.distinct() } shouldBe (true to listOf("IllegalStateException"))
        }

        test("WListBox: even with a renderer, selectedItems and selectedValue stay the element strings and elements") {
            val list = onUiThreadGet { WListBox(arrayOf<Any?>("a", 2, "c")).also { it.cellRenderer = RecordingRenderer() } }
            shown(list) {
                it.selected = 1
                val box = it as WListBox
                listOf(box.selectedItems, box.selectedValue, box.selectedItem, box.itemCount)
            } shouldBe listOf(listOf("2"), 2, "2", 3)
        }

        test("WDefaultListCellRenderer creates a label with the toString text, null becomes an empty string, and a WIcon element becomes only an icon") {
            onUiThreadGet {
                val list = WList()
                val renderer = WDefaultListCellRenderer()
                val icon: WIcon = Symbol.ACCEPT
                val text = renderer.getListCellRendererComponent(list, 42, 0, false, false) as WLabel
                val empty = renderer.getListCellRendererComponent(list, null, 1, false, false) as WLabel
                val iconOnly = renderer.getListCellRendererComponent(list, icon, 2, false, false) as WLabel
                // A new label on each call
                val again = renderer.getListCellRendererComponent(list, 42, 0, false, false)
                listOf(text.text, text.icon, empty.text, iconOnly.text, iconOnly.icon === icon, again !== text)
            } shouldBe listOf("42", null, "", "", true, true)
        }

        test("overriding getText and configure of WDefaultListCellRenderer changes the text and decoration") {
            val list = onUiThreadGet {
                WList(arrayOf("a", "b")).also { list ->
                    list.cellRenderer = object : WDefaultListCellRenderer() {
                        override fun getText(value: Any?): String = "[$value]"

                        override fun configure(label: WLabel, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                            label.accessibleDescription = "row $index"
                        }
                    }
                }
            }
            shown(list, listOf("[a]", "[b]")) { listOf(hasText(it, "[a]"), hasText(it, "[b]"), hasText(it, "a")) } shouldBe listOf(true, true, false)
        }
    }
}
