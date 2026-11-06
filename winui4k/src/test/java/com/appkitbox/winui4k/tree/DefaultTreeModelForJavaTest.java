package com.appkitbox.winui4k.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use DefaultTreeModel and TreeModelListener from Java the way a developer used to Swing's versions would write it. */
class DefaultTreeModelForJavaTest {
    /** Records notifications as strings with an anonymous class implementing all four methods (the same shape as Swing's TreeModelListener). */
    private static TreeModelListener recorder(List<String> log) {
        return new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                log.add("changed " + e.getTreePath() + " " + java.util.Arrays.toString(e.getChildIndices()));
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                log.add("inserted " + e.getTreePath() + " " + java.util.Arrays.toString(e.getChildIndices())
                        + " " + java.util.Arrays.toString(e.getChildren()));
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                log.add("removed " + e.getTreePath() + " " + java.util.Arrays.toString(e.getChildIndices())
                        + " " + java.util.Arrays.toString(e.getChildren()));
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                log.add("structure " + e.getTreePath());
            }
        };
    }

    @Test
    void insertNodeIntoAndRemoveNodeFromParentReachAnAnonymousListener() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        root.add(a);
        DefaultTreeModel model = new DefaultTreeModel(root);
        List<String> log = new ArrayList<>();
        model.addTreeModelListener(recorder(log));

        DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
        model.insertNodeInto(b, root, 1);
        model.removeNodeFromParent(a);
        model.nodeChanged(b);
        model.reload();

        assertEquals(
                java.util.Arrays.asList(
                        "inserted [root] [1] [b]",
                        "removed [root] [0] [a]",
                        "changed [root] [0]",
                        "structure [root]"),
                log);
    }

    @Test
    void modelMethodsCanBeCalledWithObjectArgumentsInTheSameShapeAsSwing() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        root.add(a);
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        assertSame(root, model.getRoot());
        assertSame(a, model.getChild(root, 0));
        assertEquals(1, model.getChildCount(root));
        assertEquals(0, model.getIndexOfChild(root, a));
        assertEquals(-1, model.getIndexOfChild(null, a));
        assertEquals(false, model.isLeaf(a));
        assertArrayEquals(new TreeNode[] {root, a}, model.getPathToRoot(a));
        assertNull(model.getPathToRoot(null));
    }

    @Test
    void valueForPathChangedReplacesTheNodeValueByTreePath() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        root.add(a);
        DefaultTreeModel model = new DefaultTreeModel(root);
        List<String> log = new ArrayList<>();
        model.addTreeModelListener(recorder(log));
        model.valueForPathChanged(new TreePath(model.getPathToRoot(a)), "renamed");
        assertEquals("renamed", a.getUserObject());
        assertEquals(java.util.Arrays.asList("changed [root] [0]"), log);
    }

    @Test
    void removeNodeFromParentOnTheRootThrowsIllegalArgumentException() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel model = new DefaultTreeModel(root);
        assertThrows(IllegalArgumentException.class, () -> model.removeNodeFromParent(root));
    }

    @Test
    void treeModelEventReturnsGetPathAndGetChildIndicesAsArrays() {
        TreeModelEvent e = new TreeModelEvent("src", new Object[] {"r", "a"}, new int[] {1}, new Object[] {"x"});
        assertArrayEquals(new Object[] {"r", "a"}, e.getPath());
        assertArrayEquals(new int[] {1}, e.getChildIndices());
        assertArrayEquals(new Object[] {"x"}, e.getChildren());
        assertEquals("src", e.getSource());
    }
}
