package dsu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DSUTest {
    @Test
    void startsWithEachElementInItsOwnSet() {
        DSU dsu = new DSU(4);

        assertEquals(4, dsu.getComponentCount());
        for (int element = 0; element < 4; element++) {
            assertEquals(element, dsu.find(element));
        }
    }

    @Test
    void mergesDistinctSetsAndReportsWhetherMergeOccurred() {
        DSU dsu = new DSU(4);

        assertTrue(dsu.union(0, 1));
        assertTrue(dsu.connected(0, 1));
        assertFalse(dsu.union(0, 1));
        assertFalse(dsu.connected(0, 2));
    }

    @Test
    void compressionPreservesRepresentativesAndMembership() {
        DSU dsu = new DSU(8);
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(0, 2);
        dsu.union(4, 5);
        dsu.union(6, 7);
        dsu.union(4, 6);
        dsu.union(0, 4);

        int representative = dsu.find(3);
        assertEquals(representative, dsu.find(7));
        assertTrue(dsu.connected(1, 6));
        assertEquals(representative, dsu.find(3));
    }

    @Test
    void unionBySizeMaintainsSetMembershipAndComponentCount() {
        DSU dsu = new DSU(6);
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(0, 2);
        dsu.union(4, 5);
        dsu.union(0, 4);

        assertEquals(1, dsu.getComponentCount());
        for (int element = 1; element < 6; element++) {
            assertTrue(dsu.connected(0, element));
        }
    }

    @Test
    void componentCountDecreasesOnlyAfterSuccessfulMerges() {
        DSU dsu = new DSU(3);

        assertEquals(3, dsu.getComponentCount());
        assertTrue(dsu.union(0, 1));
        assertEquals(2, dsu.getComponentCount());
        assertFalse(dsu.union(1, 0));
        assertEquals(2, dsu.getComponentCount());
        assertTrue(dsu.union(1, 2));
        assertEquals(1, dsu.getComponentCount());
    }

    @Test
    void rejectsInvalidElementIndices() {
        DSU dsu = new DSU(2);

        assertThrows(IndexOutOfBoundsException.class, () -> dsu.find(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.find(2));
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.union(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.connected(-1, 0));
    }

    @Test
    void allowsZeroElementsAndRejectsNegativeSize() {
        DSU empty = new DSU(0);

        assertEquals(0, empty.getComponentCount());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.find(0));
        assertThrows(IllegalArgumentException.class, () -> new DSU(-1));
    }
}
