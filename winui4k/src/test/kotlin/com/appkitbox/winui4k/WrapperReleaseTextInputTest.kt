package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import java.lang.ref.WeakReference
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify a WPasswordField with a PasswordChanged listener, and a WAutoSuggestBox with listeners (TextChanged,
 * ActionListener and the like), are not strongly captured by the handlers and are collected after removal (a
 * continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass).
 * Waits for collection while encouraging GC ([Reclamation]).
 */
class WrapperReleaseTextInputTest : FunSpec() {
    /** The values received by change notification listeners (password field, AutoSuggestBox). The listeners do not reference the field (they only record here). */
    private val changedValues = mutableListOf<String>()

    /**
     * Places [component] in the shared window, waits for Loaded, and removes it. Also waits until the Unloaded after the
     * removal has been processed on the UI thread.
     */
    private fun showAndHide(component: WComponent) {
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handler to finish running in the same cycle
    }

    /** Puts a component made by [create] (on the UI thread) into a window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        showAndHide(component)
        return WeakReference(component)
    }

    /** Shows a password field with PasswordChanged listeners (Kotlin and Java forms) that do not reference it (waiting for Loaded) and returns only a weak reference. */
    private fun shownPasswordField(): WeakReference<WPasswordField> {
        val field = onUiThreadGet {
            val values = changedValues
            WPasswordField().also {
                it.addPasswordChangedListener { password -> values += "kotlin:$password" }
                it.addPasswordChangedListenerForJava(Consumer { password -> values += "java:$password" })
            }
        }
        UiTestHarness.attachAndAwaitLoaded(field)
        return WeakReference(field)
    }

    /** Shows an AutoSuggestBox with TextChanged listeners (Kotlin and Java forms) that do not reference the box (waiting for Loaded) and returns only a weak reference. */
    private fun shownSuggestBox(): WeakReference<WAutoSuggestBox> {
        val box = onUiThreadGet {
            val values = changedValues
            WAutoSuggestBox().also {
                it.addTextChangedListener { text, reason -> values += "kotlin:$text:$reason" }
                it.addTextChangedListenerForJava(BiConsumer { text, reason -> values += "java:$text:$reason" })
            }
        }
        UiTestHarness.attachAndAwaitLoaded(box)
        return WeakReference(box)
    }

    init {
        test("a password field with PasswordChanged listeners (Kotlin and Java forms) that do not reference it is also collected after being shown in a window and removed") {
            val reference = shownAndHidden {
                WPasswordField().also {
                    it.addPasswordChangedListener { _ -> }
                    it.addPasswordChangedListenerForJava(Consumer { _ -> })
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a password field with PasswordChanged listeners is not collected while shown in a window even without references, and receives the changed password") {
            changedValues.clear()
            val reference = shownPasswordField()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                onUiThread { checkNotNull(reference.get()).password = "secret" }
                eventually(3.seconds) { onUiThreadGet { changedValues.toList() } shouldContainExactly listOf("kotlin:secret", "java:secret") }
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
                changedValues.clear()
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a password field never shown is also collected once all its PasswordChanged listeners are removed") {
            val reference = onUiThreadGet {
                val field = WPasswordField()
                val kotlin: (String) -> Unit = { _ -> }
                val java = Consumer<String> { _ -> }
                field.addPasswordChangedListener(kotlin)
                field.addPasswordChangedListenerForJava(java)
                field.removePasswordChangedListener(kotlin)
                field.removePasswordChangedListenerForJava(java)
                WeakReference(field)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WAutoSuggestBox with TextChanged listeners (Kotlin and Java forms) that do not reference the box is also collected after being shown in a window and removed") {
            val reference = shownAndHidden {
                WAutoSuggestBox().also {
                    it.addTextChangedListener { _, _ -> }
                    it.addTextChangedListenerForJava(BiConsumer { _, _ -> })
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WAutoSuggestBox with TextChanged listeners is not collected while shown in a window even without references, and receives the changed text and reason") {
            changedValues.clear()
            val reference = shownSuggestBox()
            try {
                Reclamation.survivesGc(reference).shouldBeTrue()
                onUiThread { checkNotNull(reference.get()).text = "Apple" }
                eventually(3.seconds) {
                    onUiThreadGet { changedValues.toList() } shouldContainExactly listOf(
                        "kotlin:Apple:PROGRAMMATIC_CHANGE",
                        "java:Apple:PROGRAMMATIC_CHANGE",
                    )
                }
            } finally {
                reference.get()?.let { UiTestHarness.detach(it) }
                changedValues.clear()
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WAutoSuggestBox never shown is also collected once all its TextChanged listeners are removed") {
            val reference = onUiThreadGet {
                val box = WAutoSuggestBox()
                val kotlin: (String, TextChangeReason) -> Unit = { _, _ -> }
                val java = BiConsumer<String, TextChangeReason> { _, _ -> }
                box.addTextChangedListener(kotlin)
                box.addTextChangedListenerForJava(java)
                box.removeTextChangedListener(kotlin)
                box.removeTextChangedListenerForJava(java)
                WeakReference(box)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WAutoSuggestBox with an ActionListener (the QuerySubmitted handler) and QuerySubmitted and SuggestionChosen listeners that do not reference the box is also collected after being shown in a window and removed") {
            val reference = shownAndHidden {
                WAutoSuggestBox().also {
                    it.addActionListener { _ -> }
                    it.addQuerySubmittedListener { _, _ -> }
                    it.addSuggestionChosenListener { _ -> }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WAutoSuggestBox never shown is also collected once all its ActionListeners are removed") {
            val reference = onUiThreadGet {
                val box = WAutoSuggestBox()
                val action: (WActionEvent) -> Unit = { _ -> }
                box.addActionListener(action)
                box.removeActionListener(action)
                WeakReference(box)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }
}
