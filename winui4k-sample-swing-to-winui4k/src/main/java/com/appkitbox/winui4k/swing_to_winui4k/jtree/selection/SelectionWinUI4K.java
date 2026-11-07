package com.appkitbox.winui4k.swing_to_winui4k.jtree.selection;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;
import com.appkitbox.winui4k.tree.TreePath;
import com.appkitbox.winui4k.tree.TreeSelectionModel;

/** 20.3 React to selection changes (WinUI4K). */
public class SelectionWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.3 Tree Selection (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            DefaultMutableTreeNode spec = new DefaultMutableTreeNode("Spec.docx");
            work.add(spec);
            work.add(new DefaultMutableTreeNode("Schedule.xlsx"));
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode("Photos"));
            root.add(work);
            root.add(personal);
            WTree tree = new WTree(root);

            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            WLabel detail = new WLabel("Select a node");
            tree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) {
                    detail.setText("Select a node");
                } else {
                    detail.setText(node.getUserObject() + (node.isLeaf() ? " (file)" : " (folder)"));
                }
            });

            tree.setWidth(240);
            tree.setHeight(200);
            frame.add(tree);
            frame.add(detail);

            tree.setSelectionPath(new TreePath(spec.getPath()));   // also expands "Work"

            frame.setSize(440, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
