package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.sync;

import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGridLayout;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 16.4 Scroll two panes together (WinUI4K). */
public class SyncWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("16.4 Sync (WinUI4K)");
            frame.setLayout(new WGridLayout(1, 2));

            WPanel original = new WPanel();
            WPanel translated = new WPanel();
            for (int i = 1; i <= 50; i++) {
                original.add(new WLabel("Original " + i));
                translated.add(new WLabel("Translated " + i));
            }
            WScrollPane left = new WScrollPane(original);
            WScrollPane right = new WScrollPane(translated);
            left.getVerticalScrollBar().addAdjustmentListener(
                    e -> right.getVerticalScrollBar().setValue(e.getValue()));

            frame.add(left);
            frame.add(right);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // scroll the left pane once (after the first layout)
            WinUiUtilities.schedule(500, () -> left.getVerticalScrollBar().setValue(200));
        });
    }
}
