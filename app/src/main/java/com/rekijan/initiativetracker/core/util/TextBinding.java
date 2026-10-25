package com.rekijan.initiativetracker.core.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.rekijan.initiativetracker.R;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Fills an EditText and writes every edit back to the model. Recycled views keep only one
 * watcher: the previous one is stored in a view tag and removed before the new one is added.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public final class TextBinding {

    private TextBinding() {
    }

    public static void bindText(EditText editText, String value, Consumer<String> onChanged) {
        Object old = editText.getTag(R.id.tag_text_watcher);
        if (old instanceof TextWatcher) {
            editText.removeTextChangedListener((TextWatcher) old);
        }
        editText.setText(value);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChanged.accept(s.toString());
            }
        };
        editText.setTag(R.id.tag_text_watcher, watcher);
        editText.addTextChangedListener(watcher);
    }

    /**
     * Like {@link #bindText}, for a number field. What the user types is checked with
     * {@link NumberInput}: empty or invalid text counts as 0, and the value stays inside [min, max].
     */
    public static void bindInt(EditText editText, int value, int min, int max, IntConsumer onChanged) {
        bindText(editText, String.valueOf(value),
                text -> onChanged.accept(NumberInput.parseInt(text, 0, min, max)));
    }
}
