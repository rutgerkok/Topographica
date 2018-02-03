package nl.rutgerkok.topographica.util;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Very simple array list of ints. Not thread-safe. After you are done
 * collection all the ints, use {@link #toQueue()} to chane this to a
 * thread-safe queue.
 *
 */
public final class LongArrayList {

    /**
     * A simple thread-safe queue.
     *
     */
    public static class LongQueue {
        private final long[] storage;
        private final int size;
        private AtomicInteger position = new AtomicInteger(0);

        private LongQueue(LongArrayList list) {
            this.storage = list.storage;
            this.size = list.size;

            list.storage = null; // This disables the old list from working
        }

        /**
         * Gets the next number from this queue.
         *
         * @return The next number.
         * @throws NoSuchElementException
         *             If there are no more numbers.
         */
        public long getNext() throws NoSuchElementException {
            int pos = position.getAndIncrement();
            if (pos == size) {
                throw new NoSuchElementException();
            }
            return storage[pos];
        }
    }

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

    /**
     * Converts this list to a queue. This list becomes unuseable.
     *
     * @return The queue.
     */
    public LongQueue toQueue() {
        return new LongQueue(this);
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
