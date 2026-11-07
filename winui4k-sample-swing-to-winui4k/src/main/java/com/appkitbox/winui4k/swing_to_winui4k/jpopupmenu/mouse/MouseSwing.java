package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.mouse;

import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/** 22.3 Open the menu at the mouse position yourself (Swing). */
public class MouseSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("22.3 Popup Trigger (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel target = new JLabel("Right-click this label");

            JPopupMenu popup = new JPopupMenu();
            popup.add("Rename").addActionListener(e -> target.setText("Rename"));
            popup.add("Delete").addActionListener(e -> target.setText("Delete"));
            target.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showPopup(e);
                }

                private void showPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            frame.add(target);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once at (60, 10) of the label, as if it were right-clicked there
            popup.show(target, 60, 10);
        });
    }
}
