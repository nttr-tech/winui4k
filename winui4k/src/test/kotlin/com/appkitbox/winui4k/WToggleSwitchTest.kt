package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/** Tests verifying WToggleSwitch's on/off state and Toggled listener. */
class WToggleSwitchTest : FunSpec() {
    init {
        test("isOn returns exactly the value that was set") {
            val (initial, on) = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Airplane mode")
                val initial = toggleSwitch.isOn
                toggleSwitch.isOn = true
                initial to toggleSwitch.isOn
            }
            initial.shouldBeFalse() // starts out off
            on.shouldBeTrue()
        }

        test("the ItemListener fires with the new value on every change to isOn") {
            val received = mutableListOf<Boolean>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                toggleSwitch.addItemListener { received.add(it) }
                toggleSwitch.isOn = true
                toggleSwitch.isOn = false
            }
            received shouldContainExactly listOf(true, false)
        }

        test("a listener removed via removeItemListener no longer fires") {
            val received = mutableListOf<Boolean>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                val listener: (Boolean) -> Unit = { received.add(it) }
                toggleSwitch.addItemListener(listener)
                toggleSwitch.isOn = true
                toggleSwitch.removeItemListener(listener)
                toggleSwitch.isOn = false
            }
            received shouldContainExactly listOf(true)
        }

        test("isSelected reads and writes the same value as isOn") {
            val (afterSelected, afterOff) = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                toggleSwitch.isSelected = true
                val afterSelected = toggleSwitch.isOn to toggleSwitch.isSelected
                toggleSwitch.isOn = false
                afterSelected to toggleSwitch.isSelected
            }
            afterSelected shouldBe (true to true)
            afterOff shouldBe false
        }

        test("WItemListener receives an ItemEvent of SELECTED when turned on and DESELECTED when turned off, with the switch as source") {
            val received = mutableListOf<WItemEvent>()
            val toggleSwitch = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Airplane mode")
                toggleSwitch.addItemListener(WItemListener { received.add(it) })
                toggleSwitch.isOn = true
                toggleSwitch.isSelected = false
                toggleSwitch
            }
            received.map { it.stateChange } shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            received.all { it.source === toggleSwitch && it.itemSelectable === toggleSwitch && it.item === toggleSwitch }.shouldBeTrue()
        }

        test("even if the first listener changes the state, later listeners of the same notification receive the state at the time of the notification (same order as Swing)") {
            val first = mutableListOf<Int>()
            val second = mutableListOf<Int>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                var flipped = false
                toggleSwitch.addItemListener(
                    WItemListener { e ->
                        first += e.stateChange
                        if (!flipped) {
                            flipped = true
                            toggleSwitch.isOn = false // Nested notifications are delivered first within this notification
                        }
                    },
                )
                toggleSwitch.addItemListener(WItemListener { e -> second += e.stateChange })
                toggleSwitch.isOn = true
            }
            first shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
            second shouldContainExactly listOf(WItemEvent.DESELECTED, WItemEvent.SELECTED)
        }

        test("removing the WItemListener with removeItemListener stops delivery") {
            val states = mutableListOf<Int>()
            onUiThread {
                val toggleSwitch = WToggleSwitch()
                val listener = WItemListener { states.add(it.stateChange) }
                toggleSwitch.addItemListener(listener)
                toggleSwitch.isOn = true
                toggleSwitch.removeItemListener(listener)
                toggleSwitch.isOn = false
            }
            states shouldContainExactly listOf(WItemEvent.SELECTED)
        }

        test("header is an empty string when unset, and a set string is read back from WinUI") {
            val values = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                val initial = toggleSwitch.header
                toggleSwitch.header = "Airplane mode"
                val set = toggleSwitch.header
                toggleSwitch.header = ""
                listOf(initial, set, toggleSwitch.header)
            }
            values shouldContainExactly listOf("", "Airplane mode", "")
        }

        test("the header passed to the constructor is read back as is") {
            onUiThreadGet { WToggleSwitch("Wi-Fi").header } shouldBe "Wi-Fi"
        }

        test("onContent and offContent return WinUI's default words (matching the display language), and set values are read back") {
            val values = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                val defaults = toggleSwitch.onContent to toggleSwitch.offContent
                toggleSwitch.onContent = "Enabled"
                toggleSwitch.offContent = "Disabled"
                defaults to (toggleSwitch.onContent to toggleSwitch.offContent)
            }
            // The defaults are words WinUI fills in for the display language (e.g. On / Off in English, and the localized
            // words in other display languages), so the values are not hard-coded
            values.first.first.isNotEmpty().shouldBeTrue()
            values.first.second.isNotEmpty().shouldBeTrue()
            (values.first.first != values.first.second).shouldBeTrue()
            values.second shouldBe ("Enabled" to "Disabled")
        }

        test("header, onContent and offContent read the native values, so values changed by another path are reflected") {
            val values = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Original")
                // Rewrite the native Header directly, without going through the wrapper's fields
                val boxed = PropertyValues.boxString("Native side")
                toggleSwitch.inspectable.call(
                    XamlInterop.IToggleSwitch_put_Header,
                    boxed.ptr,
                )
                boxed.release()
                toggleSwitch.header
            }
            values shouldBe "Native side"
        }

        test("doClick toggles on and off, and ItemListener receives SELECTED and DESELECTED") {
            val states = mutableListOf<Int>()
            val isOnAfter = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Notifications")
                toggleSwitch.addItemListener(WItemListener { states.add(it.stateChange) })
                toggleSwitch.doClick()
                val afterFirst = toggleSwitch.isOn
                toggleSwitch.doClick()
                afterFirst to toggleSwitch.isOn
            }
            isOnAfter shouldBe (true to false)
            states shouldContainExactly listOf(WItemEvent.SELECTED, WItemEvent.DESELECTED)
        }

        test("doClick on a disabled switch does nothing") {
            val (isOn, calls) = onUiThreadGet {
                val toggleSwitch = WToggleSwitch("Notifications")
                var calls = 0
                toggleSwitch.addItemListener { calls++ }
                toggleSwitch.isEnabled = false
                toggleSwitch.doClick()
                toggleSwitch.isOn to calls
            }
            isOn shouldBe false
            calls shouldBe 0
        }

        test("mnemonic is 0 when unset, and setMnemonic(Char) stores a letter as the uppercase key code") {
            val values = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                val initial = toggleSwitch.mnemonic
                toggleSwitch.setMnemonic('w')
                val lower = toggleSwitch.mnemonic
                toggleSwitch.mnemonic = 55
                listOf(initial, lower, toggleSwitch.mnemonic)
            }
            values shouldContainExactly listOf(0, 87, 55)
        }

        test("setting mnemonic to 0 or the NUL character clears the access key, and anything other than letters and digits throws IllegalArgumentException") {
            val values = onUiThreadGet {
                val toggleSwitch = WToggleSwitch()
                toggleSwitch.mnemonic = 67
                toggleSwitch.mnemonic = 0
                val byInt = toggleSwitch.mnemonic
                toggleSwitch.setMnemonic('C')
                toggleSwitch.setMnemonic('\u0000')
                val byChar = toggleSwitch.mnemonic
                toggleSwitch.mnemonic = 69
                val failed = listOf(97, -1, 33).map {
                    runCatching { toggleSwitch.mnemonic = it }.exceptionOrNull() is IllegalArgumentException
                } + runCatching { toggleSwitch.setMnemonic('!') }.exceptionOrNull().let { it is IllegalArgumentException }
                Triple(byInt to byChar, failed, toggleSwitch.mnemonic)
            }
            values.first shouldBe (0 to 0)
            values.second shouldContainExactly listOf(true, true, true, true)
            values.third shouldBe 69 // The value does not change on failure
        }
    }
}
