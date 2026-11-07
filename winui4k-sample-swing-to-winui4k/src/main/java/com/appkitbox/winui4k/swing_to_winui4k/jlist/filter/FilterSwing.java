package com.appkitbox.winui4k.swing_to_winui4k.jlist.filter;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** 3.13 Filter as you type (Swing). */
public class FilterSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.13 Filter as You Type (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            List<String> all = Arrays.asList("Tokyo", "Osaka", "Nagoya", "Sapporo", "Fukuoka", "Kyoto");
            DefaultListModel<String> model = new DefaultListModel<>();
            all.forEach(model::addElement);
            JList<String> list = new JList<>(model);

            JTextField filter = new JTextField(15);
            filter.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { apply(); }
                @Override public void removeUpdate(DocumentEvent e) { apply(); }
                @Override public void changedUpdate(DocumentEvent e) { apply(); }

                private void apply() {
                    model.clear();
                    all.stream().filter(s -> s.contains(filter.getText())).forEach(model::addElement);
                }
            });

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(200, 150));
            frame.add(filter);
            frame.add(scroll);

            filter.setText("ka");

            frame.setSize(420, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
