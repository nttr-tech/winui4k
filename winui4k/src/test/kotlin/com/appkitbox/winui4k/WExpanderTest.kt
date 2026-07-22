package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests verifying WExpander's expanded state and Expand / Collapse listeners. */
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

        test("expandDirection returns exactly the value that was set") {
            onUiThreadGet {
                val expander = WExpander()
                expander.expandDirection = ExpandDirection.UP
                expander.expandDirection
            } shouldBe ExpandDirection.UP
        }
    }
}
