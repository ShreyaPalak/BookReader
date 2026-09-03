package com.bookreader.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reading_positions")
public class ReadingPosition {

    @PrimaryKey
    @NonNull
    public long bookId;

    public Integer spineIndex;
    public Integer characterOffset;
    public long lastUpdated;

    public ReadingPosition() {
    }
}
