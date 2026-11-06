package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

/**
 * Tests the ComponentListener-style [WComponent.addComponentListener]. Places a box at absolute coordinates in an outer Canvas,
 * changes its size, position and visibility, and checks that the same kinds of events as Swing arrive once each (and that moving or hiding only the parent does not reach the child).
 * Events arrive after a layout or on the next turn of the UI thread, so the test waits for the record to match and checks that no extra notifications arrive after waiting a little longer.
 */
class WComponentEventTest : FunSpec() {
    /** A scene with an intermediate Canvas inside an outer Canvas and a box inside that. [events] records what the listener received. */
    private class Scene {
        val outer = WCanvas().also {
            it.width = 400.0
            it.height = 300.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
        }
        val middle = WCanvas().also {
            it.width = 200.0
            it.height = 200.0
        }
        val box = WLabel("box").also {
            it.width = 40.0
            it.height = 20.0
        }
        val events = mutableListOf<String>()
        val received = mutableListOf<WComponentEvent>()

        init {
            outer.add(middle, 0.0, 0.0)
            middle.add(box, 0.0, 0.0)
        }

        fun record(): WComponentListener = object : WComponentListener {
            override fun componentResized(e: WComponentEvent) = log("resized", e)

            override fun componentMoved(e: WComponentEvent) = log("moved", e)

            override fun componentShown(e: WComponentEvent) = log("shown", e)

            override fun componentHidden(e: WComponentEvent) = log("hidden", e)

            private fun log(name: String, e: WComponentEvent) {
                events += name
                received += e
            }
        }
    }

    /** Places the scene and finishes the layout before adding the listener to the box (so notifications from the first layout are not mixed in). */
    private fun showScene(): Pair<Scene, WComponentListener> {
        val scene = onUiThreadGet { Scene() }
        UiTestHarness.attachAndAwaitLoaded(scene.outer)
        val listener = onUiThreadGet {
            scene.outer.updateLayout()
            scene.record().also { scene.box.addComponentListener(it) }
        }
        Thread.sleep(SETTLE_MILLIS)
        onUiThread { scene.events.clear() }
        return scene to listener
    }

    /** Waits until the record becomes [expected], and checks that it does not change after waiting a little longer. */
    private suspend fun awaitEvents(scene: Scene, expected: List<String>) {
        eventually(3.seconds) { onUiThreadGet { scene.events.toList() } shouldBe expected }
        Thread.sleep(SETTLE_MILLIS)
        onUiThreadGet { scene.events.toList() } shouldContainExactly expected
    }

    init {
        test("changing the size delivers componentResized once, and the event's ID and source are the box") {
            val (scene, _) = showScene()
            try {
                onUiThread { scene.box.width = 80.0 }
                awaitEvents(scene, listOf("resized"))
                onUiThread {
                    val event = scene.received.single()
                    event.id shouldBe WComponentEvent.COMPONENT_RESIZED
                    event.source shouldBeSameInstanceAs scene.box
                    event.component shouldBeSameInstanceAs scene.box
                }
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("changing the position relative to the parent delivers componentMoved once, and moving only the parent does not") {
            val (scene, _) = showScene()
            try {
                onUiThread { scene.middle.setLocation(scene.box, 40.0, 20.0) }
                awaitEvents(scene, listOf("moved"))
                onUiThread { scene.received.single().id shouldBe WComponentEvent.COMPONENT_MOVED }
                onUiThread { scene.outer.setLocation(scene.middle, 60.0, 60.0) }
                awaitEvents(scene, listOf("moved"))
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("changing isVisible delivers componentHidden / componentShown once each, after returning from the call that changed it") {
            val (scene, _) = showScene()
            try {
                onUiThread {
                    scene.box.isVisible = false
                    scene.box.isVisible = false // Assigning an unchanged value does not trigger it
                    scene.events.shouldBeEmpty() // Called later, as in Swing
                }
                awaitEvents(scene, listOf("hidden"))
                onUiThread { scene.box.isVisible = true }
                awaitEvents(scene, listOf("hidden", "shown"))
                onUiThread {
                    scene.received.map { it.id } shouldContainExactly
                        listOf(WComponentEvent.COMPONENT_HIDDEN, WComponentEvent.COMPONENT_SHOWN)
                }
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("hiding the parent does not deliver componentHidden to the child (same as Swing)") {
            val (scene, _) = showScene()
            try {
                onUiThread { scene.middle.isVisible = false }
                Thread.sleep(SETTLE_MILLIS)
                onUiThread { scene.middle.isVisible = true }
                awaitEvents(scene, emptyList())
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("after removing all listeners, changing the size, position or visibility delivers nothing") {
            val (scene, listener) = showScene()
            try {
                onUiThread {
                    scene.box.removeComponentListener(listener)
                    scene.box.componentListeners.toList().shouldBeEmpty()
                    scene.box.width = 90.0
                    scene.middle.setLocation(scene.box, 10.0, 10.0)
                    scene.box.isVisible = false
                }
                awaitEvents(scene, emptyList())
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("a listener added twice is called twice and once after removing it once; null and listeners that were not added are ignored") {
            val (scene, listener) = showScene()
            try {
                onUiThread {
                    scene.box.addComponentListener(listener)
                    scene.box.addComponentListener(null)
                    scene.box.componentListeners.toList() shouldContainExactly listOf(listener, listener)
                    scene.box.width = 60.0
                }
                awaitEvents(scene, listOf("resized", "resized"))
                onUiThread {
                    scene.box.removeComponentListener(listener)
                    scene.box.removeComponentListener(object : WComponentAdapter() {})
                    scene.box.removeComponentListener(null)
                    scene.box.width = 70.0
                }
                awaitEvents(scene, listOf("resized", "resized", "resized"))
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
