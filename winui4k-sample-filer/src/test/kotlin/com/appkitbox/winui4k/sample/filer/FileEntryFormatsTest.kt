package com.appkitbox.winui4k.sample.filer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.util.Calendar
import java.util.Locale

/** The milliseconds of [year]-[month]-[day] [hour]:[minute] (in the default time zone). */
private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, minute)
    }.timeInMillis

class FileEntryFormatsTest : FunSpec({
    val english = Messages.bundleFor(Locale.US)
    val japanese = Messages.bundleFor(Locale.JAPAN)

    test("the date modified is in the form 2025/04/02 15:27 in Japanese (the same as before)") {
        dateTimeFormat(Locale.JAPAN).format(millisOf(2025, 4, 2, 15, 27)) shouldBe "2025/04/02 15:27"
        // The hour has 2 digits in the 24-hour clock (the same as the previous "yyyy/MM/dd HH:mm")
        dateTimeFormat(Locale.JAPAN).format(millisOf(2025, 4, 2, 0, 21)) shouldBe "2025/04/02 00:21"
    }

    test("the date modified is month/day/4-digit year with the 12-hour clock in English (United States) (the same as Explorer)") {
        val text = dateTimeFormat(Locale.US).format(millisOf(2025, 4, 2, 15, 27))
        text shouldStartWith "4/2/2025"
        text.replace(' ', ' ').endsWith("3:27 PM") shouldBe true
    }

    test("sorting by date modified uses time order, not string order (correct even for English month/day/year)") {
        val format = dateTimeFormat(Locale.US)
        val comparator = dateComparator(format)
        val december = format.format(millisOf(2024, 12, 31, 23, 0))
        val january = format.format(millisOf(2025, 1, 2, 9, 0))
        (comparator.compare(december, january) < 0) shouldBe true
        (comparator.compare(january, december) > 0) shouldBe true
        // Unreadable strings (such as the blank of folders) come first
        (comparator.compare("", december) < 0) shouldBe true
    }

    test("the number of items uses singular and plural in English, and \"個の項目\" in Japanese") {
        Messages.format(english, "status.items", 1) shouldBe "1 item"
        Messages.format(english, "status.items", 3) shouldBe "3 items"
        Messages.format(english, "status.items", 0) shouldBe "0 items"
        Messages.format(japanese, "status.items", 3) shouldBe "3 個の項目"
    }

    test("the selection display fills in the count and the total size") {
        Messages.format(english, "status.selected", 1, "12.4 MB") shouldBe "1 item selected  12.4 MB"
        Messages.format(english, "status.selected", 2, "12.4 MB") shouldBe "2 items selected  12.4 MB"
        Messages.format(japanese, "status.selected", 2, "12.4 MB") shouldBe "2 個の項目を選択 12.4 MB"
    }

    test("the type is the uppercase extension and \"ファイル\" / File") {
        Messages.format(english, "kind.file.extension", "TXT") shouldBe "TXT File"
        Messages.format(japanese, "kind.file.extension", "TXT") shouldBe "TXT ファイル"
        Messages.text(english, "kind.folder") shouldBe "File folder"
        Messages.text(japanese, "kind.folder") shouldBe "ファイル フォルダー"
    }

    test("sizes in bytes use the unit of the language") {
        Messages.format(english, "size.bytes", 512) shouldBe "512 bytes"
        Messages.format(japanese, "size.bytes", 512) shouldBe "512 バイト"
    }

    test("copies with the same name get \"- コピー\" / \"- Copy\" in the language") {
        Messages.format(english, "file.copy.name", "report") shouldBe "report - Copy"
        Messages.format(english, "file.copy.name.numbered", "report", "2") shouldBe "report - Copy (2)"
        Messages.format(japanese, "file.copy.name", "report") shouldBe "report - コピー"
        Messages.format(japanese, "file.copy.name.numbered", "report", "2") shouldBe "report - コピー (2)"
    }

    test("the permanent deletion confirmation uses singular and plural in English, and ' is not dropped") {
        Messages.format(english, "dialog.delete.message", 1) shouldBe
            "Permanently delete this item? You can't undo this."
        Messages.format(english, "dialog.delete.message", 3) shouldBe
            "Permanently delete these 3 items? You can't undo this."
        Messages.format(japanese, "dialog.delete.message", 3) shouldBe
            "3 個の項目を完全に削除します。この操作は元に戻せません。"
    }
})
