package nl.rutgerkok.topographica.scheduler;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a computation that needs multiple scheduled tasks to finish.
 *
 * @param <T>
 *            Type of the main task.
 */
public final class Computation<T> {

    /**
     * Used to represent that there are no more computations to be done.
     *
     * @return The "done" computation.
     */
    public static <T> Computation<T> done() {
        return new Computation<>();
    }

    TGRunnable<T> main;
    TGRunnable<?>[] supporting;

    private Computation() {
        this.main = null;
        this.supporting = new TGRunnable<?>[0];
    }

    /**
     * Creates a new computation.
     *
     * @param main
     *            The main task. When this task is done, the whole computation
     *            is finished.
     * @param supporting
     *            Other tasks supplying data to the main task.
     */
    public Computation(TGRunnable<T> main, TGRunnable<?>... supporting) {
        this.main = Objects.requireNonNull(main, "main");
        this.supporting = Arrays.copyOf(supporting, supporting.length);
    }
}
