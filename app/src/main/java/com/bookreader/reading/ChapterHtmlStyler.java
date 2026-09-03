package com.bookreader.reading;

/**
 * Applies theme + font size to a chapter's raw HTML by injecting a <style>
 * block. Deliberately CSS-only (no JS) — WebView JS execution stays disabled
 * per the earlier decision (EPUB content is untrusted input).
 *
 * `!important` is used because EPUB stylesheets often set their own
 * body/background colors, which would otherwise override the theme.
 */
public class ChapterHtmlStyler {

    public static String applyStyle(String rawHtml, ReaderTheme theme, int fontSizePercent) {
        String css = "<style>"
                + "html, body {"
                + "  background-color: " + theme.backgroundColor + " !important;"
                + "  color: " + theme.textColor + " !important;"
                + "}"
                + "body, p, div, span, li, h1, h2, h3, h4, h5, h6 {"
                + "  color: " + theme.textColor + " !important;"
                + "  font-size: " + fontSizePercent + "% !important;"
                + "  line-height: 1.5 !important;"
                + "}"
                + "img { max-width: 100% !important; height: auto !important; }"
                + "</style>";

        int headEnd = rawHtml.toLowerCase().indexOf("</head>");
        if (headEnd != -1) {
            return rawHtml.substring(0, headEnd) + css + rawHtml.substring(headEnd);
        }

        // No <head> tag found (some minimal/malformed EPUB chapters omit it) —
        // prepend the style block directly so it still applies rather than being dropped.
        return css + rawHtml;
    }
}
