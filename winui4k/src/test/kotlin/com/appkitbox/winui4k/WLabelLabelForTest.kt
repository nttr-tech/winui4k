package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests WLabel.labelFor / displayedMnemonic (JLabel.setLabelFor / setDisplayedMnemonic).
 * Checks by reading the target's AutomationProperties.LabeledBy and UIElement.AccessKey back from the native object.
 */
class WLabelLabelForTest : FunSpec() {
    /** Whether [target]'s LabeledBy is [label]'s text (TextBlock). */
    private fun isLabeledBy(target: WComponent, label: WLabel): Boolean {
        val labeledBy = Xaml.labeledBy(target.automationTarget) ?: return false
        return try {
            Xaml.sameObject(labeledBy, label.textBlock)
        } finally {
            labeledBy.release()
        }
    }

    private fun hasLabeledBy(target: WComponent): Boolean = Xaml.labeledBy(target.automationTarget)?.also { it.release() } != null

    /** Key code of [target]'s AccessKey (0 if unset). */
    private fun accessKeyOf(target: WComponent): Int = Mnemonics.read(target.uiElement)

    init {
        test("by default labelFor is null and displayedMnemonic is 0") {
            onUiThreadGet {
                val label = WLabel("Name")
                label.labelFor to label.displayedMnemonic
            } shouldBe (null to 0)
        }

        test("setting an input field as labelFor makes the field's LabeledBy the label's text") {
            onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                val before = hasLabeledBy(field)
                label.labelFor = field
                listOf(before, isLabeledBy(field, label), label.labelFor === field)
            } shouldContainExactly listOf(false, true, true)
        }

        test("displayedMnemonic becomes the AccessKey of the labelFor target, and goes into a target set later even if set first") {
            onUiThreadGet {
                val first = WLabel("Name")
                val firstField = WTextField()
                first.labelFor = firstField
                first.displayedMnemonic = 'N'.code
                val second = WLabel("Address")
                val secondField = WTextField()
                second.displayedMnemonic = 'A'.code
                val beforeTarget = accessKeyOf(secondField)
                second.labelFor = secondField
                listOf(accessKeyOf(firstField), beforeTarget, accessKeyOf(secondField))
            } shouldContainExactly listOf('N'.code, 0, 'A'.code)
        }

        test("setDisplayedMnemonic(char) turns a letter into the uppercase key code, and '\\u0000' clears it") {
            onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                label.labelFor = field
                label.setDisplayedMnemonic('n')
                val set = listOf(label.displayedMnemonic, accessKeyOf(field))
                label.setDisplayedMnemonic('\u0000')
                set + listOf(label.displayedMnemonic, accessKeyOf(field))
            } shouldContainExactly listOf('N'.code, 'N'.code, 0, 0)
        }

        test("a displayedMnemonic other than letters and digits cannot be set, and neither the value nor the target's access key changes") {
            val (label, field) = onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                label.labelFor = field
                label.displayedMnemonic = '1'.code
                label to field
            }
            shouldThrow<IllegalArgumentException> { onUiThread { label.setDisplayedMnemonic('#') } }
            shouldThrow<IllegalArgumentException> { onUiThread { label.displayedMnemonic = 'a'.code } }
            onUiThreadGet { label.displayedMnemonic to accessKeyOf(field) } shouldBe ('1'.code to '1'.code)
        }

        test("changing labelFor to another input field moves LabeledBy and the access key to the new target and removes them from the previous one") {
            onUiThreadGet {
                val label = WLabel("Name")
                val old = WTextField()
                val new = WTextField()
                label.labelFor = old
                label.displayedMnemonic = 'N'.code
                label.labelFor = new
                listOf(hasLabeledBy(old), accessKeyOf(old), isLabeledBy(new, label), accessKeyOf(new))
            } shouldContainExactly listOf(false, 0, true, 'N'.code)
        }

        test("setting labelFor to null removes LabeledBy and the access key from the target") {
            onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                label.labelFor = field
                label.displayedMnemonic = 'N'.code
                label.labelFor = null
                listOf(hasLabeledBy(field), accessKeyOf(field), label.displayedMnemonic)
            } shouldContainExactly listOf(false, 0, 'N'.code)
        }

        test("if the target's access key or LabeledBy was later changed to another value, it is kept even when labelFor is removed") {
            onUiThreadGet {
                val label = WLabel("Name")
                val other = WLabel("Another name")
                val button = WButton("OK")
                label.labelFor = button
                label.displayedMnemonic = 'N'.code
                button.mnemonic = 'K'.code
                other.labelFor = button
                label.labelFor = null
                listOf(button.mnemonic, isLabeledBy(button, other))
            } shouldContainExactly listOf('K'.code, true)
        }

        test("pressing Alt and the displayedMnemonic key moves focus to the labelFor input field (as in Swing)") {
            val panel = onUiThreadGet { WPanel() }
            val (field, other) = onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                val other = WButton("Another")
                label.labelFor = field
                label.displayedMnemonic = 'N'.code
                listOf(label, field, other).forEach { panel.add(it) }
                field to other
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread { check(other.requestFocusInWindow()) { "could not move focus" } }
                TestKeyboard.pressWithAlt('N'.code, SCAN_N)
                eventually(3.seconds) { onUiThreadGet { field.hasFocus() } shouldBe true }
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("without displayedMnemonic, pressing Alt and the same key does not move focus (control for the previous test)") {
            val panel = onUiThreadGet { WPanel() }
            val (field, other) = onUiThreadGet {
                val label = WLabel("Name")
                val field = WTextField()
                val other = WButton("Another")
                label.labelFor = field
                listOf(label, field, other).forEach { panel.add(it) }
                field to other
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThread { check(other.requestFocusInWindow()) { "could not move focus" } }
                TestKeyboard.pressWithAlt('N'.code, SCAN_N)
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { field.hasFocus() to other.hasFocus() } shouldBe (false to true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }

    private companion object {
        /** Scan code of the N key. */
        const val SCAN_N = 0x31

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
