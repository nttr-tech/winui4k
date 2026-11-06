package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.attachAndAwaitLoaded
import com.appkitbox.winui4k.UiTestHarness.detach
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests for the standalone behavior of WItemContainer's child (ItemContainer.Child) and isSelected. Integration with WItemsView is in WItemsViewTest. */
class WItemContainerTest : FunSpec() {
    /** Passes [ptr] to [block] and Releases it afterwards. */
    private fun <T> useReleasing(ptr: ComPtr, block: (ComPtr) -> T): T {
        try {
            return block(ptr)
        } finally {
            ptr.release()
        }
    }

    /**
     * Returns the string if the native ItemContainer.Child is a WLabel element (the first child of the Grid is a TextBlock), "null" if null,
     * otherwise "other" (call on the UI thread).
     */
    private fun nativeChildText(container: WItemContainer): String {
        val child = container.inspectable.getPtrOrNull(XamlInterop.IItemContainer_get_Child) ?: return "null"
        return useReleasing(child) {
            val panel = it.queryInterfaceOrNull(XamlInterop.IID_IPanel) ?: return@useReleasing "other"
            useReleasing(panel) { p ->
                useReleasing(p.getPtr(XamlInterop.IPanel_get_Children)) { children ->
                    useReleasing(children.getPtr(FoundationInterop.IVector_GetAt, 0)) { first ->
                        val textBlock = first.queryInterfaceOrNull(XamlInterop.IID_ITextBlock) ?: return@useReleasing "other"
                        useReleasing(textBlock) { t -> t.getString(XamlInterop.ITextBlock_get_Text) }
                    }
                }
            }
        }
    }

    init {
        test("created with no arguments, child is null, the native Child is empty and isSelected is false") {
            onUiThreadGet {
                val container = WItemContainer()
                Triple(container.child, nativeChildText(container), container.isSelected)
            } shouldBe Triple(null, "null", false)
        }

        test("the child passed to the constructor can be read as child and is also in the native Child") {
            onUiThreadGet {
                val label = WLabel("Content")
                val container = WItemContainer(label)
                (container.child === label) to nativeChildText(container)
            } shouldBe (true to "Content")
        }

        test("replacing child with another component also replaces the native Child, and null empties it") {
            onUiThreadGet {
                val container = WItemContainer(WLabel("Before"))
                val first = nativeChildText(container)
                container.child = WLabel("After")
                val second = nativeChildText(container)
                container.child = null
                listOf(first, second, nativeChildText(container), container.child.toString())
            } shouldBe listOf("Before", "After", "null", "null")
        }

        test("even a standalone ItemContainer not in a WItemsView can have isSelected assigned and read back (from true to false too)") {
            val container = onUiThreadGet { WItemContainer(WLabel("Standalone")) }
            attachAndAwaitLoaded(container)
            try {
                onUiThreadGet {
                    container.isSelected = true
                    val selected = container.isSelected
                    container.isSelected = false
                    selected to container.isSelected
                } shouldBe (true to false)
            } finally {
                detach(container)
            }
        }
    }
}
