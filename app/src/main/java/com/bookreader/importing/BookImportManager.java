package com.bookreader.importing;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bookreader.AppExecutors;
import com.bookreader.BookReaderApp;
import com.bookreader.data.AppDatabase;
import com.bookreader.data.Book;

import java.io.IOException;

/**
 * Single entry point for "user picked a file, make it a Book in the library."
 * Wires together FileImportUtil + the format-specific metadata extractors +
 * BookDao, all off the main thread (Room forbids DB writes on it).
 *
 * Usage from an Activity, after receiving a Uri from
 * Intent.ACTION_OPEN_DOCUMENT / onActivityResult or the Activity Result API:
 *
 *   BookImportManager.importBook(this, uri, displayName, new BookImportManager.Callback() {
 *       public void onSuccess(Book book) { // update UI, e.g. refresh library list }
 *       public void onError(Exception e) { // show a toast/snackbar }
 *   });
 */
public class BookImportManager {

    private static final String TAG = "BookImportManager";

    public interface Callback {
        void onSuccess(Book book);
        void onError(Exception e);
    }

    public static void importBook(Context context, Uri sourceUri, String displayName, Callback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Book book = doImport(context, sourceUri, displayName);
                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(book));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // Runs entirely on the background thread. Throws on any failure —
    // caller wraps this in try/catch and routes to onError.
    private static Book doImport(Context context, Uri sourceUri, String displayName) throws IOException {
        String extension = FileImportUtil.extensionFromFileName(displayName);
        String format;
        if ("epub".equals(extension)) {
            format = "EPUB";
        } else if ("pdf".equals(extension)) {
            format = "PDF";
        } else {
            throw new IOException("Unsupported file type: ." + extension + " (only .epub and .pdf are supported)");
        }

        // Copy first — every downstream step reads from our own stable copy,
        // never from the picker's Uri (see FileImportUtil for why).
        String localPath = FileImportUtil.copyToAppStorage(context, sourceUri, extension);

        Book book;
        try {
            if (format.equals("EPUB")) {
                EpubMetadataExtractor.EpubMetadata meta = extractEpub(context, localPath);
                book = new Book(meta.title, format, localPath);
                book.author = meta.author;
                book.totalUnits = meta.spineLength;
            } else {
                PdfMetadataExtractor.PdfMetadata meta = extractPdf(localPath);
                // No reliable title in a PDF's own metadata (see extractor note) —
                // fall back to the picked file's display name, minus extension.
                String fallbackTitle = stripExtension(displayName);
                book = new Book(fallbackTitle, format, localPath);
                book.totalUnits = meta.pageCount;
            }
        } catch (Exception e) {
            // Metadata extraction failed after we already copied the file —
            // clean up the orphaned copy so imports don't leak storage on failure.
            new java.io.File(localPath).delete();

            // Full stack trace goes to Logcat (adb logcat, filter tag
            // "BookImportManager") so the real cause is always inspectable,
            // even when the toast message alone isn't enough.
            Log.e(TAG, "Failed to read " + format + " file: " + displayName, e);

            // e.toString() (class name + message) instead of e.getMessage()
            // alone — many exceptions (NullPointerException,
            // UnsupportedOperationException thrown with no args, etc.) have
            // a null getMessage(), which was silently producing an uninformative
            // "Could not read EPUB file: null"-style toast with no real clue.
            throw new IOException("Could not read " + format + " file: " + e, e);
        }

        AppDatabase db = ((BookReaderApp) context.getApplicationContext()).getDatabase();
        long id = db.bookDao().insert(book);
        book.id = id;
        return book;
    }

    private static EpubMetadataExtractor.EpubMetadata extractEpub(Context context, String path) throws Exception {
        return EpubMetadataExtractor.extract(context, path);
    }

    private static PdfMetadataExtractor.PdfMetadata extractPdf(String path) throws Exception {
        return PdfMetadataExtractor.extract(path);
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) return "Untitled";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? fileName : fileName.substring(0, dot);
    }
}
