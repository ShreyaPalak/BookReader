package com.bookreader.reading;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Font size and theme are global reader preferences (apply to every book),
 * not per-book settings — matches how every mainstream reader app behaves
 * and keeps this simple for V1. Revisit only if per-book overrides turn out
 * to matter in practice.
 */
public class ReaderPreferences {

    private static final String PREFS_NAME = "reader_prefs";
    private static final String KEY_FONT_SIZE_PERCENT = "font_size_percent";
    private static final String KEY_THEME = "theme";

    private static final int DEFAULT_FONT_SIZE_PERCENT = 100;
    private static final int MIN_FONT_SIZE_PERCENT = 70;
    private static final int MAX_FONT_SIZE_PERCENT = 200;
    private static final int FONT_SIZE_STEP = 10;

    private final SharedPreferences prefs;

    public ReaderPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getFontSizePercent() {
        return prefs.getInt(KEY_FONT_SIZE_PERCENT, DEFAULT_FONT_SIZE_PERCENT);
    }

    public int increaseFontSize() {
        int newSize = Math.min(MAX_FONT_SIZE_PERCENT, getFontSizePercent() + FONT_SIZE_STEP);
        prefs.edit().putInt(KEY_FONT_SIZE_PERCENT, newSize).apply();
        return newSize;
    }

    public int decreaseFontSize() {
        int newSize = Math.max(MIN_FONT_SIZE_PERCENT, getFontSizePercent() - FONT_SIZE_STEP);
        prefs.edit().putInt(KEY_FONT_SIZE_PERCENT, newSize).apply();
        return newSize;
    }

    public ReaderTheme getTheme() {
        String name = prefs.getString(KEY_THEME, ReaderTheme.LIGHT.name());
        try {
            return ReaderTheme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ReaderTheme.LIGHT; // stored value somehow invalid — fall back rather than crash
        }
    }

    public void setTheme(ReaderTheme theme) {
        prefs.edit().putString(KEY_THEME, theme.name()).apply();
    }

    /** Cycles LIGHT -> SEPIA -> DARK -> BLACK -> LIGHT, for a single "next theme" button. */
    public ReaderTheme cycleTheme() {
        ReaderTheme[] values = ReaderTheme.values();
        int nextIndex = (getTheme().ordinal() + 1) % values.length;
        ReaderTheme next = values[nextIndex];
        setTheme(next);
        return next;
    }
}
