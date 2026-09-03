package com.bookreader.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "books")
public class Book {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title;

    public String author;

    @NonNull
    public String format; // "EPUB" or "PDF"

    @NonNull
    public String filePath;

    public long createdDate;
    public long lastOpenedDate;
    public int totalUnits;
    public int currentUnit;

    public Book(@NonNull String title, @NonNull String format, @NonNull String filePath) {
        this.title = title;
        this.format = format;
        this.filePath = filePath;
        this.createdDate = System.currentTimeMillis();
        this.lastOpenedDate = 0L;
        this.totalUnits = 0;
        this.currentUnit = 0;
    }
}
