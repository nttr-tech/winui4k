package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.util.Locale

/** Tests for RibbonStrings (localized strings for the ribbon's built-in UI) (no UI required). */
class RibbonStringsTest : FunSpec() {
    private lateinit var previous: RibbonStrings

    init {
        beforeEach { previous = RibbonStrings.current }
        afterEach { RibbonStrings.current = previous }

        test("every built-in language (en / de / fr / es / pl / ja) has all the English keys with non-empty strings") {
            RibbonStrings.builtInCultures shouldContainAll listOf("en", "de", "fr", "es", "pl", "ja")
            for (culture in listOf("en", "de", "fr", "es", "pl", "ja")) {
                val strings = RibbonStrings.forCulture(culture)
                val own = when (culture) {
                    "en" -> RibbonStringsEn.VALUES
                    "de" -> RibbonStringsDe.VALUES
                    "fr" -> RibbonStringsFr.VALUES
                    "es" -> RibbonStringsEs.VALUES
                    "pl" -> RibbonStringsPl.VALUES
                    else -> RibbonStringsJa.VALUES
                }
                own.keys shouldBe RibbonStringsEn.VALUES.keys
                for (key in RibbonStrings.keys) strings[key].shouldNotBeBlank()
            }
        }

        test("formatted strings have the same number of placeholders as English in every language") {
            val placeholder = Regex("""\{\d}""")
            for (culture in listOf("de", "fr", "es", "pl", "ja")) {
                val strings = RibbonStrings.forCulture(culture)
                for (key in RibbonStrings.keys) {
                    placeholder.findAll(strings[key]).count() shouldBe placeholder.findAll(RibbonStringsEn.VALUES.getValue(key)).count()
                }
            }
        }

        test("language tags with a region (ja-JP / de-AT) resolve to the language, and unknown languages to English") {
            RibbonStrings.forCulture(Locale.JAPAN).file shouldBe "ファイル"
            RibbonStrings.forCulture("ja-JP").quickAccessToolbar shouldBe "クイック アクセス ツール バー"
            RibbonStrings.forCulture("de-AT").file shouldBe "Datei"
            RibbonStrings.forCulture("sv-SE").file shouldBe "File"
            RibbonStrings.forCulture(Locale.ROOT).file shouldBe "File"
        }

        test("an unknown key that is not in English either returns the key name itself") {
            RibbonStrings.forCulture("ja")["NoSuchKey"] shouldBe "NoSuchKey"
        }

        test("format replaces {0} {1} with the arguments, and the formatting functions give the same result") {
            val english = RibbonStrings.forCulture(Locale.ROOT)
            english.format("TablePickerFormat", 3, 4) shouldBe "3x4 Table"
            english.tablePickerFormat(3, 4) shouldBe "3x4 Table"
            english.expandPanel("Draw") shouldBe "More Draw commands"
            english.splitButtonOptions("Paste") shouldBe "Paste options"
            english.dialogLauncher("Font") shouldBe "Font Settings"
            RibbonStrings.forCulture("ja").dialogLauncher("フォント") shouldBe "フォント の設定"
        }

        test("override replaces keys only for that instance") {
            val strings = RibbonStrings.forCulture("en")
            strings.override("File", "Home").file shouldBe "Home"
            RibbonStrings.forCulture("en").file shouldBe "File"
        }

        test("a language added with register can also be used with a region tag, and missing keys fall back to English") {
            RibbonStrings.register("cs", mapOf("File" to "Soubor"))
            RibbonStrings.forCulture("cs-CZ").file shouldBe "Soubor"
            RibbonStrings.forCulture("cs-CZ").search shouldBe "Search"
        }

        test("setting current, overriding current, and registering current's language notify a change, and overrides survive re-registration") {
            var raised = 0
            val listener = Runnable { raised++ }
            RibbonStrings.addCurrentChangedListener(listener)
            try {
                RibbonStrings.current = RibbonStrings.forCulture("en-US")
                RibbonStrings.current.override("File", "Start")
                RibbonStrings.current.file shouldBe "Start"
                RibbonStrings.register("en", mapOf("Options" to "Options"))
                RibbonStrings.current.file shouldBe "Start"
                raised shouldBeGreaterThanOrEqual 3
            } finally {
                RibbonStrings.removeCurrentChangedListener(listener)
            }
        }

        test("overriding an instance that is not current or registering an unrelated language does not notify") {
            RibbonStrings.current = RibbonStrings.forCulture("en")
            var raised = 0
            val listener = Runnable { raised++ }
            RibbonStrings.addCurrentChangedListener(listener)
            try {
                RibbonStrings.forCulture("en").override("File", "Other")
                RibbonStrings.register("pt", mapOf("File" to "Arquivo"))
                raised shouldBe 0
            } finally {
                RibbonStrings.removeCurrentChangedListener(listener)
            }
        }
    }
}
