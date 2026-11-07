package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.editable;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.5 Let the user type a value (WinUI4K). */
public class EditableWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.5 Editable (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WComboBox font = new WComboBox(new String[] {"Arial", "Segoe UI", "Consolas"});
            font.setEditable(true);
            WLabel result = new WLabel("Font: " + font.getSelectedItem());
            font.addActionListener(e -> result.setText("Font: " + font.getSelectedItem()));

            frame.add(font);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            font.setSelectedItem("Courier New");
        });
    }
}
