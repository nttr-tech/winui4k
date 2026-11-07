package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.close;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** 18.4 Close a tab with its close button (Swing). */
public class CloseSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("18.4 Close (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();
            for (String name : new String[] {"a.txt", "b.txt", "c.txt"}) {
                tabs.addTab(name, new JLabel("Contents of " + name));
                // a tab with a close button is a custom tab component
                JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                tab.setOpaque(false);
                tab.add(new JLabel(name));
                JButton close = new JButton("x");
                close.setMargin(new Insets(0, 2, 0, 2));
                close.addActionListener(e -> tabs.remove(tabs.indexOfTabComponent(tab)));
                tab.add(close);
                tabs.setTabComponentAt(tabs.getTabCount() - 1, tab);
            }

            frame.add(tabs, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            tabs.remove(1);     // close "b.txt"
        });
    }
}
