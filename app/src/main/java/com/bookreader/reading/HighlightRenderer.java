package com.bookreader.reading;

import com.bookreader.data.Annotation;

import java.util.List;

/**
 * Re-applies saved highlights to chapter HTML at render time. Matches each
 * highlight's frozen `selectedText` against the chapter's raw HTML with a
 * plain string search and wraps the FIRST occurrence found in a <mark> tag.
 *
 * Known limitation: if the exact same phrase appears more than once in a
 * chapter, only the first occurrence gets highlighted, even if the user
 * highlighted a later one. Precise start/end character-offset anchoring
 * would fix this but isn't implemented yet (Annotation.startOffset/
 * endOffset exist in the schema but aren't populated). Acceptable for V1 —
 * exact duplicate-phrase collisions are rare in practice — but worth fixing
 * if it turns out to bite in real use.
 */
public class HighlightRenderer {

    public static String applyHighlights(String rawHtml, List<Annotation> highlightsForChapter) {
        String result = rawHtml;
        for (Annotation highlight : highlightsForChapter) {
            if (!"HIGHLIGHT".equals(highlight.type)) continue;
            result = wrapFirstOccurrence(result, highlight.selectedText, highlight.color);
        }
        return result;
    }

    private static String wrapFirstOccurrence(String html, String targetText, String color) {
        if (targetText == null || targetText.isEmpty()) return html;

        int index = html.indexOf(targetText);
        if (index == -1) {
            // Text not found verbatim — likely inside a tag boundary the plain
            // search can't cross, or the chapter content changed. Skip silently
            // rather than corrupting the HTML with a bad insertion.
            return html;
        }

        String bgColor = (color != null && !color.isEmpty()) ? color : "#FFEB3B"; // default yellow
        String openTag = "<mark style=\"background-color:" + bgColor + " !important;\">";
        String closeTag = "</mark>";

        return html.substring(0, index)
                + openTag + targetText + closeTag
                + html.substring(index + targetText.length());
    }
}
