package com.bookreader.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {Book.class, ReadingPosition.class, ReadingSession.class, Annotation.class,
                DictionaryCacheEntry.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BookDao bookDao();
    public abstract SessionDao sessionDao();
    public abstract AnnotationDao annotationDao();
    public abstract ReadingPositionDao readingPositionDao();
    public abstract DictionaryCacheDao dictionaryCacheDao();
}
