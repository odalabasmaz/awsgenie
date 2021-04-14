package com.atlassian.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BaseExecutor {

    private final List<Future> tasks = new ArrayList<>();
    private final ThreadPoolExecutor executors;

    public BaseExecutor(int numberOfThreads, int maxQueuesize) {
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxQueuesize);
        executors = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L, TimeUnit.MILLISECONDS, workQueue);
    }

    public void runJob(BaseJob job) {
        Future future = executors.submit(() -> {
            job.run();
            return null;
        });
        tasks.add(future);
    }

    public void waitForTasks() throws ExecutionException, InterruptedException {
        try {
            for (Future task : tasks) {
                task.get();
            }
        } finally {
            tasks.clear();
            destroy(executors, 2000);
        }
    }

    private static void destroy(ExecutorService service, long gracefullWaitTimeout) {
        if (service != null) {
            service.shutdown();
            try {
                service.awaitTermination(gracefullWaitTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                service.shutdownNow();
            }

        }

    }
}
