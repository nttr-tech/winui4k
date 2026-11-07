package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.vertical;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 17.3 Split top and bottom by a ratio (Swing). */
public class VerticalSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("17.3 Vertical (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTextArea editor = new JTextArea("Editor");
            JTextArea console = new JTextArea("Console");
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(editor), new JScrollPane(console));
            split.setResizeWeight(0.7);     // the editor gets 70% of extra space

            frame.add(split, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            split.setDividerLocation(0.7);  // works only after the split pane has its size
        });
    }
}
