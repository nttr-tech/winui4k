package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Smoke tests of notifications ([WAppNotification], [WAppNotificationManager]) and jump lists ([WJumpList], [WJumpListItem]).
 * Both may not work without a package ID or registration, so the bodies are not run in environments where isSupported is false
 * (they also do not actually show notifications or save jump lists, so as not to change the user's environment).
 * The shape seen from Java is tested in WShellSmokeForJavaTest.
 */
class WShellSmokeTest : FunSpec() {
    init {
        test("isSupported returns a boolean without exceptions") {
            onUiThreadGet { WAppNotificationManager.isSupported to WJumpList.isSupported } // Must not throw
        }

        test("WAppNotification can be built by chaining body text, attribution, buttons, scenario and so on, and can be built") {
            if (!WAppNotificationManager.isSupported) return@test
            onUiThread {
                val notification = WAppNotification("Title")
                    .addText("Body")
                    .setAttributionText("Source")
                    .addArgument("action", "open")
                    .addButton("Open", "action" to "open", "id" to "7")
                    .setScenario(NotificationScenario.DEFAULT)
                    .setDuration(NotificationDuration.DEFAULT)
                    .setTag("smoke")
                    .setGroup("smoke-group")
                notification.build().release()
            }
        }

        test("WAppNotificationManager.setting returns one of the NotificationSetting values") {
            if (!WAppNotificationManager.isSupported) return@test
            val setting = onUiThreadGet { WAppNotificationManager.setting }
            NotificationSetting.entries.contains(setting).shouldBeTrue()
        }

        test("WJumpListItem.of returns the launch arguments and display name, and the description and group name read back the written values. A separator is isSeparator") {
            if (!WJumpList.isSupported) return@test
            onUiThread {
                val item = WJumpListItem.of("--open=1", "Open")
                item.arguments shouldBe "--open=1"
                item.displayName shouldBe "Open"
                item.isSeparator.shouldBeFalse()
                item.displayName = "Alias"
                item.description = "Description"
                item.groupName = "Group"
                item.displayName shouldBe "Alias"
                item.description shouldBe "Description"
                item.groupName shouldBe "Group"
                WJumpListItem.separator().isSeparator.shouldBeTrue()
            }
        }

        test("WJumpList can add to the loaded list, becomes empty with removeAll, and reads back systemGroupKind (without saving)") {
            if (!WJumpList.isSupported) return@test
            // In some environments without a package ID, LoadCurrentAsync fails (HRESULT 0x80040F07). The contents cannot be checked in those environments
            val loaded = runCatching { onUiThreadGet { WJumpList.load() } }
            if (loaded.isFailure) {
                generateSequence(loaded.exceptionOrNull()) { it.cause }.any { it is WindowsRuntimeException && it.hresult == JUMP_LIST_UNAVAILABLE }.shouldBeTrue()
                return@test
            }
            onUiThread {
                val list = loaded.getOrThrow()
                val original = list.systemGroupKind
                list.removeAll()
                list.items.size shouldBe 0
                list.add(WJumpListItem.of("a", "A"))
                list.add(WJumpListItem.separator())
                list.items.map { it.isSeparator } shouldBe listOf(false, true)
                list.items.first().arguments shouldBe "a"
                list.systemGroupKind = JumpListSystemGroupKind.RECENT
                list.systemGroupKind shouldBe JumpListSystemGroupKind.RECENT
                list.systemGroupKind = original
                list.removeAll()
                list.items.size shouldBe 0
            }
        }
    }

    private companion object {
        const val JUMP_LIST_UNAVAILABLE = 0x80040F07.toInt()
    }
}
