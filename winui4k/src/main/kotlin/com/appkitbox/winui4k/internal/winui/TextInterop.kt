package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * The WinRT ABI constants used for text field formatting, selection and input filtering (Document-like APIs) that were not in XamlInterop.
 *
 * - Microsoft.UI.Text.* were extracted from build/winmd/metadata/Microsoft.UI.Text.winmd
 * - Microsoft.UI.Xaml.Controls.* were extracted from build/winmd/metadata/Microsoft.UI.Xaml.winmd
 *
 * All values were mechanically extracted with tools/dump_winmd.py. Not a single value is hand-written or guessed.
 * The rest of ITextDocument, ITextRange and ITextCharacterFormat is in XamlInterop.
 */
internal object TextInterop {
    // ---- Microsoft.UI.Text.ITextDocument (additions) ----
    const val ITextDocument_GetDefaultCharacterFormat = 21 // GetDefaultCharacterFormat(out ITextCharacterFormat)

    // ---- Microsoft.UI.Text.ITextCharacterFormat (additions) ----
    const val ITextCharacterFormat_get_ForegroundColor = 16 // get_ForegroundColor(out Windows.UI.Color)
    const val ITextCharacterFormat_put_ForegroundColor = 17 // put_ForegroundColor(Windows.UI.Color)
    const val ITextCharacterFormat_get_Name = 27         // get_Name(out HSTRING)
    const val ITextCharacterFormat_put_Name = 28         // put_Name(HSTRING)
    const val ITextCharacterFormat_get_Size = 35         // get_Size(out r4) points
    const val ITextCharacterFormat_put_Size = 36         // put_Size(r4) points
    const val ITextCharacterFormat_get_Underline = 49    // get_Underline(out UnderlineType)
    const val ITextCharacterFormat_put_Underline = 50    // put_Underline(UnderlineType)
    const val ITextCharacterFormat_SetClone = 53         // SetClone(ITextCharacterFormat) copies all formatting

    // ---- Microsoft.UI.Text.FormatEffect / UnderlineType ----
    const val FormatEffect_Off = 0
    const val FormatEffect_On = 1
    const val UnderlineType_Undefined = 0
    const val UnderlineType_None = 1
    const val UnderlineType_Single = 2

    // ---- Microsoft.UI.Text.ITextSelection / SelectionOptions ----
    const val IID_ITextSelection = "8f5e6cb1-2b04-589f-bd24-54e5cd8dd399"
    const val ITextSelection_get_Options = 6              // get_Options(out SelectionOptions)
    const val ITextSelection_put_Options = 7              // put_Options(SelectionOptions)
    const val SelectionOptions_StartActive = 1            // The moving end of the selection (the caret) is on the start side

    // ---- Microsoft.UI.Xaml.Controls.TextBox.BeforeTextChanging ----
    const val ITextBox_add_BeforeTextChanging = 76        // add_BeforeTextChanging(TypedEventHandler<TextBox, TextBoxBeforeTextChangingEventArgs>, out token)
    const val IID_ITextBoxBeforeTextChangingEventArgs = "ba46105b-9a16-5237-a20f-5df750992a05"
    const val ITextBoxBeforeTextChangingEventArgs_get_NewText = 6 // get_NewText(out HSTRING)
    const val ITextBoxBeforeTextChangingEventArgs_put_Cancel = 8  // put_Cancel(boolean)

    /** The concrete IID of TypedEventHandler<TextBox, TextBoxBeforeTextChangingEventArgs> (computed at run time). */
    val IID_TextBoxBeforeTextChangingHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(${XamlInterop.CLS_TextBox};{${XamlInterop.IID_ITextBox}});" +
                "rc(Microsoft.UI.Xaml.Controls.TextBoxBeforeTextChangingEventArgs;{$IID_ITextBoxBeforeTextChangingEventArgs}))",
        )
    }
}
