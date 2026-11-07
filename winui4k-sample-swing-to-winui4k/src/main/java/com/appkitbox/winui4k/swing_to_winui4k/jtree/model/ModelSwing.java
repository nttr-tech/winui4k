package com.appkitbox.winui4k.swing_to_winui4k.jtree.model;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** 20.5 Add and remove nodes with a model (Swing). */
public class ModelSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.5 Tree Model (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode("Spec.docx"));
            root.add(work);
            root.add(new DefaultMutableTreeNode("Personal"));
            DefaultTreeModel model = new DefaultTreeModel(root);
            JTree tree = new JTree(model);

            JButton add = new JButton("New folder");
            add.addActionListener(e -> {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (parent == null) parent = root;
                DefaultMutableTreeNode child = new DefaultMutableTreeNode("Folder " + (parent.getChildCount() + 1));
                model.insertNodeInto(child, parent, parent.getChildCount());
                tree.scrollPathToVisible(new TreePath(child.getPath()));
            });

            JButton remove = new JButton("Delete");
            remove.addActionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null && node.getParent() != null) model.removeNodeFromParent(node);
            });

            JButton rename = new JButton("Rename");
            rename.addActionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) return;
                node.setUserObject(node.getUserObject() + " (old)");
                model.nodeChanged(node);   // tell the tree that the text changed
            });

            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(240, 200));
            frame.add(add);
            frame.add(remove);
            frame.add(rename);
            frame.add(scroll);

            tree.setSelectionPath(new TreePath(work.getPath()));
            add.doClick();
            rename.doClick();

            frame.setSize(440, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
