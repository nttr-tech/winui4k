package com.appkitbox.winui4k.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use DefaultMutableTreeNode and TreePath from Java the way a developer used to Swing's versions would write it. */
class DefaultMutableTreeNodeForJavaTest {
    private static List<String> names(Enumeration<?> e) {
        List<String> result = new ArrayList<>();
        while (e.hasMoreElements()) {
            result.add(e.nextElement().toString());
        }
        return result;
    }

    @Test
    void treeBuiltWithNewAndAddCanBeReadWithNestedLoopsAndGetChildAt() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Fruits");
        DefaultMutableTreeNode citrus = new DefaultMutableTreeNode("Citrus");
        root.add(citrus);
        citrus.add(new DefaultMutableTreeNode("Orange"));
        citrus.add(new DefaultMutableTreeNode("Lemon"));
        root.add(new DefaultMutableTreeNode("Apple"));

        assertEquals(2, root.getChildCount());
        assertEquals("Citrus", root.getChildAt(0).toString());
        assertEquals("Lemon", ((DefaultMutableTreeNode) citrus.getChildAt(1)).getUserObject());
        assertSame(root, citrus.getParent());
        assertEquals(Arrays.asList("Fruits", "Citrus", "Orange", "Lemon", "Apple"), names(root.preorderEnumeration()));
        assertEquals(Arrays.asList("Orange", "Lemon", "Citrus", "Apple", "Fruits"), names(root.postorderEnumeration()));
        assertEquals(Arrays.asList("Fruits", "Citrus", "Apple", "Orange", "Lemon"), names(root.breadthFirstEnumeration()));
    }

    @Test
    void arrayFromGetPathCanBePassedDirectlyToTreePath() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        root.add(child);
        TreePath path = new TreePath(child.getPath());
        assertEquals(2, path.getPathCount());
        assertSame(child, path.getLastPathComponent());
        assertSame(root, path.getParentPath().getLastPathComponent());
        assertEquals("[root, child]", path.toString());
        assertEquals(path, new TreePath(new Object[] {root, child}));
        assertArrayEquals(new Object[] {root, child}, path.getPath());
    }

    @Test
    void addToNodeNotAllowingChildrenThrowsIllegalStateExceptionAndAddingAnAncestorThrowsIllegalArgumentException() {
        DefaultMutableTreeNode leaf = new DefaultMutableTreeNode("leaf", false);
        assertThrows(IllegalStateException.class, () -> leaf.add(new DefaultMutableTreeNode("x")));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        root.add(child);
        assertThrows(IllegalArgumentException.class, () -> child.add(root));
        assertFalse(leaf.getAllowsChildren());
        assertTrue(new DefaultMutableTreeNode().getAllowsChildren());
    }

    @Test
    void toStringOfNodeWithoutUserObjectIsEmptyAndGetUserObjectIsNull() {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        assertNull(node.getUserObject());
        assertEquals("", node.toString());
        node.setUserObject(7);
        assertEquals("7", node.toString());
    }

    @Test
    void childrenOfNodeWithoutChildrenIsEmptyAndEmptyEnumerationIsReadableAsStaticField() {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("n");
        assertFalse(node.children().hasMoreElements());
        assertFalse(DefaultMutableTreeNode.EMPTY_ENUMERATION.hasMoreElements());
        assertThrows(java.util.NoSuchElementException.class, () -> node.children().nextElement());
    }

    @Test
    void invalidTreePathArgumentsThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TreePath(new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> new TreePath(new Object[] {"a", null}));
        assertThrows(IllegalArgumentException.class, () -> new TreePath("a").getPathComponent(1));
    }
}
