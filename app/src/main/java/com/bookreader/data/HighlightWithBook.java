package com.bookreader.data;

/** Result shape for AnnotationDao.getAllHighlightsWithBookTitle() — a highlight plus its book's title. */
public class HighlightWithBook {
    public long id;
    public long bookId;
    public String bookTitle;
    public Integer spineIndex;
    public String selectedText;
    public String color;
    public long createdDate;
}
