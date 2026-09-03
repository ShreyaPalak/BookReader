package com.bookreader.importing;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Handles copying a file picked via Storage Access Framework (Uri from
 * ACTION_OPEN_DOCUMENT) into the app's private storage. We never read
 * directly from the picked Uri long-term — permissions on it aren't
 * guaranteed to survive app restarts, so we take our own copy once,
 * at import time.
 */
public class FileImportUtil {

    /**
     * Copies the file at sourceUri into app-local storage.
     *
     * @param context   any Context
     * @param sourceUri Uri returned from the system file picker
     * @param extension "epub" or "pdf" (no leading dot)
     * @return absolute path to the app-local copy, to store in Book.filePath
     */
    public static String copyToAppStorage(Context context, Uri sourceUri, String extension) throws IOException {
        File booksDir = new File(context.getFilesDir(), "books");
        if (!booksDir.exists() && !booksDir.mkdirs()) {
            throw new IOException("Could not create books directory");
        }

        String fileName = UUID.randomUUID().toString() + "." + extension;
        File destFile = new File(booksDir, fileName);

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {

            if (in == null) {
                throw new IOException("Could not open input stream for " + sourceUri);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(bytesRead == buffer.length ? buffer : trim(buffer, bytesRead));
            }
        }

        return destFile.getAbsolutePath();
    }

    // Only trims when the last chunk is short; avoids extra array churn on full-buffer reads.
    private static byte[] trim(byte[] buffer, int length) {
        byte[] trimmed = new byte[length];
        System.arraycopy(buffer, 0, trimmed, 0, length);
        return trimmed;
    }

    /** Extracts a lowercase file extension from a display name, defaulting to "" if none found. */
    public static String extensionFromFileName(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot == -1 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }
}
