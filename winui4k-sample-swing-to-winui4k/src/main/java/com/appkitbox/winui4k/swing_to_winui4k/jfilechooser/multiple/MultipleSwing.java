package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.multiple;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 27.6 Choose several files at once (Swing). */
public class MultipleSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("27.6 Multiple (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("No files added");
            JButton add = new JButton("Add Files...");
            add.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File("files"));
                chooser.setDialogTitle("Add files");
                chooser.setMultiSelectionEnabled(true);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();
                    status.setText("Added " + files.length + " files");
                }
            });

            frame.add(add);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            add.doClick();
        });
    }
}
