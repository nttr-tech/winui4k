package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests [WComponent.putClientProperty] / [WComponent.getClientProperty], the counterparts of
 * JComponent.putClientProperty / getClientProperty.
 * The values are only kept in a map on the Kotlin side, so they do not affect the component's (native) state.
 */
class WComponentClientPropertyTest : FunSpec() {
    private data class Key(val name: String)

    init {
        test("a stored value can be retrieved with the same key, and a key never stored gives null") {
            onUiThreadGet {
                val button = WButton("OK")
                button.putClientProperty("owner", "alice")
                Triple(button.getClientProperty("owner"), button.getClientProperty("other"), button.getClientProperty("OWNER"))
            } shouldBe Triple("alice", null, null)
        }

        test("storing under the same key again replaces the value") {
            onUiThreadGet {
                val button = WButton("OK")
                button.putClientProperty("count", 1)
                button.putClientProperty("count", 2)
                button.getClientProperty("count")
            } shouldBe 2
        }

        test("storing null removes the value for that key (other keys remain)") {
            val (removed, kept) = onUiThreadGet {
                val button = WButton("OK")
                button.putClientProperty("a", "x")
                button.putClientProperty("b", "y")
                button.putClientProperty("a", null)
                button.getClientProperty("a") to button.getClientProperty("b")
            }
            removed.shouldBeNull()
            kept shouldBe "y"
        }

        test("keys are compared with equals (retrievable with a different instance of the same string and with a data class key)") {
            onUiThreadGet {
                val button = WButton("OK")
                button.putClientProperty(String(charArrayOf('k', 'e', 'y')), 1)
                button.putClientProperty(Key("name"), 2)
                button.getClientProperty("key") to button.getClientProperty(Key("name"))
            } shouldBe (1 to 2)
        }

        test("values are returned with their original type and identity (neither copied nor converted to strings)") {
            val list = mutableListOf("a", "b")
            val got = onUiThreadGet {
                val label = WLabel("L")
                label.putClientProperty("list", list)
                label.getClientProperty("list")
            }
            got shouldBeSameInstanceAs list
        }

        test("properties are independent per component") {
            onUiThreadGet {
                val first = WButton("1")
                val second = WButton("2")
                first.putClientProperty("k", "first")
                second.getClientProperty("k") to first.getClientProperty("k")
            } shouldBe (null to "first")
        }

        test("properties can also be attached to components that are not WControls (WLabel, WPanel)") {
            onUiThreadGet {
                val label = WLabel("L")
                val panel = WPanel()
                label.putClientProperty("k", "label")
                panel.putClientProperty("k", "panel")
                label.getClientProperty("k") to panel.getClientProperty("k")
            } shouldBe ("label" to "panel")
        }
    }
}
