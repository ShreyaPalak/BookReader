package com.bookreader.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bookreader.R;
import com.bookreader.data.Book;

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private List<Book> books = new ArrayList<>();
    private final OnBookClickListener clickListener;

    public BookAdapter(OnBookClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /** Replaces the current list, diffing so RecyclerView animates only what changed. */
    public void submitList(List<Book> newBooks) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BookDiffCallback(books, newBooks));
        books = newBooks;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.bind(book, clickListener);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView authorView;
        private final TextView formatBadge;
        private final TextView progressView;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.book_title);
            authorView = itemView.findViewById(R.id.book_author);
            formatBadge = itemView.findViewById(R.id.book_format_badge);
            progressView = itemView.findViewById(R.id.book_progress);
        }

        void bind(Book book, OnBookClickListener listener) {
            titleView.setText(book.title);

            // Author is optional (manual entry only, per V1 scope) — hide the row instead
            // of showing a blank/"null" line when the user hasn't set one.
            if (book.author != null && !book.author.trim().isEmpty()) {
                authorView.setText(book.author);
                authorView.setVisibility(View.VISIBLE);
            } else {
                authorView.setVisibility(View.GONE);
            }

            formatBadge.setText(book.format);

            // Placeholder until ReadingPosition is wired in (next screen) —
            // shows unit count so the row isn't empty in the meantime.
            String unitLabel = "PDF".equals(book.format) ? "pages" : "chapters";
            progressView.setText(book.totalUnits + " " + unitLabel);

            itemView.setOnClickListener(v -> listener.onBookClick(book));
        }
    }

    private static class BookDiffCallback extends DiffUtil.Callback {
        private final List<Book> oldList;
        private final List<Book> newList;

        BookDiffCallback(List<Book> oldList, List<Book> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).id == newList.get(newItemPosition).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Book oldBook = oldList.get(oldItemPosition);
            Book newBook = newList.get(newItemPosition);
            return oldBook.title.equals(newBook.title)
                    && java.util.Objects.equals(oldBook.author, newBook.author)
                    && oldBook.lastOpenedDate == newBook.lastOpenedDate
                    && oldBook.totalUnits == newBook.totalUnits;
        }
    }
}
