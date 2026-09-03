package com.bookreader.ui.library;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bookreader.AppExecutors;
import com.bookreader.BookReaderApp;
import com.bookreader.R;
import com.bookreader.data.AppDatabase;
import com.bookreader.data.Book;
import com.bookreader.importing.BookImportManager;
import com.bookreader.stats.DateUtils;
import com.bookreader.stats.ReadingStatsCalculator;

import android.widget.TextView;
import android.widget.EditText;
import android.view.ViewGroup;

import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    private BookAdapter adapter;
    private AppDatabase database;
    private TextView statsHeaderView;

    // Registers the SAF file-picker contract. Must be registered before
    // onStart(), so this is a field initialized at class construction time,
    // not called lazily from inside a click handler.
    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    handlePickedFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        database = ((BookReaderApp) getApplication()).getDatabase();

        statsHeaderView = findViewById(R.id.library_stats_header);

        RecyclerView recyclerView = findViewById(R.id.library_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(this::openBook, this::showBookOptions);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fab_add_book).setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{
                        "application/epub+zip",
                        "application/pdf"
                })
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time the screen becomes visible — covers returning
        // from the reader (lastOpenedDate changed) and after a fresh import.
        loadBooks();
        loadStats();
    }

    private void loadStats() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Integer pagesToday = database.sessionDao().getPagesReadInRange(
                    DateUtils.startOfDay(0), DateUtils.endOfDay(0));
            Integer pagesYesterday = database.sessionDao().getPagesReadInRange(
                    DateUtils.startOfDay(1), DateUtils.endOfDay(1));
            List<String> readingDays = database.sessionDao().getDistinctReadingDays();
            int streak = ReadingStatsCalculator.computeStreak(readingDays);

            int todayVal = pagesToday == null ? 0 : pagesToday;
            int yesterdayVal = pagesYesterday == null ? 0 : pagesYesterday;

            AppExecutors.getInstance().mainThread().execute(() -> {
                String streakLabel = streak == 1 ? "day" : "days";
                statsHeaderView.setText(
                        "Today: " + todayVal + " pgs   ·   Yesterday: " + yesterdayVal + " pgs   ·   🔥 "
                                + streak + " " + streakLabel);
            });
        });
    }

    private void loadBooks() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Book> books = database.bookDao().getAllBooksSortedByRecent();
            AppExecutors.getInstance().mainThread().execute(() -> adapter.submitList(books));
        });
    }

    private void handlePickedFile(Uri uri) {
        String displayName = queryDisplayName(uri);

        BookImportManager.importBook(this, uri, displayName, new BookImportManager.Callback() {
            @Override
            public void onSuccess(Book book) {
                Toast.makeText(LibraryActivity.this,
                        "Added \"" + book.title + "\"", Toast.LENGTH_SHORT).show();
                loadBooks();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LibraryActivity.this,
                        "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // SAF Uris don't carry a filename directly — it has to be queried via
    // the ContentResolver. Falls back to a generic name rather than crashing
    // if the provider doesn't return one (rare, but seen on some file managers).
    private String queryDisplayName(Uri uri) {
        String result = "imported_file";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex);
                }
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_all_highlights) {
            startActivity(new Intent(this, com.bookreader.ui.highlights.AllHighlightsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openBook(Book book) {
        // Reader screen doesn't exist yet — next step. For now, just update
        // lastOpenedDate so "recently opened" ordering is testable end-to-end.
        AppExecutors.getInstance().diskIO().execute(() -> {
            book.lastOpenedDate = System.currentTimeMillis();
            database.bookDao().update(book);
        });

        if ("EPUB".equals(book.format)) {
            Intent intent = new Intent(this, com.bookreader.ui.reader.ReaderActivity.class);
            intent.putExtra(com.bookreader.ui.reader.ReaderActivity.EXTRA_BOOK_ID, book.id);
            startActivity(intent);
        } else {
            // PDF reader not built yet (next milestone).
            Toast.makeText(this, "PDF reading isn't built yet — EPUB only for now",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showBookOptions(Book book) {
            String[] options = {"Rename book", "Delete book"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(book.title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showRenameDialog(book);
                        } else {
                            confirmDeleteBook(book);
                        }
                    })
                    .show();
        }

    private void showRenameDialog(Book book) {
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setText(book.title);
            input.setSelectAllOnFocus(true);
            int padding = (int) (24 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, 0, padding, 0);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Rename book")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newTitle = input.getText().toString().trim();
                        if (newTitle.isEmpty()) {
                            Toast.makeText(this, "Book name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            book.title = newTitle;
                            database.bookDao().update(book);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Book renamed", Toast.LENGTH_SHORT).show();
                                loadBooks();
                            });
                        });
                    })
                    .show();
        }

    private void confirmDeleteBook(Book book) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete book?")
                    .setMessage("This removes the book, its highlights, reading history, and stored file.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) ->
                            AppExecutors.getInstance().diskIO().execute(() -> {
                                database.annotationDao().deleteForBook(book.id);
                                database.sessionDao().deleteForBook(book.id);
                                database.readingPositionDao().deleteForBook(book.id);
                                database.bookDao().deleteById(book.id);
                                new java.io.File(book.filePath).delete();
                                new java.io.File(getFilesDir(), "extracted/" + book.id).delete();
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
                                    loadBooks();
                                });
                            }))
                    .show();
    }
}
