package com.bookreader;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central place for background/main-thread execution. Room forbids DB
 * access on the main thread, so every DAO call goes through diskIO().
 * If a result needs to update a UI element, post it via mainThread().
 *
 * Usage:
 *   AppExecutors.getInstance().diskIO().execute(() -> {
 *       Book book = db.bookDao().getBookById(id);
 *       AppExecutors.getInstance().mainThread().execute(() -> {
 *           // update UI with `book` here
 *       });
 *   });
 */
public class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final Executor mainThread;

    private AppExecutors(ExecutorService diskIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors(
                            Executors.newSingleThreadExecutor(),
                            new MainThreadExecutor()
                    );
                }
            }
        }
        return instance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }
    }
}
