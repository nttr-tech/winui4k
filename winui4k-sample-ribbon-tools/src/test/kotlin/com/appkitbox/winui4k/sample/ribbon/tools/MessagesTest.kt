package com.appkitbox.winui4k.sample.ribbon.tools

import com.appkitbox.winui4k.sample.ribbon.common.MessagesChecks
import com.appkitbox.winui4k.sample.ribbon.common.RibbonSampleMessages
import com.appkitbox.winui4k.sample.ribbon.common.SampleMessages
import io.kotest.core.spec.style.FunSpec

class MessagesTest : FunSpec({
    test("the keys match in English and Japanese, no value is empty, and the numbers of {0} match") {
        MessagesChecks.checkSameKeys(BASE_NAME)
        MessagesChecks.checkPlaceholders(BASE_NAME)
    }

    test("all keys used in the source are in this app's or the shared properties") {
        MessagesChecks.checkUsedKeys(listOf("src/main/kotlin"), BASE_NAME, RibbonSampleMessages.baseName)
    }

    test("returns Japanese for Japanese and English for English and other languages (French)") {
        MessagesChecks.checkLocales(BASE_NAME, "window.title") { SampleMessages(BASE_NAME, locale = it) }
    }
})
