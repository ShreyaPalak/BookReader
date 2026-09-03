package com.bookreader.ui.highlights;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bookreader.AppExecutors;
import com.bookreader.BookReaderApp;
import com.bookreader.R;
import com.bookreader.data.AppDatabase;
import com.bookreader.data.HighlightWithBook;
import com.bookreader.export.HighlightExporter;
import com.bookreader.ui.reader.ReaderActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AllHighlightsActivity extends AppCompatActivity {

    private AppDatabase database;
    private HighlightAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_highlights);

        database = ((BookReaderApp) getApplication()).getDatabase();

        RecyclerView recyclerView = findViewById(R.id.highlights_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HighlightAdapter(new java.util.ArrayList<>(), this::jumpToHighlight,
                this::confirmDeleteHighlight);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighlights();
    }

    private void loadHighlights() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<HighlightWithBook> highlights = database.annotationDao().getAllHighlightsWithBookTitle();
            AppExecutors.getInstance().mainThread().execute(() -> adapter.updateData(highlights));
        });
    }

    // Jump from a highlight straight to the book location it came from — one
    // of the original V1 annotation features requested at the very start.
    private void jumpToHighlight(HighlightWithBook highlight) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra(ReaderActivity.EXTRA_BOOK_ID, highlight.bookId);
        if (highlight.spineIndex != null) {
            intent.putExtra(ReaderActivity.EXTRA_SPINE_INDEX, highlight.spineIndex);
        }
        intent.putExtra(ReaderActivity.EXTRA_HIGHLIGHT_TEXT, highlight.selectedText);
        startActivity(intent);
    }

    private void confirmDeleteHighlight(HighlightWithBook highlight) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete highlight?")
                .setMessage("This removes the highlight from the book.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        database.annotationDao().deleteById(highlight.id);
                        runOnUiThread(() ->
                                Toast.makeText(this, "Highlight deleted", Toast.LENGTH_SHORT).show());
                        loadHighlights();
                    });
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_highlights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_highlights) {
            exportHighlights();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportHighlights() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<HighlightWithBook> highlights = database.annotationDao().getAllHighlightsWithBookTitle();
            if (highlights.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "No highlights to export yet", Toast.LENGTH_SHORT).show());
                return;
            }

            String exportText = HighlightExporter.buildExportText(highlights);
            try {
                File file = HighlightExporter.writeToFile(this, exportText);
                runOnUiThread(() -> shareFile(file));
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareFile(File file) {
        // FileProvider is required on modern Android — a raw file:// Uri would
        // be rejected by the share sheet due to StrictMode/FileUriExposed rules.
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share highlights"));
    }
}
