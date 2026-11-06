package com.appkitbox.winui4k

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for [WInputMap] and [WActionMap], like InputMap / ActionMap (WinUI is not used).
 * The expected values are decided from the contracts of Swing's InputMap / ActionMap (look up in the parent, put of null is remove, keys of an empty map is null).
 */
@Tags("Pure")
class WInputMapTest : FunSpec() {
    private val ctrlS = WKeyStroke.getKeyStroke("ctrl S")!!
    private val f7 = WKeyStroke.getKeyStroke("F7")!!
    private val escape = WKeyStroke.getKeyStroke("ESCAPE")!!

    private fun action(): WAction = object : WAbstractAction() {
        override fun actionPerformed(e: WActionEvent) = Unit
    }

    init {
        context("Defaults") {
            test("a new map has no parent, size 0, and null keys and allKeys") {
                val inputs = WInputMap()
                inputs.parent.shouldBeNull()
                inputs.size() shouldBe 0
                inputs.keys().shouldBeNull()
                inputs.allKeys().shouldBeNull()
                val actions = WActionMap()
                actions.parent.shouldBeNull()
                actions.size() shouldBe 0
                actions.keys().shouldBeNull()
                actions.allKeys().shouldBeNull()
            }
        }

        context("Reading and writing") {
            test("a mapping put can be looked up with get, put of the same keystroke replaces the value, and put of null is the same as remove") {
                val inputs = WInputMap()
                inputs.put(ctrlS, "save")
                inputs[ctrlS] shouldBe "save"
                inputs[WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_MASK)] shouldBe "save"
                inputs.put(ctrlS, "saveAll")
                inputs[ctrlS] shouldBe "saveAll"
                inputs.size() shouldBe 1
                inputs.put(ctrlS, null)
                inputs[ctrlS].shouldBeNull()
                inputs.keys()!!.toList().shouldBeEmpty() // keys of a map that has been put to once is an empty array (the same as Swing)
            }

            test("keystrokes not in the map are looked up in the parent, its own mappings take precedence over the parent, and remove and clear do not change the parent") {
                val parent = WInputMap().apply {
                    put(ctrlS, "parentSave")
                    put(escape, "cancel")
                }
                val child = WInputMap().apply {
                    put(ctrlS, "save")
                    put(f7, "find")
                    this.parent = parent
                }
                child[ctrlS] shouldBe "save"
                child[escape] shouldBe "cancel"
                child.keys()!!.toList() shouldContainExactly listOf(ctrlS, f7)
                child.allKeys()!!.toList() shouldContainExactly listOf(ctrlS, f7, escape)
                child.remove(ctrlS)
                child[ctrlS] shouldBe "parentSave"
                child.clear()
                child[escape] shouldBe "cancel"
                child.allKeys()!!.toList() shouldContainExactly listOf(ctrlS, escape)
                parent.size() shouldBe 2
            }

            test("an ActionMap looks up a put action with get, looks it up in the parent if absent, and put of null is the same as remove") {
                val save = action()
                val cancel = action()
                val parent = WActionMap().apply { put("cancel", cancel) }
                val actions = WActionMap().apply {
                    put("save", save)
                    this.parent = parent
                }
                actions["save"] shouldBeSameInstanceAs save
                actions["cancel"] shouldBeSameInstanceAs cancel
                actions.keys()!!.toList() shouldContainExactly listOf("save")
                actions.allKeys()!!.toList() shouldContainExactly listOf("save", "cancel")
                actions.put("save", null)
                actions["save"].shouldBeNull()
                actions.size() shouldBe 0
            }
        }

        context("Boundaries and exceptions") {
            test("put with a null keystroke or key does nothing, and get returns null") {
                val inputs = WInputMap()
                inputs.put(null, "save")
                inputs.keys().shouldBeNull()
                inputs[null].shouldBeNull()
                val actions = WActionMap()
                actions.put(null, action())
                actions.keys().shouldBeNull()
                actions[null].shouldBeNull()
            }

            test("making itself or a map that leads back to itself through its parents the parent throws IllegalArgumentException") {
                val first = WInputMap()
                val second = WInputMap().apply { parent = first }
                shouldThrow<IllegalArgumentException> { first.parent = first }
                shouldThrow<IllegalArgumentException> { first.parent = second }
                first.parent.shouldBeNull()
                val firstActions = WActionMap()
                val secondActions = WActionMap().apply { parent = firstActions }
                shouldThrow<IllegalArgumentException> { firstActions.parent = secondActions }
            }
        }
    }
}
