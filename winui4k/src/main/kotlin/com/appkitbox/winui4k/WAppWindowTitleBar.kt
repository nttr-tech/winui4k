package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.ColorReference
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * Microsoft.UI.Windowing.TitleBarHeightOption (the title bar's height).
 * Values are extracted from the winmd (Standard=0, Tall=1, Collapsed=2).
 */
enum class TitleBarHeightOption(internal val native: Int) {
    /** The standard height. */
    STANDARD(0),

    /** A taller option (equivalent to 32px) to fit denser content like tabs. */
    TALL(1),

    /** Collapsed (height 0). */
    COLLAPSED(2),
    ;

    internal companion object {
        fun of(native: Int): TitleBarHeightOption = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Windowing.TitleBarTheme (the title bar's color theme).
 * Values are extracted from the winmd (Legacy=0, UseDefaultAppMode=1, Light=2, Dark=3).
 */
enum class TitleBarTheme(internal val native: Int) {
    /** The legacy color scheme (the default when ExtendsContentIntoTitleBar=false). */
    LEGACY(0),

    /** Follows the app's overall theme (RequestedTheme) (the default). */
    USE_DEFAULT_APP_MODE(1),

    /** Always uses the light theme's colors. */
    LIGHT(2),

    /** Always uses the dark theme's colors. */
    DARK(3),
    ;

    internal companion object {
        fun of(native: Int): TitleBarTheme = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's AppWindowTitleBar (the system title bar's appearance settings). No Swing equivalent.
 * Obtained from [WAppWindow.titleBar] (it cannot be created directly).
 *
 * Provides the 12 color properties (all null = restore the default color) and
 * [extendsContentIntoTitleBar] / [height] / [leftInset] / [rightInset] / [preferredHeightOption] /
 * [preferredTheme] / [resetToDefault].
 */
class WAppWindowTitleBar internal constructor(private val titleBar: ComPtr) {
    /** An IAppWindowTitleBar2 view for PreferredHeightOption. */
    private val titleBar2: ComPtr by lazy { titleBar.queryInterface(WindowingInterop.IID_IAppWindowTitleBar2) }

    /** An IAppWindowTitleBar3 view for PreferredTheme. */
    private val titleBar3: ComPtr by lazy { titleBar.queryInterface(WindowingInterop.IID_IAppWindowTitleBar3) }

    /** The whole title bar's background color. */
    var backgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_BackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_BackgroundColor, value)

    /** The caption buttons' (minimize/maximize/close) background color. */
    var buttonBackgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonBackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonBackgroundColor, value)

    /** The caption buttons' icon color. */
    var buttonForegroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonForegroundColor, value)

    /** The caption buttons' background color while hovered. */
    var buttonHoverBackgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonHoverBackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonHoverBackgroundColor, value)

    /** The caption buttons' icon color while hovered. */
    var buttonHoverForegroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonHoverForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonHoverForegroundColor, value)

    /** The caption buttons' background color while the window is inactive. */
    var buttonInactiveBackgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonInactiveBackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonInactiveBackgroundColor, value)

    /** The caption buttons' icon color while the window is inactive. */
    var buttonInactiveForegroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonInactiveForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonInactiveForegroundColor, value)

    /** The caption buttons' background color while pressed. */
    var buttonPressedBackgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonPressedBackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonPressedBackgroundColor, value)

    /** The caption buttons' icon color while pressed. */
    var buttonPressedForegroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ButtonPressedForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ButtonPressedForegroundColor, value)

    /** The title text's color. */
    var foregroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_ForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_ForegroundColor, value)

    /** The background color while the window is inactive. */
    var inactiveBackgroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_InactiveBackgroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_InactiveBackgroundColor, value)

    /** The title text's color while the window is inactive. */
    var inactiveForegroundColor: WColor?
        get() = getColor(WindowingInterop.IAppWindowTitleBar_get_InactiveForegroundColor)
        set(value) = putColor(WindowingInterop.IAppWindowTitleBar_put_InactiveForegroundColor, value)

    /**
     * Whether the app's content extends into the title bar area (AppWindowTitleBar.ExtendsContentIntoTitleBar).
     * Paired with [WFrame.extendsContentIntoTitleBar] (setting either one keeps them in sync).
     */
    var extendsContentIntoTitleBar: Boolean
        get() = titleBar.getBool(WindowingInterop.IAppWindowTitleBar_get_ExtendsContentIntoTitleBar)
        set(value) = titleBar.putBool(WindowingInterop.IAppWindowTitleBar_put_ExtendsContentIntoTitleBar, value)

    /** The title bar's height (px, read-only). */
    val height: Int get() = titleBar.getInt(WindowingInterop.IAppWindowTitleBar_get_Height)

    /** The system-reserved width on the left (px, read-only). Used to compute the non-draggable area. */
    val leftInset: Int get() = titleBar.getInt(WindowingInterop.IAppWindowTitleBar_get_LeftInset)

    /** The system-reserved width on the right (px, read-only). Used to compute the caption button area. */
    val rightInset: Int get() = titleBar.getInt(WindowingInterop.IAppWindowTitleBar_get_RightInset)

    /** The title bar's preferred height (AppWindowTitleBar2.PreferredHeightOption). */
    var preferredHeightOption: TitleBarHeightOption
        get() = TitleBarHeightOption.of(titleBar2.getInt(WindowingInterop.IAppWindowTitleBar2_get_PreferredHeightOption))
        set(value) = titleBar2.call(WindowingInterop.IAppWindowTitleBar2_put_PreferredHeightOption, value.native)

    /** The title bar's color theme (AppWindowTitleBar3.PreferredTheme). */
    var preferredTheme: TitleBarTheme
        get() = TitleBarTheme.of(titleBar3.getInt(WindowingInterop.IAppWindowTitleBar3_get_PreferredTheme))
        set(value) = titleBar3.call(WindowingInterop.IAppWindowTitleBar3_put_PreferredTheme, value.native)

    /** Resets all color settings to their defaults (AppWindowTitleBar.ResetToDefault). */
    fun resetToDefault() = titleBar.call(WindowingInterop.IAppWindowTitleBar_ResetToDefault)

    private fun getColor(slot: Int): WColor? {
        val boxed = titleBar.getPtrOrNull(slot) ?: return null
        return try {
            val (a, r, g, b) = XamlStructs.getColor(boxed, FoundationInterop.IReference_Color_get_Value)
            WColor(r, g, b, a)
        } finally {
            boxed.release()
        }
    }

    private fun putColor(slot: Int, value: WColor?) {
        if (value == null) {
            titleBar.call(slot, null)
            return
        }
        val reference = ColorReference.create(value.alpha, value.red, value.green, value.blue)
        try {
            titleBar.call(slot, reference.primary)
        } finally {
            reference.release() // put holds a reference to it; it's reclaimed on the next put/Reset's Release
        }
    }
}
