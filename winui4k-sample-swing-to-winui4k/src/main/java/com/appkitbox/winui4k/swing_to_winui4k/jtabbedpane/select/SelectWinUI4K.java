package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.select;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WinUiUtilities;

/** 18.3 Select a tab and react to the selection (WinUI4K). */
public class SelectWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("18.3 Select (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WTabView tabs = new WTabView();
            tabs.addTab("Inbox", new WLabel("3 new messages"));
            tabs.addTab("Sent", new WLabel("12 messages"));
            tabs.addTab("Drafts", new WLabel("1 draft"));
            WLabel status = new WLabel(" ");
            tabs.addChangeListener(e -> {
                int index = tabs.getSelectedIndex();
                status.setText("Selected: " + tabs.getTitleAt(index) + " (" + index + ")");
            });

            frame.add(tabs, WBorderLayout.CENTER);
            frame.add(status, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            tabs.setSelectedIndex(2);       // select "Drafts"
        });
    }
}
