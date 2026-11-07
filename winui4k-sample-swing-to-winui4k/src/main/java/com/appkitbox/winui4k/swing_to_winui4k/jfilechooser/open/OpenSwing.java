package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.open;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 27.2 Choose a file to open (Swing). */
public class OpenSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("27.2 Open (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("No file opened");
            JButton open = new JButton("Open...");
            open.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File("files"));
                chooser.setDialogTitle("Open a file");
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    status.setText("Opened " + file.getName());
                }
            });

            frame.add(open);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            open.doClick();
        });
    }
}
