package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appkitbox.winui4k.tree.DefaultMutableTreeNode;
import com.appkitbox.winui4k.tree.DefaultTreeModel;
import com.appkitbox.winui4k.tree.TreeModel;
import com.appkitbox.winui4k.tree.TreePath;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTree from Java in the same way developers who have used Swing's JTree and DefaultMutableTreeNode would write it.
 * Compiling at all verifies the shape of the API, and the bodies check rows, paths, expansion and the results of model changes.
 */
class WTreeForJavaTest {
    private static DefaultMutableTreeNode sampleRoot() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Folder");
        DefaultMutableTreeNode documents = new DefaultMutableTreeNode("Documents");
        documents.add(new DefaultMutableTreeNode("report.txt"));
        root.add(documents);
        root.add(new DefaultMutableTreeNode("Pictures"));
        return root;
    }

    @Test
    void passingARootToNewWTreeListsRowsWithTheRootExpandedAndExpandRowAddsChildren() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            WTree tree = new WTree(root);
            assertEquals(3, tree.getRowCount());
            assertEquals("Documents", tree.getPathForRow(1).getLastPathComponent().toString());
            tree.expandRow(1);
            assertEquals(4, tree.getRowCount());
            TreePath report = tree.getPathForRow(2);
            assertEquals(3, report.getPathCount());
            assertEquals("report.txt", report.getLastPathComponent().toString());
            assertTrue(tree.isExpanded(1));
            tree.collapseRow(1);
            assertTrue(tree.isCollapsed(1));
            assertEquals(-1, tree.getRowForPath(report));
        });
    }

    @Test
    void setRootVisibleFalseHidesTheRootAndGetModelReturnsThePassedModel() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            DefaultTreeModel model = new DefaultTreeModel(root);
            WTree tree = new WTree(model);
            tree.setRootVisible(false);
            assertFalse(tree.isRootVisible());
            assertSame(model, tree.getModel());
            assertEquals(2, tree.getRowCount());
            assertEquals(0, tree.getRowForPath(new TreePath(new Object[] {root, root.getChildAt(0)})));
            tree.setShowsRootHandles(true);
            assertTrue(tree.getShowsRootHandles());
        });
    }

    @Test
    void insertNodeIntoAndRemoveNodeFromParentOfDefaultTreeModelAreReflectedInTheRows() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            DefaultTreeModel model = new DefaultTreeModel(root);
            WTree tree = new WTree(model);
            DefaultMutableTreeNode music = new DefaultMutableTreeNode("Music");
            model.insertNodeInto(music, root, 0);
            assertEquals("Music", tree.getPathForRow(1).getLastPathComponent().toString());
            model.removeNodeFromParent(music);
            assertEquals(3, tree.getRowCount());
            tree.expandPath(new TreePath(music.getPath()));
            assertEquals(3, tree.getRowCount());
        });
    }

    @Test
    void addRootNodeThrowsIllegalStateExceptionWhileAModelIsShownAndSetModelNullReturnsToBuilding() {
        onUiThread(() -> {
            WTree tree = new WTree(sampleRoot());
            assertThrows(IllegalStateException.class, () -> tree.addRootNode(new WTreeNode("x")));
            TreeModel none = null;
            tree.setModel(none);
            assertNull(tree.getModel());
            tree.addRootNode(new WTreeNode("Node"));
            assertEquals(1, tree.getRowCount());
            assertEquals("", tree.convertValueToText(null, false, false, true, 0, false));
        });
    }
}
