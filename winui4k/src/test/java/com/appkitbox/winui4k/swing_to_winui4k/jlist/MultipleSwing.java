package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 3.6 Allowing multiple selection (Swing). */
public class MultipleSwing {
    private static void openFile(String name) {
        System.out.println("Open: " + name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.6 Allowing multiple selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            String[] names = {"Report.docx", "Budget.xlsx", "Presentation.pptx", "Photo.png", "Memo.txt", "Minutes.md"};
            JList<String> files = new JList<>(names);
            // MULTIPLE_INTERVAL_SELECTION by default (several items with Ctrl / Shift)
            JButton open = new JButton("Open");
            open.addActionListener(e -> {
                for (String name : files.getSelectedValuesList()) openFile(name);
            });

            JScrollPane scroll = new JScrollPane(files);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(scroll);
            frame.add(open);

            files.setSelectionInterval(1, 2);
            files.addSelectionInterval(4, 4);

            frame.setSize(420, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
