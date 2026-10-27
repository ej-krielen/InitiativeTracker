package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.TextViewCompat;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.app.InitiativeTrackerApp;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.core.util.NumberInput;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.Collections;

/**
 * The hp dialog: the user types an amount and adds it, sets it or subtracts it, with a preview
 * of each result, so there is no math to do at the table. When an NPC drops below 0 the app
 * offers to remove it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public final class HpDialog {

    private HpDialog() {
    }

    public static void show(Context context, Encounter encounter, CharacterModel character) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_hp, null);
        TextView oldValueTextView = view.findViewById(R.id.old_value_textView);
        TextView addPreviewTextView = view.findViewById(R.id.add_preview_textView);
        TextView setPreviewTextView = view.findViewById(R.id.set_preview_textView);
        TextView subtractPreviewTextView = view.findViewById(R.id.subtract_preview_textView);
        EditText inputEditText = view.findViewById(R.id.input_editText);

        String initialValue = String.valueOf(character.getHp());
        oldValueTextView.setText(initialValue);
        addPreviewTextView.setText(initialValue);
        setPreviewTextView.setText(initialValue);
        subtractPreviewTextView.setText(initialValue);

        inputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int amount = amount(inputEditText);
                addPreviewTextView.setText(String.valueOf(clampHp(character.getHp() + amount)));
                setPreviewTextView.setText(String.valueOf(amount));
                subtractPreviewTextView.setText(String.valueOf(clampHp(character.getHp() - amount)));
            }
        });

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.hp_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        Button addButton = view.findViewById(R.id.add_btn);
        Button setButton = view.findViewById(R.id.set_btn);
        Button subtractButton = view.findViewById(R.id.subtract_btn);

        addButton.setOnClickListener(v -> {
            character.setHp(character.getHp() + amount(inputEditText));
            encounter.notifyChanged();
            dialog.dismiss();
        });
        setButton.setOnClickListener(v -> {
            character.setHp(amount(inputEditText));
            encounter.notifyChanged();
            dialog.dismiss();
        });
        subtractButton.setOnClickListener(v -> {
            character.setHp(character.getHp() - amount(inputEditText));
            encounter.notifyChanged();
            dialog.dismiss();
            if (character.shouldOfferRemoval()) askRemoval(context, encounter, character);
        });
        matchTextSizes(addButton, setButton, subtractButton);

        dialog.show();
    }

    /**
     * The buttons shrink their label to fit on one line, each on its own. After the first layout
     * all three take the smallest size, so a long label (Subtract) doesn't stand out.
     */
    private static void matchTextSizes(Button... buttons) {
        View row = (View) buttons[0].getParent();
        row.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                row.removeOnLayoutChangeListener(this);
                float smallest = Float.MAX_VALUE;
                for (Button b : buttons) smallest = Math.min(smallest, b.getTextSize());
                for (Button b : buttons) {
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(b, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallest);
                }
            }
        });
    }

    private static void askRemoval(Context context, Encounter encounter, CharacterModel character) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_delete_low_title)
                .setMessage(R.string.dialog_delete_low)
                .setPositiveButton(R.string.dialog_delete_low_positive, (d, w) ->
                        ((InitiativeTrackerApp) context.getApplicationContext()).getSync()
                                .removeCharacters(Collections.singletonList(character)))
                .setNegativeButton(R.string.dialog_delete_low_negative, null)
                .show();
    }

    private static int amount(EditText input) {
        return NumberInput.parseInt(input.getText().toString(), 0, 0, CharacterModel.MAX_HP);
    }

    private static int clampHp(int hp) {
        return NumberInput.clamp(hp, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
    }
}
