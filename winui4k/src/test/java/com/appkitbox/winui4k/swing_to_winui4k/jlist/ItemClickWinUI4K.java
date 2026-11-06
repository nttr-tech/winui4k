package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import com.appkitbox.winui4k.ListViewSelectionMode;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.15 Item clicks and check box selection (WinUI4K). */
public class ItemClickWinUI4K {
    private static void navigateTo(String item) {
        System.out.println("Go to: " + item);
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.15 Item clicks and check boxes (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            // Item clicks: receive that an item was "pressed", separately from selection (navigation menus etc.)
            WList menu = new WList(new String[] {"Documents", "Pictures", "Music"});
            menu.setItemClickEnabled(true);
            menu.addItemClickListener(item -> navigateTo(item));   // item is the string of the pressed item

            // Multiple selection with check boxes (easy to select several items even by touch)
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
