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
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that wrappers and native controls that are no longer referenced from anywhere are released.
 * Waits for collection while prompting GC ([Reclamation]). Destruction of the native object is checked with
 * IWeakReference ([NativeProbe]).
 */
class WrapperReleaseTest : FunSpec() {
    /** The source of events received on click. The listener does not reference the button itself (it only records here). */
    private val clickSources = mutableListOf<Any>()

    /** The source of events received by the ItemListener. The listener does not reference the toggle (it only records here). */
    private val itemSources = mutableListOf<Any>()

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

    /** Puts a WSplitButton with a listener into a window (waiting for Loaded) and returns only a weak reference. */
    private fun shownSplitButton(): WeakReference<WSplitButton> {
        val sources = clickSources
        val button = onUiThreadGet { WSplitButton("Paste").also { it.addActionListener { e -> sources += e.source } } }
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

    /** Puts a check box with an ItemListener into a window (waiting for Loaded) and returns only a weak reference. */
    private fun shownCheckBox(): WeakReference<WCheckBox> {
        val sources = itemSources
        val checkBox = onUiThreadGet {
            WCheckBox("Agree").also { it.addItemListener(WItemListener { e -> sources += e.source }) }
        }
        UiTestHarness.attachAndAwaitLoaded(checkBox)
        return WeakReference(checkBox)
    }

    /** Weak references to a button group and its buttons (the group, 2 check boxes and 1 radio button). */
    private class GroupRefs(
        val group: WeakReference<WButtonGroup>,
        val checkBoxes: List<WeakReference<WCheckBox>>,
        val radio: WeakReference<WRadioButton>,
    ) {
        fun all(): List<WeakReference<*>> = listOf(group, radio) + checkBoxes
    }

    /** Puts buttons in a group onto a panel, shows and removes it, releases the group and panel, and returns only weak references. */
    private fun shownAndHiddenGroup(): GroupRefs {
        val panel = onUiThreadGet { WPanel() }
        val refs = onUiThreadGet {
            val group = WButtonGroup()
            val checkBoxes = listOf(WCheckBox("A"), WCheckBox("B"))
            val radio = WRadioButton("R")
            (checkBoxes + radio).forEach {
                panel.add(it)
                group.add(it)
            }
            GroupRefs(WeakReference(group), checkBoxes.map { WeakReference(it) }, WeakReference(radio))
        }
        showAndHide(panel)
        return refs
    }

    /**
     * Shows a panel holding 2 check boxes in a group (waiting for Loaded), releases the group, and returns only weak references.
     * The panel goes into [panelHolder] (the caller holds it, to keep the buttons alive as the root of the detached subtree
     * until it is removed and released).
     */
    private fun shownGroupedCheckBoxes(panelHolder: Array<WPanel?>): List<WeakReference<WCheckBox>> {
        val panel = onUiThreadGet { WPanel() }
        panelHolder[0] = panel
        val refs = onUiThreadGet {
            val group = WButtonGroup()
            listOf(WCheckBox("A"), WCheckBox("B")).map {
                panel.add(it)
                group.add(it)
                WeakReference(it)
            }
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        return refs
    }

    /** Removes the panel in [panelHolder] from the window and releases the panel too. */
    private fun hideAndDropPanel(panelHolder: Array<WPanel?>) {
        showAndHideTail(checkNotNull(panelHolder[0]))
        panelHolder[0] = null
    }

    private fun showAndHideTail(panel: WPanel) {
        UiTestHarness.detach(panel)
        awaitUnloaded(panel)
    }

    /** Puts 2 never-shown check boxes into a group, removes them all, and returns only weak references. */
    private fun removedGroupedCheckBoxes(): GroupRefs = onUiThreadGet {
        val group = WButtonGroup()
        val checkBoxes = listOf(WCheckBox("A"), WCheckBox("B"))
        val radio = WRadioButton("R")
        (checkBoxes + radio).forEach { group.add(it) }
        (checkBoxes + radio).forEach { group.remove(it) }
        GroupRefs(WeakReference(group), checkBoxes.map { WeakReference(it) }, WeakReference(radio))
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

        test("a WSplitButton and a WToggleSplitButton with an ActionListener that does not reference the component are also collected after being shown in a window and removed") {
            val sources = clickSources
            val split = shownAndHidden { WSplitButton("Paste").also { it.addActionListener { e -> sources += e.source } } }
            val toggle = shownAndHidden { WToggleSplitButton("Bold").also { it.addActionListener { e -> sources += e.source } } }
            Reclamation.awaitCollected(split).shouldBeTrue()
            Reclamation.awaitCollected(toggle).shouldBeTrue()
        }

        test("a WSplitButton with an ActionListener is not collected while shown in a window even without references, and the click source is that WSplitButton") {
            val reference = shownSplitButton()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                val sources = onUiThreadGet {
                    clickSources.clear()
                    val button = checkNotNull(reference.get())
                    button.doClick()
                    clickSources.map { if (it === button) "button" else it.javaClass.name }.also { clickSources.clear() }
                }
                sources shouldContainExactly listOf("button")
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WSplitButton never shown is also collected once all three forms of its ActionListener are removed") {
            val reference = onUiThreadGet {
                val button = WSplitButton("Remove")
                val lambda: (WActionEvent) -> Unit = { }
                val runnable = Runnable { }
                val listener = WActionListener { }
                button.addActionListener(lambda)
                button.addActionListenerForJava(runnable)
                button.addActionListenerForJava(listener)
                button.removeActionListener(lambda)
                button.removeActionListenerForJava(runnable)
                button.removeActionListenerForJava(listener)
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

        test("a panel with mouse listeners (all 3 kinds) that do not reference the component is also collected after being shown in a window and removed") {
            val reference = shownAndHidden {
                WPanel().also { panel ->
                    val adapter = object : WMouseAdapter() {}
                    panel.addMouseListener(adapter)
                    panel.addMouseMotionListener(adapter)
                    panel.addMouseWheelListener(adapter)
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a panel never shown is also collected once all its mouse listeners are removed") {
            val reference = onUiThreadGet {
                val panel = WPanel()
                val adapter = object : WMouseAdapter() {}
                panel.addMouseListener(adapter)
                panel.addMouseMotionListener(adapter)
                panel.addMouseWheelListener(adapter)
                panel.removeMouseListener(adapter)
                panel.removeMouseMotionListener(adapter)
                panel.removeMouseWheelListener(adapter)
                WeakReference(panel)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field with a key listener that does not reference the component is also collected after being shown in a window and removed") {
            val reference = shownAndHidden { WTextField().also { it.addKeyListener(object : WKeyAdapter() {}) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field with a DocumentListener that does not reference the component is also collected after being shown in a window and removed") {
            val reference = shownAndHidden { WTextField().also { it.addDocumentListener(EmptyDocumentListener()) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field never shown is also collected once all its DocumentListeners are removed") {
            val reference = onUiThreadGet {
                val field = WTextField()
                val listener = EmptyDocumentListener()
                field.addDocumentListener(listener)
                field.text = "Changed"
                field.removeDocumentListener(listener)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field with a CaretListener that does not reference the component is also collected after being shown in a window and removed") {
            val reference = shownAndHidden { WTextField().also { it.addCaretListener(WCaretListener { }) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field never shown is also collected once all its CaretListeners are removed") {
            val reference = onUiThreadGet {
                val field = WTextField()
                val listener = WCaretListener { }
                field.addCaretListener(listener)
                field.text = "Changed"
                field.selectAll()
                field.removeCaretListener(listener)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field with TextChanged listeners (Kotlin and Java forms) that do not reference the field is also collected after being shown in a window and removed") {
            val reference = shownAndHidden {
                WTextField().also {
                    it.addTextChangedListener { _ -> }
                    it.addTextChangedListenerForJava(Consumer { _ -> })
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field never shown is also collected once all its TextChanged listeners are removed") {
            val reference = onUiThreadGet {
                val field = WTextField()
                val kotlin: (String) -> Unit = { _ -> }
                val java = Consumer<String> { _ -> }
                field.addTextChangedListener(kotlin)
                field.addTextChangedListenerForJava(java)
                field.removeTextChangedListener(kotlin)
                field.removeTextChangedListenerForJava(java)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field with columns set is also collected after being shown in a window and removed") {
            Reclamation.awaitCollected(shownAndHidden { WTextField(12) }).shouldBeTrue()
        }

        test("a text field never shown is also collected once columns is set back to 0") {
            val reference = onUiThreadGet {
                val field = WTextField(12)
                field.columns = 0
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a text field never shown is also collected once all its key listeners are removed") {
            val reference = onUiThreadGet {
                val field = WTextField()
                val listener = object : WKeyAdapter() {}
                field.addKeyListener(listener)
                field.removeKeyListener(listener)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WCheckBox with an ItemListener that does not reference the component is also collected after being shown in a window and removed") {
            val sources = itemSources
            val reference = shownAndHidden {
                WCheckBox("Agree").also { checkBox ->
                    checkBox.addItemListener(WItemListener { e -> sources += e.source })
                    checkBox.addItemListener { checked -> sources += checked.toString() }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WCheckBox with an ItemListener is not collected while shown in a window even without references, and the ItemEvent source is that check box") {
            val reference = shownCheckBox()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                val described = onUiThreadGet {
                    itemSources.clear()
                    val target = checkNotNull(reference.get())
                    target.isSelected = true
                    itemSources.map { if (it === target) "checkBox" else it.javaClass.name }.also { itemSources.clear() }
                }
                described shouldContainExactly listOf("checkBox")
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WCheckBox never shown is also collected once all its ItemListeners are removed") {
            val reference = onUiThreadGet {
                val checkBox = WCheckBox("Remove")
                val listener = WItemListener { }
                val lambda: (Boolean?) -> Unit = { }
                checkBox.addItemListener(listener)
                checkBox.addItemListener(lambda)
                checkBox.removeItemListener(listener)
                checkBox.removeItemListener(lambda)
                WeakReference(checkBox)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WToggleSwitch and a WToggleSplitButton with an ItemListener that does not reference the component are also collected after being shown in a window and removed") {
            val sources = itemSources
            val switch = shownAndHidden { WToggleSwitch("Notifications").also { it.addItemListener(WItemListener { e -> sources += e.source }) } }
            val split = shownAndHidden { WToggleSplitButton("Bold").also { it.addItemListener(WItemListener { e -> sources += e.source }) } }
            Reclamation.awaitCollected(switch).shouldBeTrue()
            Reclamation.awaitCollected(split).shouldBeTrue()
        }

        test("a WCheckBox bound to a shared WAction with SELECTED_KEY is collected after being removed from the window even if the action remains") {
            val action = object : WAbstractAction("Shared") {
                override fun actionPerformed(e: WActionEvent) = Unit
            }
            action.putValue(WAction.SELECTED_KEY, true)
            val reference = shownAndHidden { WCheckBox(action) }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            onUiThread { action.putValue(WAction.SELECTED_KEY, false) } // Notifications to collected buttons are discarded
        }

        test("a button group shown in a window and removed is entirely collected once neither the group nor the buttons are referenced") {
            val refs = shownAndHiddenGroup()
            refs.all().forEach { Reclamation.awaitCollected(it).shouldBeTrue() }
        }

        test("while shown, the group stays alive through the buttons even after the reference to the button group is released, and exclusivity is kept") {
            val panelHolder = arrayOfNulls<WPanel>(1)
            val refs = shownGroupedCheckBoxes(panelHolder)
            try {
                refs.forEach { Reclamation.survivesGc(it).shouldBeTrue() }
                val states = onUiThreadGet {
                    val (a, b) = refs.map { checkNotNull(it.get()) }
                    a.doClick()
                    b.doClick()
                    listOf(a.isSelected, b.isSelected)
                }
                states shouldBe listOf(false, true)
            } finally {
                hideAndDropPanel(panelHolder)
            }
            refs.forEach { Reclamation.awaitCollected(it).shouldBeTrue() }
        }

        test("buttons never shown are also collected together with the group once removed from the group") {
            val refs = removedGroupedCheckBoxes()
            refs.all().forEach { Reclamation.awaitCollected(it).shouldBeTrue() }
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

/** A DocumentListener that does nothing (does not reference the component). */
private class EmptyDocumentListener : WDocumentListener {
    override fun insertUpdate(e: WDocumentEvent) {
        // Does nothing, since this is a collection test
    }

    override fun removeUpdate(e: WDocumentEvent) {
        // Does nothing, since this is a collection test
    }

    override fun changedUpdate(e: WDocumentEvent) {
        // Does nothing, since this is a collection test
    }
}
