package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.DataTransferInterop

/**
 * Whether content set on the clipboard may enter history or roam:
 * Windows.ApplicationModel.DataTransfer.ClipboardContentOptions.
 * Passed to [Clipboard.setContent]'s options overload. Both are allowed by default.
 */
class ClipboardContentOptions {
    internal val options: ComPtr = Activation.activate(
        DataTransferInterop.CLS_ClipboardContentOptions,
        DataTransferInterop.IID_IClipboardContentOptions,
    )

    init {
        // Release on the UI thread once it becomes unreachable to GC (same mechanism as WComponent)
        ComLifetime.adopt(this, options)
    }

    /** Whether the content may be kept in clipboard history (Win+V) (IsAllowedInHistory). */
    var isAllowedInHistory: Boolean
        get() = options.getBool(DataTransferInterop.IClipboardContentOptions_get_IsAllowedInHistory)
        set(value) = options.putBool(DataTransferInterop.IClipboardContentOptions_put_IsAllowedInHistory, value)

    /** Whether syncing to other devices (roaming) is allowed (IsRoamable). */
    var isRoamable: Boolean
        get() = options.getBool(DataTransferInterop.IClipboardContentOptions_get_IsRoamable)
        set(value) = options.putBool(DataTransferInterop.IClipboardContentOptions_put_IsRoamable, value)
}
