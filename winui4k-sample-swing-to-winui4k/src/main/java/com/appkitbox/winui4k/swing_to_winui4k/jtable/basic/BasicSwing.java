package com.appkitbox.winui4k.swing_to_winui4k.jtable.basic;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/** 19.2 Show data in a table (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("19.2 Basic Table (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] columns = {"Name", "Department", "Age"};
            Object[][] data = {
                {"Alice", "Sales", 34},
                {"Bob", "Engineering", 28},
                {"Carol", "Admin", 45},
                {"Dave", "Engineering", 31},
            };
            JTable table = new JTable(data, columns);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(360, 160));
            frame.add(scroll);

            frame.setSize(440, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
