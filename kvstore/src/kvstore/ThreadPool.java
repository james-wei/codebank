package kvstore;

import java.util.LinkedList;
import java.util.Queue;

public class ThreadPool {

    /* Array of threads in the threadpool */
    public Thread threads[];
    
    /* Queue of Runnables to be executed by workers. */
    private Queue<Runnable> tasks;
    
    /* Thread pool status indicator. */
    volatile boolean running;

    /**
     * Constructs a Threadpool with a certain number of threads.
     *
     * @param size number of threads in the thread pool
     */
    public ThreadPool(int size) {
        running = true;
        threads = new Thread[size];
        tasks = new LinkedList<Runnable>();
        for (int i = 0; i < size; i++) {
            threads[i] = new WorkerThread(this);
            threads[i].start();
        }
    }

    /**
     * Add a job to the queue of jobs that have to be executed. As soon as a
     * thread is available, the thread will retrieve a job from this queue if
     * if one exists and start processing it.
     *
     * @param r job that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public void addJob(Runnable r) throws InterruptedException {
        synchronized (tasks) {
            boolean added = false;
            while (!added) {
                added = tasks.offer(r); 
            }
            tasks.notify();
        }
    }

    /**
     * Block until a job is present in the queue and retrieve the job
     * @return A runnable task that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public Runnable getJob() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.peek() == null) {
                tasks.wait();
            }
            return tasks.poll();
        }
    }
    
    /**
     * Signal workers to exit cleanly.
     */
    public void close() {
        running = false;
    }
    
    /**
     * A thread in the thread pool.
     */
    public class WorkerThread extends Thread {

        public ThreadPool threadPool;

        /**
         * Constructs a thread for this particular ThreadPool.
         *
         * @param pool the ThreadPool containing this thread
         */
        public WorkerThread(ThreadPool pool) {
            threadPool = pool;
        }

        /**
         * Scan for and execute tasks.
         */
        @Override
        public void run() {
            Runnable job;
            while (threadPool.running) {
                try {
                    job = threadPool.getJob();
                    if (job != null) {
                        job.run();
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }
}
