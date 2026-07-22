package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Tests verifying WPasswordField (PasswordBox)'s password handling and PasswordChanged listener. */
class WPasswordFieldTest : FunSpec() {
    init {
        test("password returns exactly the string that was set") {
            onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.password = "SecretPassword123"
                passwordField.password
            } shouldBe "SecretPassword123"
        }

        test("the PasswordChanged listener fires with the new value on every change to password") {
            // PasswordChanged fires via the message loop for a PasswordBox on the visual tree
            val received = LinkedBlockingQueue<String>()
            val passwordField = onUiThreadGet {
                WPasswordField().also { it.addPasswordChangedListener { password -> received.add(password) } }
            }
            UiTestHarness.attach(passwordField)
            try {
                onUiThread { passwordField.password = "a" }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "a"
                onUiThread { passwordField.password = "ab" }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "ab"
            } finally {
                UiTestHarness.detach(passwordField)
            }
        }

        test("passwordChar, passwordRevealMode, and maxLength return exactly the values that were set") {
            val (char, mode, maxLength) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.passwordChar = "*"
                passwordField.passwordRevealMode = PasswordRevealMode.VISIBLE
                passwordField.maxLength = 16
                Triple(passwordField.passwordChar, passwordField.passwordRevealMode, passwordField.maxLength)
            }
            char shouldBe "*"
            mode shouldBe PasswordRevealMode.VISIBLE
            maxLength shouldBe 16
        }

        test("the placeholder text passed to the constructor can be retrieved via placeholderText") {
            onUiThreadGet { WPasswordField("Enter your password").placeholderText } shouldBe "Enter your password"
        }
    }
}
