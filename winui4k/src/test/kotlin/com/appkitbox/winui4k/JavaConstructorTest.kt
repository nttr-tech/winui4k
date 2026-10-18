package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies, by checking for public constructors on the JVM, that Java can call both `new X()` and `new X("text")`
 * (@JvmOverloads).
 */
class JavaConstructorTest : FunSpec() {
    init {
        test("WToggleButton exposes both the no-arg and the text constructors to Java") {
            val noArg = WToggleButton::class.java.getConstructor()
            val withText = WToggleButton::class.java.getConstructor(String::class.java)
            onUiThreadGet { noArg.newInstance().text } shouldBe ""
            onUiThreadGet { withText.newInstance("Toggle").text } shouldBe "Toggle"
        }

        test("WSplitButton exposes both the no-arg and the text constructors to Java") {
            val noArg = WSplitButton::class.java.getConstructor()
            val withText = WSplitButton::class.java.getConstructor(String::class.java)
            onUiThreadGet { noArg.newInstance().text } shouldBe ""
            onUiThreadGet { withText.newInstance("Split").text } shouldBe "Split"
        }

        test("WXamlUICommand exposes both the no-arg and the label constructors to Java") {
            val noArg = WXamlUICommand::class.java.getConstructor()
            val withLabel = WXamlUICommand::class.java.getConstructor(String::class.java)
            onUiThreadGet { noArg.newInstance().label } shouldBe ""
            onUiThreadGet { withLabel.newInstance("Command").label } shouldBe "Command"
        }
    }
}
