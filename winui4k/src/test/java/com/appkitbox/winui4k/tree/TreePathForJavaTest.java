package com.appkitbox.winui4k.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests that use Swing's TreePath in the same way developers who have used it would write it in Java. Expected values come from the behavior of javax.swing.tree.TreePath. */
class TreePathForJavaTest {
    /** A subclass that can be created from a parent and the last element with the protected constructor, as in Swing. */
    private static final class NamedPath extends TreePath {
        NamedPath(TreePath parent, Object last) {
            super(parent, last);
        }
    }

    @Test
    void createFromAnArrayAndReadElementsWithGetPathGetPathComponentAndGetParentPath() {
        TreePath path = new TreePath(new Object[] {"root", "fruit", "apple"});
        assertEquals(3, path.getPathCount());
        assertArrayEquals(new Object[] {"root", "fruit", "apple"}, path.getPath());
        assertEquals("fruit", path.getPathComponent(1));
        assertEquals("apple", path.getLastPathComponent());
        assertEquals(new TreePath(new Object[] {"root", "fruit"}), path.getParentPath());
        assertNull(new TreePath("root").getParentPath());
        assertEquals("[root, fruit, apple]", path.toString());
        assertThrows(IllegalArgumentException.class, () -> path.getPathComponent(3));
    }

    @Test
    void pathByAddingChildIsDescendantAndEquals() {
        TreePath fruit = new TreePath(new Object[] {"root", "fruit"});
        TreePath apple = fruit.pathByAddingChild("apple");
        assertTrue(fruit.isDescendant(apple));
        assertTrue(fruit.isDescendant(fruit));
        assertFalse(apple.isDescendant(fruit));
        assertFalse(fruit.isDescendant(null));
        assertEquals(new TreePath(new Object[] {"root", "fruit", "apple"}), apple);
        assertEquals(apple.hashCode(), "apple".hashCode());
        assertNotEquals(new TreePath(new Object[] {"another root", "fruit", "apple"}), apple);
    }

    @Test
    void anEmptyArrayOrNullElementsThrowIllegalArgumentExceptionAndSubclassesCanBeCreatedWithTheProtectedConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new TreePath(new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> new TreePath(new Object[] {"root", null}));
        TreePath named = new NamedPath(new TreePath("root"), "leaf");
        assertEquals(new TreePath(new Object[] {"root", "leaf"}), named);
    }
}
