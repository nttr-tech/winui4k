package com.appkitbox.winui4k.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use Swing's DefaultTreeSelectionModel and TreeSelectionListener in the same way developers who have used them would write it in Java.
 * Expected values come from the behavior of javax.swing.tree.DefaultTreeSelectionModel.
 */
class DefaultTreeSelectionModelForJavaTest {
    private final TreePath a = new TreePath(new Object[] {"root", "a"});
    private final TreePath b = new TreePath(new Object[] {"root", "b"});
    private final TreePath c = new TreePath(new Object[] {"root", "c"});

    @Test
    void chooseTheModeWithConstantsAndReceiveAddedAndRemovedPathsWithAnAnonymousTreeSelectionListener() {
        DefaultTreeSelectionModel model = new DefaultTreeSelectionModel();
        assertEquals(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION, model.getSelectionMode());
        List<String> events = new ArrayList<>();
        model.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                for (TreePath path : e.getPaths()) {
                    events.add((e.isAddedPath(path) ? "+" : "-") + path.getLastPathComponent());
                }
            }
        });
        model.setSelectionPaths(new TreePath[] {a, b});
        model.setSelectionPath(c);
        assertEquals(Arrays.asList("+a", "+b", "+c", "-a", "-b"), events);
        assertSame(c, model.getLeadSelectionPath());
        assertEquals(1, model.getSelectionCount());
        assertEquals(1, model.getTreeSelectionListeners().length);
    }

    @Test
    void singleTreeSelectionSelectsOnlyTheFirstPathAndClearSelectionEmptiesIt() {
        DefaultTreeSelectionModel model = new DefaultTreeSelectionModel();
        model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        model.addSelectionPaths(new TreePath[] {b, c});
        assertArrayEquals(new TreePath[] {b}, model.getSelectionPaths());
        assertTrue(model.isPathSelected(b));
        model.clearSelection();
        assertTrue(model.isSelectionEmpty());
        assertNull(model.getSelectionPath());
        assertEquals(0, model.getSelectionPaths().length);
    }

    @Test
    void aLambdaRowMapperGivesRowsAndContiguousKeepsOnlyTheFirstPathForNonContiguousPaths() {
        DefaultTreeSelectionModel model = new DefaultTreeSelectionModel();
        List<TreePath> rows = Arrays.asList(a, b, c);
        model.setRowMapper(paths -> {
            int[] result = new int[paths.length];
            for (int i = 0; i < paths.length; i++) {
                result[i] = rows.indexOf(paths[i]);
            }
            return result;
        });
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.setSelectionPaths(new TreePath[] {a, c});
        assertArrayEquals(new TreePath[] {a}, model.getSelectionPaths());
        model.addSelectionPath(b);
        assertArrayEquals(new int[] {0, 1}, model.getSelectionRows());
        assertEquals(0, model.getMinSelectionRow());
        assertEquals(1, model.getMaxSelectionRow());
        assertEquals(1, model.getLeadSelectionRow());
        assertTrue(model.isRowSelected(1));
        assertFalse(model.isRowSelected(2));
    }
}
