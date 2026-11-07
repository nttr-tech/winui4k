package com.appkitbox.winui4k.swing_to_winui4k.jcombobox.renderer;

import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

/** 8.6 Show your own objects (Swing). */
public class RendererSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("8.6 Own objects (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JComboBox<Country> country = new JComboBox<>(new Country[] {
                new Country("JP", "Japan"), new Country("US", "United States"), new Country("FR", "France")});
            country.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    Country c = (Country) value;
                    String text = c == null ? "" : c.name + " (" + c.code + ")";
                    return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                }
            });
            JLabel result = new JLabel();
            country.addActionListener(e -> result.setText("Code: " + ((Country) country.getSelectedItem()).code));

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
