package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that check keys put in the `getInputMap()` (WHEN_FOCUSED) of a [WList] also work when an item has focus.
 * Swing's JList has focus itself, so the Delete key works with `list.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), ...)`.
 * WinUI's ListView moves focus to the item (ListViewItem), so keys on items are also looked up in the list's WHEN_FOCUSED.
 */
@Tags("E2E")
class WListKeyBindingTest : FunSpec() {
    private class Form {
        val root = WPanel()
        val model = WDefaultListModel<String>().apply { listOf("Apple", "Orange", "Grape").forEach(::addElement) }
        val list = WList(model)
        val other = WButton("Outside")
        val events = mutableListOf<String>()

        init {
            root.add(list)
            root.add(other)
        }

        fun bind(condition: Int) {
            list.getInputMap(condition).put(WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, 0), "delete")
            list.actionMap.put(
                "delete",
                object : WAbstractAction() {
                    override fun actionPerformed(e: WActionEvent) {
                        events += "delete:${list.selectedIndex}:${e.source === list}"
                        val index = list.selectedIndex
                        if (index >= 0) model.remove(index)
                    }
                },
            )
        }
    }

    private suspend fun withForm(block: suspend (Form) -> Unit) {
        val form = onUiThreadGet { Form() }
        UiTestHarness.attachAndAwaitLoaded(form.root)
        try {
            block(form)
        } finally {
            UiTestHarness.detach(form.root)
        }
    }

    /** Selects the first item, moves focus to the list, and checks that focus is on an item (SelectorItem). */
    private suspend fun selectAndFocus(form: Form) {
        onUiThread {
            form.events.clear()
            form.list.selectedIndex = 1
            check(form.list.requestFocusInWindow()) { "cannot move focus" }
        }
        eventually(3.seconds) { onUiThreadGet { isItemFocused(form.list) } shouldBe true }
    }

    /** Whether the element that has focus now is an item container (SelectorItem) rather than the list itself. */
    private fun isItemFocused(list: WList): Boolean {
        val root = list.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        val focused = try {
            Xaml.focusedElement(root)
        } finally {
            root.release()
        } ?: return false
        return try {
            val item = focused.queryInterfaceOrNull(XamlInterop.IID_ISelectorItem)
            item?.release()
            item != null
        } finally {
            focused.release()
        }
    }

    private suspend fun pressDelete(form: Form, expected: List<String>) {
        TestKeyboard.press(VK_DELETE, SCAN_DELETE)
        eventually(3.seconds) { onUiThreadGet { form.events.toList() } shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        onUiThreadGet { form.events.toList() } shouldContainExactly expected
    }

    init {
        test("Delete in getInputMap() (WHEN_FOCUSED) works on a list whose item has focus, and source is the list") {
            withForm { form ->
                onUiThread { form.bind(WComponent.WHEN_FOCUSED) }
                selectAndFocus(form)
                pressDelete(form, listOf("delete:1:true"))
                onUiThreadGet { form.model.size } shouldBe 2
            }
        }

        test("Delete in WHEN_FOCUSED does not work when focus is outside the list") {
            withForm { form ->
                onUiThread {
                    form.bind(WComponent.WHEN_FOCUSED)
                    form.list.selectedIndex = 1
                    form.events.clear()
                    check(form.other.requestFocusInWindow()) { "cannot move focus" }
                }
                pressDelete(form, emptyList())
            }
        }

        test("Delete in WHEN_ANCESTOR_OF_FOCUSED_COMPONENT also still works on a list whose item has focus") {
            withForm { form ->
                onUiThread { form.bind(WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) }
                selectAndFocus(form)
                pressDelete(form, listOf("delete:1:true"))
            }
        }
    }

    private companion object {
        const val VK_DELETE = 0x2E
        const val SCAN_DELETE = 0x53
        const val SETTLE_MILLIS = 500L
    }
}
