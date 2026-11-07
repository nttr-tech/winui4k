package com.appkitbox.winui4k.swing_to_winui4k.jtabbedpane.state;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** 18.5 Disable a tab, rename it, and give it a mnemonic (Swing). */
public class StateSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("18.5 State (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Document", new JLabel("Hello"));
            tabs.addTab("Preview", new JLabel("Preview"));
            tabs.addTab("Publish", new JLabel("Publish"));

            tabs.setTitleAt(0, "Document *");           // mark as modified
            tabs.setEnabledAt(2, false);                // cannot publish yet
            tabs.setToolTipTextAt(2, "Save the document first");
            tabs.setMnemonicAt(1, KeyEvent.VK_P);       // Alt+P selects "Preview"

            frame.add(tabs, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
