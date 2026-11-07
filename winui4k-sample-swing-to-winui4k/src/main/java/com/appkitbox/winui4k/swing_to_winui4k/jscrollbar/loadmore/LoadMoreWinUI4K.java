package com.appkitbox.winui4k.swing_to_winui4k.jscrollbar.loadmore;

import com.appkitbox.winui4k.WAdjustable;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WinUiUtilities;

/** 16.5 Load more items when scrolled to the end (WinUI4K). */
public class LoadMoreWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("16.5 LoadMore (WinUI4K)");
            frame.setLayout(new WBorderLayout());

            WPanel items = new WPanel();
            WLabel status = new WLabel();
            Runnable loadMore = () -> {
                int start = items.getComponentCount();
                for (int i = start + 1; i <= start + 20; i++) {
                    items.add(new WLabel("Item " + i));
                }
                items.revalidate();
                status.setText(items.getComponentCount() + " items loaded");
            };
            loadMore.run();

            WScrollPane scroll = new WScrollPane(items);
            WAdjustable bar = scroll.getVerticalScrollBar();
            bar.addAdjustmentListener(e -> {
                boolean atEnd = e.getValue() + bar.getVisibleAmount() >= bar.getMaximum();
                if (atEnd && !e.getValueIsAdjusting()) {
                    loadMore.run();
                }
            });

            frame.add(scroll, WBorderLayout.CENTER);
            frame.add(status, WBorderLayout.SOUTH);

            frame.setSize(400, 260);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            bar.setValue(bar.getMaximum());     // scroll to the end once
        });
    }
}
