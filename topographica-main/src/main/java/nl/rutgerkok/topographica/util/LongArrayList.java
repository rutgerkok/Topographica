package nl.rutgerkok.topographica.util;

import java.util.Arrays;

/**
 * Very simple array list of ints. Not thread-safe.
 *
 */
public final class LongArrayList {
    private long[] storage = new long[64];
    private int size = 0;

    /**
     * Adds a new number to the array list.
     *
     * @param number
     *            The number.
     */
    public void add(long number) {
        if (size == storage.length) {
            storage = Arrays.copyOf(storage, (int) (storage.length * 1.5));
        }
        storage[size] = number;
        size++;
    }

    /**
     * Gets a number.
     *
     * @param i
     *            The index.
     * @return The number.
     * @throws IndexOutOfBoundsException
     *             When {@code i < 0 || i >= size()}.
     */
    public long get(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException("size: " + size + ", index: " + i);
        }
        return storage[i];
    }

    /**
     * Gets the size of the list.
     *
     * @return The size.
     */
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i <= 20 && i < size; i++) {
            builder.append(storage[i]);
            builder.append(',');
        }
        if (size > 20) {
            builder.append(" ... (").append(size - 20).append(" more values) ]");
        } else {
            builder.setCharAt(builder.length() - 1, ']');
        }
        return builder.toString();
    }

}
