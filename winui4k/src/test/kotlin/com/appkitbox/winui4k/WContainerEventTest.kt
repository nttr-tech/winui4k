package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests the ContainerListener-style [WContainer.addContainerListener]. The expected values follow the behavior of Swing's Container
 * (called synchronously after add / remove, removeAll one at a time from the last child, componentRemoved to the old parent on reparenting).
 */
class WContainerEventTest : FunSpec() {
    /** A listener that records each received event in [log] in the form "container name:added:child name". */
    private class Recorder(private val log: MutableList<String>) : WContainerListener {
        val events = mutableListOf<WContainerEvent>()

        override fun componentAdded(e: WContainerEvent) {
            events += e
            log += "${e.container.name}:added:${e.child.name}"
        }

        override fun componentRemoved(e: WContainerEvent) {
            events += e
            log += "${e.container.name}:removed:${e.child.name}"
        }
    }

    private fun panel(name: String): WPanel = WPanel().also { it.name = name }

    private fun label(name: String): WLabel = WLabel(name).also { it.name = name }

    init {
        test("componentAdded / componentRemoved are called after add and remove, and the event has the ID, container and child") {
            onUiThread {
                val log = mutableListOf<String>()
                val container = panel("p")
                val recorder = Recorder(log)
                container.addContainerListener(recorder)
                val child = label("a")
                container.add(child)
                container.remove(child)
                log shouldContainExactly listOf("p:added:a", "p:removed:a")
                val added = recorder.events[0]
                added.id shouldBe WContainerEvent.COMPONENT_ADDED
                added.source shouldBeSameInstanceAs container
                added.container shouldBeSameInstanceAs container
                added.component shouldBeSameInstanceAs container
                added.child shouldBeSameInstanceAs child
                recorder.events[1].id shouldBe WContainerEvent.COMPONENT_REMOVED
            }
        }

        test("inside the listener, the child list and parent have already changed") {
            onUiThread {
                val container = panel("p")
                val child = label("a")
                val seen = mutableListOf<String>()
                container.addContainerListener(
                    object : WContainerAdapter() {
                        override fun componentAdded(e: WContainerEvent) {
                            seen += "added:${container.componentCount}:${e.child.parent === container}"
                        }

                        override fun componentRemoved(e: WContainerEvent) {
                            seen += "removed:${container.componentCount}:${e.child.parent == null}"
                        }
                    },
                )
                container.add(child)
                container.remove(0)
                seen shouldContainExactly listOf("added:1:true", "removed:0:true")
            }
        }

        test("removeAll calls componentRemoved one at a time from the last child (same as Swing)") {
            onUiThread {
                val log = mutableListOf<String>()
                val container = panel("p")
                listOf("a", "b", "c").forEach { container.add(label(it)) }
                container.addContainerListener(Recorder(log))
                container.removeAll()
                log shouldContainExactly listOf("p:removed:c", "p:removed:b", "p:removed:a")
            }
        }

        test("adding a child with a parent to another container delivers the old container's componentRemoved first") {
            onUiThread {
                val log = mutableListOf<String>()
                val first = panel("first")
                val second = panel("second")
                val child = label("a")
                first.add(child)
                first.addContainerListener(Recorder(log))
                second.addContainerListener(Recorder(log))
                second.add(child)
                // Moving it to a single-child host also removes it from the original container
                WBorder().child = child
                log shouldContainExactly listOf("first:removed:a", "second:added:a", "second:removed:a")
            }
        }

        test("no events arrive for an add with an out-of-range position or an add with constraints the manager rejects") {
            onUiThread {
                val log = mutableListOf<String>()
                val container = WLayoutPanel(WBorderLayout()).also { it.name = "p" }
                container.addContainerListener(Recorder(log))
                shouldThrow<IllegalArgumentException> { container.add(label("a"), 3) }
                shouldThrow<IllegalArgumentException> { container.add(label("b"), "Nowhere") }
                log.shouldBeEmpty()
            }
        }

        test("a listener added twice is called twice and once after removing it once; null and listeners that were not added are ignored") {
            onUiThread {
                val log = mutableListOf<String>()
                val container = panel("p")
                val recorder = Recorder(log)
                container.addContainerListener(recorder)
                container.addContainerListener(recorder)
                container.addContainerListener(null)
                container.containerListeners.toList() shouldContainExactly listOf(recorder, recorder)
                container.add(label("a"))
                container.removeContainerListener(recorder)
                container.removeContainerListener(Recorder(log))
                container.removeContainerListener(null)
                container.add(label("b"))
                log shouldContainExactly listOf("p:added:a", "p:added:a", "p:added:b")
                container.removeContainerListener(recorder)
                container.containerListeners.toList().shouldBeEmpty()
            }
        }
    }
}
