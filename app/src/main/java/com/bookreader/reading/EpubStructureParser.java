package com.bookreader.reading;

import android.content.Context;

import com.bookreader.epub.EpubParseResult;
import com.bookreader.epub.EpubPublicationBridge;

import java.io.IOException;
import java.util.List;

/**
 * Delegates EPUB structure parsing to Readium while preserving the public
 * EpubStructure shape used by ReaderActivity.
 */
public class EpubStructureParser {

    public static class EpubStructure {
        public String opfBaseDir = "";
        public List<String> spineFiles;
    }

    public static EpubStructure parse(Context context, String epubFilePath) throws IOException {
        EpubParseResult result = EpubPublicationBridge.parse(context, epubFilePath);

        EpubStructure structure = new EpubStructure();
        structure.spineFiles = result.getSpineHrefs();
        return structure;
    }
}
