package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference

/**
 * Tests that value components sharing a model (WSlider, WSpinner, WComboBox) are not collected while shown even without listeners,
 * and keep writing natively changed values back to the model (continuation of [WrapperReleaseTest]).
 *
 * The components are put directly into the panel's native Children (not into the wrapper's list), to create a state with no strong references
 * from the Kotlin side to the component wrappers, the same as when the native side keeps a subtree and shows it again (Flyouts or tabs).
 */
@Tags("Memory")
class WrapperReleaseSharedModelTest : FunSpec() {
    /** Puts [components] directly into the native Children of [host]. Call on the UI thread. */
    private fun appendNatively(host: WPanel, vararg components: WComponent) = editChildren(host) { children ->
        for (component in components) children.call(FoundationInterop.IVector_Append, component.uiElement.ptr)
    }

    private fun editChildren(host: WPanel, block: (ComPtr) -> Unit) {
        val panel = host.inspectable.queryInterface(XamlInterop.IID_IPanel)
        try {
            val children = panel.getPtr(XamlInterop.IPanel_get_Children)
            try {
                block(children)
            } finally {
                children.release()
            }
        } finally {
            panel.release()
        }
    }

    /**
     * Creates the components with [create] (on the UI thread), puts them into [host] natively only, shows [host], and then calls [verify].
     * [create] returns a weak reference to the first component and the native views used later (owned by the caller; released at the end).
     */
    private fun withNativelyShown(
        create: (WPanel) -> Pair<WeakReference<WComponent>, List<ComPtr>>,
        verify: (WeakReference<WComponent>, List<ComPtr>) -> Unit,
    ) {
        val host = onUiThreadGet { WPanel() }
        val (reference, views) = onUiThreadGet { create(host) }
        UiTestHarness.attachAndAwaitLoaded(host)
        try {
            verify(reference, views)
        } finally {
            UiTestHarness.detach(host)
            onUiThread {
                editChildren(host) { it.call(FoundationInterop.IVector_Clear) }
                views.forEach { it.release() }
            }
        }
    }

    init {
        test("a WSlider given a shared model is not collected while shown even without listeners, and a value moved natively reaches a WProgressBar through the model") {
            withNativelyShown(
                create = { host ->
                    val shared = WDefaultBoundedRangeModel(10.0, 0.0, 0.0, 100.0)
                    val slider = WSlider(shared)
                    val bar = WProgressBar(shared)
                    appendNatively(host, slider, bar)
                    WeakReference<WComponent>(slider) to listOf(
                        slider.inspectable.queryInterface(XamlInterop.IID_IRangeBase),
                        bar.inspectable.queryInterface(XamlInterop.IID_IRangeBase),
                    )
                },
                verify = { slider, (sliderRange, barRange) ->
                    Reclamation.survivesGc(slider).shouldBeTrue()
                    onUiThreadGet {
                        sliderRange.call(XamlInterop.IRangeBase_put_Value, 70.0)
                        barRange.getDouble(XamlInterop.IRangeBase_get_Value)
                    } shouldBe 70.0
                },
            )
        }

        test("a WSpinner given a shared model is not collected while shown even without listeners, and a value changed natively reaches another spinner through the model") {
            withNativelyShown(
                create = { host ->
                    val shared = WSpinnerNumberModel(1, 0, 10, 1)
                    val first = WSpinner(shared)
                    val second = WSpinner(shared)
                    appendNatively(host, first, second)
                    WeakReference<WComponent>(first) to listOf(
                        first.inspectable.queryInterface(XamlInterop.IID_INumberBox),
                        second.inspectable.queryInterface(XamlInterop.IID_INumberBox),
                    )
                },
                verify = { first, (firstBox, secondBox) ->
                    Reclamation.survivesGc(first).shouldBeTrue()
                    onUiThreadGet {
                        firstBox.call(XamlInterop.INumberBox_put_Value, 5.0)
                        secondBox.getDouble(XamlInterop.INumberBox_get_Value)
                    } shouldBe 5.0
                },
            )
        }

        test("a WComboBox given a shared model is not collected while shown even without listeners, and a selection changed natively reaches another combo through the model") {
            withNativelyShown(
                create = { host ->
                    val shared = WDefaultComboBoxModel(arrayOf("Red", "Green", "Blue"))
                    val first = WComboBox(shared)
                    val second = WComboBox(shared)
                    appendNatively(host, first, second)
                    WeakReference<WComponent>(first) to listOf(
                        first.inspectable.queryInterface(XamlInterop.IID_ISelector),
                        second.inspectable.queryInterface(XamlInterop.IID_ISelector),
                    )
                },
                verify = { first, (firstSelector, secondSelector) ->
                    Reclamation.survivesGc(first).shouldBeTrue()
                    onUiThreadGet {
                        firstSelector.call(XamlInterop.ISelector_put_SelectedIndex, 2)
                        secondSelector.getInt(XamlInterop.ISelector_get_SelectedIndex)
                    } shouldBe 2
                },
            )
        }
    }
}
