package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that wrappers and native controls that are no longer referenced from anywhere are released.
 * Waits for collection while prompting GC ([Reclamation]). Destruction of the native object is checked with
 * IWeakReference ([NativeProbe]).
 */
class WrapperReleaseTest : FunSpec() {
    /** The source of events received on click. The listener does not reference the button itself (it only records here). */
    private val clickSources = mutableListOf<Any>()

    /** Creates a button with a listener that does not reference the button. The test holds it only through a weak reference. */
    private fun buttonWithListener(): WButton = WButton("Press").also { button ->
        val sources = clickSources
        button.addActionListener { e -> sources += e.source }
    }

    /**
     * Places [component] in the shared window, waits for Loaded, and removes it. Also waits until the Unloaded after the
     * removal has been processed on the UI thread.
     */
    private fun showAndHide(component: WComponent) {
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        awaitUnloaded(component)
    }

    private fun awaitUnloaded(component: WComponent) {
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handler to finish running in the same cycle
    }

    /**
     * Calls doClick on the button of [reference] and returns a description of the source the listener received ("button"
     * if it is that button, otherwise the type name). The source itself is not returned so that the test side keeps no
     * reference to the button.
     */
    private fun clickSourcesOf(reference: WeakReference<out WButtonBase>): List<String> = onUiThreadGet {
        clickSources.clear()
        val button = checkNotNull(reference.get())
        button.doClick()
        clickSources.map { if (it === button) "button" else it.javaClass.name }.also { clickSources.clear() }
    }

    /** Places a button with a listener in the window, removes it, and returns only a weak reference. */
    private fun shownAndHiddenButton(): WeakReference<WButton> {
        val button = onUiThreadGet { buttonWithListener() }
        showAndHide(button)
        return WeakReference(button)
    }

    /** Places a button with a listener in the window (waiting for Loaded) and returns only a weak reference. */
    private fun shownButton(): WeakReference<WButton> {
        val button = onUiThreadGet { buttonWithListener() }
        UiTestHarness.attachAndAwaitLoaded(button)
        return WeakReference(button)
    }

    /**
     * Places and removes a panel containing a button with a listener (the caller keeps holding the panel, as when the
     * native side keeps a subtree around).
     */
    private fun hiddenPanelWithButton(panel: WPanel): WeakReference<WButton> {
        val button = onUiThreadGet { buttonWithListener().also { panel.add(it) } }
        showAndHide(panel)
        return WeakReference(button)
    }

    /**
     * Places and removes a panel containing a button whose listener was attached after placement (the caller keeps
     * holding the panel).
     */
    private fun hiddenPanelWithLateListenerButton(panel: WPanel): WeakReference<WButton> {
        val button = onUiThreadGet { WButton("Added later").also { panel.add(it) } }
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            val sources = clickSources
            button.addActionListener { e -> sources += e.source }
        }
        UiTestHarness.detach(panel)
        awaitUnloaded(panel)
        return WeakReference(button)
    }

    /** Creates a single-line field with an ActionListener that does not reference the field. */
    private fun fieldWithListener(): WTextField = WTextField().also { field ->
        val sources = clickSources
        field.addActionListener { e -> sources += e.source }
    }

    /** Places a field with a listener in the window, removes it, and returns only a weak reference. */
    private fun shownAndHiddenField(): WeakReference<WTextField> {
        val field = onUiThreadGet { fieldWithListener() }
        showAndHide(field)
        return WeakReference(field)
    }

    /** Places a field with a listener in the window (waiting for Loaded) and returns only a weak reference. */
    private fun shownField(): WeakReference<WTextField> {
        val field = onUiThreadGet { fieldWithListener() }
        UiTestHarness.attachAndAwaitLoaded(field)
        return WeakReference(field)
    }

    /**
     * Moves focus to the field of [reference], presses Enter, and returns a description of the source the listener
     * received (same form as [clickSourcesOf]).
     */
    private suspend fun enterSourcesOf(reference: WeakReference<WTextField>): List<String> {
        onUiThread {
            clickSources.clear()
            check(checkNotNull(reference.get()).requestFocusInWindow()) { "could not move focus" }
        }
        TestKeyboard.pressEnter()
        eventually(3.seconds) { onUiThreadGet { clickSources.size } shouldBe 1 }
        return onUiThreadGet {
            val field = reference.get()
            clickSources.map { if (it === field) "field" else it.javaClass.name }.also { clickSources.clear() }
        }
    }

    /** Puts a component made by [create] (on the UI thread) into a window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        showAndHide(component)
        return WeakReference(component)
    }

    /** Also lets go of the [hiddenPanelWithButton] panel and returns weak references to the panel and the button. */
    private fun droppedPanelWithButton(): Pair<WeakReference<WPanel>, WeakReference<WButton>> {
        val panel = onUiThreadGet { WPanel() }
        return WeakReference(panel) to hiddenPanelWithButton(panel)
    }

    /** Places a button bound to [action] in the window, removes it, and returns only a weak reference. */
    private fun shownAndHiddenActionButton(action: WAction): WeakReference<WButton> {
        val button = onUiThreadGet { WButton(action) }
        showAndHide(button)
        return WeakReference(button)
    }

    init {
        test("a button with an ActionListener that does not reference it is also collected after being placed in and removed from the window") {
            Reclamation.awaitCollected(shownAndHiddenButton()).shouldBeTrue()
        }

        test("a button with an ActionListener is not collected while placed in the window even without references, and the click source is that button") {
            val reference = shownButton()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                clickSourcesOf(reference) shouldContainExactly listOf("button")
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a button inside a removed panel is not collected while the panel remains, and becomes the click source when the panel is placed again") {
            val panel = onUiThreadGet { WPanel() }
            val reference = hiddenPanelWithButton(panel)
            Reclamation.survivesGc(reference).shouldBeTrue()
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                clickSourcesOf(reference) shouldContainExactly listOf("button")
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a button whose ActionListener was attached after placement is also not collected while the removed panel remains") {
            val panel = onUiThreadGet { WPanel() }
            val reference = hiddenPanelWithLateListenerButton(panel)
            Reclamation.survivesGc(reference).shouldBeTrue()
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                clickSourcesOf(reference) shouldContainExactly listOf("button")
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a removed panel and the button inside it are both collected once the panel is no longer referenced either") {
            val (panel, button) = droppedPanelWithButton()
            Reclamation.awaitCollected(button).shouldBeTrue()
            Reclamation.awaitCollected(panel).shouldBeTrue()
        }

        test("a button that was never placed is also collected once all ActionListeners are removed") {
            val reference = onUiThreadGet {
                val button = WButton("Remove")
                val listener: (WActionEvent) -> Unit = { }
                button.addActionListener(listener)
                button.removeActionListener(listener)
                WeakReference(button)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a button bound to a shared WAction is collected after removal from the window even while the action remains") {
            val action = object : WAbstractAction("Shared") {
                override fun actionPerformed(e: WActionEvent) = Unit
            }
            Reclamation.awaitCollected(shownAndHiddenActionButton(action)).shouldBeTrue()
            onUiThread { action.putValue(WAction.NAME, "After collection") } // Notifications to the collected button are discarded
        }

        test("a WTextField with an ActionListener that does not reference it is also collected after being placed in and removed from the window") {
            Reclamation.awaitCollected(shownAndHiddenField()).shouldBeTrue()
        }

        test("a WTextField with an ActionListener is not collected while placed in the window even without references, and the Enter source is that field") {
            val reference = shownField()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                enterSourcesOf(reference) shouldContainExactly listOf("field")
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WPanel that is not placed anywhere has its native StackPanel destroyed once the wrapper is collected") {
            val probe = onUiThreadGet { NativeProbe(WPanel(spacing = 4.0).inspectable) }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WTextField that is not placed anywhere has its native TextBox destroyed once the wrapper is collected") {
            val probe = onUiThreadGet { NativeProbe(WTextField("Prompt").inspectable) }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("changing the text of a button with an icon destroys the StackPanel previously used as its Content") {
            val button = onUiThreadGet { WButton("Before", Symbol.SAVE) }
            val probe = onUiThreadGet {
                val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
                val content = try {
                    contentControl.getPtr(XamlInterop.IContentControl_get_Content)
                } finally {
                    contentControl.release()
                }
                try {
                    NativeProbe(content)
                } finally {
                    content.release()
                }
            }
            onUiThread { button.text = "After" }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
            onUiThreadGet { button.text } // Keep the button alive while waiting for collection
        }

        test("a text field with a focus listener that does not reference the component is also collected after being shown in a window and removed") {
            Reclamation.awaitCollected(shownAndHidden { WTextField().also { it.addFocusListener(object : WFocusAdapter() {}) } })
                .shouldBeTrue()
        }

        test("a text field never shown is also collected once all its focus listeners are removed") {
            val reference = onUiThreadGet {
                val field = WTextField()
                val listener = object : WFocusAdapter() {}
                field.addFocusListener(listener)
                field.addFocusListener(listener)
                field.removeFocusListener(listener)
                field.removeFocusListener(listener)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("even if the return value of Xaml.addHandlerHandledToo (the remove function) is discarded, the handler and what it captured are collected once the element is destroyed") {
            val captured = onUiThreadGet {
                val captured = Any()
                Xaml.addHandlerHandledToo(
                    WButton("With handler"),
                    XamlInterop.IUIElementStatics_get_KeyDownEvent,
                    XamlInterop.IID_KeyEventHandler,
                    FoundationInterop.IID_IReference_KeyEventHandler,
                ) { _, _ -> captured.hashCode() }
                WeakReference(captured)
            }
            Reclamation.awaitCollected(captured).shouldBeTrue()
        }
    }
}
