package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.append;

import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 12.4 Append lines to a log (Swing). */
public class AppendSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("12.4 Append to a log (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextArea log = new JTextArea(6, 30);
            log.setEditable(false);
            JButton add = new JButton("Add log");
            int[] count = {0};
            add.addActionListener(e -> {
                count[0]++;
                log.append(LocalTime.of(9, 0, count[0]).format(DateTimeFormatter.ISO_LOCAL_TIME)
                        + " Step " + count[0] + " finished\n");
                log.setCaretPosition(log.getDocument().getLength());   // scroll to the end
            });

            frame.add(new JScrollPane(log));
            frame.add(add);

            frame.setSize(520, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            for (int i = 0; i < 10; i++) {
                add.doClick();
            }
        });
    }
}
