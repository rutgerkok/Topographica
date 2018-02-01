package nl.rutgerkok.topographica.scheduler;

import java.util.NoSuchElementException;

/**
 * For when an unknown amount of computations needs to be run. First
 * {@link #initialCalculations()} is called, then {@link #next(Object)} is
 * called repeatedly until there are no more computations to be done.
 * {@link #handleResult(Object)} is called for every finished computation
 * returned by {@link #next(Object)}.
 *
 * <p>
 * The initial calculations can be used to calculate what tasks need to be done
 * exactly, so that {@link #next(Object)} can run quickly.
 *
 * @param <F>
 *            The result of the initial computation.
 *
 * @param <T>
 *            The result types of all computations.
 */
public abstract class ComputationFactory<F, T> {

    /**
     * Called after a computation has finished.
     *
     * @param result
     *            The result of the calculation.
     * @throws Throwable
     *             If an error occurs handling this result. thrown, the
     */
    public abstract void handleResult(T result) throws Throwable;

    /**
     * Performs initial calculations.
     *
     * @return The first computation that needs to be performed.
     */
    public abstract Computation<F> initialCalculations();

    /**
     * Returns the next calculation.
     *
     * @param initialCalculations
     *            Results of the inital calculation.
     *
     * @return The calculation, or empty if there are no more calculations to be
     *         done.
     * @throws NoSuchElementException
     *             When there are no more calculations.
     */
    public abstract Computation<T> next(F initialCalculations) throws NoSuchElementException;
}
