package com.appkitbox.winui4k.swing_to_winui4k.jlist.renderer;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WDefaultListCellRenderer;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WIcon;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WinUiUtilities;

/** 3.9 Show custom objects with a renderer (WinUI4K). */
public class RendererWinUI4K {
    // Data class shared by Swing and WinUI4K
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
        public String toString() { return name; }   // text shown when no renderer is used
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("3.9 Custom Renderer (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            Employee[] employees = {
                new Employee("Alice Johnson", "Sales"),
                new Employee("Bob Smith", "Engineering"),
                new Employee("Carol Brown", "Admin"),
                new Employee("Dave Wilson", "HR"),
            };

            WList list = new WList(employees);
            list.setCellRenderer(new WDefaultListCellRenderer() {
                @Override
                protected String getText(Object value) {
                    Employee employee = (Employee) value;
                    return employee.getName() + " (" + employee.getDepartment() + ")";
                }

                @Override
                protected WIcon getIcon(Object value) {
                    return Symbol.CONTACT;
                }
            });

            list.setWidth(300);
            list.setHeight(200);
            frame.add(list);

            frame.setSize(380, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
