package com.appkitbox.winui4k.swing_to_winui4k.jpopupmenu.listener;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** 22.4 Update items just before the menu opens (Swing). */
public class ListenerSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("22.4 Popup Menu Listener (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            JLabel target = new JLabel("Right-click this label");
            List<String> history = new ArrayList<>();
            history.add("Typing");

            JPopupMenu popup = new JPopupMenu();
            JMenuItem undo = popup.add("Undo");
            undo.addActionListener(e -> history.remove(history.size() - 1));
            popup.add("Select All");
            popup.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    undo.setEnabled(!history.isEmpty());
                    undo.setText(history.isEmpty() ? "Undo" : "Undo " + history.get(history.size() - 1));
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            target.setComponentPopupMenu(popup);

            frame.add(target);

            frame.setSize(400, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // open the menu once so that the screenshot shows the updated item
            popup.show(target, 20, 20);
        });
    }
}
