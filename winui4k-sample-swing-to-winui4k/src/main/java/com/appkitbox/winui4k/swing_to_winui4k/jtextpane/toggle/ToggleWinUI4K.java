package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.toggle;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 13.4 Bold and italic buttons for the selection (WinUI4K). */
public class ToggleWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("13.4 Bold and italic buttons (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextPane pane = new WTextPane();
            pane.setText("Select some words and press a button.");
            pane.setWidth(400);
            pane.setHeight(100);
            WButton bold = new WButton("Bold");
            bold.addActionListener(e -> pane.toggleSelectionBold());
            WButton italic = new WButton("Italic");
            italic.addActionListener(e -> pane.toggleSelectionItalic());

            frame.add(pane);
            frame.add(bold);
            frame.add(italic);

            frame.setSize(560, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Select "some words" and press Bold
            pane.select(7, 17);
            bold.doClick();
        });
    }
}
