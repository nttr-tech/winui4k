package com.appkitbox.winui4k.sample.ribbon.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class SampleMessagesTest : FunSpec({
    val baseName = RibbonSampleMessages.baseName
    val testBaseName = "com.appkitbox.winui4k.sample.ribbon.common.test"

    test("the shared strings have the same keys in English and Japanese, no empty values, and matching numbers of {0}") {
        MessagesChecks.checkSameKeys(baseName)
        MessagesChecks.checkPlaceholders(baseName)
    }

    test("returns Japanese for Japanese and English for English and other languages (French), and the key itself for a missing key") {
        MessagesChecks.checkLocales(baseName, "common.paste") { SampleMessages(baseName, locale = it) }
        SampleMessages(baseName, locale = Locale.JAPAN).get("common.paste") shouldBe "貼り付け"
        SampleMessages(baseName, locale = Locale.US).get("common.paste") shouldBe "Paste"
    }

    test("reads UTF-8 properties (no garbled text with Java 8's default ISO-8859-1)") {
        SampleMessages(testBaseName, locale = Locale.JAPAN).get("greeting", "花子", 3) shouldBe "こんにちは、花子 さん。3 です。"
    }

    test("arguments are filled with MessageFormat ('' becomes '), and without arguments ' is returned as is") {
        val en = SampleMessages(testBaseName, locale = Locale.US)
        en.get("greeting", "Hanako", "Monday") shouldBe "Hello, Hanako! It's Monday."
        en.get("plain") shouldBe "It's plain"
        SampleMessages(testBaseName, locale = Locale.JAPAN).get("plain") shouldBe "引数なしの ' はそのまま"
    }

    test("keys missing here are read from the parent, and if the parent lacks them too, the key itself") {
        val child = SampleMessages(testBaseName, SampleMessages(baseName, locale = Locale.US), Locale.US)
        child.get("common.paste") shouldBe "Paste"
        child.contains("common.paste") shouldBe true
        child.contains("greeting") shouldBe true
        child.contains("no.such.key") shouldBe false
        child.get("no.such.key") shouldBe "no.such.key"
    }
})
