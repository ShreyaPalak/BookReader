package com.bookreader.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "annotations")
public class Annotation {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long bookId;

    @NonNull
    public String type; // "HIGHLIGHT" or "NOTE"

    // --- PDF anchor (null when format == EPUB) ---
    public Integer pageNumber;
    public String boundingRectsJson;

    // --- EPUB anchor (null when format == PDF) ---
    public Integer spineIndex;
    public Integer startOffset;
    public Integer endOffset;

    // Frozen copy of the highlighted text at creation time — the reliable
    // field for export and for re-locating text in the rendered chapter,
    // since precise char-offset anchoring isn't implemented yet (see
    // HighlightRenderer for how this gets matched back into the page).
    @NonNull
    public String selectedText;

    public String noteText;
    public String color;
    public long createdDate;
}
