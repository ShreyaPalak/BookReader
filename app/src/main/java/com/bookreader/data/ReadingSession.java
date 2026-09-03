package com.bookreader.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reading_sessions")
public class ReadingSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long bookId;
    public long startTime;
    public long endTime;
    public int startUnit;
    public int endUnit;
    public int pagesRead;
}
