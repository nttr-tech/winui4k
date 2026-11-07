package com.appkitbox.winui4k.swing_to_winui4k.jfilechooser.save;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 27.4 Choose where to save (Swing). */
public class SaveSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("27.4 Save (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel("Not saved");
            JButton save = new JButton("Save As...");
            save.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File("files"));
                chooser.setDialogTitle("Save the report");
                chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
                chooser.setSelectedFile(new File("report.txt"));   // suggested name
                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    status.setText("Saved to " + file.getPath());
                }
            });

            frame.add(save);
            frame.add(status);

            frame.setSize(480, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            save.doClick();
        });
    }
}
