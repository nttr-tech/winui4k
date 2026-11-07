package com.appkitbox.winui4k.swing_to_winui4k.jdialog.closeoperation;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 25.4 Handle the close operation yourself (Swing). */
public class CloseOperationSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("25.4 Close Operation (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JLabel message = new JLabel("You have unsaved changes.");
            JDialog dialog = new JDialog(frame, "Settings", true);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    message.setText("Click Save or Discard to close.");
                }
            });
            JButton save = new JButton("Save");
            save.addActionListener(e -> dialog.dispose());
            JButton discard = new JButton("Discard");
            discard.addActionListener(e -> dialog.dispose());
            JPanel content = new JPanel(new GridLayout(0, 1, 4, 4));
            content.add(message);
            JPanel buttons = new JPanel();
            buttons.add(save);
            buttons.add(discard);
            content.add(buttons);
            dialog.add(content);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }
}
