package com.bookreader.reading;

/**
 * Reading themes, per the original feature request: adjustable white/yellow/
 * black/dark reading modes. Colors are applied via injected CSS rather than
 * WebView's native force-dark, since force-dark support is inconsistent
 * across Android versions/WebView builds and doesn't give per-theme control
 * over a sepia/yellow option anyway.
 */
public enum ReaderTheme {
    LIGHT("#FFFFFF", "#000000"),
    SEPIA("#F5ECD9", "#5B4636"),
    DARK("#1E1E1E", "#D0D0D0"),
    BLACK("#000000", "#AAAAAA");

    public final String backgroundColor;
    public final String textColor;

    ReaderTheme(String backgroundColor, String textColor) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }
}
