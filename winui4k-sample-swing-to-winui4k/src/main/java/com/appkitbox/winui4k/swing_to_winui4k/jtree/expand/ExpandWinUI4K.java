package com.appkitbox.winui4k.swing_to_winui4k.jtree.expand;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;
import com.appkitbox.winui4k.tree.TreeExpansionEvent;
import com.appkitbox.winui4k.tree.TreeExpansionListener;

/** 20.4 Expand and collapse from code (WinUI4K). */
public class ExpandWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.4 Expand and Collapse (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode("Spec.docx"));
            work.add(new DefaultMutableTreeNode("Schedule.xlsx"));
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode("Photos"));
            root.add(work);
            root.add(personal);
            WTree tree = new WTree(root);

            WButton expandAll = new WButton("Expand all");
            expandAll.addActionListener(e -> {
                for (int row = 0; row < tree.getRowCount(); row++) {
                    tree.expandRow(row);   // the row count grows while expanding
                }
            });
            WButton collapseAll = new WButton("Collapse all");
            collapseAll.addActionListener(e -> {
                for (int row = tree.getRowCount() - 1; row >= 1; row--) {
                    tree.collapseRow(row);
                }
            });

            WLabel status = new WLabel(" ");
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

            tree.setWidth(240);
            tree.setHeight(200);
            frame.add(expandAll);
            frame.add(collapseAll);
            frame.add(tree);
            frame.add(status);

            expandAll.doClick();

            frame.setSize(440, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
