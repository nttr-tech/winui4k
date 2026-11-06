package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tests WTabView's tab management, selection and properties, and updating WTabViewItem's header.
 * For the JTabbedPane-shaped API (addTab / insertTab / removeTabAt / indexOfTab and so on), the selection rules and ChangeListener,
 * the expected values are based on the behavior of Swing's JTabbedPane (the source of insertTab / removeTabAt / removeAll).
 */
class WTabViewTest : FunSpec() {
    /** Passes the IVector<Object> view of the native TabItems to [block] (so tests can manipulate the native order directly). */
    private fun <T> withNativeTabItems(tabView: WTabView, block: (ComPtr) -> T): T {
        val items = tabView.inspectable.getPtr(XamlInterop.ITabView_get_TabItems)
        val vector = items.queryInterface(FoundationInterop.IID_IVector_Object)
        items.release()
        return try {
            block(vector)
        } finally {
            vector.release()
        }
    }

    /** Moves the tab at [from] to [to] within the native TabItems, as when the user reorders tabs by dragging. */
    private fun moveNatively(tabView: WTabView, from: Int, to: Int) = withNativeTabItems(tabView) { vector ->
        val item = vector.getPtr(FoundationInterop.IVector_GetAt, from)
        try {
            vector.call(FoundationInterop.IVector_RemoveAt, from)
            vector.call(FoundationInterop.IVector_InsertAt, to, item.ptr)
        } finally {
            item.release()
        }
    }

    /** Places [tabView] in the shared window, waits for Loaded, calls [block] on the UI thread, and then removes it. */
    private fun <T> withShown(tabView: WTabView, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(tabView)
        try {
            onUiThread { tabView.updateLayout() } // Finish the internal ListView's Loaded (the replacement of TabItems)
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(tabView)
        }
    }

    /** The kind of [tab]'s IconSource ("none" / "symbol" / "font" / "image" / "other"). */
    private fun iconSourceKind(tab: WTabViewItem): String {
        val source = tab.inspectable.getPtrOrNull(XamlInterop.ITabViewItem_get_IconSource) ?: return "none"
        try {
            val kinds = listOf(
                "symbol" to XamlInterop.IID_ISymbolIconSource,
                "font" to XamlInterop.IID_IFontIconSource,
                "image" to XamlInterop.IID_IImageIconSource,
            )
            for ((kind, iid) in kinds) {
                val view = source.queryInterfaceOrNull(iid) ?: continue
                view.release()
                return kind
            }
            return "other"
        } finally {
            source.release()
        }
    }

    /**
     * Performs in order the operations that move the selection under Swing's rules, and returns selectedIndex and the selected tab's label after each operation.
     * In JTabbedPane: adding the first tab gives 0, inserting before the selection moves it back by one, removing before the selection moves it forward by one,
     * removing the selected last tab gives the previous tab, and removeAll gives -1.
     */
    private fun selectionSequence(tabView: WTabView): List<Pair<Int, String?>> {
        val seen = mutableListOf<Pair<Int, String?>>()
        fun record() {
            val index = tabView.selectedIndex
            seen += index to (if (index < 0) null else tabView.getTitleAt(index))
        }
        record()
        tabView.addTab("A", WLabel("a"))
        record()
        tabView.addTab("B", WLabel("b"))
        record()
        tabView.selectedIndex = 1
        record()
        tabView.insertTab("Z", null, WLabel("z"), null, 0)
        record()
        tabView.removeTabAt(0)
        record()
        tabView.removeTabAt(1)
        record()
        tabView.removeAll()
        record()
        return seen
    }

    private val expectedSelectionSequence = listOf(
        -1 to null,
        0 to "A",
        0 to "A",
        1 to "B",
        2 to "B",
        1 to "B",
        0 to "A",
        -1 to null,
    )

    init {
        test("tabs added via addTab can be retrieved from tabCount and getTab") {
            val (count, header) = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.addTab(WTabViewItem("Tab 2"))
                tabView.tabCount to tabView.getTab(1).header
            }
            count shouldBe 2
            header shouldBe "Tab 2"
        }

        test("header returns exactly the value that was set") {
            onUiThreadGet {
                val tab = WTabViewItem("old label")
                tab.header = "new label"
                tab.header
            } shouldBe "new label"
        }

        test("selectedIndex returns exactly the value that was set") {
            onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.selectedIndex = 2
                tabView.selectedIndex
            } shouldBe 2
        }

        test("removeTab decreases tabCount and shifts the later tabs down") {
            val (count, header) = onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.removeTab(1)
                tabView.tabCount to tabView.getTab(1).header
            }
            count shouldBe 2
            header shouldBe "Tab 2"
        }

        test("a tab added via addTab after Loaded is reflected in the display (the tab strip's width grows)") {
            // If adding to TabItems (the model) isn't reflected in rendering, SIZE_TO_CONTENT's width won't change
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                // Make the width content-dependent so a change in tab count can be observed via actualWidth
                tabView.horizontalAlignment = HorizontalAlignment.LEFT
                tabView.tabWidthMode = TabViewWidthMode.SIZE_TO_CONTENT
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                val (before, after) = onUiThreadGet {
                    tabView.updateLayout()
                    val before = tabView.actualWidth
                    tabView.addTab(WTabViewItem("Tab 2"))
                    tabView.updateLayout()
                    before to tabView.actualWidth
                }
                withClue("The tab strip's width after adding (before=$before after=$after)") {
                    (after > before).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
        }

        test("isClosable=false hides the close button (the tab's width shrinks)") {
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                // Make the width content-dependent so whether the close button is shown can be observed via actualWidth
                tabView.horizontalAlignment = HorizontalAlignment.LEFT
                tabView.tabWidthMode = TabViewWidthMode.SIZE_TO_CONTENT
                tabView.addTab(WTabViewItem("Tab 1"))
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                val (before, after) = onUiThreadGet {
                    tabView.updateLayout()
                    val before = tabView.actualWidth
                    tabView.getTab(0).isClosable = false
                    tabView.updateLayout()
                    before to tabView.actualWidth
                }
                withClue("The tab strip's width after hiding the close button (before=$before after=$after)") {
                    (after < before).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
        }

        test("the SelectionChanged listener fires on every change to selectedIndex") {
            // SelectionChanged only fires once the internal ListView's template has been applied,
            // so attach it to a shared window and wait for Loaded before switching the selection
            var count = 0
            val tabView = onUiThreadGet {
                val tabView = WTabView()
                repeat(3) { tabView.addTab(WTabViewItem("Tab $it")) }
                tabView.selectedIndex = 0
                tabView
            }
            UiTestHarness.attachAndAwaitLoaded(tabView)
            try {
                onUiThread {
                    tabView.addSelectionListener { count++ }
                    tabView.selectedIndex = 1
                    tabView.selectedIndex = 2
                }
            } finally {
                UiTestHarness.detach(tabView)
            }
            count shouldBe 2
        }

        test("isAddTabButtonVisible and canReorderTabs return exactly the values that were set") {
            val (addVisible, reorder) = onUiThreadGet {
                val tabView = WTabView()
                tabView.isAddTabButtonVisible = false
                tabView.canReorderTabs = false
                tabView.isAddTabButtonVisible to tabView.canReorderTabs
            }
            addVisible.shouldBeFalse()
            reorder.shouldBeFalse()
        }

        test("Swing-shaped addTab creates a tab with a label, icon, tooltip and content and without a close button") {
            val result = onUiThreadGet {
                val tabView = WTabView()
                val first = WLabel("first")
                val second = WLabel("second")
                tabView.addTab("A", first)
                tabView.addTab("B", Symbol.SAVE, second, "Save tab")
                tabView.addTab("C", WFontIcon("X"), null)
                listOf(
                    tabView.tabCount,
                    (0 until 3).map(tabView::getTitleAt),
                    (0 until 3).map(tabView::getIconAt),
                    (0 until 3).map(tabView::getToolTipTextAt),
                    tabView.getComponentAt(0) === first && tabView.getComponentAt(1) === second && tabView.getComponentAt(2) == null,
                    (0 until 3).map { tabView.getTab(it).isClosable },
                    (0 until 3).map { iconSourceKind(tabView.getTab(it)) },
                )
            }
            result shouldBe listOf(
                3,
                listOf("A", "B", "C"),
                listOf(null, Symbol.SAVE, WFontIcon("X")),
                listOf(null, "Save tab", null),
                true,
                listOf(false, false, false),
                listOf("none", "symbol", "font"),
            )
        }

        test("setIconAt sets an image icon and null removes it, and setTitleAt, setToolTipTextAt, setEnabledAt and setComponentAt can be read back") {
            val result = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab("A", WLabel("a"))
                val image = WImageIcon("icons/save.png")
                tabView.setIconAt(0, image)
                val kindWhileSet = iconSourceKind(tabView.getTab(0))
                val iconWhileSet = tabView.getIconAt(0)
                tabView.setIconAt(0, null)
                val kindAfterClear = iconSourceKind(tabView.getTab(0))
                tabView.setTitleAt(0, "New name")
                tabView.setToolTipTextAt(0, "Tip")
                tabView.setEnabledAt(0, false)
                val replacement = WLabel("replacement")
                tabView.setComponentAt(0, replacement)
                listOf(
                    kindWhileSet,
                    iconWhileSet == image,
                    kindAfterClear,
                    tabView.getTitleAt(0),
                    tabView.getToolTipTextAt(0),
                    tabView.isEnabledAt(0),
                    tabView.getComponentAt(0) === replacement,
                    tabView.selectedComponent === replacement,
                )
            }
            result shouldBe listOf("image", true, "none", "New name", "Tip", false, true, true)
        }

        test("insertTab inserts at the given position, and positions outside 0 to tabCount throw IndexOutOfBoundsException") {
            val (titles, errors) = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab("A", WLabel("a"))
                tabView.addTab("C", WLabel("c"))
                tabView.insertTab("B", null, WLabel("b"), null, 1)
                tabView.insertTab("D", null, WLabel("d"), null, 3) // The end (tabCount) can also be inserted at
                val errors = listOf(-1, 5).map { index ->
                    runCatching { tabView.insertTab("X", null, WLabel("x"), null, index) }.exceptionOrNull()?.javaClass
                }
                (0 until tabView.tabCount).map(tabView::getTitleAt) to errors
            }
            titles shouldBe listOf("A", "B", "C", "D")
            errors shouldBe listOf(IndexOutOfBoundsException::class.java, IndexOutOfBoundsException::class.java)
        }

        test("insertTab with a component that is already a tab's content removes the original tab and moves it (same as Swing)") {
            val (titles, index) = onUiThreadGet {
                val tabView = WTabView()
                val shared = WLabel("shared")
                tabView.addTab("A", shared)
                tabView.addTab("B", WLabel("b"))
                tabView.insertTab("C", null, shared, null, 2) // A has been removed, so it goes into 1
                (0 until tabView.tabCount).map(tabView::getTitleAt) to tabView.indexOfComponent(shared)
            }
            titles shouldBe listOf("B", "C")
            index shouldBe 1
        }

        test("adding a tab that is in another WTabView with addTab removes it from the original tab view and moves it (the same as Swing's add)") {
            onUiThread {
                val from = WTabView()
                val to = WTabView()
                val moved = WTabViewItem("moved")
                val stays = WTabViewItem("stays")
                from.addTab(moved)
                from.addTab(stays)
                to.addTab(moved)
                from.tabCount shouldBe 1
                from.getTab(0).header shouldBe "stays"
                from.selectedIndex shouldBe 0 // The selected first tab was removed, so the next tab at the same position is selected
                to.tabCount shouldBe 1
                to.getTab(0).header shouldBe "moved"
                // Moving it back to the original tab view removes it from where it moved to
                from.addTab(moved)
                to.tabCount shouldBe 0
                (0 until from.tabCount).map { from.getTab(it).header } shouldBe listOf("stays", "moved")
                // Adding a tab removed with removeTab to another tab view does not remove the other tabs of the original tab view
                from.removeTab(1)
                to.addTab(moved)
                from.tabCount shouldBe 1
                to.tabCount shouldBe 1
            }
        }

        test("indexOfTab and indexOfComponent return the first matching tab, or -1 if none") {
            val result = onUiThreadGet {
                val tabView = WTabView()
                val label = WLabel("b")
                tabView.addTab("A", WLabel("a"))
                tabView.addTab("B", Symbol.SAVE, label)
                tabView.addTab("A", WLabel("a2"))
                listOf(
                    tabView.indexOfTab("A"),
                    tabView.indexOfTab("Missing"),
                    tabView.indexOfTab(Symbol.SAVE),
                    tabView.indexOfTab(Symbol.DELETE),
                    tabView.indexOfComponent(label),
                    tabView.indexOfComponent(WLabel("other")),
                )
            }
            result shouldBe listOf(0, -1, 1, -1, 1, -1)
        }

        test("remove(component) removes by content or by the tab itself, and does nothing if it is neither") {
            val titles = onUiThreadGet {
                val tabView = WTabView()
                val content = WLabel("b")
                val tab = WTabViewItem("C")
                tabView.addTab("A", WLabel("a"))
                tabView.addTab("B", content)
                tabView.addTab(tab)
                tabView.remove(content)
                tabView.remove(tab)
                tabView.remove(WLabel("other"))
                tabView.remove(null)
                (0 until tabView.tabCount).map(tabView::getTitleAt)
            }
            titles shouldBe listOf("A")
        }

        test("out-of-range indexes throw IndexOutOfBoundsException for getTab, removeTabAt, getTitleAt and setting selectedIndex") {
            val errors = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab("A", WLabel("a"))
                listOf<() -> Unit>(
                    { tabView.getTab(1) },
                    { tabView.removeTabAt(-1) },
                    { tabView.getTitleAt(1) },
                    { tabView.selectedIndex = 1 },
                    { tabView.selectedIndex = -2 },
                ).map { runCatching(it).exceptionOrNull()?.javaClass }
            }
            errors shouldBe List(5) { IndexOutOfBoundsException::class.java }
        }

        test("setting selectedIndex to -1 deselects and makes selectedComponent null, and selecting something that is not a tab's content throws IllegalArgumentException") {
            val (index, component) = onUiThreadGet {
                val tabView = WTabView()
                tabView.addTab("A", WLabel("a"))
                tabView.selectedIndex = -1
                shouldThrow<IllegalArgumentException> { tabView.selectedComponent = WLabel("other") }
                tabView.selectedIndex to tabView.selectedComponent
            }
            index shouldBe -1
            component shouldBe null
        }

        test("even before being placed in the window, the selection moves by Swing's rules (-1 if empty, the first tab is selected, inserting or removing before shifts it)") {
            onUiThreadGet { selectionSequence(WTabView()) } shouldBe expectedSelectionSequence
        }

        test("after being placed in the window, the selection also moves by Swing's rules") {
            val tabView = onUiThreadGet { WTabView() }
            withShown(tabView) { selectionSequence(tabView) } shouldBe expectedSelectionSequence
        }

        test("WinUI's TabView does not select a tab added directly to an empty TabItems after loading (WTabView's addTab does)") {
            val tabView = onUiThreadGet { WTabView() }
            val (native, wrapper) = withShown(tabView) {
                val item = WTabViewItem("native")
                withNativeTabItems(tabView) { it.call(FoundationInterop.IVector_Append, item.inspectable.ptr) }
                val native = tabView.inspectable.getInt(XamlInterop.ITabView_get_SelectedIndex)
                withNativeTabItems(tabView) { it.call(FoundationInterop.IVector_Clear) }
                tabView.addTab("A", WLabel("a"))
                native to tabView.selectedIndex
            }
            native shouldBe -1
            wrapper shouldBe 0
        }

        listOf(false, true).forEach { shown ->
            test("ChangeListener is called once every time the selected index or the selected tab changes (after placement: $shown)") {
                val tabView = onUiThreadGet { WTabView() }
                val run = {
                    val notified = mutableListOf<Int>()
                    val sources = mutableListOf<Any>()
                    tabView.addChangeListener { e ->
                        sources += e.source
                        notified += (e.source as WTabView).selectedIndex
                    }
                    selectionSequence(tabView)
                    tabView.addTab("A", WLabel("a"))
                    tabView.addTab("B", WLabel("b"))
                    tabView.addTab("C", WLabel("c"))
                    tabView.selectedIndex = 1
                    tabView.selectedIndex = 1 // Not called because it does not change
                    tabView.removeTabAt(2) // Not called because it is after the selection
                    tabView.removeTabAt(1) // Removing the selected last tab selects the previous tab
                    notified to sources.all { it === tabView }
                }
                val (notified, sourceIsTabView) = if (shown) withShown(tabView, run) else onUiThreadGet(run)
                // For selectionSequence (0, 1, 2, 1, 0, -1), add 3 and the first tab (0), select 1 (1), remove B and A (0)
                notified shouldBe listOf(0, 1, 2, 1, 0, -1, 0, 1, 0)
                sourceIsTabView.shouldBeTrue()
            }
        }

        test("ChangeListener is also called when the selected tab is removed and the next tab at the same position is selected (same as Swing)") {
            val (count, title) = onUiThreadGet {
                val tabView = WTabView()
                listOf("A", "B", "C").forEach { tabView.addTab(it, WLabel(it)) }
                tabView.selectedIndex = 1
                var count = 0
                tabView.addChangeListener { count++ }
                tabView.removeTabAt(1)
                count to tabView.getTitleAt(tabView.selectedIndex)
            }
            count shouldBe 1
            title shouldBe "C"
        }

        test("ChangeListener is called when the user selects a tab (automation selection), and removeChangeListener removes it") {
            val tabView = onUiThreadGet {
                WTabView().also { tabView -> listOf("A", "B", "C").forEach { tabView.addTab(it, WLabel(it)) } }
            }
            val notified = withShown(tabView) {
                val notified = mutableListOf<Int>()
                val listener = WChangeListener { notified += tabView.selectedIndex }
                tabView.addChangeListener(listener)
                Xaml.invoke(tabView.getTab(2).inspectable)
                tabView.removeChangeListener(listener)
                Xaml.invoke(tabView.getTab(1).inspectable)
                notified + tabView.selectedIndex + tabView.getChangeListeners().size
            }
            notified shouldBe listOf(2, 1, 0)
        }

        test("after reordering by dragging, indexes refer to the native order, and the close button's index is the new position too") {
            val tabView = onUiThreadGet {
                WTabView().also { tabView -> listOf("A", "B", "C").forEach { tabView.addTab(WTabViewItem(it)) } }
            }
            val result = withShown(tabView) {
                val closeRequested = mutableListOf<Int>()
                tabView.addTabCloseRequestedListener { closeRequested += it }
                moveNatively(tabView, 0, 2)
                tabView.updateLayout() // Make it rebuild the containers of the moved elements
                val titles = (0 until tabView.tabCount).map { tabView.getTab(it).header }
                val closeButton = checkNotNull(Xaml.templatePartOrNull(tabView.getTab(2).inspectable, "CloseButton"))
                try {
                    Xaml.invoke(closeButton)
                } finally {
                    closeButton.release()
                }
                listOf(titles, tabView.indexOfTab("A"), closeRequested)
            }
            result shouldBe listOf(listOf("B", "C", "A"), 2, listOf(2))
        }

        context("Events") {
            test("pressing the \"+\" button calls addAddTabButtonClickListener listeners, and removed listeners are not called") {
                val tabView = onUiThreadGet { WTabView().also { it.addTab("A", WLabel("a")) } }
                val calls = withShown(tabView) {
                    val calls = mutableListOf<String>()
                    val first: () -> Unit = { calls += "first" }
                    val second: () -> Unit = { calls += "second" }
                    tabView.addAddTabButtonClickListener(first)
                    tabView.addAddTabButtonClickListener(second)
                    val addButton = checkNotNull(Xaml.templatePartOrNull(tabView.inspectable, "AddButton"))
                    try {
                        Xaml.invoke(addButton)
                        tabView.removeAddTabButtonClickListener(first)
                        Xaml.invoke(addButton)
                    } finally {
                        addButton.release()
                    }
                    calls
                }
                calls shouldBe listOf("first", "second", "second")
            }

            test("listeners removed with removeTabCloseRequestedListener and removeSelectionListener are not called") {
                val tabView = onUiThreadGet {
                    WTabView().also { tabView -> listOf("A", "B").forEach { tabView.addTab(WTabViewItem(it)) } }
                }
                val result = withShown(tabView) {
                    val closes = mutableListOf<Int>()
                    var selections = 0
                    val close: (Int) -> Unit = { closes += it }
                    val selection: () -> Unit = { selections++ }
                    tabView.addTabCloseRequestedListener(close)
                    tabView.addSelectionListener(selection)
                    tabView.removeTabCloseRequestedListener(close)
                    tabView.removeSelectionListener(selection)
                    tabView.selectedIndex = 1
                    val closeButton = checkNotNull(Xaml.templatePartOrNull(tabView.getTab(0).inspectable, "CloseButton"))
                    try {
                        Xaml.invoke(closeButton)
                    } finally {
                        closeButton.release()
                    }
                    listOf(closes, selections, tabView.tabCount)
                }
                result shouldBe listOf(emptyList<Int>(), 0, 2) // A close request only notifies the listeners and does not remove the tab
            }
        }

        context("Swing compatibility") {
            test("JTabbedPane's add forms: (title, c), (c, \"title\") and (c, icon) are addTab, (c) and (c, null) use the name as the title, and all return c") {
                val result = onUiThreadGet {
                    val tabView = WTabView()
                    val named = WLabel("named").also { it.name = "Name" }
                    val a = WLabel("a")
                    val b = WLabel("b")
                    val c = WLabel("c")
                    val unnamed = WLabel("unnamed")
                    val returned = listOf(tabView.add("A", a), tabView.add(named))
                    tabView.add(b, "B")
                    tabView.add(c, Symbol.SAVE)
                    tabView.add(unnamed, null as Any?)
                    listOf(
                        returned[0] === a && returned[1] === named,
                        (0 until tabView.tabCount).map(tabView::getTitleAt),
                        (0 until tabView.tabCount).map(tabView::getIconAt),
                        (0 until tabView.tabCount).map { tabView.getComponentAt(it)?.name },
                        (0 until tabView.tabCount).map { tabView.getTab(it).isClosable },
                    )
                }
                result shouldBe listOf(
                    true,
                    listOf("A", "Name", "B", "", ""),
                    listOf(null, null, null, Symbol.SAVE, null),
                    listOf("", "Name", "", "", ""),
                    listOf(false, false, false, false, false),
                )
            }

            test("add(c, index) and add(c, constraints, index) insert at index (-1 for the end), and out of range throws IndexOutOfBoundsException") {
                val result = onUiThreadGet {
                    val tabView = WTabView()
                    tabView.addTab("A", WLabel("a"))
                    tabView.add(WLabel("z").also { it.name = "Z" }, 0)
                    tabView.add(WLabel("e").also { it.name = "E" }, -1)
                    tabView.add(WLabel("m"), "M", 1)
                    tabView.add(WLabel("i"), Symbol.SAVE, -1)
                    val outOfRange = runCatching { tabView.add(WLabel("x"), 9) }.exceptionOrNull()
                    listOf(
                        (0 until tabView.tabCount).map(tabView::getTitleAt),
                        tabView.getIconAt(4),
                        outOfRange?.javaClass,
                        tabView.tabCount,
                    )
                }
                result shouldBe listOf(listOf("Z", "M", "A", "E", ""), Symbol.SAVE, IndexOutOfBoundsException::class.java, 5)
            }

            test("setMnemonicAt sets a tab's access key with the same values as KeyEvent's VK, and 0 removes it. Non-letters and non-digits and an out-of-range index throw") {
                val result = onUiThreadGet {
                    val tabView = WTabView()
                    tabView.addTab("File", WLabel("f"))
                    tabView.addTab("Edit", WLabel("e"))
                    val before = tabView.getMnemonicAt(0)
                    tabView.setMnemonicAt(0, 'F'.code)
                    tabView.setMnemonicAt(1, '2'.code)
                    val set = listOf(tabView.getMnemonicAt(0), tabView.getMnemonicAt(1)) +
                        (0..1).map { tabView.getTab(it).uiElement.getString(XamlInterop.IUIElement_get_AccessKey) }
                    tabView.setMnemonicAt(0, 0)
                    val cleared = tabView.getTab(0).uiElement.getString(XamlInterop.IUIElement_get_AccessKey)
                    val invalid = runCatching { tabView.setMnemonicAt(1, '!'.code) }.exceptionOrNull()?.javaClass
                    val outOfRange = runCatching { tabView.setMnemonicAt(2, 'A'.code) }.exceptionOrNull()?.javaClass
                    listOf(before, set, cleared, tabView.getMnemonicAt(1), invalid, outOfRange)
                }
                result shouldBe listOf(
                    0,
                    listOf('F'.code, '2'.code, "F", "2"),
                    "",
                    '2'.code,
                    IllegalArgumentException::class.java,
                    IndexOutOfBoundsException::class.java,
                )
            }

            test("add(WTabViewItem) adds that tab itself (the same as addTab), and add of a component that is already the content of a tab moves it from the original tab") {
                val result = onUiThreadGet {
                    val tabView = WTabView()
                    val item = WTabViewItem("Item")
                    val content = WLabel("Content")
                    tabView.add(item)
                    tabView.add("Before", content)
                    tabView.add("After", content)
                    listOf(tabView.tabCount, tabView.getTab(0) === item, tabView.getTitleAt(1), tabView.getComponentAt(1) === content)
                }
                result shouldBe listOf(2, true, "After", true)
            }
        }
    }
}
