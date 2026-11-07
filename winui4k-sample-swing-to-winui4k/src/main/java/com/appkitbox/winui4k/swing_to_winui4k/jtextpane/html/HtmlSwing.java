package com.appkitbox.winui4k.swing_to_winui4k.jtextpane.html;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** 13.7 Show read-only styled text (Swing). */
public class HtmlSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("13.7 Read-only styled text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JEditorPane notice = new JEditorPane("text/html",
                    "<p>Version 2.0 is <b>ready</b>. Read the <i>release notes</i> before you <u>upgrade</u>.</p>"
                            + "<p>Old settings are kept.</p>");
            notice.setEditable(false);
            notice.setPreferredSize(new Dimension(400, 100));

            frame.add(notice);

            frame.setSize(480, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
