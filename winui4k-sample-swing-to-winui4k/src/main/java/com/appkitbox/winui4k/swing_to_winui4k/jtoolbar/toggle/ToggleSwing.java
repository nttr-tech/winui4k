package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.toggle;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/** 23.4 Toggle buttons and separators (Swing). */
public class ToggleSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("23.4 Toggle Buttons (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JLabel status = new JLabel("Bold: true");

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);             // don't let the user drag the toolbar out
            toolBar.add(new JButton(new ImageIcon("icons/cut.png")));
            toolBar.add(new JButton(new ImageIcon("icons/copy.png")));
            toolBar.add(new JButton(new ImageIcon("icons/paste.png")));
            toolBar.addSeparator();

            JToggleButton bold = new JToggleButton(new ImageIcon("icons/bold.png"), true);
            bold.setToolTipText("Bold");
            bold.addItemListener(e -> status.setText("Bold: " + bold.isSelected()));
            JToggleButton italic = new JToggleButton(new ImageIcon("icons/italic.png"));
            italic.setToolTipText("Italic");
            JToggleButton underline = new JToggleButton(new ImageIcon("icons/underline.png"));
            underline.setToolTipText("Underline");
            toolBar.add(bold);
            toolBar.add(italic);
            toolBar.add(underline);

            frame.add(toolBar, BorderLayout.NORTH);
            frame.add(status, BorderLayout.CENTER);

            frame.setSize(600, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
