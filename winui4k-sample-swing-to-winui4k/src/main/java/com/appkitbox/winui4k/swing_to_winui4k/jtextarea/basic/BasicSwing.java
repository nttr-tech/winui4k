package com.appkitbox.winui4k.swing_to_winui4k.jtextarea.basic;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** 12.2 Create a text area and read and write its text (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("12.2 Read and write text (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JTextArea memo = new JTextArea(5, 30);
            memo.setText("Buy milk\nCall Alice\nSend the report");
            JLabel status = new JLabel();
            JButton count = new JButton("Count lines");
            count.addActionListener(e -> status.setText(memo.getLineCount() + " lines, "
                    + memo.getText().length() + " characters"));

            frame.add(new JScrollPane(memo));
            frame.add(count);
            frame.add(status);

            frame.setSize(480, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            count.doClick();
        });
    }
}
