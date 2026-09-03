package com.bookreader.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DictionaryCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DictionaryCacheEntry entry);

    @Query("SELECT * FROM dictionary_cache WHERE word = :word")
    DictionaryCacheEntry getByWord(String word);
}
