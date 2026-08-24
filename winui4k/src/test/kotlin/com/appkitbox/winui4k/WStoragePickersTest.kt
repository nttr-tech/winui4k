package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests that verify creating the storage pickers (WFileOpenPicker / WFileSavePicker /
 * WFolderPicker) and round-tripping their properties. Operations that open the dialog
 * (pick*) never complete without user input, so they aren't verified here.
 */
class WStoragePickersTest : FunSpec() {
    init {
        test("WFileOpenPicker properties read back the values that were set") {
            val frame = UiTestHarness.sharedFrame()
            val (viewMode, location, commitText) = onUiThreadGet {
                val picker = WFileOpenPicker(frame)
                picker.viewMode = PickerViewMode.THUMBNAIL
                picker.suggestedStartLocation = PickerLocationId.DESKTOP
                picker.commitButtonText = "Open this file"
                Triple(picker.viewMode, picker.suggestedStartLocation, picker.commitButtonText)
            }
            viewMode shouldBe PickerViewMode.THUMBNAIL
            location shouldBe PickerLocationId.DESKTOP
            commitText shouldBe "Open this file"
        }

        test("WFileOpenPicker file types appear in fileTypeFilter in the order they were added") {
            val frame = UiTestHarness.sharedFrame()
            onUiThreadGet {
                val picker = WFileOpenPicker(frame)
                picker.addFileTypeFilter(".txt", ".md")
                picker.addFileTypeFilter("*")
                picker.fileTypeFilter
            } shouldContainExactly listOf(".txt", ".md", "*")
        }

        test("WFileOpenPicker clearFileTypeFilter empties the file types") {
            val frame = UiTestHarness.sharedFrame()
            onUiThreadGet {
                val picker = WFileOpenPicker(frame)
                picker.addFileTypeFilter(".txt")
                picker.clearFileTypeFilter()
                picker.fileTypeFilter
            }.shouldBeEmpty()
        }

        test("WFileSavePicker properties read back the values that were set") {
            val frame = UiTestHarness.sharedFrame()
            val results = onUiThreadGet {
                val picker = WFileSavePicker(frame)
                picker.suggestedStartLocation = PickerLocationId.DOWNLOADS
                picker.commitButtonText = "Save here"
                picker.defaultFileExtension = ".txt"
                picker.suggestedFileName = "NewDocument"
                listOf(
                    picker.suggestedStartLocation.name,
                    picker.commitButtonText,
                    picker.defaultFileExtension,
                    picker.suggestedFileName,
                )
            }
            results shouldContainExactly listOf("DOWNLOADS", "Save here", ".txt", "NewDocument")
        }

        test("WFileSavePicker suggestedFolder reads back the path that was set") {
            val folder = System.getProperty("java.io.tmpdir").trimEnd('\\')
            val frame = UiTestHarness.sharedFrame()
            onUiThreadGet {
                val picker = WFileSavePicker(frame)
                picker.suggestedFolder = folder
                picker.suggestedFolder
            } shouldBe folder
        }

        test("WFileSavePicker accepts file type choices (display name + extension list)") {
            // FileTypeChoices has no read-back API, so verify that IMap.Insert (passing a
            // StringVector as the value) succeeds without an HRESULT exception
            val frame = UiTestHarness.sharedFrame()
            onUiThread {
                val picker = WFileSavePicker(frame)
                picker.addFileTypeChoice("Text", ".txt")
                picker.addFileTypeChoice("Images", ".jpg", ".png")
            }
        }

        test("WFolderPicker properties read back the values that were set") {
            val frame = UiTestHarness.sharedFrame()
            val (viewMode, location, commitText) = onUiThreadGet {
                val picker = WFolderPicker(frame)
                picker.viewMode = PickerViewMode.LIST
                picker.suggestedStartLocation = PickerLocationId.PICTURES_LIBRARY
                picker.commitButtonText = "Use this folder"
                Triple(picker.viewMode, picker.suggestedStartLocation, picker.commitButtonText)
            }
            viewMode shouldBe PickerViewMode.LIST
            location shouldBe PickerLocationId.PICTURES_LIBRARY
            commitText shouldBe "Use this folder"
        }
    }
}
