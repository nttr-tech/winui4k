package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verification of the JToolBar-like API of WCommandBar / WCommandBarFlyout (add, addSeparator, getComponentAtIndex, remove, removeAll,
 * orientation, floatable, rollover). It also reads and checks that the Kotlin-side record of the sequence agrees with the native PrimaryCommands.
 * The shape seen from Java is tested in WCommandBarToolBarForJavaTest.
 */
@Tags("E2E")
class WCommandBarToolBarTest : FunSpec() {
    private class RecordingAction(name: String) : WAbstractAction(name, null) {
        var performed = 0

        override fun actionPerformed(e: WActionEvent) {
            performed++
        }
    }

    /** The number of elements in CommandBar.PrimaryCommands (native). */
    private fun nativePrimaryCount(bar: WCommandBar): Int {
        val observable = bar.inspectable.getPtr(XamlInterop.ICommandBar_get_PrimaryCommands)
        return try {
            val vector = observable.queryInterface(XamlInterop.IID_IVector_ICommandBarElement)
            try {
                vector.getInt(FoundationInterop.IVector_get_Size)
            } finally {
                vector.release()
            }
        } finally {
            observable.release()
        }
    }

    init {
        test("add(WAction) creates a WAppBarButton labeled with the WAction's name at the end, and pressing it calls actionPerformed") {
            onUiThreadGet {
                val bar = WCommandBar()
                val action = RecordingAction("Save")
                val button = bar.add(action)
                button.doClick()
                listOf(button.label, button.action === action, action.performed, bar.componentCount, nativePrimaryCount(bar))
            } shouldBe listOf("Save", true, 1, 1, 1)
        }

        test("add(WComponent) puts AppBar buttons and separators as is and wraps other components in a container, keeps the order added, and returns the component itself") {
            onUiThreadGet {
                val bar = WCommandBar()
                val button = WAppBarButton("Open")
                val field = WTextField()
                val separator = bar.addSeparator()
                val returnedButton = bar.add(button)
                val returnedField = bar.add(field)
                listOf(
                    returnedButton === button,
                    returnedField === field,
                    bar.getComponentAtIndex(0) === separator,
                    bar.getComponentAtIndex(1) === button,
                    bar.getComponentAtIndex(2) === field,
                    bar.getComponentIndex(field),
                    bar.componentCount,
                    nativePrimaryCount(bar),
                    field.parent === bar,
                    button.parent === bar,
                )
            } shouldBe listOf(true, true, true, true, true, 2, 3, 3, true, true)
        }

        test("add(WComponent, index) inserts at the given position, and out of range throws IllegalArgumentException") {
            onUiThreadGet {
                val bar = WCommandBar()
                val a = bar.add(WAppBarButton("A"))
                val c = bar.add(WAppBarButton("C"))
                val b = bar.add(WAppBarButton("B"), 1)
                val order = bar.components.toList()
                val error = runCatching { bar.add(WAppBarButton("X"), 4) }.exceptionOrNull()
                listOf(order == listOf(a, b, c), error is IllegalArgumentException, bar.componentCount, nativePrimaryCount(bar))
            } shouldBe listOf(true, true, 3, 3)
        }

        test("getComponentAtIndex returns null when out of range, and getComponentIndex returns -1 for non-children (as in Swing)") {
            onUiThreadGet {
                val bar = WCommandBar()
                bar.add(WAppBarButton("A"))
                listOf(
                    bar.getComponentAtIndex(1),
                    bar.getComponentAtIndex(-1),
                    bar.getComponentIndex(WAppBarButton()),
                    bar.getComponentIndex(null),
                )
            } shouldBe listOf(null, null, -1, -1)
        }

        test("remove(component) also removes wrapped components, and removed components can be put on another bar") {
            onUiThreadGet {
                val bar = WCommandBar()
                val other = WCommandBar()
                val field = WTextField()
                val button = bar.add(WAppBarButton("A"))
                bar.add(field)
                bar.remove(field)
                val afterRemove = listOf(
                    bar.componentCount,
                    nativePrimaryCount(bar),
                    field.parent == null,
                    bar.getComponentAtIndex(0) === button,
                )
                other.add(field)
                afterRemove + listOf(other.componentCount, nativePrimaryCount(other), field.parent === other)
            } shouldBe listOf(1, 1, true, true, 1, 1, true)
        }

        test("adding a component that is on another bar removes it from the original bar and moves it") {
            onUiThreadGet {
                val first = WCommandBar()
                val second = WCommandBar()
                val field = WTextField()
                first.add(field)
                second.add(field)
                listOf(
                    first.componentCount,
                    nativePrimaryCount(first),
                    second.componentCount,
                    nativePrimaryCount(second),
                    field.parent === second,
                )
            } shouldBe listOf(0, 0, 1, 1, true)
        }

        test("remove(index) throws ArrayIndexOutOfBoundsException when out of range, and removeAll removes everything but keeps Secondary") {
            onUiThreadGet {
                val bar = WCommandBar()
                val a = bar.add(WAppBarButton("A"))
                bar.add(WTextField())
                bar.addSecondaryCommand(WAppBarButton("S"))
                val outOfRange = runCatching { bar.remove(2) }.exceptionOrNull()
                bar.remove(0)
                val afterIndex = bar.componentCount
                bar.removeAll()
                listOf(
                    outOfRange is ArrayIndexOutOfBoundsException,
                    afterIndex,
                    bar.componentCount,
                    nativePrimaryCount(bar),
                    a.parent == null,
                )
            } shouldBe listOf(true, 1, 0, 0, true)
        }

        test("addPrimaryCommand joins the sequence, and components that are not ICommandBarElement throw IllegalArgumentException instead of an HRESULT") {
            onUiThreadGet {
                val bar = WCommandBar()
                val button = WAppBarButton("A")
                bar.addPrimaryCommand(button)
                val error = runCatching { bar.addPrimaryCommand(WTextField()) }.exceptionOrNull()
                val secondary = runCatching { bar.addSecondaryCommand(WTextField()) }.exceptionOrNull()
                listOf(
                    bar.getComponentAtIndex(0) === button,
                    error is IllegalArgumentException,
                    secondary is IllegalArgumentException,
                    bar.componentCount,
                )
            } shouldBe listOf(true, true, true, 1)
        }

        test("adding itself throws IllegalArgumentException") {
            onUiThreadGet {
                val bar = WCommandBar()
                runCatching { bar.add(bar) }.exceptionOrNull() is IllegalArgumentException
            } shouldBe true
        }

        test("floatable is always false, rollover is always true, orientation is horizontal only, and VERTICAL throws IllegalArgumentException") {
            onUiThreadGet {
                val bar = WCommandBar()
                bar.isFloatable = true
                bar.isRollover = false
                bar.orientation = WCommandBar.HORIZONTAL
                val vertical = runCatching { bar.orientation = WCommandBar.VERTICAL }.exceptionOrNull()
                val bogus = runCatching { bar.orientation = 7 }.exceptionOrNull()
                listOf(
                    bar.isFloatable,
                    bar.isRollover,
                    bar.orientation,
                    vertical is IllegalArgumentException,
                    bogus is IllegalArgumentException,
                )
            } shouldBe listOf(false, true, 0, true, true)
        }

        test("WCommandBarFlyout also has add(WAction), add(WComponent), addSeparator, remove and removeAll") {
            onUiThreadGet {
                val flyout = WCommandBarFlyout()
                val button = flyout.add(RecordingAction("Copy"))
                val field = flyout.add(WTextField())
                val separator = flyout.addSeparator()
                val before = listOf(
                    button.label,
                    flyout.componentCount,
                    flyout.getComponentIndex(field),
                    flyout.getComponentAtIndex(2) === separator,
                )
                flyout.remove(field)
                val mid = flyout.componentCount
                flyout.removeAll()
                before + listOf(mid, flyout.componentCount)
            } shouldBe listOf("Copy", 3, 1, true, 2, 0)
        }

        test("WCommandBarFlyout's addPrimaryCommand also throws IllegalArgumentException for non-ICommandBarElement") {
            onUiThreadGet {
                runCatching { WCommandBarFlyout().addPrimaryCommand(WTextField()) }.exceptionOrNull() is IllegalArgumentException
            } shouldBe true
        }
    }
}
