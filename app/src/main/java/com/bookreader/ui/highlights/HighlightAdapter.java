package com.bookreader.ui.highlights;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bookreader.R;
import com.bookreader.data.HighlightWithBook;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HighlightAdapter extends RecyclerView.Adapter<HighlightAdapter.HighlightViewHolder> {

    public interface OnHighlightClickListener {
        void onHighlightClick(HighlightWithBook highlight);
    }

    private List<HighlightWithBook> highlights;
    private final OnHighlightClickListener clickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    public HighlightAdapter(List<HighlightWithBook> highlights, OnHighlightClickListener clickListener) {
        this.highlights = highlights;
        this.clickListener = clickListener;
    }

    public void updateData(List<HighlightWithBook> newHighlights) {
        this.highlights = newHighlights;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_highlight, parent, false);
        return new HighlightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighlightViewHolder holder, int position) {
        holder.bind(highlights.get(position), clickListener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return highlights.size();
    }

    static class HighlightViewHolder extends RecyclerView.ViewHolder {
        private final View colorSwatch;
        private final TextView textView;
        private final TextView bookTitleView;
        private final TextView dateView;

        HighlightViewHolder(@NonNull View itemView) {
            super(itemView);
            colorSwatch = itemView.findViewById(R.id.highlight_color_swatch);
            textView = itemView.findViewById(R.id.highlight_text);
            bookTitleView = itemView.findViewById(R.id.highlight_book_title);
            dateView = itemView.findViewById(R.id.highlight_date);
        }

        void bind(HighlightWithBook highlight, OnHighlightClickListener listener, SimpleDateFormat dateFormat) {
            textView.setText("\u201C" + highlight.selectedText + "\u201D");
            bookTitleView.setText(highlight.bookTitle);
            dateView.setText(dateFormat.format(highlight.createdDate));

            try {
                colorSwatch.setBackgroundColor(Color.parseColor(highlight.color));
            } catch (Exception e) {
                colorSwatch.setBackgroundColor(Color.parseColor("#FFEB3B")); // fallback yellow
            }

            itemView.setOnClickListener(v -> listener.onHighlightClick(highlight));
        }
    }
}
