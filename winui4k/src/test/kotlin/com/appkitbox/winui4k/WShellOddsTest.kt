package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Verification of WDisplayArea.all / bounds, WAppNotificationManager.addActionListener and WBadgeNotification.setCount(0).
 * Notifications and badges may not work in environments without a package ID or notification permission, so those parts are not run
 * in environments where WAppNotificationManager.isSupported is false. The shape seen from Java is tested in WShellOddsForJavaTest.
 */
@Tags("Integration")
class WShellOddsTest : FunSpec() {
    init {
        test("WDisplayArea.all returns one or more, exactly one of which is the primary, at the origin (0, 0) (Windows specification)") {
            val areas = onUiThreadGet { WDisplayArea.all() }
            areas.shouldNotBeEmpty()
            areas.count { it.isPrimary } shouldBe 1
            val primary = onUiThreadGet { WDisplayArea.all().first { it.isPrimary }.bounds }
            primary.x shouldBe 0
            primary.y shouldBe 0
            primary.width shouldBeGreaterThan 0
            primary.height shouldBeGreaterThan 0
        }

        test("WDisplayArea.bounds equals outerBounds, and the work area fits inside it") {
            onUiThreadGet {
                WDisplayArea.all().map { Triple(it.bounds, it.outerBounds, it.workArea) }
            }.forEach { (bounds, outer, work) ->
                bounds shouldBe outer
                (work.x >= bounds.x && work.y >= bounds.y) shouldBe true
                (work.x + work.width <= bounds.x + bounds.width) shouldBe true
                (work.y + work.height <= bounds.y + bounds.height) shouldBe true
            }
        }

        test("the primary in WDisplayArea.all refers to the same area as primary()") {
            val (fromAll, fromPrimary) = onUiThreadGet {
                WDisplayArea.all().first { it.isPrimary }.bounds to WDisplayArea.primary().bounds
            }
            fromAll shouldBe fromPrimary
        }

        test("addActionListener calls listeners in registration order with the notification argument as actionCommand, and remove removes them") {
            if (!WAppNotificationManager.isSupported) return@test
            val seen = mutableListOf<String>()
            val sources = mutableListOf<Any>()
            val first: (WActionEvent) -> Unit = {
                seen += "1:${it.actionCommand}"
                sources += it.source
            }
            val second: (WActionEvent) -> Unit = { seen += "2:${it.actionCommand}" }
            onUiThread {
                WAppNotificationManager.addActionListener(first)
                WAppNotificationManager.addActionListener(second)
                WAppNotificationManager.deliverInvoked("action=join;id=7")
                WAppNotificationManager.removeActionListener(first)
                WAppNotificationManager.deliverInvoked("action=later")
                WAppNotificationManager.removeActionListener(second)
                WAppNotificationManager.deliverInvoked("action=none")
            }
            seen shouldContainExactly listOf("1:action=join;id=7", "2:action=join;id=7", "2:action=later")
            sources.single() shouldBeSameInstanceAs WAppNotificationManager
        }

        test("WBadgeNotification.setCount(0) goes through without exceptions as a call that removes the badge, and negative values are rejected") {
            if (!WAppNotificationManager.isSupported) return@test
            shouldThrow<IllegalArgumentException> { WBadgeNotification.setCount(-1) }
            // Without a package ID, badges fail with E_ILLEGAL_METHOD_CALL (0x8000000E). In that environment the removal cannot be observed
            val unpackaged = runCatching { onUiThread { WBadgeNotification.setCount(3) } }.exceptionOrNull()
                ?.let { generateSequence(it) { e -> e.cause }.any { e -> e is WindowsRuntimeException && e.hresult == ILLEGAL_METHOD_CALL } }
                ?: false
            if (unpackaged) return@test
            onUiThread {
                WBadgeNotification.setCount(3)
                WBadgeNotification.setCount(0)
            }
        }
    }

    private companion object {
        const val ILLEGAL_METHOD_CALL = 0x8000000E.toInt()
    }
}
