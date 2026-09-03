package com.bookreader.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AnnotationDao {

    @Insert
    long insert(Annotation annotation);

    @Update
    void update(Annotation annotation);

    @Delete
    void delete(Annotation annotation);

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdDate ASC")
    List<Annotation> getAnnotationsForBook(long bookId);

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND spineIndex = :spineIndex ORDER BY createdDate ASC")
    List<Annotation> getAnnotationsForBookAndSpine(long bookId, int spineIndex);

    @Query("SELECT * FROM annotations WHERE type = 'HIGHLIGHT' ORDER BY createdDate DESC")
    List<Annotation> getAllHighlights();

    // Backs the "all highlights in one place" screen — joins in the book title
    // since that screen shows highlights across every book, not just one.
    @Query("SELECT annotations.id AS id, annotations.bookId AS bookId, books.title AS bookTitle, "
            + "annotations.spineIndex AS spineIndex, annotations.selectedText AS selectedText, "
            + "annotations.color AS color, annotations.createdDate AS createdDate "
            + "FROM annotations JOIN books ON annotations.bookId = books.id "
            + "WHERE annotations.type = 'HIGHLIGHT' ORDER BY annotations.createdDate DESC")
    List<HighlightWithBook> getAllHighlightsWithBookTitle();
}
