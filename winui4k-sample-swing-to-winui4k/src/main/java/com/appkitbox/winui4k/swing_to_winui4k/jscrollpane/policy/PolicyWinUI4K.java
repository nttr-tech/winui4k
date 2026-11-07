package com.appkitbox.winui4k.swing_to_winui4k.jscrollpane.policy;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WScrollPaneConstants;
import com.appkitbox.winui4k.WinUiUtilities;

/** 15.3 Choose when the scroll bars appear (WinUI4K). */
public class PolicyWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("15.3 Policy (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel lines = new WPanel();
            for (int i = 1; i <= 5; i++) {
                lines.add(new WLabel("Line " + i + ": a long line of text that is much wider than the window, so it needs a horizontal scroll bar"));
            }
            WScrollPane scroll = new WScrollPane(lines,
                    WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            frame.add(scroll, WBorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
