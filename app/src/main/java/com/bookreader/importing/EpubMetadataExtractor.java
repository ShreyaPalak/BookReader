package com.bookreader.importing;

import android.content.Context;

import com.bookreader.epub.EpubParseResult;
import com.bookreader.epub.EpubPublicationBridge;

import java.io.IOException;

/**
 * Thin wrapper kept around Readium so BookImportManager's call site didn't
 * need to change shape (still "extract metadata for this file"). All actual
 * parsing now happens in EpubPublicationBridge.kt — this class exists only
 * to keep a stable, Java-native entry point and to translate Readium's
 * result into the plain EpubMetadata shape the rest of the app expects.
 */
public class EpubMetadataExtractor {

    public static class EpubMetadata {
        public String title;
        public String author;
        public int spineLength;
    }

    public static EpubMetadata extract(Context context, String epubFilePath) throws IOException {
        EpubParseResult result = EpubPublicationBridge.parse(context, epubFilePath);

        EpubMetadata metadata = new EpubMetadata();
        metadata.title = result.getTitle();
        metadata.author = result.getAuthor();
        metadata.spineLength = result.getSpineHrefs().size();
        return metadata;
    }
}
