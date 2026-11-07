package com.appkitbox.winui4k.swing_to_winui4k.jtree.renderer;

import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WDefaultTreeCellRenderer;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WTree;
import com.appkitbox.winui4k.WinUiUtilities;
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;

/** 20.6 Change icons and text with a renderer (WinUI4K). */
public class RendererWinUI4K {
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
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("20.6 Tree Renderer (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documents");
            DefaultMutableTreeNode work = new DefaultMutableTreeNode("Work");
            work.add(new DefaultMutableTreeNode(new FileItem("Spec.docx", 48_000)));
            work.add(new DefaultMutableTreeNode(new FileItem("Schedule.xlsx", 12_500)));
            root.add(work);
            DefaultMutableTreeNode personal = new DefaultMutableTreeNode("Personal");
            personal.add(new DefaultMutableTreeNode(new FileItem("Photo.jpg", 2_400_000)));
            root.add(personal);
            WTree tree = new WTree(root);

            WDefaultTreeCellRenderer renderer = new WDefaultTreeCellRenderer() {
                @Override
                protected String getText(WTree tree, Object value, boolean selected,
                        boolean expanded, boolean leaf, int row, boolean hasFocus) {
                    Object item = ((DefaultMutableTreeNode) value).getUserObject();
                    if (item instanceof FileItem) {
                        FileItem file = (FileItem) item;
                        return file.name + " (" + file.size / 1000 + " KB)";
                    }
                    return super.getText(tree, value, selected, expanded, leaf, row, hasFocus);
                }
            };
            renderer.setLeafIcon(Symbol.DOCUMENT);
            renderer.setOpenIcon(Symbol.FOLDER);
            renderer.setClosedIcon(Symbol.FOLDER);
            tree.setCellRenderer(renderer);

            tree.setWidth(260);
            tree.setHeight(200);
            frame.add(tree);

            tree.expandRow(1);

            frame.setSize(400, 280);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
