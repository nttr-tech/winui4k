package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.loadmore;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 16.5 Load more items when scrolled to the end (Swing). */
public class LoadMoreSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("16.5 LoadMore (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel items = new JPanel(new GridLayout(0, 1));
            JLabel status = new JLabel();
            Runnable loadMore = () -> {
                int start = items.getComponentCount();
                for (int i = start + 1; i <= start + 20; i++) {
                    items.add(new JLabel("Item " + i));
                }
                items.revalidate();
                status.setText(items.getComponentCount() + " items loaded");
            };
            loadMore.run();

            JScrollPane scroll = new JScrollPane(items);
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.addAdjustmentListener(e -> {
                boolean atEnd = e.getValue() + bar.getVisibleAmount() >= bar.getMaximum();
                if (atEnd && !e.getValueIsAdjusting()) {
                    loadMore.run();
                }
            });

            frame.add(scroll, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            bar.setValue(bar.getMaximum());     // scroll to the end once
        });
    }
}
