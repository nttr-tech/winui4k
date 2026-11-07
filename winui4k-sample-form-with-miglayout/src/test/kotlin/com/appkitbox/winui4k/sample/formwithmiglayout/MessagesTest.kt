package com.appkitbox.winui4k.sample.formwithmiglayout

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties

private const val RESOURCE_DIRECTORY = "com/appkitbox/winui4k/sample/formwithmiglayout"

/** Reads properties on the classpath as UTF-8 (looking only at the file's contents without mixing in ResourceBundle's parent bundles). */
private fun loadProperties(name: String): Map<String, String> {
    val stream = checkNotNull(Messages::class.java.classLoader.getResourceAsStream("$RESOURCE_DIRECTORY/$name")) { name }
    val properties = Properties()
    InputStreamReader(stream, Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}

/** The set of argument numbers such as `{0}`. */
private fun argumentIndexes(pattern: String): Set<String> = Regex("""\{(\d+)""").findAll(pattern).map { it.groupValues[1] }.toSet()

/** The keys used in Messages.get("...") in the source (src/main/kotlin). */
private fun keysUsedInSource(): Set<String> =
    File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }
        .flatMap { file -> Regex("""Messages\.get\(\s*"([^"]+)"""").findAll(file.readText()).map { it.groupValues[1] } }
        .toSet()

class MessagesTest : FunSpec({
    val english = loadProperties("messages.properties")
    val japanese = loadProperties("messages_ja.properties")

    test("the English and Japanese properties have the same set of keys, and no value is empty") {
        (english.keys - japanese.keys).shouldBeEmpty()
        (japanese.keys - english.keys).shouldBeEmpty()
        english.filterValues { it.isBlank() }.keys.shouldBeEmpty()
        japanese.filterValues { it.isBlank() }.keys.shouldBeEmpty()
    }

    test("all keys used by Messages.get in the source are in the properties, and there are no unused keys") {
        val used = keysUsedInSource()
        used shouldNotBe emptySet<String>()
        (used - english.keys).shouldBeEmpty()
        (english.keys - used).shouldBeEmpty()
    }

    test("the first argument of Messages.get in the source is a string literal (so the keys can be picked up with a regular expression)") {
        val dynamic = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" && it.name != "Messages.kt" }
            .flatMap { file -> Regex("""Messages\.get\(\s*[^"\s]""").findAll(file.readText()).map { "${file.name}: ${it.value}" } }
            .toList()
        dynamic.shouldBeEmpty()
    }

    test("keys with arguments have matching numbers such as {0} in English and Japanese") {
        english.keys.filter { japanese.containsKey(it) }
            .filter { argumentIndexes(english.getValue(it)) != argumentIndexes(japanese.getValue(it)) }
            .shouldBeEmpty()
    }

    test("keys with arguments write ' as '' (so MessageFormat does not drop it)") {
        (english + japanese.mapKeys { "ja:${it.key}" })
            .filterValues { argumentIndexes(it).isNotEmpty() && Regex("(?<!')'(?!')").containsMatchIn(it) }
            .keys.shouldBeEmpty()
    }

    test("returns Japanese strings for Japanese, and English strings for English and other languages (French)") {
        Messages.text(Messages.bundleFor(Locale.JAPAN), "form.title") shouldBe japanese.getValue("form.title")
        Messages.text(Messages.bundleFor(Locale.US), "form.title") shouldBe english.getValue("form.title")
        Messages.text(Messages.bundleFor(Locale.FRANCE), "form.title") shouldBe english.getValue("form.title")
        english.getValue("form.title") shouldNotBe japanese.getValue("form.title")
    }

    test("a missing key does not throw and returns the key itself") {
        Messages.text(Messages.bundleFor(Locale.US), "no.such.key") shouldBe "no.such.key"
        Messages.get("no.such.key") shouldBe "no.such.key"
    }
})
