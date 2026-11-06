package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests WExpander's expanded state, the Expanding / Collapsed listeners and the Swing ChangeListener-style addChangeListener.
 * Checks that ChangeListeners are called once per change in either direction, and that isExpanded has the new value when they are called.
 */
class WExpanderTest : FunSpec() {
    init {
        test("the header and content passed to the constructor can both be retrieved") {
            val (header, content, label) = onUiThreadGet {
                val label = WLabel("Content")
                val expander = WExpander("Advanced settings", label)
                Triple(expander.header, expander.content, label)
            }
            header shouldBe "Advanced settings"
            content shouldBeSameInstanceAs label
        }

        test("isExpanded returns exactly the value that was set (starts out collapsed)") {
            val (initial, expanded) = onUiThreadGet {
                val expander = WExpander()
                val initial = expander.isExpanded
                expander.isExpanded = true
                initial to expander.isExpanded
            }
            initial.shouldBeFalse()
            expanded.shouldBeTrue()
        }

        test("the Expand / Collapse listeners fire on every expand/collapse") {
            val events = mutableListOf<String>()
            onUiThread {
                val expander = WExpander("Expand/collapse test")
                expander.addExpandListener { events.add("expand") }
                expander.addCollapseListener { events.add("collapse") }
                expander.isExpanded = true
                expander.isExpanded = false
            }
            events shouldContainExactly listOf("expand", "collapse")
        }

        test("ChangeListeners are called once both when expanding and collapsing, isExpanded has the new value when called, and the source is the Expander") {
            val log = mutableListOf<String>()
            val sameSource = onUiThreadGet {
                val expander = WExpander("Change")
                var sources = true
                expander.addChangeListener(
                    WChangeListener { e ->
                        log += "isExpanded=${expander.isExpanded}"
                        sources = sources && e.source === expander
                    },
                )
                expander.isExpanded = true
                expander.isExpanded = false
                sources
            }
            log shouldContainExactly listOf("isExpanded=true", "isExpanded=false")
            sameSource.shouldBeTrue()
        }

        test("ChangeListeners are not called for settings that do not change the value, are no longer called once removed, and all are called in the order they were added") {
            val log = mutableListOf<String>()
            onUiThread {
                val expander = WExpander("Change")
                val first = WChangeListener { log += "first" }
                val second = WChangeListener { log += "second" }
                expander.addChangeListener(first)
                expander.addChangeListener(second)
                expander.getChangeListeners().toList() shouldContainExactly listOf(first, second)
                expander.isExpanded = false // Already collapsed, so no change
                log.shouldBeEmpty()
                expander.isExpanded = true
                log shouldContainExactly listOf("first", "second")
                expander.removeChangeListener(first)
                expander.isExpanded = false
                log shouldContainExactly listOf("first", "second", "second")
                expander.getChangeListeners().toList() shouldContainExactly listOf(second)
            }
        }

        test("ChangeListeners and Expand / Collapse listeners can be used together on the same expand/collapse, with the same counts as Expanding when expanding and Collapsed when collapsing") {
            val log = mutableListOf<String>()
            onUiThread {
                val expander = WExpander("Combined")
                expander.addExpandListener { log += "expand:${expander.isExpanded}" }
                expander.addCollapseListener { log += "collapse:${expander.isExpanded}" }
                expander.addChangeListener(WChangeListener { log += "change:${expander.isExpanded}" })
                expander.isExpanded = true
                expander.isExpanded = false
            }
            // Each listener sees the new value. ChangeListeners are called once per expand/collapse
            log.filter { it.startsWith("change") } shouldContainExactly listOf("change:true", "change:false")
            log.filter { it.startsWith("expand") } shouldContainExactly listOf("expand:true")
            log.filter { it.startsWith("collapse") } shouldContainExactly listOf("collapse:false")
        }

        test("ChangeListeners are called once per expand/collapse even for an Expander placed in the window") {
            val expander = onUiThreadGet { WExpander("Placed", WLabel("Content")) }
            val log = mutableListOf<Boolean>()
            onUiThread { expander.addChangeListener(WChangeListener { log += expander.isExpanded }) }
            UiTestHarness.attachAndAwaitLoaded(expander)
            try {
                onUiThread { expander.isExpanded = true }
                onUiThread { expander.isExpanded = false }
                log shouldContainExactly listOf(true, false)
            } finally {
                UiTestHarness.detach(expander)
            }
        }

        test("header is read back from the native side (empty by default, the string set, back to empty)") {
            onUiThreadGet {
                val expander = WExpander()
                val initial = expander.header
                expander.header = "Details"
                val set = expander.header
                expander.header = ""
                Triple(initial, set, expander.header)
            } shouldBe Triple("", "Details", "")
        }

        test("expandDirection returns exactly the value that was set") {
            onUiThreadGet {
                val expander = WExpander()
                expander.expandDirection = ExpandDirection.UP
                expander.expandDirection
            } shouldBe ExpandDirection.UP
        }
    }
}
