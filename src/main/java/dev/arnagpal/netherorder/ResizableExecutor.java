package dev.arnagpal.netherorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// stands in for mc's worker pool so the thread count can change while the game is running
public final class ResizableExecutor extends AbstractExecutorService {

    private static final Logger LOGGER = LogManager.getLogger("nether-order");
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final String name;
    private volatile ForkJoinPool pool;

    public ResizableExecutor(String name, int threads) {
        this.name = name;
        this.pool = create(threads);
    }

    public int threads() {
        return pool.getParallelism();
    }

    // swap in a new pool, the old one finishes what it already has and then shuts down
    public synchronized void resize(int threads) {
        if (threads == pool.getParallelism()) {
            return;
        }

        ForkJoinPool old = pool;
        pool = create(threads);
        old.shutdown();

        LOGGER.info("worker pool {} now has {} threads", name, threads);
    }

    private ForkJoinPool create(int threads) {
        return new ForkJoinPool(threads, forkJoinPool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(forkJoinPool);
            thread.setName("Worker-" + name + "-" + NEXT_ID.getAndIncrement());
            return thread;
        }, (thread, throwable) -> LOGGER.error("Caught exception in thread {}", thread, throwable), true);
    }

    @Override
    public void execute(Runnable command) {
        pool.execute(command);
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return pool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return pool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return pool.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }
}
