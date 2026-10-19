package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that WDropDownButton, which extends WButton, can use WButton's flyout, icon, action and colors as they are.
 * For colors, it reads DropDownButton's template (the RootGrid background and the ContentPresenter foreground) to confirm
 * that Button's lightweight styling keys (ButtonBackground* / ButtonForeground*) take effect. The Java-side shape is
 * covered by WDropDownButtonForJavaTest.
 */
class WDropDownButtonTest : FunSpec() {
    /** The element in the template whose name (FrameworkElement.Name) is [name]. The caller releases it. */
    private fun templatePart(button: WButtonBase, name: String): ComPtr? {
        val root = Xaml.childAt(button.inspectable, 0) ?: return null
        return findByName(root, name)
    }

    /** Searches [element] (taking ownership) and its descendants for the element named [name]. */
    private fun findByName(element: ComPtr, name: String): ComPtr? {
        val framework = element.queryInterface(XamlInterop.IID_IFrameworkElement)
        val found = try {
            framework.getString(XamlInterop.IFrameworkElement_get_Name) == name
        } finally {
            framework.release()
        }
        if (found) return element
        try {
            for (i in 0 until Xaml.childCount(element)) {
                val child = Xaml.childAt(element, i) ?: continue
                findByName(child, name)?.let { return it }
            }
        } finally {
            element.release()
        }
        return null
    }

    private fun brushColor(brush: ComPtr?): WColor? = brush?.let {
        try {
            colorOf(it)
        } finally {
            it.release()
        }
    }

    /** The RootGrid background color, the ContentPresenter foreground color, and the chevron (ChevronIcon) foreground color. */
    private data class Look(val background: WColor?, val foreground: WColor?, val chevron: WColor?)

    private fun lookOf(button: WDropDownButton): Look {
        val grid = checkNotNull(templatePart(button, "RootGrid")) { "the template has not been applied" }
        val background = try {
            val panel = grid.queryInterface(XamlInterop.IID_IPanel)
            try {
                brushColor(panel.getPtrOrNull(XamlInterop.IPanel_get_Background))
            } finally {
                panel.release()
            }
        } finally {
            grid.release()
        }
        val presenter = checkNotNull(templatePart(button, "ContentPresenter"))
        val foreground = try {
            val view = presenter.queryInterface(XamlInterop.IID_IContentPresenter)
            try {
                brushColor(view.getPtrOrNull(XamlInterop.IContentPresenter_get_Foreground))
            } finally {
                view.release()
            }
        } finally {
            presenter.release()
        }
        val chevron = checkNotNull(templatePart(button, "ChevronIcon"))
        val chevronColor = try {
            val icon = chevron.queryInterface(XamlInterop.IID_IIconElement)
            try {
                brushColor(icon.getPtrOrNull(XamlInterop.IIconElement_get_Foreground))
            } finally {
                icon.release()
            }
        } finally {
            chevron.release()
        }
        return Look(background, foreground, chevronColor)
    }

    /** Puts [button] in the visual state [state], waits until its look becomes [expected] (ignoring the chevron), and returns it. */
    private suspend fun lookIn(button: WDropDownButton, state: String, expected: Look? = null): Look {
        onUiThread { check(Xaml.goToState(button.inspectable, state)) { "cannot go to visual state $state" } }
        if (expected == null) {
            Thread.sleep(STATE_SETTLE_MILLIS)
            return onUiThreadGet { lookOf(button) }
        }
        var last: Look? = null
        eventually(3.seconds) {
            last = onUiThreadGet { lookOf(button) }
            last?.copy(chevron = null) shouldBe expected.copy(chevron = null)
        }
        return checkNotNull(last)
    }

    private fun loaded(configure: (WDropDownButton) -> Unit = {}): WDropDownButton {
        val button = onUiThreadGet { WDropDownButton("Other").also(configure) }
        UiTestHarness.attachAndAwaitLoaded(button)
        return button
    }

    init {
        test("WDropDownButton is a WButton, and flyout goes into the native Button.Flyout") {
            onUiThreadGet {
                val button = WDropDownButton("Other")
                val flyout = WMenuFlyout()
                button.flyout = flyout
                val native = checkNotNull(
                    button.inspectable.queryInterface(XamlInterop.IID_IButton).let { view ->
                        try {
                            view.getPtrOrNull(XamlInterop.IButton_get_Flyout)
                        } finally {
                            view.release()
                        }
                    },
                )
                val same = try {
                    Xaml.sameObject(native, flyout.flyoutBase)
                } finally {
                    native.release()
                }
                listOf((button as WButtonBase) is WButton, button.flyout === flyout, same)
            } shouldContainExactly listOf(true, true, true)
        }

        test("icon and action inherited from WButton can be used, and a click calls the action") {
            onUiThreadGet {
                var performed = 0
                val action = object : WAbstractAction("Share", Symbol.SHARE) {
                    override fun actionPerformed(e: WActionEvent) {
                        performed++
                    }
                }
                val button = WDropDownButton()
                button.action = action
                button.doClick()
                listOf(button.text, ButtonContents.contentOf(button), performed)
            } shouldContainExactly listOf("Share", listOf("Panel:HORIZONTAL:4.0", "Symbol:SHARE", "TextBlock:Share"), 1)
        }

        test("background and foreground take effect in each state with the DropDownButton template too, and the chevron color stays the theme's") {
            val reference = loaded()
            val defaults = try {
                listOf("Normal", "PointerOver", "Pressed").map { lookIn(reference, it) }
            } finally {
                UiTestHarness.detach(reference)
            }
            val button = loaded()
            try {
                onUiThread {
                    button.background = WColor(200, 40, 40)
                    button.foreground = WColor(0, 0, 160)
                }
                val looks = listOf(
                    lookIn(button, "Normal", Look(WColor(200, 40, 40, 255), WColor(0, 0, 160, 255), null)),
                    lookIn(button, "PointerOver", Look(WColor(200, 40, 40, 230), WColor(0, 0, 160, 255), null)),
                    lookIn(button, "Pressed", Look(WColor(200, 40, 40, 204), WColor(0, 0, 160, 179), null)),
                )
                // The chevron is drawn with DropDownButtonForegroundSecondary* and does not follow foreground
                looks.map { it.chevron } shouldContainExactly defaults.map { it.chevron }
                looks.forEach { it.chevron shouldNotBe WColor(0, 0, 160, 255) }
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("the DropDownButton template (chevron) is kept even with isAccent and isContentAreaFilled = false") {
            val button = loaded {
                it.isAccent = true
            }
            try {
                onUiThreadGet {
                    val withAccent = templatePart(button, "ChevronIcon")?.also { it.release() } != null
                    button.isAccent = false
                    button.isContentAreaFilled = false
                    button.uiElement.call(XamlInterop.IUIElement_UpdateLayout)
                    val withoutFill = templatePart(button, "ChevronIcon")?.also { it.release() } != null
                    withAccent to withoutFill
                } shouldBe (true to true)
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("a WDropDownButton with isContentAreaFilled = false does not paint background") {
            val reference = loaded()
            val default = try {
                lookIn(reference, "Normal")
            } finally {
                UiTestHarness.detach(reference)
            }
            val button = loaded {
                it.background = WColor(200, 40, 40)
                it.isContentAreaFilled = false
            }
            try {
                lookIn(button, "Normal", default)
            } finally {
                UiTestHarness.detach(button)
            }
        }
    }

    private companion object {
        const val STATE_SETTLE_MILLIS = 300L
    }
}
