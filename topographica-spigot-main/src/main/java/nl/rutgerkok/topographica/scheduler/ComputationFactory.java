package nl.rutgerkok.topographica.scheduler;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * A class that keeps on generating new computational tasks.
 *
 * @param <T>
 *            The result types of all computations.
 */
public abstract class ComputationFactory<T> {

    /**
     * Based solely on class and {@link #getUniqueId()}.
     */
    @Override
    public final boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (other.getClass() != this.getClass()) {
            return false;
        }
        return ((ComputationFactory<?>) other).getUniqueId().equals(this.getUniqueId());
    }

    /**
     * Gets an unique id for this factory. Only one factory with this id can be
     * active at the same time.
     *
     * @return The id.
     */
    public abstract UUID getUniqueId();

    /**
     * Called after a computation has finished. This method is called on an
     * async thread.
     *
     * @param result
     *            The result of the calculation.
     * @throws Throwable
     *             If an error occurs handling this result. thrown, the
     */
    public abstract void handleResult(T result) throws Throwable;

    /**
     * Derived solely from {@link #getUniqueId()}.
     */
    @Override
    public final int hashCode() {
        return getUniqueId().hashCode();
    }

    /**
     * Returns the next calculation. This method can be called on any thread
     * (server thread or not), and must return quickly. Expensive computations
     * must be done inside the returned {@link Computation}.
     *
     * @return The calculation, or empty if there are no more calculations to be
     *         done at this moment.
     * @throws NoSuchElementException
     *             When there are no more calculations for now. This method will
     *             be called again after some time.
     */
    public abstract Computation<T> next() throws NoSuchElementException;
}
