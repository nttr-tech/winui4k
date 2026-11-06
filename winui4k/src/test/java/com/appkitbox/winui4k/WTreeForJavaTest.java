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
import com.appkitbox.winui4k.tree.ExpandVetoException;
import com.appkitbox.winui4k.tree.TreeExpansionEvent;
import com.appkitbox.winui4k.tree.TreeExpansionListener;
import com.appkitbox.winui4k.tree.TreeModel;
import com.appkitbox.winui4k.tree.TreePath;
import com.appkitbox.winui4k.tree.TreeSelectionModel;
import com.appkitbox.winui4k.tree.TreeWillExpandListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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

    @Test
    void addTreeSelectionListenerLambdasAndGetSelectionModelModeConstantsCanBeUsedTheSameWayAsSwing() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            WTree tree = new WTree(root);
            List<String> selected = new ArrayList<>();
            tree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                selected.add(node == null ? "none" : node.getUserObject() + (e.isAddedPath() ? "+" : "-"));
                assertSame(tree, e.getSource());
            });
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.setSelectionPath(tree.getPathForRow(2));
            tree.clearSelection();
            assertEquals(Arrays.asList("Pictures+", "none"), selected);
            assertNull(tree.getSelectionPaths());
            assertTrue(tree.isSelectionEmpty());

            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            assertEquals(TreeViewSelectionMode.MULTIPLE, tree.getSelectionMode());
            tree.setSelectionRows(new int[] {1, 2});
            assertEquals(2, tree.getSelectionCount());
            assertEquals(1, tree.getMinSelectionRow());
            assertEquals(2, tree.getMaxSelectionRow());
        });
    }

    @Test
    void anAnonymousTreeWillExpandListenerThrowingExpandVetoExceptionPreventsExpansion() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            WTree tree = new WTree(root);
            List<String> calls = new ArrayList<>();
            tree.addTreeWillExpandListener(new TreeWillExpandListener() {
                @Override
                public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                    calls.add("will " + event.getPath().getLastPathComponent());
                    if (event.getPath().getLastPathComponent().toString().equals("Documents")) {
                        throw new ExpandVetoException(event, "Do not open");
                    }
                }

                @Override
                public void treeWillCollapse(TreeExpansionEvent event) {
                    calls.add("will collapse " + event.getPath().getLastPathComponent());
                }
            });
            tree.addTreeExpansionListener(new TreeExpansionListener() {
                @Override
                public void treeExpanded(TreeExpansionEvent event) {
                    calls.add("expanded " + event.getPath().getLastPathComponent());
                }

                @Override
                public void treeCollapsed(TreeExpansionEvent event) {
                    calls.add("collapsed " + event.getPath().getLastPathComponent());
                }
            });
            tree.expandRow(1);
            assertFalse(tree.isExpanded(1));
            tree.collapseRow(0);
            assertEquals(Arrays.asList("will Documents", "will collapse Folder", "collapsed Folder"), calls);
            assertEquals(1, tree.getRowCount());
        });
    }

    @Test
    void buildWTreeNodesWithNewAndAddAndSelectWithGetSelectedNodeAndSetSelectedNode() {
        onUiThread(() -> {
            WTree tree = new WTree();
            WTreeNode root = new WTreeNode("Folder");
            WTreeNode documents = new WTreeNode("Documents");
            WTreeNode images = new WTreeNode("Pictures");
            root.add(documents);
            root.add(images);
            tree.addRootNode(root);
            assertEquals(1, tree.getRootNodes().size());
            assertEquals(2, root.getChildCount());
            assertSame(images, root.getChildren().get(1));
            assertTrue(root.getHasChildren());
            assertNull(tree.getSelectedNode());
            tree.setSelectedNode(documents);
            assertSame(documents, tree.getSelectedNode());
            assertSame(documents, tree.getSelectionPath().getLastPathComponent());
            root.remove(images);
            assertEquals(1, root.getChildCount());
            tree.setSelectionMode(TreeViewSelectionMode.MULTIPLE);
            tree.selectAll();
            assertEquals(2, tree.getSelectedNodes().size());
            tree.removeRootNode(root);
            assertTrue(tree.getRootNodes().isEmpty());
        });
    }

    @Test
    void consumerListenersReceiveExpansionAndCollapseWhileShown() {
        WTreeNode root = UiTestHarness.onUiThreadGet(() -> {
            WTreeNode node = new WTreeNode("Root");
            node.add(new WTreeNode("Child"));
            return node;
        });
        WTree tree = UiTestHarness.onUiThreadGet(() -> {
            WTree created = new WTree();
            created.addRootNode(root);
            return created;
        });
        List<String> calls = new ArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(tree);
        try {
            onUiThread(() -> {
                Consumer<WTreeNode> expanding = node -> calls.add("expanding " + node.getText());
                tree.addExpandingListener(expanding);
                tree.addCollapsedListener(node -> calls.add("collapsed " + node.getText()));
                tree.addItemInvokedListener(node -> calls.add("invoked " + node.getText()));
                root.setExpanded(true);
                root.setExpanded(false);
                tree.removeExpandingListener(expanding);
                root.setExpanded(true);
            });
        } finally {
            UiTestHarness.detach(tree);
        }
        assertEquals(Arrays.asList("expanding Root", "collapsed Root"), calls);
    }

    @Test
    void selectByRowIntervalsRemoveWithRemoveSelectionRowsAndIterateGetExpandedDescendantsWithWhile() {
        onUiThread(() -> {
            DefaultMutableTreeNode root = sampleRoot();
            WTree tree = new WTree(root, true);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            tree.setSelectionInterval(0, 2);
            assertEquals(3, tree.getSelectionCount());
            assertEquals(0, tree.getMinSelectionRow());
            assertEquals(2, tree.getMaxSelectionRow());
            tree.removeSelectionInterval(1, 1);
            tree.removeSelectionRows(new int[] {0});
            assertEquals(2, tree.getLeadSelectionRow());
            tree.addSelectionInterval(0, 0);
            tree.removeSelectionRow(2);
            assertEquals(tree.getPathForRow(0), tree.getSelectionPath());
            tree.removeSelectionPaths(new TreePath[] {tree.getPathForRow(0)});
            assertTrue(tree.isSelectionEmpty());

            tree.expandRow(1);
            List<String> expanded = new ArrayList<>();
            java.util.Enumeration<TreePath> e = tree.getExpandedDescendants(tree.getPathForRow(0));
            while (e.hasMoreElements()) {
                expanded.add(e.nextElement().getLastPathComponent().toString());
            }
            assertEquals(Arrays.asList("Documents"), expanded);
            assertNull(tree.getExpandedDescendants(tree.getPathForRow(2)));
            assertNull(tree.getRowBounds(0)); // No row positions before it is shown
            assertEquals(-1, tree.getClosestRowForLocation(5, 5));
        });
    }

    @Test
    void fireTreeWillExpandThrowsExpandVetoExceptionAsACheckedExceptionAndListenersCanBeRemoved() {
        onUiThread(() -> {
            WTree tree = new WTree(sampleRoot());
            List<String> calls = new ArrayList<>();
            TreeWillExpandListener veto = new TreeWillExpandListener() {
                @Override
                public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                    throw new ExpandVetoException(event);
                }

                @Override
                public void treeWillCollapse(TreeExpansionEvent event) {
                    calls.add("will collapse");
                }
            };
            TreeExpansionListener expansion = new TreeExpansionListener() {
                @Override
                public void treeExpanded(TreeExpansionEvent event) {
                    calls.add("expanded " + event.getPath().getLastPathComponent());
                }

                @Override
                public void treeCollapsed(TreeExpansionEvent event) {
                    calls.add("collapsed");
                }
            };
            tree.addTreeWillExpandListener(veto);
            tree.addTreeExpansionListener(expansion);
            assertEquals(1, tree.getTreeWillExpandListeners().length);
            assertEquals(1, tree.getTreeExpansionListeners().length);
            TreePath documents = tree.getPathForRow(1);
            boolean vetoed = false;
            try {
                tree.fireTreeWillExpand(documents);
            } catch (ExpandVetoException ex) {
                vetoed = true;
            }
            assertTrue(vetoed);
            tree.fireTreeExpanded(documents);
            tree.removeTreeWillExpandListener(veto);
            tree.removeTreeExpansionListener(expansion);
            tree.fireTreeExpanded(documents);
            tree.collapseRow(0);
            assertEquals(0, tree.getTreeWillExpandListeners().length);
            assertEquals(Arrays.asList("expanded Documents"), calls);
        });
    }
}
