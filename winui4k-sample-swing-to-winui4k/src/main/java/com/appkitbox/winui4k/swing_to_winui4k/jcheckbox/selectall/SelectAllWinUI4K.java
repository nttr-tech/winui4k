package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.selectall;

import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 5.4 Select all (WinUI4K). */
public class SelectAllWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("5.4 Select all (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WCheckBox all = new WCheckBox("Select all");
            WCheckBox[] fruits = {new WCheckBox("Apples"), new WCheckBox("Bananas"), new WCheckBox("Cherries")};
            all.addActionListener(e -> {
                for (WCheckBox fruit : fruits) {
                    fruit.setSelected(all.isSelected());
                }
            });

            frame.add(all);
            for (WCheckBox fruit : fruits) {
                frame.add(fruit);
            }

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            all.doClick(); // check "Select all" once at startup
        });
    }
}
