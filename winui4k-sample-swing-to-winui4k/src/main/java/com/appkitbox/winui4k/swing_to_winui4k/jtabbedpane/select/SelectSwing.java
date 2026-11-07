package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.select;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** 18.3 Select a tab and react to the selection (Swing). */
public class SelectSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("18.3 Select (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Inbox", new JLabel("3 new messages"));
            tabs.addTab("Sent", new JLabel("12 messages"));
            tabs.addTab("Drafts", new JLabel("1 draft"));
            JLabel status = new JLabel(" ");
            tabs.addChangeListener(e -> {
                int index = tabs.getSelectedIndex();
                status.setText("Selected: " + tabs.getTitleAt(index) + " (" + index + ")");
            });

            frame.add(tabs, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            tabs.setSelectedIndex(2);       // select "Drafts"
        });
    }
}
