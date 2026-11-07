package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.header;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 15.6 Show line numbers that scroll with the content (WinUI4K). */
public class HeaderWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("15.6 Header (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel lines = new WPanel();
            WPanel numbers = new WPanel();
            for (int i = 1; i <= 40; i++) {
                lines.add(new WLabel("Line " + i));
                numbers.add(new WLabel(String.valueOf(i)));
            }
            numbers.setMargin(4, 0, 8, 0);     // left, top, right, bottom
            WScrollPane scroll = new WScrollPane(lines);
            scroll.setRowHeaderView(numbers);
            scroll.setColumnHeaderView(new WLabel("Text"));

            frame.add(scroll, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            scroll.getVerticalScrollBar().setValue(100);    // scroll once
        });
    }
}
