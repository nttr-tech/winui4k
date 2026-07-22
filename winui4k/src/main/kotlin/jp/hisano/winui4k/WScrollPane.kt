package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winui.Abi

/**
 * JScrollPane-like: WinUI 3's ScrollViewer (a ContentControl subclass).
 * Shows scrollbars when [content] is larger than the viewport.
 */
class WScrollPane(content: WComponent? = null) : WControl(
    Activation.activate(Abi.CLS_ScrollViewer).queryInterface(Abi.IID_IScrollViewer), // created via the default factory
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }

    /** The content to scroll (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    init {
        if (content != null) this.content = content
    }
}
