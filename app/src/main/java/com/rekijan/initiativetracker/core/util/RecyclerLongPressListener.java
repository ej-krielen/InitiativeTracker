package com.rekijan.initiativetracker.core.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.IntConsumer;

/**
 * Reports a long press anywhere on a RecyclerView row, also on the EditTexts inside it (which
 * would otherwise take the long press for text selection).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class RecyclerLongPressListener implements RecyclerView.OnItemTouchListener {

    private final GestureDetector gestureDetector;

    /**
     * @param onLongPress Gets the adapter position of the row that was long pressed.
     */
    public RecyclerLongPressListener(Context context, RecyclerView recyclerView, IntConsumer onLongPress) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child == null) return;
                int position = recyclerView.getChildAdapterPosition(child);
                if (position != RecyclerView.NO_POSITION) onLongPress.accept(position);
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}
