package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.toggle;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WAppBarButton;
import com.appkitbox.winui4k.WAppBarToggleButton;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 23.4 Toggle buttons and separators (WinUI4K). */
public class ToggleWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("23.4 Toggle Buttons (WinUI4K)");
            WLabel status = new WLabel("Bold: true");

            WCommandBar toolBar = new WCommandBar();
            toolBar.setFloatable(false);             // has no effect: a WCommandBar never floats
            toolBar.add(new WAppBarButton("Cut", Symbol.CUT));
            toolBar.add(new WAppBarButton("Copy", Symbol.COPY));
            toolBar.add(new WAppBarButton("Paste", Symbol.PASTE));
            toolBar.addSeparator();

            WAppBarToggleButton bold = new WAppBarToggleButton("Bold", Symbol.BOLD);
            bold.setSelected(true);
            bold.setToolTipText("Bold");
            bold.addItemListener(e -> status.setText("Bold: " + bold.isSelected()));
            WAppBarToggleButton italic = new WAppBarToggleButton("Italic", Symbol.ITALIC);
            italic.setToolTipText("Italic");
            WAppBarToggleButton underline = new WAppBarToggleButton("Underline", Symbol.UNDERLINE);
            underline.setToolTipText("Underline");
            toolBar.add(bold);
            toolBar.add(italic);
            toolBar.add(underline);

            frame.add(toolBar, WBorderLayout.NORTH);
            frame.add(status, WBorderLayout.CENTER);

            frame.setSize(600, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
