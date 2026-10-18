package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Verifies that wrappers and native controls that are no longer referenced from anywhere are released.
 * Waits for collection while prompting GC ([Reclamation]). Destruction of the native object is checked with
 * IWeakReference ([NativeProbe]).
 */
class WrapperReleaseTest : FunSpec() {
    init {
        test("a WPanel that is not placed anywhere has its native StackPanel destroyed once the wrapper is collected") {
            val probe = onUiThreadGet { NativeProbe(WPanel(spacing = 4.0).inspectable) }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("a WTextField that is not placed anywhere has its native TextBox destroyed once the wrapper is collected") {
            val probe = onUiThreadGet { NativeProbe(WTextField("Prompt").inspectable) }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
        }

        test("changing the text of a button with an icon destroys the StackPanel previously used as its Content") {
            val button = onUiThreadGet { WButton("Before", Symbol.SAVE) }
            val probe = onUiThreadGet {
                val contentControl = button.inspectable.queryInterface(XamlInterop.IID_IContentControl)
                val content = try {
                    contentControl.getPtr(XamlInterop.IContentControl_get_Content)
                } finally {
                    contentControl.release()
                }
                try {
                    NativeProbe(content)
                } finally {
                    content.release()
                }
            }
            onUiThread { button.text = "After" }
            Reclamation.awaitDestroyed(probe).shouldBeTrue()
            onUiThreadGet { button.text } // Keep the button alive while waiting for collection
        }
    }
}
