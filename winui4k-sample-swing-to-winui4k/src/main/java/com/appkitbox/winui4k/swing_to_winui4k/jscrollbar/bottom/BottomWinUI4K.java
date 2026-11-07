package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.bottom;

import com.appkitbox.winui4k.WAdjustable;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 16.2 Scroll to the end after adding a line (WinUI4K). */
public class BottomWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("16.2 Bottom (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel log = new WPanel();
            WScrollPane scroll = new WScrollPane(log);
            WButton add = new WButton("Add a line");
            add.addActionListener(e -> {
                log.add(new WLabel("Line " + (log.getComponentCount() + 1)));
                // no revalidate(): WPanel measures itself again
                // the maximum grows after the layout, so scroll later
                WinUiUtilities.invokeLater(() -> {
                    WAdjustable bar = scroll.getVerticalScrollBar();
                    bar.setValue(bar.getMaximum());
                });
            });

            frame.add(scroll, WBorderLayout.CENTER);
            frame.add(add, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // add 30 lines (after the first layout)
            WinUiUtilities.schedule(500, () -> {
                for (int i = 0; i < 30; i++) {
                    add.doClick();
                }
            });
        });
    }
}
