package com.appkitbox.winui4k.swing_to_winui4k.jtree.expand;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/** 20.4 Expand and collapse from code (Swing). */
public class ExpandSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.4 Expand and Collapse (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode("Spec.docx"));
            work.add(new DefaultMutableTreeNode("Schedule.xlsx"));
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode("Photos"));
            root.add(work);
            root.add(personal);
            JTree tree = new JTree(root);

            JButton expandAll = new JButton("Expand all");
            expandAll.addActionListener(e -> {
                for (int row = 0; row < tree.getRowCount(); row++) {
                    tree.expandRow(row);   // the row count grows while expanding
                }
            });
            JButton collapseAll = new JButton("Collapse all");
            collapseAll.addActionListener(e -> {
                for (int row = tree.getRowCount() - 1; row >= 1; row--) {
                    tree.collapseRow(row);
                }
            });

            JLabel status = new JLabel(" ");
            tree.addTreeExpansionListener(new TreeExpansionListener() {
                @Override
                public void treeExpanded(TreeExpansionEvent e) {
                    status.setText("Expanded: " + e.getPath().getLastPathComponent());
                }

                @Override
                public void treeCollapsed(TreeExpansionEvent e) {
                    status.setText("Collapsed: " + e.getPath().getLastPathComponent());
                }
            });

            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(240, 200));
            frame.add(expandAll);
            frame.add(collapseAll);
            frame.add(scroll);
            frame.add(status);

            expandAll.doClick();

            frame.setSize(440, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
