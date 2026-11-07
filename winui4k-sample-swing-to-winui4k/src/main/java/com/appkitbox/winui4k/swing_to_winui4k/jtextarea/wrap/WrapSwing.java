package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.wrap;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 12.3 Wrap long lines (Swing). */
public class WrapSwing {
    private static final String TEXT =
            "WinUI4K lets you call WinUI from Java and Kotlin, so a Swing application can be moved to a modern look.";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("12.3 Wrap long lines (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextArea noWrap = new JTextArea(TEXT, 4, 20);      // does not wrap by default

            JTextArea wrapped = new JTextArea(TEXT, 4, 20);
            wrapped.setLineWrap(true);
            wrapped.setWrapStyleWord(true);                     // break at word boundaries

            frame.add(new JScrollPane(noWrap));
            frame.add(new JScrollPane(wrapped));

            frame.setSize(560, 340);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
