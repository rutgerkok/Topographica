package nl.rutgerkok.topographica.scheduler;

import java.util.Objects;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Smallest unit of a computation. Used for long-running computations, which
 * considers either of one long run, or multiple smaller runs. When the
 * computation is done (or has failed), the code inside {@link #run()} can let
 * others know using the {@link #future} instance.
 *
 * @see Scheduler This class is designed for use with the Scheduler.
 *
 * @param <T>
 *            Type of the result of the computation.
 */
public abstract class TGRunnable<T> {

    /**
     * Type of computation.
     */
    public enum Type
    {
        /**
         * Runs every tick on the server thread.
         */
        EVERY_TICK,
        /**
         * Runs once for a long time.
         */
        LONG_RUNNING
    }

    protected final Type type;
    final String name;

    /**
     * This future will automatically be cancelled on server shutdown.
     */
    protected final SettableFuture<T> future = SettableFuture.create();

    public TGRunnable(Type type, String name) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Runs the code. When done, it should call
     * {@link SettableFuture#set(Object)}.
     *
     * @throws Throwable
     *             When an error occurs. The scheduler will automatically call
     *             {@link SettableFuture#setException(Throwable)}. If this is a
     *             repeating task, no more repeats will be performed.
     */
    public abstract void run() throws Throwable;

    @Override
    public String toString() {
        return name;
    }

}
