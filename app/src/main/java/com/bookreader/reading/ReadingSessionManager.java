package com.bookreader.reading;

import com.bookreader.AppExecutors;
import com.bookreader.data.AppDatabase;
import com.bookreader.data.ReadingPosition;
import com.bookreader.data.ReadingSession;

/**
 * One instance per reader screen visit. Call startSession() in onCreate/onStart,
 * call endSession() in onPause/onStop with the ending spine index so pagesRead
 * can be derived. Not thread-safe for concurrent use — one ReaderActivity
 * owns one instance at a time, which matches how it's used.
 */
public class ReadingSessionManager {

    private final AppDatabase database;
    private final long bookId;

    private long currentSessionId = -1;
    private long sessionStartTime;
    private int sessionStartUnit;

    public ReadingSessionManager(AppDatabase database, long bookId) {
        this.database = database;
        this.bookId = bookId;
    }

    /** Call when the reader becomes visible. startUnit is the spine index (EPUB) or page number (PDF) at open. */
    public void startSession(int startUnit) {
        sessionStartTime = System.currentTimeMillis();
        sessionStartUnit = startUnit;

        AppExecutors.getInstance().diskIO().execute(() -> {
            ReadingSession session = new ReadingSession();
            session.bookId = bookId;
            session.startTime = sessionStartTime;
            session.endTime = 0; // marks the session as still open
            session.startUnit = startUnit;
            session.endUnit = startUnit;
            session.pagesRead = 0;
            currentSessionId = database.sessionDao().insert(session);
        });
    }

    /**
     * Call when the reader is backgrounded or closed. endUnit is the spine
     * index / page number the user was at. Also updates ReadingPosition so
     * "resume where I left off" works on next open.
     */
    public void endSession(int endUnit, EpubPositionSnapshot epubPosition) {
        long endTime = System.currentTimeMillis();

        AppExecutors.getInstance().diskIO().execute(() -> {
            if (currentSessionId != -1) {
                ReadingSession session = new ReadingSession();
                session.id = currentSessionId;
                session.bookId = bookId;
                session.startTime = sessionStartTime;
                session.endTime = endTime;
                session.startUnit = sessionStartUnit;
                session.endUnit = endUnit;
                // Pages read this session — clamped at 0 in case the user
                // navigated backward (re-reading shouldn't count as negative progress).
                session.pagesRead = Math.max(0, endUnit - sessionStartUnit);
                database.sessionDao().update(session);
            }

            ReadingPosition position = new ReadingPosition();
            position.bookId = bookId;
            if (epubPosition != null) {
                position.spineIndex = epubPosition.spineIndex;
                position.characterOffset = epubPosition.characterOffset;
            }
            position.lastUpdated = endTime;
            database.readingPositionDao().insertOrUpdate(position);
        });
    }

    /** Simple holder for EPUB position fields, kept separate so PDF callers can pass null. */
    public static class EpubPositionSnapshot {
        public final int spineIndex;
        public final int characterOffset;

        public EpubPositionSnapshot(int spineIndex, int characterOffset) {
            this.spineIndex = spineIndex;
            this.characterOffset = characterOffset;
        }
    }
}
