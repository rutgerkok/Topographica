package nl.rutgerkok.topographica.scheduler;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Used to manage long-running computations.
 *
 */
public final class Scheduler {

    private volatile boolean stopping = false;

    private final Set<TGRunnable<?>> actives = Collections
            .newSetFromMap(new ConcurrentHashMap<TGRunnable<?>, Boolean>());
    private final Plugin plugin;

    public Scheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs a task on a worker thread. The {@link TGRunnable#run()} is expected
     * to run for a long time, until the computation is finished. The runnable
     * should periodically check {@link TGRunnable#future} to make sure it has
     * not been cancelled. (If it is, the method should return immediately.)
     *
     * @param runnable
     *            The runnable.
     * @return A future, to cancel the task or check its completion.
     */
    private <T> ListenableFuture<T> runAsync(final TGRunnable<T> runnable) {
        actives.add(runnable);
        Futures.addCallback(runnable.future, new FutureCallback<T>() {

            @Override
            public void onFailure(Throwable t) {
                actives.remove(runnable);
            }

            @Override
            public void onSuccess(T result) {
                actives.remove(runnable);
            }
        });
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                try {
                    runnable.run();
                    if (!runnable.future.isDone()) {
                        throw new RuntimeException("Forgot to mark as finished!");
                    }
                } catch (Throwable e) {
                    runnable.future.setException(e);
                }
            }
        });
        return runnable.future;
    }

    /**
     * Runs a small part of a large computation on the server thread every tick
     * until it is cancelled or completed. The runnable can cancel or complete
     * itself using {@link TGRunnable#future}.
     *
     * @param runnable
     *            The runnable.
     * @return A future, which can be used to cancel the task, or listen for
     *         completion or failure.
     */
    private <T> ListenableFuture<T> runEveryTick(final TGRunnable<T> runnable) {
        final BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

            @Override
            public void run() {
                // Called every tick until future.set(..) or
                // future.setException(..) is called
                try {
                    runnable.run();
                } catch (Throwable e) {
                    // Failed, stop task from repeating
                    runnable.future.setException(e);
                }
            }
        }, 0, 1);
        Futures.addCallback(runnable.future, new FutureCallback<T>() {

            @Override
            public void onFailure(Throwable t) {
                actives.remove(runnable);
                task.cancel(); // Stop repeating the task
            }

            @Override
            public void onSuccess(T result) {
                actives.remove(runnable);
                task.cancel(); // Stop repeating the task
            }
        });
        return runnable.future;
    }

    private <F, T> void runFactory(final ComputationFactory<F, T> factory, final F seed,
            final SettableFuture<Void> supervisor) {
        try {
            Computation<T> next = factory.next(seed);

            Futures.addCallback(submitComputation(next), new FutureCallback<T>() {

                @Override
                public void onFailure(Throwable t) {
                    supervisor.setException(t);
                }

                @Override
                public void onSuccess(T result) {
                    try {
                        factory.handleResult(result);

                        // On to next computation
                        runFactory(factory, seed, supervisor);
                    } catch (Throwable t) {
                        supervisor.setException(t);
                    }
                }
            });
        } catch (NoSuchElementException e) {
            // Done! No more computations.
            supervisor.set(null);
        }
    }

    /**
     * Stops all current tasks gracefully, and prevents new tasks from running.
     */
    public void stopAll() {
        stopping = true;
        for (TGRunnable<?> active : actives) {
            active.future.cancel(true);
        }
    }

    /**
     * Runs the specified task.
     *
     * @param runnable
     *            The task.
     * @return Future for tracking completion.
     */
    public <T> ListenableFuture<T> submit(TGRunnable<T> runnable) {
        if (stopping) {
            return Futures.immediateCancelledFuture();
        }
        switch (runnable.type) {
            case EVERY_TICK:
                return runEveryTick(runnable);
            case LONG_RUNNING:
                return runAsync(runnable);
            default:
                throw new IllegalArgumentException(runnable.type + " not recognized");
        }
    }

    /**
     * Submits all tasks.
     *
     * @param factory
     *            A factory that keeps generating tasks until it is done.
     * @return A future that will return when all computations are finished.
     */
    public <F, T> ListenableFuture<Void> submitAll(final ComputationFactory<F, T> factory) {
        final SettableFuture<Void> supervisor = SettableFuture.create();
        Futures.addCallback(submitComputation(factory.initialCalculations()), new FutureCallback<F>() {

            @Override
            public void onFailure(Throwable t) {
                supervisor.setException(t);
            }

            @Override
            public void onSuccess(F result) {
                runFactory(factory, result, supervisor);
            }
        });
        return supervisor;
    }

    /**
     * Submits a computation, consisting of one or more runnables. The
     * computation is considered complete when its main runnable is finished.
     *
     * @param computation
     *            The computation.
     * @return Future for tracking the computation.
     */
    public <T> ListenableFuture<T> submitComputation(Computation<T> computation) {
        ListenableFuture<T> future = submit(computation.main);
        for (TGRunnable<?> runnable : computation.supporting) {
            submit(runnable);
        }
        return future;
    }
}
