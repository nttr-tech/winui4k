package com.appkitbox.winui4k.sample.ribbon.common

import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.TitleBarTheme
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.extension.ribbon.WRibbon
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme
import com.appkitbox.winui4k.extension.ribbon.WRibbonTitleBar
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons

/** The kind of demo app (the app icon in the title bar). */
enum class RibbonDemoApp(internal val icon: RibbonIcon) {
    /** Word style. */
    WORD(RibbonIcons.DOCUMENT),

    /** Excel style. */
    EXCEL(RibbonIcons.TABLE),

    /** PowerPoint style. */
    POWER_POINT(RibbonIcons.PRESENT),

    /** Tools (menu bar, options bar, and tool palette). */
    TOOLS(RibbonIcons.PEN),

    /** CAD. */
    CAD(RibbonIcons.LAYERS),
}

/**
 * The window frame of the ribbon demo apps: a title bar at the top (a [WRibbonTitleBar] if there is a ribbon),
 * [top] (the ribbon or menu bar) and [content] below it, and [statusBar] at the bottom.
 */
class RibbonDemoWindow(
    /** The window title. */
    title: String,
    /** This app (used for the title bar icon). */
    private val app: RibbonDemoApp,
    /** The ribbon to connect to the title bar (if null, no title bar is created). */
    val ribbon: WRibbon?,
) {
    /** The window. */
    val frame: WFrame = WFrame(title = title)

    /** The title bar (when there is a ribbon). */
    val titleBar: WRibbonTitleBar? = ribbon?.let { WRibbonTitleBar(it) }

    private val root = WGrid()

    init {
        root.addRow(GridLength.AUTO)
        root.addRow(GridLength.AUTO)
        root.addRow(GridLength.star())
        root.addRow(GridLength.AUTO)
        titleBar?.let {
            it.appIcon = app.icon
            root.add(it, row = 0, column = 0)
        }
    }

    /** The top area below the title bar (ribbon, menu bar, or options bar). */
    fun setTop(component: WComponent) = root.add(component, row = 1, column = 0)

    /** The content in the middle (placed on the theme's window background surface). */
    fun setContent(component: WComponent, brushKey: String = "RibbonWindowBackgroundBrush") =
        root.add(WRibbonTheme.surface(component, brushKey), row = 2, column = 0)

    /** The status bar at the bottom. */
    fun setStatusBar(component: WComponent) = root.add(component, row = 3, column = 0)

    /** Switches between light and dark. */
    fun setDark(dark: Boolean) {
        WRibbonTheme.setTheme(root, if (dark) ElementTheme.DARK else ElementTheme.LIGHT)
        frame.appWindow.titleBar.preferredTheme = if (dark) TitleBarTheme.DARK else TitleBarTheme.LIGHT
    }

    /** Shows the window ([width] x [height] are in physical pixels). */
    fun show(width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT) {
        frame.setContentPane(WRibbonTheme.surface(root, "RibbonChromeBackgroundBrush"))
        titleBar?.attachToWindow(frame)
        frame.appWindow.resize(width, height)
        frame.isVisible = true
    }

    companion object {
        private const val DEFAULT_WIDTH = 2400
        private const val DEFAULT_HEIGHT = 1350
        private const val AVATAR_SIZE = 32.0
        private const val SEMI_BOLD = 600

        /** The account at the right end of the title bar (a circle with initials). */
        @JvmStatic
        fun avatar(initials: String, color: WColor): WComponent {
            val circle = WBorder(
                WLabel(initials).also {
                    it.foreground = WColor(255, 255, 255)
                    it.fontWeight = SEMI_BOLD
                    it.horizontalAlignment = HorizontalAlignment.CENTER
                    it.verticalAlignment = VerticalAlignment.CENTER
                },
            )
            circle.width = AVATAR_SIZE
            circle.height = AVATAR_SIZE
            circle.cornerRadius = AVATAR_SIZE / 2
            circle.background = color
            return circle
        }
    }
}
