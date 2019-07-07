package nl.rutgerkok.topographica.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Provides access to the server thread.
 */
public interface ServerThreadGetter<T> {

    /**
     * Runs the given code on the server thread, and returns the result.
     *
     * @param <T>
     *            The type of the result.
     * @param callable
     *            The code.
     * @return The result.
     */
    Future<T> runOnServerThread(Callable<T> callable);
}
