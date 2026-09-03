package com.bookreader;

import android.app.Application;
import androidx.room.Room;

import com.bookreader.data.AppDatabase;

public class BookReaderApp extends Application {

    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "bookreader.db"
        )
                // Room requires an explicit migration strategy once you ship
                // v1 to your own device. For now, during active development,
                // this just wipes and recreates the DB on schema changes
                // instead of crashing. Replace with real migrations before
                // you have data you care about keeping.
                .fallbackToDestructiveMigration()
                .build();
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
