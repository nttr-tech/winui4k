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
            // PasswordChanged fires via the message loop once the PasswordBox has been loaded (template applied)
            val received = LinkedBlockingQueue<String>()
            val passwordField = onUiThreadGet {
                WPasswordField().also { it.addPasswordChangedListener { password -> received.add(password) } }
            }
            UiTestHarness.attachAndAwaitLoaded(passwordField)
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

        test("the default passwordRevealMode is HIDDEN, and setting it back to PEEK gives the eye-button mode (Swing's JPasswordField has no eye button)") {
            val (initial, peek) = onUiThreadGet {
                val passwordField = WPasswordField()
                val initial = passwordField.passwordRevealMode
                passwordField.passwordRevealMode = PasswordRevealMode.PEEK
                initial to passwordField.passwordRevealMode
            }
            initial shouldBe PasswordRevealMode.HIDDEN
            peek shouldBe PasswordRevealMode.PEEK
        }

        test("getPassword returns an array of the entered password's characters, and modifying the array does not change the field") {
            val (chars, afterZeroing, second) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.password = "xyabc"
                val chars = passwordField.getPassword()
                chars.fill('\u0000') // Swing practice: fill the array with zeros after use
                Triple(chars, passwordField.password, passwordField.getPassword())
            }
            chars.toList() shouldBe List(5) { '\u0000' }
            afterZeroing shouldBe "xyabc"
            second.concatToString() shouldBe "xyabc"
            (second === chars) shouldBe false
        }

        test("getPassword returns a zero-length array for an empty field") {
            onUiThreadGet { WPasswordField().getPassword().size } shouldBe 0
        }

        test("setText sets the password the same as password, and null empties it") {
            val (set, cleared) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.setText("pw1")
                val set = passwordField.password
                passwordField.setText(null)
                set to passwordField.password
            }
            set shouldBe "pw1"
            cleared shouldBe ""
        }

        test("echoChar defaults to PasswordBox's echo character \"●\", and echoCharIsSet is true") {
            val (char, isSet) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.echoChar to passwordField.echoCharIsSet()
            }
            char shouldBe '●'
            isSet shouldBe true
        }

        test("setting a character to echoChar makes it passwordChar and stays HIDDEN") {
            val (char, passwordChar, mode) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.echoChar = '*'
                Triple(passwordField.echoChar, passwordField.passwordChar, passwordField.passwordRevealMode)
            }
            char shouldBe '*'
            passwordChar shouldBe "*"
            mode shouldBe PasswordRevealMode.HIDDEN
        }

        test("setting echoChar to the zero character shows typed characters as they are (VISIBLE), and echoCharIsSet is false") {
            val (char, isSet, mode) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.echoChar = '\u0000'
                Triple(passwordField.echoChar, passwordField.echoCharIsSet(), passwordField.passwordRevealMode)
            }
            char shouldBe '\u0000'
            isSet shouldBe false
            mode shouldBe PasswordRevealMode.VISIBLE
        }

        test("setting another character after the zero character returns to HIDDEN with that echo character") {
            val (char, passwordChar, mode) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.echoChar = '\u0000'
                passwordField.echoChar = '#'
                Triple(passwordField.echoChar, passwordField.passwordChar, passwordField.passwordRevealMode)
            }
            char shouldBe '#'
            passwordChar shouldBe "#"
            mode shouldBe PasswordRevealMode.HIDDEN
        }

        test("setting echoChar while PEEK stays PEEK, echoChar is 0 when passwordRevealMode is set to VISIBLE, and echo characters return when set back to HIDDEN") {
            val (peek, visibleChar, restored) = onUiThreadGet {
                val passwordField = WPasswordField()
                passwordField.passwordRevealMode = PasswordRevealMode.PEEK
                passwordField.echoChar = '+'
                val peek = passwordField.passwordRevealMode
                passwordField.passwordRevealMode = PasswordRevealMode.VISIBLE
                val visibleChar = passwordField.echoChar
                passwordField.passwordRevealMode = PasswordRevealMode.HIDDEN
                Triple(peek, visibleChar, passwordField.echoChar)
            }
            peek shouldBe PasswordRevealMode.PEEK
            visibleChar shouldBe '\u0000'
            restored shouldBe '+'
        }

        test("setting password also reaches PasswordChanged listeners (via setText as well)") {
            val received = LinkedBlockingQueue<String>()
            val passwordField = onUiThreadGet {
                WPasswordField().also { it.addPasswordChangedListener { password -> received.add(password) } }
            }
            UiTestHarness.attachAndAwaitLoaded(passwordField)
            try {
                onUiThread { passwordField.setText("viaSetText") }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "viaSetText"
                received.poll(500, TimeUnit.MILLISECONDS) shouldBe null
            } finally {
                UiTestHarness.detach(passwordField)
            }
        }

        test("the placeholder text passed to the constructor can be retrieved via placeholderText") {
            onUiThreadGet { WPasswordField("Enter your password").placeholderText } shouldBe "Enter your password"
        }
    }
}
