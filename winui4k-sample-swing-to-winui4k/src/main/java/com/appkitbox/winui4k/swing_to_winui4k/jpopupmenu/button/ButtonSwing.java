package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.button;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/** 22.5 Open a menu below a button (Swing). */
public class ButtonSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("22.5 Button Menu (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Not exported");

            JPopupMenu popup = new JPopupMenu();
            popup.add("Export as PDF").addActionListener(e -> status.setText("PDF"));
            popup.add("Export as CSV").addActionListener(e -> status.setText("CSV"));
            JButton export = new JButton("Export");
            export.addActionListener(e -> popup.show(export, 0, export.getHeight()));

            frame.add(export);
            frame.add(status);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // click the button once so that the screenshot shows the menu
            export.doClick();
        });
    }
}
