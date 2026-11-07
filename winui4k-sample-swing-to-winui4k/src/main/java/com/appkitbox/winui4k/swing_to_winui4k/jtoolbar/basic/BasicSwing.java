package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.basic;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/** 23.2 A toolbar with buttons (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("23.2 Toolbar (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel status = new JLabel("Click a toolbar button");

            JButton newButton = new JButton(new ImageIcon("icons/new.png"));
            newButton.setToolTipText("New file");
            newButton.addActionListener(e -> status.setText("New"));
            JButton openButton = new JButton(new ImageIcon("icons/open.png"));
            openButton.setToolTipText("Open a file");
            openButton.addActionListener(e -> status.setText("Open"));
            JButton saveButton = new JButton(new ImageIcon("icons/save.png"));
            saveButton.setToolTipText("Save the file");
            saveButton.addActionListener(e -> status.setText("Save"));

            JToolBar toolBar = new JToolBar();
            toolBar.add(newButton);
            toolBar.add(openButton);
            toolBar.add(saveButton);

            frame.add(toolBar, BorderLayout.NORTH);
            frame.add(status, BorderLayout.CENTER);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
