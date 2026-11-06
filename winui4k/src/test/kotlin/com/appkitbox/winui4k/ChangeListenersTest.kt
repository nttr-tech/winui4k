package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests the internal ChangeListeners (the list of a component's ChangeListeners) by passing a function that only
 * counts calls in place of the native registration. A WLabel is used as the owner (a component with a ListenerAnchor).
 */
class ChangeListenersTest : FunSpec() {
    /** A ChangeListeners that counts native registrations, and its owner. */
    private class Fixture {
        val owner = WLabel("owner")
        var nativeRegistrations = 0
        val listeners = ChangeListeners(owner) { nativeRegistrations++ }
    }

    init {
        test("the native handler is registered only once, when the first listener is added, and is not unregistered when all are removed") {
            val registrations = onUiThreadGet {
                val f = Fixture()
                val before = f.nativeRegistrations
                val a = WChangeListener { }
                f.listeners.add(a)
                f.listeners.add { }
                f.listeners.remove(a)
                listOf(before, f.nativeRegistrations)
            }
            registrations shouldContainExactly listOf(0, 1)
        }

        test("fire without listeners does nothing and does not register the native handler") {
            val registrations = onUiThreadGet {
                val f = Fixture()
                f.listeners.fire()
                f.nativeRegistrations
            }
            registrations shouldBe 0
        }

        test("fire passes one and the same event whose source is the owner to every listener in the order they were added") {
            val result = onUiThreadGet {
                val f = Fixture()
                val events = mutableListOf<WChangeEvent>()
                val calls = mutableListOf<String>()
                f.listeners.add {
                    calls += "a"
                    events += it
                }
                f.listeners.add {
                    calls += "b"
                    events += it
                }
                f.listeners.fire()
                Triple(calls, events.size == 2 && events[0] === events[1], events[0].source === f.owner)
            }
            result.first shouldContainExactly listOf("a", "b")
            result.second shouldBe true
            result.third shouldBe true
        }

        test("a listener added twice is called twice, and remove removes only one, from the end") {
            val counts = onUiThreadGet {
                val f = Fixture()
                var count = 0
                val listener = WChangeListener { count++ }
                f.listeners.add(listener)
                f.listeners.add(listener)
                f.listeners.fire()
                val twice = count
                f.listeners.remove(listener)
                f.listeners.fire()
                f.listeners.remove(listener)
                f.listeners.fire()
                f.listeners.remove(listener) // Does nothing when it is not added
                listOf(twice, count)
            }
            counts shouldContainExactly listOf(2, 3)
        }

        test("a listener removed during a notification still receives it, and a listener added during it is not called for it") {
            val calls = onUiThreadGet {
                val f = Fixture()
                val calls = mutableListOf<String>()
                val second = WChangeListener { calls += "second" }
                f.listeners.add {
                    calls += "first"
                    f.listeners.remove(second)
                    f.listeners.add { calls += "late" }
                }
                f.listeners.add(second)
                f.listeners.fire()
                val afterFirst = calls.toList()
                calls.clear()
                f.listeners.fire()
                afterFirst + "|" + calls
            }
            calls shouldContainExactly listOf("first", "second", "|", "first", "late")
        }

        test("a listener added with addAdapter can be removed by passing the original key to removeAdapter") {
            val counts = onUiThreadGet {
                val f = Fixture()
                var count = 0
                val key = Any()
                f.listeners.addAdapter(key) { count++ }
                f.listeners.fire()
                f.listeners.removeAdapter(key)
                f.listeners.fire()
                count
            }
            counts shouldBe 1
        }

        test("fireIfChanged notifies only when the values differ, and drops NaN versus NaN as the same value") {
            val result = onUiThreadGet {
                val f = Fixture()
                var count = 0
                f.listeners.add { count++ }
                val flags = listOf(
                    f.listeners.fireIfChanged(3.0, 3.0),
                    f.listeners.fireIfChanged(3.0, 4.0),
                    f.listeners.fireIfChanged(Double.NaN, Double.NaN),
                    f.listeners.fireIfChanged(null, "x"),
                )
                flags to count
            }
            result.first shouldContainExactly listOf(false, true, false, true)
            result.second shouldBe 2
        }

        test("toArray returns the added listeners in the order they were added") {
            val matches = onUiThreadGet {
                val f = Fixture()
                val a = WChangeListener { }
                val b = WChangeListener { }
                f.listeners.add(a)
                f.listeners.add(b)
                f.listeners.toArray().toList() == listOf(a, b)
            }
            matches shouldBe true
        }
    }
}
