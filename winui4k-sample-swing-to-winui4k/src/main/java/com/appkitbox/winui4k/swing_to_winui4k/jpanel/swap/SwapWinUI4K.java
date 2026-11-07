package com.appkitbox.winui4k.swing_to_winui4k.jpanel.swap;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 14.6 Replace what a panel shows (WinUI4K). */
public class SwapWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("14.6 Swap (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WLayoutPanel page = new WLayoutPanel(new WFlowLayout(WFlowLayout.LEFT));
            page.add(new WLabel("Step 1: choose a file"));
            WButton next = new WButton("Next");
            next.addActionListener(e -> {
                page.removeAll();
                page.add(new WLabel("Step 2: confirm"));
                page.add(new WButton("Finish"));
                page.revalidate();
                page.repaint();
            });

            frame.add(page, WBorderLayout.CENTER);
            frame.add(next, WBorderLayout.SOUTH);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            next.doClick();     // show step 2
        });
    }
}
