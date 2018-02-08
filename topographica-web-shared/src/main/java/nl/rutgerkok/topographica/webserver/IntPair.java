package nl.rutgerkok.topographica.webserver;

/**
 * Allows you to store two ints as a long number. This avoids the overhead of
 * objects, which consume more memory and need to be garbage collected.
 *
 */
public final class IntPair {

    /**
     * Gets the first int from a pair.
     *
     * @param pos
     *            The pair.
     * @return An int.
     */
    public static int getX(long pos) {
        return (int) (pos >>> Integer.SIZE);
    }

    /**
     * Gets the second int from a pair.
     *
     * @param pos
     *            The pair.
     * @return An int.
     */
    public static int getZ(long pos) {
        return (int) pos;
    }

    /**
     * Stores two ints as a single long.
     *
     * @param x
     *            The first int.
     * @param z
     *            The second int.
     * @return The long.
     */
    public static long toLong(int x, int z) {
        long xLong = x & 0x00000000ffffffffL;
        long zLong = z & 0x00000000ffffffffL;
        return (xLong << Integer.SIZE) | zLong;
    }

    private IntPair() {
    }
}
