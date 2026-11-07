package com.appkitbox.winui4k.swing_to_winui4k.jtoolbar.component;

import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WCommandBar;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WinUiUtilities;

/** 23.5 Put a combo box and a text field on the toolbar (WinUI4K). */
public class ComponentWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("23.5 Toolbar Components (WinUI4K)");
            WLabel status = new WLabel("Font: Segoe UI");

            WComboBox font = new WComboBox(new String[] {"Segoe UI", "Yu Gothic UI", "Consolas"});
            font.addActionListener(e -> status.setText("Font: " + font.getSelectedItem()));
            WTextField search = new WTextField(10);
            search.addActionListener(e -> status.setText("Search: " + search.getText()));

            WCommandBar toolBar = new WCommandBar();
            toolBar.add(new WLabel("Font: "));
            toolBar.add(font);                        // any component is wrapped and put on the bar
            toolBar.addSeparator();
            toolBar.add(search);

            frame.add(toolBar, WBorderLayout.NORTH);
            frame.add(status, WBorderLayout.CENTER);

            frame.setSize(460, 220);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
