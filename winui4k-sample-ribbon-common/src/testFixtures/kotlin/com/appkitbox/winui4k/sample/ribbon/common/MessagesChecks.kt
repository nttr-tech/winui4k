package com.appkitbox.winui4k.sample.ribbon.common

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties

/**
 * Checks the strings of a sample module (messages.properties / messages_ja.properties) (no window is shown).
 * The source is read from the test's working directory (the module's directory).
 */
object MessagesChecks {
    private val USE = Regex("""Messages\.get\(\s*"([^"]+)"""")
    private val PLACEHOLDER = Regex("""\{(\d+)[^}]*}""")

    /** Reads the properties of [baseName] (e.g. `com.appkitbox.winui4k.sample.ribbon.word.messages`) from the classpath (Japanese if [suffix] is `_ja`). */
    fun load(baseName: String, suffix: String = ""): Properties {
        val path = baseName.replace('.', '/') + suffix + ".properties"
        val stream = MessagesChecks::class.java.classLoader.getResourceAsStream(path)
        withClue("$path is missing") { (stream != null) shouldBe true }
        return Properties().also { p -> InputStreamReader(stream!!, Charsets.UTF_8).use { p.load(it) } }
    }

    /** The English and Japanese key sets are the same, and no value is empty. */
    fun checkSameKeys(baseName: String) {
        val en = load(baseName)
        val ja = load(baseName, "_ja")
        withClue("keys missing in Japanese") { (en.stringPropertyNames() - ja.stringPropertyNames()).sorted().shouldBeEmpty() }
        withClue("keys missing in English") { (ja.stringPropertyNames() - en.stringPropertyNames()).sorted().shouldBeEmpty() }
        withClue("keys with empty values") {
            (en.stringPropertyNames().filter { en.getProperty(it).isBlank() } + ja.stringPropertyNames().filter { ja.getProperty(it).isBlank() }).sorted().shouldBeEmpty()
        }
    }

    /** For keys with arguments, the sets of numbers such as `{0}` match in English and Japanese, and `'` is written as `''`. */
    fun checkPlaceholders(baseName: String) {
        val en = load(baseName)
        val ja = load(baseName, "_ja")
        val mismatched = en.stringPropertyNames().filter { key ->
            ja.getProperty(key) != null && placeholders(en.getProperty(key)) != placeholders(ja.getProperty(key))
        }
        withClue("keys whose {0} etc. do not match") { mismatched.sorted().shouldBeEmpty() }
        val loneQuotes = (en.stringPropertyNames().map { it to en.getProperty(it) } + ja.stringPropertyNames().map { it to ja.getProperty(it) })
            .filter { (_, value) -> placeholders(value).isNotEmpty() && value.replace("''", "").contains('\'') }
            .map { it.first }
        withClue("keys with arguments that do not write ' as ''") { loneQuotes.sorted().shouldBeEmpty() }
    }

    /** All keys of `Messages.get("...")` in the source (.kt / .java in [sourceDirs]) are in the English properties of one of [bundles]. */
    fun checkUsedKeys(sourceDirs: List<String>, vararg bundles: String) {
        val keys = bundles.flatMap { load(it).stringPropertyNames() }.toSet()
        val used = usedKeys(sourceDirs)
        withClue("the source uses ${used.size} keys") { used.isEmpty() shouldBe false }
        withClue("keys missing in the properties") { (used - keys).sorted().shouldBeEmpty() }
    }

    /** The keys of `Messages.get("...")` in [sourceDirs]. */
    fun usedKeys(sourceDirs: List<String>): Set<String> = sourceDirs.flatMap { dir ->
        File(dir).walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .flatMap { file -> USE.findAll(file.readText(Charsets.UTF_8)).map { it.groupValues[1] } }.toList()
    }.toSet()

    /** With the way [messages] is created, returns the strings for Japanese, English and other languages (French -> English). */
    fun checkLocales(baseName: String, key: String, create: (Locale) -> SampleMessages) {
        val en = load(baseName).getProperty(key)
        val ja = load(baseName, "_ja").getProperty(key)
        en shouldNotBe ja
        create(Locale.JAPAN).get(key) shouldBe ja
        create(Locale.JAPANESE).get(key) shouldBe ja
        create(Locale.US).get(key) shouldBe en
        create(Locale.ENGLISH).get(key) shouldBe en
        create(Locale.FRANCE).get(key) shouldBe en
        create(Locale.US).get("no.such.key") shouldBe "no.such.key"
    }

    private fun placeholders(value: String): Set<String> = PLACEHOLDER.findAll(value.replace("''", "")).map { it.groupValues[1] }.toSet()
}
