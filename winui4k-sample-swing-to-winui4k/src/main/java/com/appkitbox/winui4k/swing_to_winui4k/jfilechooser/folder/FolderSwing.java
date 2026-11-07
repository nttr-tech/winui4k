package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.folder;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** 27.5 Choose a folder (Swing). */
public class FolderSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("27.5 Folder (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("No folder chosen");
            JButton browse = new JButton("Browse...");
            browse.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File("files"));
                chooser.setDialogTitle("Choose the output folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setApproveButtonText("Select");
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    status.setText("Output: " + chooser.getSelectedFile().getPath());
                }
            });

            frame.add(browse);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            browse.doClick();
        });
    }
}
