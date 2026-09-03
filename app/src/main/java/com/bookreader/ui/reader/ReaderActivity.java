package com.bookreader.ui.reader;

import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bookreader.AppExecutors;
import com.bookreader.BookReaderApp;
import com.bookreader.R;
import com.bookreader.data.AppDatabase;
import com.bookreader.data.Book;
import com.bookreader.data.ReadingPosition;
import com.bookreader.data.Annotation;
import com.bookreader.dictionary.DictionaryApiClient;
import com.bookreader.dictionary.DictionaryRepository;
import com.bookreader.reading.ChapterHtmlStyler;
import com.bookreader.reading.EpubExtractor;
import com.bookreader.reading.EpubStructureParser;
import com.bookreader.reading.HighlightRenderer;
import com.bookreader.reading.ReaderPreferences;
import com.bookreader.reading.ReaderTheme;
import com.bookreader.reading.ReadingSessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    private AppDatabase database;
    private WebView webView;
    private ReadingSessionManager sessionManager;
    private ReaderPreferences preferences;
    private DictionaryRepository dictionaryRepository;

    private long bookId;
    private String extractionRootPath;
    private List<String> spineFiles;
    private String opfBaseDir;
    private int currentSpineIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, -1);
        if (bookId == -1) {
            Toast.makeText(this, "No book specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = ((BookReaderApp) getApplication()).getDatabase();
        preferences = new ReaderPreferences(this);
        dictionaryRepository = new DictionaryRepository(database);
        webView = findViewById(R.id.reader_webview);

        // JS enabled for word-lookup selection detection (only bridge method exposed below).
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WordLookupBridge(), "AndroidLookup");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSelectionListener();
            }
        });

        findViewById(R.id.btn_prev_chapter).setOnClickListener(v -> goToChapter(currentSpineIndex - 1));
        findViewById(R.id.btn_next_chapter).setOnClickListener(v -> goToChapter(currentSpineIndex + 1));

        findViewById(R.id.btn_font_smaller).setOnClickListener(v -> {
            preferences.decreaseFontSize();
            goToChapter(currentSpineIndex); // reload current chapter with new size applied
        });
        findViewById(R.id.btn_font_larger).setOnClickListener(v -> {
            preferences.increaseFontSize();
            goToChapter(currentSpineIndex);
        });
        findViewById(R.id.btn_theme_toggle).setOnClickListener(v -> {
            ReaderTheme newTheme = preferences.cycleTheme();
            Toast.makeText(this, newTheme.name() + " theme", Toast.LENGTH_SHORT).show();
            goToChapter(currentSpineIndex);
        });

        loadBookAndOpen();
    }

    private void loadBookAndOpen() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Book book = database.bookDao().getBookById(bookId);
            if (book == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            try {
                File extractionRoot = new File(getFilesDir(), "extracted/" + bookId);
                EpubExtractor.extractIfNeeded(book.filePath, extractionRoot);
                EpubStructureParser.EpubStructure structure = EpubStructureParser.parse(extractionRoot);

                extractionRootPath = extractionRoot.getAbsolutePath();
                spineFiles = structure.spineFiles;
                opfBaseDir = structure.opfBaseDir;

                ReadingPosition savedPosition = database.readingPositionDao().getPositionForBook(bookId);
                int startIndex = (savedPosition != null && savedPosition.spineIndex != null)
                        ? savedPosition.spineIndex : 0;

                sessionManager = new ReadingSessionManager(database, bookId);

                runOnUiThread(() -> {
                    sessionManager.startSession(startIndex);
                    goToChapter(startIndex);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Could not open book: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void goToChapter(int spineIndex) {
        if (spineFiles == null || spineIndex < 0 || spineIndex >= spineFiles.size()) {
            return; // silently ignore out-of-range nav (start/end of book)
        }
        currentSpineIndex = spineIndex;

        String chapterPath = opfBaseDir.isEmpty()
                ? spineFiles.get(spineIndex)
                : opfBaseDir + "/" + spineFiles.get(spineIndex);

        File chapterFile = new File(extractionRootPath, chapterPath);

        AppExecutors.getInstance().diskIO().execute(() -> {
            String styledHtml;
            try {
                String rawHtml = new String(Files.readAllBytes(chapterFile.toPath()), StandardCharsets.UTF_8);

                List<Annotation> chapterHighlights =
                        database.annotationDao().getAnnotationsForBookAndSpine(bookId, spineIndex);
                String withHighlights = HighlightRenderer.applyHighlights(rawHtml, chapterHighlights);

                styledHtml = ChapterHtmlStyler.applyStyle(
                        withHighlights, preferences.getTheme(), preferences.getFontSizePercent());
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not load chapter: " + e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }

            // Base URL must end in "/" and point at the chapter's own directory so
            // relative <img src="..."> and <link href="..."> references still resolve.
            String baseUrl = "file://" + chapterFile.getParentFile().getAbsolutePath() + "/";

            runOnUiThread(() -> {
                webView.setBackgroundColor(android.graphics.Color.parseColor(preferences.getTheme().backgroundColor));
                webView.loadDataWithBaseURL(baseUrl, styledHtml, "text/html", "UTF-8", null);
                setTitle("Chapter " + (spineIndex + 1) + " of " + spineFiles.size());
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // End the session whenever the reader leaves the foreground, not just
        // on destroy — covers the user switching apps mid-chapter without
        // losing today's reading progress.
        if (sessionManager != null) {
            ReadingSessionManager.EpubPositionSnapshot snapshot =
                    new ReadingSessionManager.EpubPositionSnapshot(currentSpineIndex, 0);
            sessionManager.endSession(currentSpineIndex, snapshot);
        }
    }

    // Fires whenever the user's text selection changes. Debounced loosely by
    // only acting on non-empty, short selections — long selections (a whole
    // paragraph) are almost never a single-word lookup and would just error
    // out against the dictionary API anyway.
    private void injectSelectionListener() {
        String js = "(function() {"
                + "  document.addEventListener('selectionchange', function() {"
                + "    var text = window.getSelection().toString().trim();"
                + "    if (text.length > 0 && text.length < 500) {"
                + "      AndroidLookup.onTextSelected(text);"
                + "    }"
                + "  });"
                + "})();";
        webView.evaluateJavascript(js, null);
    }

    /**
     * The only surface exposed to page JS. Deliberately a single method with
     * a single String argument — no reflection-friendly surface for the page
     * content to do anything beyond reporting a selected word.
     */
    private class WordLookupBridge {
        @JavascriptInterface
        public void onTextSelected(String text) {
            runOnUiThread(() -> showSelectionActionDialog(text));
        }
    }

    private void showSelectionActionDialog(String selectedText) {
        boolean isSingleWord = !selectedText.contains(" ") && selectedText.length() < 40;
        String[] options = isSingleWord
                ? new String[]{"Highlight", "Look up definition"}
                : new String[]{"Highlight"};

        new AlertDialog.Builder(this)
                .setTitle(truncateForTitle(selectedText))
                .setItems(options, (dialog, which) -> {
                    String choice = options[which];
                    if (choice.equals("Highlight")) {
                        showColorPickerDialog(selectedText);
                    } else if (choice.equals("Look up definition")) {
                        performLookup(selectedText);
                    }
                })
                .show();
    }

    private void showColorPickerDialog(String selectedText) {
        String[] colorNames = {"Yellow", "Green", "Pink", "Blue"};
        String[] colorHex = {"#FFEB3B", "#A5D6A7", "#F48FB1", "#90CAF9"};

        new AlertDialog.Builder(this)
                .setTitle("Highlight color")
                .setItems(colorNames, (dialog, which) -> saveHighlight(selectedText, colorHex[which]))
                .show();
    }

    private void saveHighlight(String selectedText, String colorHex) {
        int spineIndexAtSelection = currentSpineIndex;

        AppExecutors.getInstance().diskIO().execute(() -> {
            Annotation annotation = new Annotation();
            annotation.bookId = bookId;
            annotation.type = "HIGHLIGHT";
            annotation.spineIndex = spineIndexAtSelection;
            annotation.selectedText = selectedText;
            annotation.color = colorHex;
            annotation.createdDate = System.currentTimeMillis();
            database.annotationDao().insert(annotation);

            runOnUiThread(() -> {
                Toast.makeText(this, "Highlighted", Toast.LENGTH_SHORT).show();
                // Reload the chapter so the new highlight renders immediately.
                goToChapter(spineIndexAtSelection);
            });
        });
    }

    private String truncateForTitle(String text) {
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }

    private void performLookup(String word) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            DictionaryApiClient.LookupResult result;
            try {
                result = dictionaryRepository.lookup(word);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }

            runOnUiThread(() -> {
                if (result == null) {
                    Toast.makeText(this, "No definition found for \"" + word + "\"", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDefinitionDialog(result);
            });
        });
    }

    private void showDefinitionDialog(DictionaryApiClient.LookupResult result) {
        StringBuilder message = new StringBuilder();
        if (result.phonetic != null) {
            message.append(result.phonetic).append("\n\n");
        }
        if (result.partOfSpeech != null) {
            message.append("(").append(result.partOfSpeech).append(") ");
        }
        message.append(result.definition);
        if (result.example != null) {
            message.append("\n\nExample: \"").append(result.example).append("\"");
        }

        new AlertDialog.Builder(this)
                .setTitle(result.word)
                .setMessage(message.toString())
                .setPositiveButton("Close", null)
                .show();
    }
}
