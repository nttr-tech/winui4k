package com.appkitbox.winui4k.swing_to_winui4k.jtree.lazy;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;
import com.appkitbox.winui4k.tree.DefaultTreeModel;
import com.appkitbox.winui4k.tree.TreeExpansionEvent;
import com.appkitbox.winui4k.tree.TreeWillExpandListener;

/** 20.7 Hide the root and load children when a node is expanded (WinUI4K). */
public class LazyWinUI4K {
    /** Stands in for reading a folder from the disk or a server. */
    private static String[] loadFolders(Object folder) {
        return new String[] {"Users", "Windows", "Program Files"};
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.7 Lazy Loading (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Computer");
            root.add(new DefaultMutableTreeNode("C:", true));
            root.add(new DefaultMutableTreeNode("D:", true));
            DefaultTreeModel model = new DefaultTreeModel(root, true);   // empty folders are not leaves
            WTree tree = new WTree(model);
            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);

            tree.addTreeWillExpandListener(new TreeWillExpandListener() {
                @Override
                public void treeWillExpand(TreeExpansionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                    if (node.getChildCount() > 0) return;   // already loaded
                    for (String name : loadFolders(node.getUserObject())) {
                        model.insertNodeInto(new DefaultMutableTreeNode(name, true), node, node.getChildCount());
                    }
                }

                @Override
                public void treeWillCollapse(TreeExpansionEvent e) {
                }
            });

            tree.setWidth(240);
            tree.setHeight(200);
            frame.add(tree);

            tree.expandRow(0);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
