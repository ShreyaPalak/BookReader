package com.bookreader.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dictionary_cache")
public class DictionaryCacheEntry {

    @PrimaryKey
    @NonNull
    public String word; // lowercase, trimmed — matches the lookup key used in DictionaryApiClient

    public String phonetic;
    public String partOfSpeech;
    public String definition;
    public String example;
    public long cachedDate;

    public DictionaryCacheEntry(@NonNull String word) {
        this.word = word;
    }
}
