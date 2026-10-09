package com.appkitbox.winui4k.sample.ribbon.shell

import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.TitleBarTheme
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WRibbon
import com.appkitbox.winui4k.WRibbonTheme
import com.appkitbox.winui4k.WRibbonTitleBar
import com.appkitbox.winui4k.WRibbonToolBar
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIcons
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonToolBarModel
import com.appkitbox.winui4k.ribbon.RibbonToolBarOrientation

/** The kind of demo app (the items in the rail on the left). */
enum class RibbonDemoApp(internal val label: String, internal val icon: RibbonIcon, internal val module: String) {
    /** Word style. */
    WORD("Word", RibbonIcons.DOCUMENT, "winui4k-sample-ribbon-word"),

    /** Excel style. */
    EXCEL("Excel", RibbonIcons.TABLE, "winui4k-sample-ribbon-excel"),

    /** PowerPoint style. */
    POWER_POINT("PowerPoint", RibbonIcons.PRESENT, "winui4k-sample-ribbon-powerpoint"),

    /** Tools (menu bar, options bar, and tool palette). */
    TOOLS("Tools", RibbonIcons.PEN, "winui4k-sample-ribbon-tools"),

    /** CAD. */
    CAD("CAD", RibbonIcons.LAYERS, "winui4k-sample-ribbon-cad"),
}

/**
 * The window frame of the ribbon demo apps (same layout as the RibbonSpace demos): a title bar at the top (a
 * [WRibbonTitleBar] if there is a ribbon), a rail on the left for switching demo apps, [top] (the ribbon or menu bar) and
 * [content] in the middle, and [statusBar] at the bottom.
 */
class RibbonDemoWindow(
    /** The window title. */
    title: String,
    /** This app (shown as selected in the rail). */
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
        root.addColumn(GridLength.AUTO)
        root.addColumn(GridLength.star())
        titleBar?.let {
            it.appIcon = app.icon
            root.add(it, row = 0, column = 0, columnSpan = 2)
        }
        root.add(appRail(), row = 1, column = 0, rowSpan = 3)
    }

    /** The top area next to the rail (ribbon, menu bar, or options bar). */
    fun setTop(component: WComponent) = root.add(component, row = 1, column = 1)

    /** The content in the middle (placed on the theme's window background surface). */
    fun setContent(component: WComponent, brushKey: String = "RibbonWindowBackgroundBrush") =
        root.add(WRibbonTheme.surface(component, brushKey), row = 2, column = 1)

    /** The status bar at the bottom. */
    fun setStatusBar(component: WComponent) = root.add(component, row = 3, column = 1)

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

    /** The rail for switching demo apps (a vertical, icon-only toolbar). */
    private fun appRail(): WComponent {
        val model = RibbonToolBarModel("rail")
        model.orientation = RibbonToolBarOrientation.VERTICAL
        model.showLabels = false
        for (entry in RibbonDemoApp.entries) {
            model.items.add(
                toggle("rail.${entry.name}", entry.label, entry.icon, RibbonItemSize.SMALL).also { item ->
                    item.groupName = "rail"
                    item.isChecked = entry == app
                    item.addActionListener {
                        if (entry != app) {
                            item.isChecked = false
                            model.items.filterIsInstance<com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel>().first { it.id == "rail.${app.name}" }.isChecked = true
                            showOtherApp(entry)
                        }
                    }
                },
            )
        }
        model.items.add(button("rail.settings", "Settings", RibbonIcons.SETTINGS, RibbonItemSize.SMALL))
        val rail = WRibbonToolBar(model)
        rail.ribbon = ribbon
        rail.margin = RAIL_MARGIN
        return rail
    }

    private fun showOtherApp(entry: RibbonDemoApp) {
        val text = WLabel("The ${entry.label} demo is a separate app. Launch it with gradlew :${entry.module}:run.")
        val dialog = WContentDialog(entry.label, text)
        dialog.closeButtonText = "OK"
        dialog.show(root)
    }

    companion object {
        private const val DEFAULT_WIDTH = 2400
        private const val DEFAULT_HEIGHT = 1350
        private const val RAIL_MARGIN = 4.0
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
