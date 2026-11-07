package com.appkitbox.winui4k.swing_to_winui4k.layout.box;

import com.appkitbox.winui4k.WBox;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 30.5 BoxLayout and Box (WinUI4K). */
public class BoxWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("30.5 BoxLayout and Box (WinUI4K)");

            // a row of buttons pushed to the right by glue
            WLayoutPanel buttons = WBox.createHorizontalBox();
            buttons.add(WBox.createHorizontalGlue());
            buttons.add(new WButton("OK"));
            buttons.add(WBox.createHorizontalStrut(8));
            buttons.add(new WButton("Cancel"));
            buttons.setAlignmentX(WComponent.LEFT_ALIGNMENT);

            // a column: a fixed gap (strut) between the first two, the rest of the height (glue) above the row
            WLayoutPanel column = WBox.createVerticalBox();
            column.add(new WButton("First"));
            column.add(WBox.createVerticalStrut(8));
            column.add(new WButton("Second"));
            column.add(WBox.createVerticalGlue());
            column.add(buttons);

            frame.add(column);

            frame.setSize(360, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
