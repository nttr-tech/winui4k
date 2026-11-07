package com.appkitbox.winui4k.sample.gallery

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties

/** Checks the contents of messages.properties (English) and messages_ja.properties (Japanese), and how Messages chooses the language. */
class MessagesTest : FunSpec({
    val en = loadProperties("messages.properties")
    val ja = loadProperties("messages_ja.properties")

    test("the English and Japanese key sets are the same") {
        (en.keys - ja.keys).sorted() shouldBe emptyList()
        (ja.keys - en.keys).sorted() shouldBe emptyList()
    }

    test("no value is empty") {
        (en.filterValues { it.isBlank() }.keys + ja.filterValues { it.isBlank() }.keys).sorted().shouldBeEmpty()
    }

    test("keys consist of lowercase letters, digits and dots") {
        en.keys.filterNot { KEY_PATTERN.matches(it) }.sorted().shouldBeEmpty()
    }

    test("all keys used by Messages.get in the source are in the properties, and the keys are written as string literals") {
        val usages = sourceUsages(File("src/main/kotlin"))
        usages.isEmpty() shouldBe false
        usages.filterNot { KEY_PATTERN.matches(it) }.shouldBeEmpty()
        usages.filterNot { it in en.keys }.sorted().distinct().shouldBeEmpty()
    }

    test("the argument numbers such as {0} match in English and Japanese") {
        en.keys.filter { placeholders(en.getValue(it)) != placeholders(ja[it].orEmpty()) }.sorted().shouldBeEmpty()
    }

    test("messages with arguments write ' as '', and messages without arguments do not use '' (MessageFormat is used only with arguments)") {
        val wrong = (en.entries + ja.entries).filter { (_, value) -> !apostrophesFit(value) }.map { it.key }
        wrong.sorted().shouldBeEmpty()
    }

    test("returns Japanese for the Japanese locale, and English for English and other languages (fr)") {
        val jaBundle = Messages.bundleFor(Locale.JAPAN)
        val enBundle = Messages.bundleFor(Locale.US)
        val frBundle = Messages.bundleFor(Locale.FRANCE)
        en.keys.filter { jaBundle.getString(it) != ja[it] }.shouldBeEmpty()
        en.keys.filter { enBundle.getString(it) != en[it] }.shouldBeEmpty()
        en.keys.filter { frBundle.getString(it) != en[it] }.shouldBeEmpty()
    }

    test("messages with arguments filled in, and the key itself for a missing key") {
        val key = en.keys.first { placeholders(en.getValue(it)).isNotEmpty() }
        val count = placeholders(en.getValue(key)).maxOf { it.toInt() } + 1
        val args = Array<Any?>(count) { "<$it>" }
        val filled = Messages.format(Messages.bundleFor(Locale.US), key, args)
        placeholders(en.getValue(key)).forEach { filled.contains("<$it>") shouldBe true }
        Messages.get("no.such.key") shouldBe "no.such.key"
    }
})

private val KEY_PATTERN = Regex("[a-z0-9]+(\\.[a-z0-9]+)+")
private val PLACEHOLDER = Regex("\\{(\\d+)")
private val USAGE = Regex("Messages\\.get\\(\\s*(\"([^\"]*)\")?")

private fun loadProperties(name: String): Map<String, String> {
    val path = "com/appkitbox/winui4k/sample/gallery/$name"
    val stream = checkNotNull(MessagesTest::class.java.classLoader.getResourceAsStream(path)) { "$path is missing" }
    val properties = Properties()
    InputStreamReader(stream, Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}

/** The first argument of Messages.get( in the source. Non-string-literal arguments are returned as "<dynamic>" (they fail the key format check). */
private fun sourceUsages(dir: File): List<String> = dir.walkTopDown()
    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
    .flatMap { file -> USAGE.findAll(file.readText()).map { it.groups[2]?.value ?: "<dynamic in ${file.name}>" } }
    .toList()

private fun placeholders(value: String): Set<String> = PLACEHOLDER.findAll(value).map { it.groupValues[1] }.toSet()

private fun apostrophesFit(value: String): Boolean = if (placeholders(value).isEmpty()) {
    !value.contains("''")
} else {
    !value.replace("''", "").contains("'")
}
