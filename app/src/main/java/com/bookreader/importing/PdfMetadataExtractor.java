package com.bookreader.importing;

import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * PDF handling stays deliberately minimal per the "render as-is, no reflow"
 * decision. All we need at import time is the page count (Book.totalUnits).
 * android.graphics.pdf.PdfRenderer is built into the platform (API 21+) —
 * no PDFBox or other dependency required for this step.
 *
 * Title is not extracted from PDF metadata here; PDFs' embedded title
 * fields are unreliable/often missing, so we fall back to the picked
 * file's display name and let the user rename manually if they care.
 * (Matches the earlier decision: manual metadata entry is fine for V1.)
 */
public class PdfMetadataExtractor {

    public static class PdfMetadata {
        public int pageCount;
    }

    public static PdfMetadata extract(String pdfFilePath) throws IOException {
        File file = new File(pdfFilePath);
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(pfd)) {

            PdfMetadata metadata = new PdfMetadata();
            metadata.pageCount = renderer.getPageCount();
            return metadata;
        }
    }
}
