package com.bookreader.dictionary;

import com.bookreader.data.AppDatabase;
import com.bookreader.data.DictionaryCacheEntry;

import java.io.IOException;

/**
 * Sits between ReaderActivity and DictionaryApiClient. Call lookup() from a
 * background thread only — it does its own DB + network calls synchronously.
 *
 * Strategy: cache is checked first (works fully offline for anything looked
 * up before). On a cache miss, hits the network; a successful result is
 * cached for next time. On a network failure with no cache entry, throws so
 * the caller can show a real "you're offline and this word isn't cached yet"
 * message rather than a silent empty result.
 */
public class DictionaryRepository {

    private final AppDatabase database;

    public DictionaryRepository(AppDatabase database) {
        this.database = database;
    }

    public DictionaryApiClient.LookupResult lookup(String rawWord) throws IOException {
        String key = rawWord.trim().toLowerCase();

        DictionaryCacheEntry cached = database.dictionaryCacheDao().getByWord(key);
        if (cached != null) {
            return toLookupResult(cached);
        }

        try {
            DictionaryApiClient.LookupResult networkResult = DictionaryApiClient.lookup(key);
            if (networkResult != null) {
                database.dictionaryCacheDao().insertOrUpdate(toCacheEntry(networkResult));
            }
            return networkResult;
        } catch (IOException networkError) {
            // No cache entry and no network — nothing we can offer for this word.
            // Re-throw with a clearer message than the raw network exception.
            throw new IOException("Offline and \"" + key + "\" isn't cached yet", networkError);
        }
    }

    private static DictionaryApiClient.LookupResult toLookupResult(DictionaryCacheEntry entry) {
        DictionaryApiClient.LookupResult result = new DictionaryApiClient.LookupResult();
        result.word = entry.word;
        result.phonetic = entry.phonetic;
        result.partOfSpeech = entry.partOfSpeech;
        result.definition = entry.definition;
        result.example = entry.example;
        return result;
    }

    private static DictionaryCacheEntry toCacheEntry(DictionaryApiClient.LookupResult result) {
        DictionaryCacheEntry entry = new DictionaryCacheEntry(result.word.trim().toLowerCase());
        entry.phonetic = result.phonetic;
        entry.partOfSpeech = result.partOfSpeech;
        entry.definition = result.definition;
        entry.example = result.example;
        entry.cachedDate = System.currentTimeMillis();
        return entry;
    }
}
