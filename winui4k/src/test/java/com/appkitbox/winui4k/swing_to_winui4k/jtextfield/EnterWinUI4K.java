package com.appkitbox.winui4k.swing_to_winui4k.jtextfield;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 2.3 Handling Enter (WinUI4K). */
public class EnterWinUI4K {
    static void search(String text) {
        System.out.println("Search: " + text);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("2.3 Handling Enter (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WTextField query = new WTextField(20);
            query.addActionListener(e -> search(query.getText()));
            query.setText("Apple");

            frame.add(query);

            frame.setSize(400, 140);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
