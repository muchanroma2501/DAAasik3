package dsu;

/**
 * Disjoint Set Union (DSU) over integer element IDs in {@code [0, size)}.
 *
 * <p>This implementation uses path compression and union by size. A sequence
 * of operations takes amortized {@code O(alpha(V))} time per operation, where
 * {@code alpha} is the inverse Ackermann function, and the structure uses
 * {@code O(V)} space.</p>
 */
public class DSU {
    private final int[] parent;
    private final int[] size;
    private int componentCount;

    /**
     * Creates a DSU in which every element begins in its own set.
     *
     * @param elementCount number of elements; must be zero or greater
     * @throws IllegalArgumentException if {@code elementCount} is negative
     */
    public DSU(int elementCount) {
        if (elementCount < 0) {
            throw new IllegalArgumentException("Element count cannot be negative: " + elementCount);
        }

        parent = new int[elementCount];
        size = new int[elementCount];
        componentCount = elementCount;
        for (int element = 0; element < elementCount; element++) {
            parent[element] = element;
            size[element] = 1;
        }
    }

    /**
     * Returns the representative of the set containing {@code element},
     * compressing the path to that representative.
     *
     * @param element element ID
     * @return its set representative
     * @throws IndexOutOfBoundsException if the element ID is invalid
     */
    public int find(int element) {
        validateElement(element);
        if (parent[element] != element) {
            parent[element] = find(parent[element]);
        }
        return parent[element];
    }

    /**
     * Merges the sets containing {@code first} and {@code second}, attaching
     * the smaller set to the larger set.
     *
     * @param first first element ID
     * @param second second element ID
     * @return {@code true} if two distinct sets were merged; otherwise
     *         {@code false}
     * @throws IndexOutOfBoundsException if either element ID is invalid
     */
    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }

        if (size[firstRoot] < size[secondRoot]) {
            int temporaryRoot = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporaryRoot;
        }

        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        componentCount--;
        return true;
    }

    /**
     * Checks whether two elements currently belong to the same set.
     *
     * @param first first element ID
     * @param second second element ID
     * @return {@code true} if both elements have the same representative
     * @throws IndexOutOfBoundsException if either element ID is invalid
     */
    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    /**
     * Returns the number of disjoint sets currently represented.
     *
     * @return current connected-component count
     */
    public int getComponentCount() {
        return componentCount;
    }

    private void validateElement(int element) {
        if (element < 0 || element >= parent.length) {
            throw new IndexOutOfBoundsException(
                    "Element index " + element + " is outside [0, " + parent.length + ")");
        }
    }
}
