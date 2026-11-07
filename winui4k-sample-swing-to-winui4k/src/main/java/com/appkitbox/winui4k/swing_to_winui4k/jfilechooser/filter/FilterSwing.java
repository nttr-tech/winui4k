package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.filter;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 27.3 Show only some kinds of files (Swing). */
public class FilterSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("27.3 Filter (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("No file opened");
            JButton open = new JButton("Open...");
            open.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File("files"));
                chooser.setDialogTitle("Open a text file");
                chooser.setAcceptAllFileFilterUsed(false);   // remove "All Files"
                chooser.addChoosableFileFilter(new FileNameExtensionFilter("Markdown files", "md"));
                chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text files", "txt", "csv"));  // selected
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    status.setText("Opened " + chooser.getSelectedFile().getName());
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
