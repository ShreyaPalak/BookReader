package com.bookreader.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ReadingPositionDao {

    // REPLACE strategy matches the "one row per book, overwritten as you read"
    // design from the original data model — no separate insert-vs-update branching needed.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ReadingPosition position);

    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    ReadingPosition getPositionForBook(long bookId);
}
