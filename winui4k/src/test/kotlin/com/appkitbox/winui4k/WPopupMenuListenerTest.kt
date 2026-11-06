package com.appkitbox.winui4k

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests only the WPopupMenuListener / WPopupMenuEvent / WPopupMenuAdapter types (not connected to any control; no window is used).
 * The shape seen from Java is in WPopupMenuListenerForJavaTest.
 */
class WPopupMenuListenerTest : FunSpec() {
    init {
        test("the source of WPopupMenuEvent is the object passed in") {
            val source = Any()
            (WPopupMenuEvent(source).source === source) shouldBe true
        }

        test("WPopupMenuAdapter allows overriding just one method, and the rest do nothing and throw nothing") {
            val calls = mutableListOf<String>()
            val adapter = object : WPopupMenuAdapter() {
                override fun popupMenuCanceled(e: WPopupMenuEvent) {
                    calls += "canceled"
                }
            }
            val event = WPopupMenuEvent(Any())
            adapter.popupMenuWillBecomeVisible(event)
            adapter.popupMenuCanceled(event)
            adapter.popupMenuWillBecomeInvisible(event)
            calls shouldContainExactly listOf("canceled")
        }

        test("WPopupMenuListener receives its three methods in the order they are called") {
            val calls = mutableListOf<String>()
            val listener: WPopupMenuListener = object : WPopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                    calls += "visible"
                }

                override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                    calls += "invisible"
                }

                override fun popupMenuCanceled(e: WPopupMenuEvent) {
                    calls += "canceled"
                }
            }
            val event = WPopupMenuEvent(Any())
            listener.popupMenuWillBecomeVisible(event)
            listener.popupMenuCanceled(event)
            listener.popupMenuWillBecomeInvisible(event)
            calls shouldContainExactly listOf("visible", "canceled", "invisible")
        }
    }
}
