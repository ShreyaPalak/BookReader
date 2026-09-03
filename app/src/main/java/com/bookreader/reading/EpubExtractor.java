package com.bookreader.reading;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts an EPUB's contents to a folder so WebView can render chapters via
 * file:// URLs with relative CSS/image references working automatically.
 * Idempotent: skips extraction if the destination already has content, so
 * this is safe to call every time a book is opened.
 */
public class EpubExtractor {

    /**
     * @param epubFilePath path to the imported .epub (from Book.filePath)
     * @param destDir      folder to extract into — caller decides location,
     *                     typically <filesDir>/extracted/<bookId>/
     */
    public static void extractIfNeeded(String epubFilePath, File destDir) throws IOException {
        if (destDir.exists() && destDir.list() != null && destDir.list().length > 0) {
            return; // already extracted from a previous open
        }
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new IOException("Could not create extraction directory: " + destDir);
        }

        try (ZipFile zipFile = new ZipFile(epubFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File outFile = new File(destDir, entry.getName());

                // Guard against zip-slip: a maliciously crafted entry name like
                // "../../etc/something" could otherwise write outside destDir.
                if (!outFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("Invalid EPUB entry (path traversal): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                }

                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                try (InputStream in = zipFile.getInputStream(entry);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
}
