package com.bookreader.dictionary;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Uses dictionaryapi.dev — free, no API key required. Good enough for V1;
 * if it ever goes down or rate-limits, swap the URL/parsing here without
 * touching any caller.
 */
public class DictionaryApiClient {

    public static class LookupResult {
        public String word;
        public String phonetic;      // may be null if not provided
        public String partOfSpeech;  // e.g. "noun" — from the first definition found
        public String definition;
        public String example;       // may be null
    }

    /** Synchronous — call from a background thread only. Returns null if the word wasn't found. */
    public static LookupResult lookup(String word) throws IOException {
        String encoded = URLEncoder.encode(word.trim().toLowerCase(), "UTF-8");
        URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + encoded);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);

        try {
            int code = connection.getResponseCode();
            if (code == 404) {
                return null; // word not found — not an error, just no result
            }
            if (code != 200) {
                throw new IOException("Dictionary lookup failed: HTTP " + code);
            }

            String body = readStream(connection);
            return parseFirstDefinition(body);
        } finally {
            connection.disconnect();
        }
    }

    private static String readStream(HttpURLConnection connection) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    // API returns an array of entries, each with "meanings" -> "definitions".
    // We just surface the first definition found — good enough for a quick lookup,
    // full multi-sense display can be a later refinement if it turns out to matter.
    private static LookupResult parseFirstDefinition(String jsonBody) throws IOException {
        try {
            JSONArray entries = new JSONArray(jsonBody);
            if (entries.length() == 0) return null;

            JSONObject entry = entries.getJSONObject(0);
            LookupResult result = new LookupResult();
            result.word = entry.optString("word");
            result.phonetic = entry.isNull("phonetic") ? null : entry.optString("phonetic", null);

            JSONArray meanings = entry.optJSONArray("meanings");
            if (meanings == null || meanings.length() == 0) return null;

            JSONObject firstMeaning = meanings.getJSONObject(0);
            result.partOfSpeech = firstMeaning.optString("partOfSpeech", null);

            JSONArray definitions = firstMeaning.optJSONArray("definitions");
            if (definitions == null || definitions.length() == 0) return null;

            JSONObject firstDef = definitions.getJSONObject(0);
            result.definition = firstDef.optString("definition", null);
            result.example = firstDef.isNull("example") ? null : firstDef.optString("example", null);

            return result;
        } catch (Exception e) {
            throw new IOException("Could not parse dictionary response", e);
        }
    }
}
