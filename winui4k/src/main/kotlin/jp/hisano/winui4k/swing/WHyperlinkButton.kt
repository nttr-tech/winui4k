package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winui.Abi

/**
 * JButton-like hyperlink display: WinUI 3's HyperlinkButton.
 * Setting [navigateUri] makes a click open the default browser.
 * If left unset, it behaves like a normal button and can handle Click via [addActionListener].
 */
class WHyperlinkButton(text: String = "", navigateUri: String = "") : WButtonBase(
    Activation.composeDefault(Abi.CLS_HyperlinkButton, Abi.IID_IHyperlinkButtonFactory),
) {
    /**
     * The URI opened on click (HyperlinkButton.NavigateUri). Set to "" to clear it.
     * Creates a Windows.Foundation.Uri via IUriRuntimeClassFactory.CreateUri and passes it in.
     */
    var navigateUri: String
        get() {
            val uri = inspectable.getPtrOrNull(Abi.IHyperlinkButton_get_NavigateUri) ?: return ""
            return try {
                uri.getString(Abi.IUriRuntimeClass_get_AbsoluteUri)
            } finally {
                uri.release()
            }
        }
        set(value) {
            if (value.isEmpty()) {
                inspectable.call(Abi.IHyperlinkButton_put_NavigateUri, null)
                return
            }
            val factory = Activation.factory(Abi.CLS_Uri, Abi.IID_IUriRuntimeClassFactory)
            val uri = Hstring.use(value) { h ->
                factory.getPtr(Abi.IUriRuntimeClassFactory_CreateUri, h)
            }
            factory.release()
            inspectable.call(Abi.IHyperlinkButton_put_NavigateUri, uri.ptr)
            uri.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (navigateUri.isNotEmpty()) this.navigateUri = navigateUri
    }
}
