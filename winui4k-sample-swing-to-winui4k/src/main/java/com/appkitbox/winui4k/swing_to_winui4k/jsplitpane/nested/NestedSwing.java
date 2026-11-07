package com.appkitbox.winui4k.swing_to_winui4k.jsplitpane.nested;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 17.5 Nest split panes into three areas (Swing). */
public class NestedSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("17.5 Nested (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JList<String> files = new JList<>(new String[] {"Main.java", "Util.java"});
            JTextArea editor = new JTextArea("class Main {}");
            JTextArea console = new JTextArea("BUILD SUCCESSFUL");

            JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(editor), new JScrollPane(console));
            right.setResizeWeight(1.0);         // only the editor grows
            JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(files), right);
            main.setDividerLocation(120);
            main.setOneTouchExpandable(true);   // small arrows on the divider

            frame.add(main, BorderLayout.CENTER);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            right.setDividerLocation(150);
        });
    }
}
