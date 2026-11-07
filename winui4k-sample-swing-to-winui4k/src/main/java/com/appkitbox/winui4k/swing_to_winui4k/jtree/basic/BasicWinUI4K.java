package com.appkitbox.winui4k.swing_to_winui4k.jtree.basic;

import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;

/** 20.2 Build nodes and show a tree (WinUI4K). */
public class BasicWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.2 Basic Tree (WinUI4K)");
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
            tree.setWidth(240);
            tree.setHeight(200);
            frame.add(tree);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
