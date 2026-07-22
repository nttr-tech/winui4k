package com.appkitbox.winui4k.sample.filer

import com.appkitbox.winui4k.WinUiUtilities

/**
 * A Fluent Design file manager (a practical-app sample for winui4k).
 * MVP scope: a single pane, tabs, detail / icon views, basic file operations (no undo),
 * breadcrumbs, a sidebar, filtering, and Explorer-compatible shortcuts.
 */
fun main() {
    WinUiUtilities.invokeLater {
        FilerWindow().show()
    }
}
