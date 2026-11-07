package com.appkitbox.winui4k.swing_to_winui4k.jtree.selection;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/** 20.3 React to selection changes (Swing). */
public class SelectionSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.3 Tree Selection (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            DefaultMutableTreeNode spec = new DefaultMutableTreeNode("Spec.docx");
            work.add(spec);
            work.add(new DefaultMutableTreeNode("Schedule.xlsx"));
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode("Photos"));
            root.add(work);
            root.add(personal);
            JTree tree = new JTree(root);

            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            JLabel detail = new JLabel("Select a node");
            tree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) {
                    detail.setText("Select a node");
                } else {
                    detail.setText(node.getUserObject() + (node.isLeaf() ? " (file)" : " (folder)"));
                }
            });

            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(240, 200));
            frame.add(scroll);
            frame.add(detail);

            tree.setSelectionPath(new TreePath(spec.getPath()));   // also expands "Work"

            frame.setSize(440, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
