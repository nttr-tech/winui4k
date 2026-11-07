package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** Tests verifying WComboBox's item management, selection, and SelectionChanged listener. */
@Tags("E2E")
class WComboBoxTest : FunSpec() {
    /** The actual heights of the first item (ComboBoxItem) laid out in the open drop-down and of the drop-down border (PopupBorder, the child of the template's Popup). */
    private fun dropDownHeights(comboBox: WComboBox): Pair<Double, Double> {
        val mapping = comboBox.inspectable.queryInterface(XamlInterop.IID_IItemContainerMapping)
        val row = try {
            // While unselected, items are laid out centered on the middle item, so use the first item that is laid out
            (0 until comboBox.itemCount).firstNotNullOfOrNull { mapping.getPtrOrNull(XamlInterop.IItemContainerMapping_ContainerFromIndex, it) }
                ?.let(::actualHeight) ?: 0.0
        } finally {
            mapping.release()
        }
        val popup = checkNotNull(TemplateParts.find(comboBox, "Popup")) { "No Popup" }
        val border = try {
            popup.queryInterface(XamlInterop.IID_IPopup).let { view ->
                try {
                    view.getPtrOrNull(XamlInterop.IPopup_get_Child)
                } finally {
                    view.release()
                }
            }
        } finally {
            popup.release()
        }
        return row to (border?.let(::actualHeight) ?: 0.0)
    }

    /** The ActualHeight of [element] (takes ownership). */
    private fun actualHeight(element: ComPtr): Double = try {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        try {
            framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)
        } finally {
            framework.release()
        }
    } finally {
        element.release()
    }

    init {
        test("the open drop-down lays out maximumRowCount rows of items with the actual item height (even with a large font whose item height differs from the default estimate)") {
            val comboBox = onUiThreadGet {
                WComboBox((1..20).map { "Item $it" }).also {
                    it.font = WFont("Segoe UI", WFont.PLAIN, 24)
                    it.maximumRowCount = 2
                }
            }
            UiTestHarness.attachAndAwaitLoaded(comboBox)
            try {
                // The shared window is small, so check with 2 rows so that the drop-down is not cut by the window height
                onUiThread { comboBox.isPopupVisible = true }
                eventually(5.seconds) {
                    val (row, dropDown) = onUiThreadGet { dropDownHeights(comboBox) }
                    row shouldBeGreaterThan 36.0
                    // WinUI lays out the whole items that fit and shrinks, so the border height is "rows laid out × row height + border and padding (about 10 DIP)"
                    ((dropDown - 10.0) / row).roundToInt() shouldBe 2
                }
            } finally {
                onUiThread { comboBox.isPopupVisible = false }
                UiTestHarness.detach(comboBox)
            }
        }

        test("the items passed to the constructor can be retrieved via itemCount and getItem") {
            val (count, first, second) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                Triple(comboBox.itemCount, comboBox.getItem(0), comboBox.getItem(1))
            }
            count shouldBe 3
            first shouldBe "Red"
            second shouldBe "Green"
        }

        test("creating from a List selects the first item (selectedIndex = 0), as with JComboBox(Vector). Nothing if empty") {
            val (selected, empty) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                (comboBox.selectedIndex to comboBox.selectedItem) to WComboBox().let { it.selectedIndex to it.selectedItem }
            }
            selected shouldBe (0 to "Red")
            empty shouldBe (-1 to null)
        }

        test("setting selectedIndex makes selectedItem return the matching string") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                comboBox.selectedIndex = 2
                comboBox.selectedItem
            } shouldBe "Blue"
        }

        test("the ListSelectionListener fires on every selection change") {
            var count = 0
            onUiThread {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue")).also { it.selectedIndex = -1 } // Start with nothing selected
                comboBox.addListSelectionListener { count++ }
                comboBox.selectedIndex = 0
                comboBox.selectedIndex = 1
            }
            count shouldBe 2
        }

        test("removing with removeItemAt decreases the item count and moves the following items up") {
            val (count, item) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                comboBox.removeItemAt(1)
                comboBox.itemCount to comboBox.getItem(1)
            }
            count shouldBe 2
            item shouldBe "Blue"
        }

        test("removeAllItems clears every item and also clears the selection") {
            val (count, index) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                comboBox.selectedIndex = 0
                comboBox.removeAllItems()
                comboBox.itemCount to comboBox.selectedIndex
            }
            count shouldBe 0
            index shouldBe -1
        }

        test("isEditable = true allows setting text to a string not among the items") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red"))
                comboBox.isEditable = true
                comboBox.text = "Freeform input"
                comboBox.text
            } shouldBe "Freeform input"
        }

        context("Lifetime") {
            test("even after adding a listener that adds a Loaded handler to a shown editable combo box, choosing a value not in the list makes it appear in text") {
                val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).also { it.isEditable = true } }
                UiTestHarness.attachAndAwaitLoaded(comboBox)
                onUiThread {
                    // Adding an ancestor listener adds a Loaded handler and temporarily makes IsLoaded of the loaded element false
                    comboBox.addAncestorListener(
                        object : WAncestorListener {
                            override fun ancestorAdded(event: WAncestorEvent) = Unit

                            override fun ancestorRemoved(event: WAncestorEvent) = Unit

                            override fun ancestorMoved(event: WAncestorEvent) = Unit
                        },
                    )
                    comboBox.setSelectedItem("Unregistered value")
                }
                onUiThreadGet { comboBox.text } shouldBe "Unregistered value"
                UiTestHarness.detach(comboBox)
            }
        }
    }
}
