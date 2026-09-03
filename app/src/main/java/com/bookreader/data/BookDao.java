package com.bookreader.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookDao {

    @Insert
    long insert(Book book);

    @Update
    void update(Book book);

    @Delete
    void delete(Book book);

    @Query("SELECT * FROM books ORDER BY lastOpenedDate DESC")
    List<Book> getAllBooksSortedByRecent();

    @Query("SELECT * FROM books WHERE id = :bookId")
    Book getBookById(long bookId);
}
