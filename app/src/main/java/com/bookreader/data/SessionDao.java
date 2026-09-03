package com.bookreader.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SessionDao {

    @Insert
    long insert(ReadingSession session);

    @Update
    void update(ReadingSession session);

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    List<ReadingSession> getSessionsForBook(long bookId);

    // Pass day boundaries as epoch millis (start of day, start of next day).
    @Query("SELECT SUM(pagesRead) FROM reading_sessions WHERE startTime >= :dayStart AND startTime < :dayEnd")
    Integer getPagesReadInRange(long dayStart, long dayEnd);

    @Query("SELECT SUM(endTime - startTime) FROM reading_sessions WHERE startTime >= :dayStart AND startTime < :dayEnd")
    Long getMillisReadInRange(long dayStart, long dayEnd);

    // One row per calendar day (device-local) that had at least one session —
    // raw material for streak calculation. 'localtime' modifier matters here:
    // without it, SQLite buckets by UTC date, which silently misaligns with
    // DateUtils' local-midnight boundaries near midnight in most timezones.
    @Query("SELECT DISTINCT date(startTime / 1000, 'unixepoch', 'localtime') AS day " +
           "FROM reading_sessions ORDER BY day DESC")
    List<String> getDistinctReadingDays();

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    void deleteForBook(long bookId);
}
