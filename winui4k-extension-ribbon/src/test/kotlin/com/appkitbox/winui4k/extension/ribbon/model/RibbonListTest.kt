package com.appkitbox.winui4k.extension.ribbon.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests for RibbonList, a list that notifies changes (no UI required). */
class RibbonListTest : FunSpec() {
    /** Records the change notifications of [list] as strings of the form "type index elements oldElements". */
    private fun record(list: RibbonList<String>): MutableList<String> {
        val events = mutableListOf<String>()
        list.addListListener { e -> events += "${e.type} ${e.index} ${e.elements} ${e.oldElements}" }
        return events
    }

    init {
        synchronousModelNotifications()

        test("add, insert, remove, and replace are each notified with position and elements") {
            val list = RibbonList<String>()
            val events = record(list)
            list += "a"
            list.add(0, "b")
            list[1] = "c"
            list.removeAt(0)
            list shouldContainExactly listOf("c")
            events shouldContainExactly listOf(
                "ADDED 0 [a] []",
                "ADDED 0 [b] []",
                "REPLACED 1 [c] [a]",
                "REMOVED 0 [b] []",
            )
        }

        test("addAll and clear are notified only once") {
            val list = RibbonList(listOf("x"))
            val events = record(list)
            list.addAll(listOf("a", "b"))
            list.clear()
            list.shouldBeEmpty()
            events shouldContainExactly listOf("ADDED 1 [a, b] []", "REMOVED 0 [x, a, b] []")
        }

        test("an empty addAll and clearing an empty list do not notify") {
            val list = RibbonList<String>()
            val events = record(list)
            list.addAll(emptyList()) shouldBe false
            list.clear()
            events.shouldBeEmpty()
        }

        test("replacing with the same element does not notify") {
            val element = "a"
            val list = RibbonList(listOf(element))
            val events = record(list)
            list[0] = element
            events.shouldBeEmpty()
        }

        test("move relocates an element by removing and adding it, and a range removal is notified only once") {
            val list = RibbonList(listOf("a", "b", "c", "d"))
            val events = record(list)
            list.move(0, 2)
            list shouldContainExactly listOf("b", "c", "a", "d")
            list.subList(1, 3).clear()
            list shouldContainExactly listOf("b", "d")
            events shouldContainExactly listOf("REMOVED 0 [a] []", "ADDED 2 [a] []", "REMOVED 1 [c, a] []")
        }

        test("a removed listener is not notified") {
            val list = RibbonList<String>()
            val events = mutableListOf<String>()
            val listener = RibbonListListener<String> { events += it.type.name }
            list.addListListener(listener)
            list += "a"
            list.removeListListener(listener)
            list += "b"
            events shouldContainExactly listOf("ADDED")
        }
    }
}
