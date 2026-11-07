package com.appkitbox.winui4k.swing_to_winui4k.jlist.itemclick;

import com.appkitbox.winui4k.ListViewSelectionMode;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.15 Item click and check box selection (WinUI4K). */
public class ItemClickWinUI4K {
    private static void navigateTo(String item) {
        System.out.println("Navigate: " + item);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.15 Item Click and Check Boxes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            // Item click: receive a "clicked" event separately from selection (e.g. navigation menus)
            WList menu = new WList(new String[] {"Documents", "Pictures", "Music"});
            menu.setItemClickEnabled(true);
            menu.addItemClickListener(item -> navigateTo(item));   // item is the text of the clicked item

            // Multiple selection with check boxes (easier to multi-select by touch)
            WList tags = new WList(new String[] {"Work", "Family", "Hobbies", "Shopping"});
            tags.setSelectionMode(ListViewSelectionMode.MULTIPLE);

            menu.setWidth(200);
            menu.setHeight(130);
            tags.setWidth(200);
            tags.setHeight(180);
            frame.add(menu);
            frame.add(tags);

            frame.setSize(420, 440);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
