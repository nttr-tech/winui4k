package com.appkitbox.winui4k.swing_to_winui4k.jcheckbox.selectall;

import java.awt.FlowLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 5.4 Select all (Swing). */
public class SelectAllSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("5.4 Select all (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JCheckBox all = new JCheckBox("Select all");
            JCheckBox[] fruits = {new JCheckBox("Apples"), new JCheckBox("Bananas"), new JCheckBox("Cherries")};
            all.addActionListener(e -> {
                for (JCheckBox fruit : fruits) {
                    fruit.setSelected(all.isSelected());
                }
            });

            frame.add(all);
            for (JCheckBox fruit : fruits) {
                frame.add(fruit);
            }

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            all.doClick(); // check "Select all" once at startup
        });
    }
}
