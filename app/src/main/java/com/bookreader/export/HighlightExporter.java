package com.bookreader.export;

import android.content.Context;

import com.bookreader.data.HighlightWithBook;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recreates the one loved feature from the user's old app: every highlight,
 * across every book, in a single readable document. Groups by book title so
 * it's easy to scan; plain text (not markdown/HTML) so it opens anywhere.
 */
public class HighlightExporter {

    /** Builds the export text in memory — callers can also just display this, not only write it to a file. */
    public static String buildExportText(List<HighlightWithBook> highlights) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

        // LinkedHashMap preserves insertion order — since the query is already
        // ordered by createdDate DESC, books naturally group by most-recent-highlight-first.
        Map<String, StringBuilder> byBook = new LinkedHashMap<>();

        for (HighlightWithBook h : highlights) {
            StringBuilder section = byBook.computeIfAbsent(h.bookTitle, k -> new StringBuilder());
            section.append("\"").append(h.selectedText).append("\"\n");
            section.append("— ").append(dateFormat.format(h.createdDate)).append("\n\n");
        }

        StringBuilder output = new StringBuilder();
        output.append("My Highlights\n");
        output.append("=============\n\n");
        for (Map.Entry<String, StringBuilder> entry : byBook.entrySet()) {
            output.append(entry.getKey()).append("\n");
            output.append("-".repeat(entry.getKey().length())).append("\n\n");
            output.append(entry.getValue());
        }

        return output.toString();
    }

    /**
     * Writes the export to app-external files storage (no special permission
     * needed on modern Android) and returns the File so the caller can share it.
     */
    public static File writeToFile(Context context, String exportText) throws IOException {
        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Could not create export directory");
        }
        File outFile = new File(exportDir, "highlights.txt");
        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write(exportText);
        }
        return outFile;
    }
}
