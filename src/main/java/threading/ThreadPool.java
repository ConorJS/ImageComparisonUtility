package threading;

import java.util.List;

// TODO: Race condition in waitingThreads, should be a sync queue instead of a sync arraylist

public class ThreadPool {

    private static final int MAX_THREADS = 6;

    private Thread threadManager = null;

    private List<Thread> waitingThreads = new SynArrayList<>();

    private Thread[] workers;
    private boolean[] locks;    // Locks are true when in use - one lock per worker in Thread[] workers

    public boolean allTasksGiven = false;

    public ThreadPool() {

        locks = new boolean[MAX_THREADS];
        workers = new Thread[MAX_THREADS];

        for (int i = 0; i < MAX_THREADS; i++) {
            locks[i] = false;
        }

        threadManager = new Thread("Thread manager") {
            public void run() {
                while(!allTasksGiven || (waitingThreads.size() > 0) || (workersStillActive(false))) {

                    // if any thread are waiting, check to see if one of the workers is free
                    if (waitingThreads.size() > 0) {
                        for (int i = 0; i < MAX_THREADS; i++) {

                            if ((!locks[i]) && (waitingThreads.size() > 0)) {

                                locks[i] = true;
                                if (waitingThreads.size() > 0) { // TODO: refactor this, use queue
                                    workers[i] = waitingThreads.get(0);

                                    // wait
                                    while (workers[i] != waitingThreads.get(0)) {
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    // FIXME: This breaks sometimes, the worker is not actually removed and the loop below runs indefinitely
                                    waitingThreads.remove(workers[i]);

                                    while (waitingThreads.contains(workers[i])){
                                        try {
                                            Thread.sleep(50);
                                            waitingThreads.remove(workers[i]);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    workers[i].start();
                                }
                            }
                        }
                    }

                    // check to see if any threads have died - release their respective locks if so
                    for (int i = 0; i < MAX_THREADS; i++) {
                        if ((workers[i] != null) && (!workers[i].isAlive())) {
                            locks[i] = false;
                        }
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        // thread will only die when all tasks have been assigned and have been completed, see while conditional
        threadManager.start();
    }

    private boolean workersStillActive(boolean debug) {
        boolean workersStillActive = false;
        for (int i = 0; i < MAX_THREADS; i++) {
            if (locks[i]) {
                if (debug) {
                    System.out.println("Worker " + i + " is still active");
                }
                workersStillActive = true;
            }
        }

        return workersStillActive;
    }


    public synchronized void assignWorker(Thread thread) {
        waitingThreads.add(thread);
    }

    // FIXME: This won't ever return true if #TASKS < #THREADS (edge case, but should still be repaired)
    public boolean threadsRunning() {
        return threadManager.isAlive();
    }

    public void notifyAllTasksGiven() {
        this.allTasksGiven = true;
    }
}
