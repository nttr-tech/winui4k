package com.appkitbox.winui4k.swing_to_winui4k.jtree.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/** 20.6 Change icons and text with a renderer (Swing). */
public class RendererSwing {
    /** A file with its size. */
    static class FileItem {
        final String name;
        final long size;

        FileItem(String name, long size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("20.6 Tree Renderer (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode(new FileItem("Spec.docx", 48_000)));
            work.add(new DefaultMutableTreeNode(new FileItem("Schedule.xlsx", 12_500)));
            root.add(work);
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode(new FileItem("Photo.jpg", 2_400_000)));
            root.add(personal);
            JTree tree = new JTree(root);

            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
                @Override
                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                        boolean expanded, boolean leaf, int row, boolean hasFocus) {
                    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                    Object item = ((DefaultMutableTreeNode) value).getUserObject();
                    if (item instanceof FileItem) {
                        FileItem file = (FileItem) item;
                        setText(file.name + " (" + file.size / 1000 + " KB)");
                    }
                    return this;
                }
            };
            renderer.setLeafIcon(new ImageIcon("icons/file.png"));
            renderer.setOpenIcon(new ImageIcon("icons/folder.png"));
            renderer.setClosedIcon(new ImageIcon("icons/folder.png"));
            tree.setCellRenderer(renderer);

            JScrollPane scroll = new JScrollPane(tree);
            scroll.setPreferredSize(new Dimension(260, 200));
            frame.add(scroll);

            tree.expandRow(1);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
