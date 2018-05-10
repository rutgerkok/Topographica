package nl.rutgerkok.topographica.scheduler;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import nl.rutgerkok.topographica.util.ConcurrentHashSet;

/**
 * Used to manage long-running computations.
 *
 */
public final class Scheduler {

    /**
     * Once this is set to true, the {@link #submit(TGRunnable)} method will not
     * accept new submissions anymore.
     */
    private volatile boolean stopping = false;

    private final Set<TGRunnable<?>> activeRunnables = ConcurrentHashSet.create();
    private final Set<UUID> activeFactories = ConcurrentHashSet.create();
    private final Plugin plugin;

    private Executor asyncExecutor = new Executor() {

        @Override
        public void execute(Runnable command) {
            if (plugin.getServer().isPrimaryThread() && plugin.isEnabled()) {
                // Switch to another thread if we are on the server thread
                // Except during shutdown, then the task needs to be executed
                // immediately (starting an async task is not possible)
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, command);
            } else {
                command.run();
            }
        }
    };

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
        activeRunnables.add(runnable);
        Futures.addCallback(runnable.future, new FutureCallback<T>() {

            @Override
            public void onFailure(Throwable t) {
                activeRunnables.remove(runnable);
            }

            @Override
            public void onSuccess(T result) {
                activeRunnables.remove(runnable);
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
                if (stopping) {
                    plugin.getLogger().info("Successfully ended task: " + runnable.name);
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
                activeRunnables.remove(runnable);
                task.cancel(); // Stop repeating the task
            }

            @Override
            public void onSuccess(T result) {
                activeRunnables.remove(runnable);
                task.cancel(); // Stop repeating the task
            }
        });
        return runnable.future;
    }

    /**
     * Stops all current tasks gracefully, and prevents new tasks from running.
     */
    public void stopAll() {
        Logger logger = plugin.getLogger();
        stopping = true;
        for (TGRunnable<?> active : activeRunnables) {
            logger.info("Ending task: " + active.name);
            active.future.cancel(true);
        }
    }

    /**
     * Runs the specified task. If the scheduler is currently stopping, the task
     * will be cancelled before it can run.
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

    /**
     * Starts running a factory. After every computation, failed or not, a new
     * computation is started, until {@link ComputationFactory#next()} throws a
     * {@link NoSuchElementException} or until {@link #stopAll()} is called.
     * This method can be called from any thread.
     *
     * <p>
     * If another computation with the same uuid is already running, this method
     * does nothing.
     *
     * @param factory
     *            The factory to run.
     */
    public <T> void submitFactory(final ComputationFactory<T> factory) {
        Objects.requireNonNull(factory, "factory");
        if (this.activeFactories.add(factory.getUniqueId())) {
            submitFactory0(factory);
        }
    }

    private <T> void submitFactory0(final ComputationFactory<T> factory) {
        Computation<T> next;
        try {
            next = factory.next();
        } catch (NoSuchElementException e) {
            // Done! No more computations
            this.activeFactories.remove(factory.getUniqueId());
            return;
        }
        Futures.addCallback(this.submitComputation(next), new FutureCallback<T>() {

            @Override
            public void onFailure(Throwable t) {
                if (!(stopping && t instanceof CancellationException)) {
                    plugin.getLogger().log(Level.SEVERE, "Error executing task", t);
                }
                // Do not resubmit - factory will stop
            }

            @Override
            public void onSuccess(T result) {
                try {
                    factory.handleResult(result);
                } catch (Throwable e) {
                    plugin.getLogger().log(Level.SEVERE, "Error handling result of task", e);
                }
                // Continue for next element
                submitFactory0(factory);
            }
        }, asyncExecutor);
    }
}
