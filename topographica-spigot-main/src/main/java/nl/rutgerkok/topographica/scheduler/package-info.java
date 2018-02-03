/**
 * The scheduler can handle:
 *
 * <ul>
 * <li>Individual TGRunnables, that either execute for a long time in a worker
 * thread, or execute in small batches on the server thread.
 * <li>Computations, which submit one or more TGRunnables.
 * <li>ComputationFactories, which submit an unknown amount of Computations.
 * </ul>
 */
package nl.rutgerkok.topographica.scheduler;
