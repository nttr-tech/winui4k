package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.renderer;

import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WDefaultComboBoxModel;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 8.6 Show your own objects (WinUI4K). */
public class RendererWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("8.6 Own objects (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WDefaultComboBoxModel<Country> model = new WDefaultComboBoxModel<>(new Country[] {
                new Country("JP", "Japan"), new Country("US", "United States"), new Country("FR", "France")});
            WComboBox country = new WComboBox(model);
            country.setRenderer(value -> {
                Country c = (Country) value;
                return c == null ? "" : c.name + " (" + c.code + ")";
            });
            WLabel result = new WLabel();
            country.addActionListener(e -> result.setText("Code: " + ((Country) model.getSelectedItem()).code));

            frame.add(country);
            frame.add(result);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            country.setSelectedIndex(2);
        });
    }

    /** An item with a code and a display name. */
    static final class Country {
        final String code;
        final String name;

        Country(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
