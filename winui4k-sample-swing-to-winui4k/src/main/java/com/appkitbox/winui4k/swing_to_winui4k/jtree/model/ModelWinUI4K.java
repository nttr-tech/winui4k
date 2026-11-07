package com.appkitbox.winui4k.swing_to_winui4k.jtree.model;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;
import com.appkitbox.winui4k.tree.DefaultTreeModel;
import com.appkitbox.winui4k.tree.TreePath;

/** 20.5 Add and remove nodes with a model (WinUI4K). */
public class ModelWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.5 Tree Model (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode("Spec.docx"));
            root.add(work);
            root.add(new DefaultMutableTreeNode("Personal"));
            DefaultTreeModel model = new DefaultTreeModel(root);
            WTree tree = new WTree(model);

            WButton add = new WButton("New folder");
            add.addActionListener(e -> {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (parent == null) parent = root;
                DefaultMutableTreeNode child = new DefaultMutableTreeNode("Folder " + (parent.getChildCount() + 1));
                model.insertNodeInto(child, parent, parent.getChildCount());
                tree.scrollPathToVisible(new TreePath(child.getPath()));
            });

            WButton remove = new WButton("Delete");
            remove.addActionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null && node.getParent() != null) model.removeNodeFromParent(node);
            });

            WButton rename = new WButton("Rename");
            rename.addActionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) return;
                node.setUserObject(node.getUserObject() + " (old)");
                model.nodeChanged(node);   // tell the tree that the text changed
            });

            tree.setWidth(240);
            tree.setHeight(200);
            frame.add(add);
            frame.add(remove);
            frame.add(rename);
            frame.add(tree);

            tree.setSelectionPath(new TreePath(work.getPath()));
            add.doClick();
            rename.doClick();

            frame.setSize(440, 320);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
