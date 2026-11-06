package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that adding and removing items in the menu containers (WMenuBar / WMenuBarItem / WMenuFlyoutSubItem / WMenuFlyout) gives the same return values,
 * positions and exceptions as Swing's JMenuBar / JMenu / JPopupMenu, and that the native Items are in the same order.
 * The shape seen from Java is tested in WMenuContainerForJavaTest.
 */
@Tags("Integration")
class WMenuContainerTest : FunSpec() {
    /** Represents the contents of a native IVector<MenuFlyoutItemBase> as "text" / "sub:text" / "-" (separator). */
    private fun nativeLabels(vector: ComPtr): List<String> =
        (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
            val element = vector.getPtr(FoundationInterop.IVector_GetAt, index)
            try {
                describe(element)
            } finally {
                element.release()
            }
        }

    private fun describe(element: ComPtr): String {
        element.queryInterfaceOrNull(XamlInterop.IID_IMenuFlyoutItem)?.let { item ->
            return try {
                item.getString(XamlInterop.IMenuFlyoutItem_get_Text)
            } finally {
                item.release()
            }
        }
        element.queryInterfaceOrNull(XamlInterop.IID_IMenuFlyoutSubItem)?.let { sub ->
            return try {
                "sub:" + sub.getString(XamlInterop.IMenuFlyoutSubItem_get_Text)
            } finally {
                sub.release()
            }
        }
        return "-"
    }

    private fun <T> withVector(owner: ComPtr, slot: Int, block: (ComPtr) -> T): T {
        val vector = owner.getPtr(slot)
        return try {
            block(vector)
        } finally {
            vector.release()
        }
    }

    private fun nativeLabels(menu: WMenuFlyout): List<String> =
        withVector(menu.inspectable, XamlInterop.IMenuFlyout_get_Items) { nativeLabels(it) }

    private fun nativeLabels(menu: WMenuBarItem): List<String> =
        withVector(menu.inspectable, XamlInterop.IMenuBarItem_get_Items) { nativeLabels(it) }

    private fun nativeLabels(menu: WMenuFlyoutSubItem): List<String> =
        withVector(menu.inspectable, XamlInterop.IMenuFlyoutSubItem_get_Items) { nativeLabels(it) }

    private fun nativeTitles(bar: WMenuBar): List<String> = withVector(bar.inspectable, XamlInterop.IMenuBar_get_Items) { vector ->
        (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
            val element = vector.getPtr(FoundationInterop.IVector_GetAt, index)
            try {
                element.getString(XamlInterop.IMenuBarItem_get_Title)
            } finally {
                element.release()
            }
        }
    }

    /** Represents the Kotlin-side list in the same form as nativeLabels. */
    private fun labels(items: Array<WMenuFlyoutItemBase>): List<String> = items.map {
        when (it) {
            is WMenuFlyoutItem -> it.text
            is WMenuFlyoutSubItem -> "sub:" + it.text
            else -> "-"
        }
    }

    /** The items of the open menu (the ContentButton's ContextFlyout) that MenuBarItem creates when its template is applied. null if not yet. */
    private fun openedMenuLabels(item: WMenuBarItem): List<String>? {
        val button = Xaml.templatePartOrNull(item.inspectable, "ContentButton") ?: return null
        val element = try {
            button.queryInterface(XamlInterop.IID_IUIElement)
        } finally {
            button.release()
        }
        val flyout = try {
            element.getPtrOrNull(XamlInterop.IUIElement_get_ContextFlyout) ?: return null
        } finally {
            element.release()
        }
        val menu = try {
            flyout.queryInterface(XamlInterop.IID_IMenuFlyout)
        } finally {
            flyout.release()
        }
        return try {
            withVector(menu, XamlInterop.IMenuFlyout_get_Items) { nativeLabels(it) }
        } finally {
            menu.release()
        }
    }

    private class NamedAction(name: String) : WAbstractAction(name) {
        override fun actionPerformed(e: WActionEvent) = Unit
    }

    init {
        test("WMenuFlyout's add returns the passed item, creates new items from text and actions, and can add separators") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                val open = WMenuFlyoutItem("Open")
                val returned = menu.add(open)
                val save = menu.add("Save")
                menu.addSeparator()
                val exit = menu.add(NamedAction("Exit"))
                val sub = menu.add(WMenuFlyoutSubItem("Recent files"))
                listOf(
                    returned === open,
                    save.text,
                    exit.text,
                    exit.action?.getValue(WAction.NAME),
                    sub.text,
                    menu.itemCount,
                    menu.menuComponentCount,
                    menu.componentCount,
                    labels(menu.menuComponents),
                    nativeLabels(menu),
                )
            } shouldContainExactly listOf(
                true, "Save", "Exit", "Exit", "Recent files", 5, 5, 5,
                listOf("Open", "Save", "-", "Exit", "sub:Recent files"),
                listOf("Open", "Save", "-", "Exit", "sub:Recent files"),
            )
        }

        test("getItem returns null for separators and submenus as is, and getMenuComponent and getComponent also return separators") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                val open = menu.add("Open")
                menu.addSeparator()
                val sub = menu.add(WMenuFlyoutSubItem("Sub"))
                listOf(
                    menu.getItem(0) === open,
                    menu.getItem(1),
                    menu.getItem(2) === sub,
                    menu.getMenuComponent(1) is WMenuFlyoutSeparator,
                    menu.getComponent(1) is WMenuFlyoutSeparator,
                    menu.getComponentIndex(sub),
                    menu.getComponentIndex(WMenuFlyoutItem("Outside")),
                    menu.getComponentIndex(null),
                )
            } shouldContainExactly listOf(true, null, true, true, true, 2, -1, -1)
        }

        test("a negative getItem position throws IllegalArgumentException, and at or beyond the number of items ArrayIndexOutOfBoundsException (as in Swing)") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                menu.add("Open")
                listOf(
                    runCatching { menu.getItem(-1) }.exceptionOrNull()?.javaClass,
                    runCatching { menu.getItem(1) }.exceptionOrNull()?.javaClass,
                    runCatching { menu.getMenuComponent(1) }.exceptionOrNull()?.javaClass,
                )
            } shouldContainExactly listOf(
                IllegalArgumentException::class.java,
                ArrayIndexOutOfBoundsException::class.java,
                ArrayIndexOutOfBoundsException::class.java,
            )
        }

        test("insert inserts at the position and returns the item, and at the end for positions at or beyond the number of items") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                menu.add("B")
                val a = WMenuFlyoutItem("A")
                val returned = menu.insert(a, 0)
                val d = menu.insert("D", 10)
                val c = menu.insert(NamedAction("C"), 2)
                menu.insertSeparator(1)
                listOf(returned === a, d.text, c.text, labels(menu.menuComponents), nativeLabels(menu))
            } shouldContainExactly listOf(
                true,
                "D",
                "C",
                listOf("A", "-", "B", "C", "D"),
                listOf("A", "-", "B", "C", "D"),
            )
        }

        test("negative positions for insert and insertSeparator throw IllegalArgumentException, and the items do not change") {
            val (errors, labels) = onUiThreadGet {
                val menu = WMenuFlyout()
                menu.add("A")
                val errors = listOf(
                    runCatching { menu.insert(WMenuFlyoutItem("X"), -1) },
                    runCatching { menu.insert("X", -1) },
                    runCatching { menu.insert(NamedAction("X"), -1) },
                    runCatching { menu.insertSeparator(-1) },
                ).map { it.exceptionOrNull()?.javaClass }
                errors to nativeLabels(menu)
            }
            errors shouldContainExactly List(4) { IllegalArgumentException::class.java }
            labels shouldContainExactly listOf("A")
        }

        test("remove removes by item or by position, and does nothing for items of other menus") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                val a = menu.add("A")
                menu.add("B")
                menu.addSeparator()
                menu.add("C")
                menu.remove(a)
                menu.remove(1) // The separator
                menu.remove(WMenuFlyoutItem("Outside"))
                listOf(labels(menu.menuComponents), nativeLabels(menu), a.menuOwner)
            } shouldContainExactly listOf(listOf("B", "C"), listOf("B", "C"), null)
        }

        test("negative positions and positions at or beyond the number of items for remove throw IllegalArgumentException (same as JPopupMenu.remove(int))") {
            val (errors, labels) = onUiThreadGet {
                val menu = WMenuFlyout()
                menu.add("A")
                listOf(-1, 1).map { index -> runCatching { menu.remove(index) }.exceptionOrNull()?.javaClass } to nativeLabels(menu)
            }
            errors shouldContainExactly List(2) { IllegalArgumentException::class.java }
            labels shouldContainExactly listOf("A")
        }

        test("removeAll empties both native and Kotlin, and removed items can be put into other menus") {
            onUiThreadGet {
                val menu = WMenuFlyout()
                val a = menu.add("A")
                menu.addSeparator()
                menu.removeAll()
                val other = WMenuFlyout()
                other.add(a)
                listOf(menu.itemCount, nativeLabels(menu), nativeLabels(other))
            } shouldContainExactly listOf(0, emptyList<String>(), listOf("A"))
        }

        test("adding an item that is in another menu moves it from there, and adding it again to the same menu moves it to the end (same as Container.add)") {
            onUiThreadGet {
                val first = WMenuFlyout()
                val second = WMenuBarItem("Edit")
                val a = first.add("A")
                first.add("B")
                second.add("X")
                second.add(a)
                first.add("C")
                val b = first.getItem(0)!!
                first.add(b)
                listOf(nativeLabels(first), labels(first.menuComponents), nativeLabels(second), labels(second.menuComponents))
            } shouldContainExactly listOf(listOf("C", "B"), listOf("C", "B"), listOf("X", "A"), listOf("X", "A"))
        }

        test("WMenuBarItem and WMenuFlyoutSubItem can also add and remove items with the same API") {
            onUiThreadGet {
                val file = WMenuBarItem("File")
                val recent = file.add(WMenuFlyoutSubItem("Recent files"))
                file.insert("Open", 0)
                file.insertSeparator(1)
                file.add(NamedAction("Exit"))
                recent.add("a.txt")
                recent.add("b.txt")
                recent.insert(WMenuFlyoutItem("new.txt"), 0)
                recent.remove(2)
                recent.addSeparator()
                recent.insertSeparator(0)
                recent.remove(0)
                listOf(
                    nativeLabels(file),
                    labels(file.menuComponents),
                    file.getItem(1),
                    file.getItem(2) === recent,
                    file.itemCount,
                    nativeLabels(recent),
                    labels(recent.menuComponents),
                    recent.menuComponentCount,
                    recent.getMenuComponent(2) is WMenuFlyoutSeparator,
                )
            } shouldContainExactly listOf(
                listOf("Open", "-", "sub:Recent files", "Exit"),
                listOf("Open", "-", "sub:Recent files", "Exit"),
                null, true, 4,
                listOf("new.txt", "a.txt", "-"),
                listOf("new.txt", "a.txt", "-"),
                3, true,
            )
        }

        test("removeAll of WMenuBarItem and WMenuFlyoutSubItem and the exceptions for wrong positions") {
            onUiThreadGet {
                val file = WMenuBarItem("File")
                val sub = WMenuFlyoutSubItem("Sub")
                file.add("A")
                sub.add("B")
                val errors = listOf(
                    runCatching { file.remove(1) },
                    runCatching { sub.remove(-1) },
                    runCatching { sub.insert("X", -1) },
                    runCatching { file.getItem(-1) },
                    runCatching { sub.getItem(1) },
                ).map { it.exceptionOrNull()?.javaClass }
                file.removeAll()
                sub.removeAll()
                listOf(errors, nativeLabels(file), nativeLabels(sub), file.itemCount, sub.itemCount)
            } shouldContainExactly listOf(
                listOf(
                    IllegalArgumentException::class.java,
                    IllegalArgumentException::class.java,
                    IllegalArgumentException::class.java,
                    IllegalArgumentException::class.java,
                    ArrayIndexOutOfBoundsException::class.java,
                ),
                emptyList<String>(),
                emptyList<String>(),
                0,
                0,
            )
        }

        test("WMenuBar's add returns the menu, and getMenu / menuCount / getComponentIndex / remove / removeAll agree with native") {
            onUiThreadGet {
                val bar = WMenuBar()
                val file = WMenuBarItem("File")
                val returned = bar.add(file)
                val edit = bar.add(WMenuBarItem("Edit"))
                bar.add(WMenuBarItem("View"))
                val afterAdd = nativeTitles(bar)
                val state = listOf(returned === file, bar.getMenu(1) === edit, bar.menuCount, bar.getComponentIndex(edit), bar.getComponentIndex(null))
                bar.remove(edit)
                bar.remove(WMenuBarItem("Outside"))
                val afterRemove = nativeTitles(bar)
                bar.remove(0)
                val afterRemoveAt = nativeTitles(bar) + bar.menuCount.toString()
                bar.removeAll()
                listOf(afterAdd, state, afterRemove, afterRemoveAt, nativeTitles(bar), bar.menuCount)
            } shouldContainExactly listOf(
                listOf("File", "Edit", "View"),
                listOf(true, true, 3, 1, -1),
                listOf("File", "View"),
                listOf("View", "1"),
                emptyList<String>(),
                0,
            )
        }

        test("wrong positions in WMenuBar throw ArrayIndexOutOfBoundsException (same as Container), and menus move to other bars") {
            onUiThreadGet {
                val first = WMenuBar()
                val second = WMenuBar()
                val file = first.add(WMenuBarItem("File"))
                second.add(file)
                listOf(
                    runCatching { first.getMenu(0) }.exceptionOrNull()?.javaClass,
                    runCatching { first.remove(0) }.exceptionOrNull()?.javaClass,
                    runCatching { second.remove(-1) }.exceptionOrNull()?.javaClass,
                    nativeTitles(first),
                    nativeTitles(second),
                )
            } shouldContainExactly listOf(
                ArrayIndexOutOfBoundsException::class.java,
                ArrayIndexOutOfBoundsException::class.java,
                ArrayIndexOutOfBoundsException::class.java,
                emptyList<String>(),
                listOf("File"),
            )
        }

        test("adding and removing (including removeAll) in a WMenuBarItem after it is put in a window reach the open menu (the ContentButton's ContextFlyout)") {
            val panel = onUiThreadGet { WPanel() }
            val file = onUiThreadGet {
                val bar = WMenuBar().also { panel.add(it) }
                bar.add(WMenuBarItem("File")).also { it.add("Open") }
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                eventually(5.seconds) { onUiThreadGet { openedMenuLabels(file) } shouldBe listOf("Open") }
                onUiThreadGet {
                    val save = file.add("Save")
                    file.insert("New", 0)
                    file.addSeparator()
                    val afterAdd = openedMenuLabels(file)
                    file.remove(save)
                    val afterRemove = openedMenuLabels(file)
                    file.removeAll()
                    listOf(afterAdd, afterRemove, openedMenuLabels(file))
                } shouldContainExactly listOf(listOf("New", "Open", "Save", "-"), listOf("New", "Open", "-"), emptyList<String>())
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }
}
