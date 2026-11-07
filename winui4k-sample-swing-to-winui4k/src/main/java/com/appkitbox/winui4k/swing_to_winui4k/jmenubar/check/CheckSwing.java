package com.appkitbox.winui4k.swing_to_winui4k.jmenubar.check;

import java.awt.FlowLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

/** 21.5 Check box and radio button menu items (Swing). */
public class CheckSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("21.5 Check and Radio Items (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel status = new JLabel("Zoom: Medium");

            JMenu viewMenu = new JMenu("View");
            JCheckBoxMenuItem statusBar = new JCheckBoxMenuItem("Status Bar", true);
            statusBar.addItemListener(e -> status.setVisible(statusBar.isSelected()));
            JCheckBoxMenuItem wordWrap = new JCheckBoxMenuItem("Word Wrap");
            viewMenu.add(statusBar);
            viewMenu.add(wordWrap);
            viewMenu.addSeparator();

            ButtonGroup zoom = new ButtonGroup();
            for (String size : new String[] {"Small", "Medium", "Large"}) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(size, size.equals("Medium"));
                item.addActionListener(e -> status.setText("Zoom: " + e.getActionCommand()));
                zoom.add(item);
                viewMenu.add(item);
            }

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(viewMenu);
            frame.setJMenuBar(menuBar);
            frame.add(status);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the View menu once so that the screenshot shows the check marks
            viewMenu.doClick();
        });
    }
}
