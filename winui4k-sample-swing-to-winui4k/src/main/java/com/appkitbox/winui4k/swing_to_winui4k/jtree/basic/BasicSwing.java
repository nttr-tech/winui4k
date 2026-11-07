package com.appkitbox.winui4k.swing_to_winui4k.jtree.basic;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

/** 20.2 Build nodes and show a tree (Swing). */
public class BasicSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.2 Basic Tree (Swing)");
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
            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(240, 200));
            frame.add(scroll);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
