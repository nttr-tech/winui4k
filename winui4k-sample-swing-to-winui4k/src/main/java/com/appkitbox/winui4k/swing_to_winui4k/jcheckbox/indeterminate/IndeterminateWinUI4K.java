package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.indeterminate;

import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WItemListener;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.Arrays;

/** 5.5 Indeterminate state (WinUI4K). */
public class IndeterminateWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("5.5 Indeterminate state (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WCheckBox all = new WCheckBox("Select all");
            WCheckBox[] fruits = {new WCheckBox("Apples"), new WCheckBox("Bananas"), new WCheckBox("Cherries")};
            all.addActionListener(e -> {
                for (WCheckBox fruit : fruits) {
                    fruit.setSelected(all.isSelected());
                }
            });
            // Show "Select all" as indeterminate (a dash) when only some fruits are checked
            WItemListener update = e -> {
                long checked = Arrays.stream(fruits).filter(WCheckBox::isSelected).count();
                all.setChecked(checked == 0 ? Boolean.FALSE : checked == fruits.length ? Boolean.TRUE : null);
            };
            for (WCheckBox fruit : fruits) {
                fruit.addItemListener(update);
            }

            frame.add(all);
            for (WCheckBox fruit : fruits) {
                frame.add(fruit);
            }

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            fruits[0].setSelected(true); // check only one fruit at startup
        });
    }
}
