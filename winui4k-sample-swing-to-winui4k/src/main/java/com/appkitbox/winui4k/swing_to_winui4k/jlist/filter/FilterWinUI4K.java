package com.appkitbox.winui4k.swing_to_winui4k.jlist.filter;

import com.appkitbox.winui4k.WDefaultListModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.Arrays;
import java.util.List;

/** 3.13 Filter as you type (WinUI4K). */
public class FilterWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.13 Filter as You Type (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            List<String> all = Arrays.asList("Tokyo", "Osaka", "Nagoya", "Sapporo", "Fukuoka", "Kyoto");
            WDefaultListModel<String> model = new WDefaultListModel<>();
            all.forEach(model::addElement);
            WList list = new WList(model);

            WTextField filter = new WTextField();
            filter.setPlaceholderText("Filter");
            filter.addTextChangedListener(text -> {
                model.clear();
                all.stream().filter(s -> s.contains(text)).forEach(model::addElement);
            });

            list.setWidth(200);
            list.setHeight(150);
            frame.add(filter);
            frame.add(list);

            filter.setText("ka");

            frame.setSize(420, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
