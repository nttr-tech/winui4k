package com.appkitbox.winui4k.swing_to_winui4k.jlist;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** 3.9 Listing your own objects and changing how they are shown (Swing). */
public class RendererSwing {
    // A data class shared by Swing and WinUI4K
    public static final class Employee {
        private final String name;
        private final String department;

        public Employee(String name, String department) {
            this.name = name;
            this.department = department;
        }

        public String getName() { return name; }
        public String getDepartment() { return department; }

        @Override
        public String toString() { return name; }   // What is shown without a renderer
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3.9 Listing your own objects (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            Employee[] employees = {
                new Employee("Taro Sato", "Sales"),
                new Employee("Hanako Suzuki", "Development"),
                new Employee("Jiro Takahashi", "General Affairs"),
                new Employee("Misaki Tanaka", "Human Resources"),
            };
            ImageIcon personIcon = new ImageIcon("icons/person.png");

            JList<Employee> list = new JList<>(employees);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    Employee employee = (Employee) value;
                    label.setText(employee.getName() + " (" + employee.getDepartment() + ")");
                    label.setIcon(personIcon);
                    return label;
                }
            });

            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(300, 200));
            frame.add(scroll);

            frame.setSize(380, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
