package com.appkitbox.winui4k.swing_to_winui4k.jtree.lazy;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/** 20.7 Hide the root and load children when a node is expanded (Swing). */
public class LazySwing {
    /** Stands in for reading a folder from the disk or a server. */
    private static String[] loadFolders(Object folder) {
        return new String[] {"Users", "Windows", "Program Files"};
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.7 Lazy Loading (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Computer");
            root.add(new DefaultMutableTreeNode("C:", true));
            root.add(new DefaultMutableTreeNode("D:", true));
            DefaultTreeModel model = new DefaultTreeModel(root, true);   // empty folders are not leaves
            JTree tree = new JTree(model);
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

            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(240, 200));
            frame.add(scroll);

            tree.expandRow(0);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
