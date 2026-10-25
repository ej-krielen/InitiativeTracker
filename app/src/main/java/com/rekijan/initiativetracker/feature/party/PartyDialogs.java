package com.rekijan.initiativetracker.feature.party;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;

import java.util.function.Function;

/**
 * The party dialogs: name a new party or rename one (checked with {@link PartyNames}, the error
 * shows under the field and the dialog stays open), confirm a delete, and the "all slots used" and
 * "get more parties" offers. Only UI; the caller does the actual change.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class PartyDialogs {

    /** Gets the accepted, cleaned name. */
    public interface NameListener {
        void onName(String name);
    }

    private PartyDialogs() {
    }

    /**
     * @param excludeId The party being renamed, or null for a new party.
     */
    public static void askName(Context context, @StringRes int title, @StringRes int positive,
                               String initialName, PartyBook book, @Nullable String excludeId,
                               NameListener listener) {
        askName(context, title, positive, R.string.dialog_party_name_hint, initialName,
                name -> book.checkName(name, excludeId), listener);
    }

    /**
     * Asks for a name with the same rules as a party name, checked by the given check; used for
     * player characters too.
     */
    public static void askName(Context context, @StringRes int title, @StringRes int positive,
                               @StringRes int hint, String initialName,
                               Function<String, PartyNames.Check> check, NameListener listener) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_party_name, null);
        TextInputLayout layout = content.findViewById(R.id.party_name_layout);
        layout.setHint(hint);
        EditText input = content.findViewById(R.id.party_name_editText);
        layout.setCounterMaxLength(PartyNames.MAX_LENGTH);
        input.setText(initialName);
        input.selectAll();

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(positive, null)
                .create();

        Runnable submit = () -> {
            String name = input.getText().toString();
            String error = errorFor(context, check.apply(name));
            if (error != null) {
                layout.setError(error);
                return;
            }
            dialog.dismiss();
            listener.onName(PartyNames.clean(name));
        };
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit.run();
            return true;
        });

        // Set after show(), so a wrong name keeps the dialog open
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> submit.run()));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
        dialog.show();
    }

    public static void confirmDelete(Context context, String name, int characterCount, Runnable onDelete) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_party_delete_title)
                .setMessage(context.getResources().getQuantityString(
                        R.plurals.dialog_party_delete_message, characterCount, name, characterCount))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_party_delete_button, (d, w) -> onDelete.run())
                .show();
    }

    /**
     * Tells the user all party slots are used. With a price (the next pack exists in Google Play)
     * it offers that pack; without, it only says to delete a party.
     */
    public static void showLimit(Context context, int used, int limit, @Nullable String price,
                                 Runnable onBuy) {
        AlertDialog.Builder builder = Dialogs.builder(context).setTitle(R.string.dialog_party_limit_title);
        if (price == null) {
            builder.setMessage(context.getString(R.string.dialog_party_limit_message, used, limit))
                    .setPositiveButton(R.string.dialog_ok, null);
        } else {
            builder.setMessage(context.getString(R.string.dialog_party_limit_buy_message,
                            used, limit, PartySlots.PER_PACK, price))
                    .setNegativeButton(R.string.dialog_not_now, null)
                    .setPositiveButton(context.getString(R.string.button_get_party_pack, PartySlots.PER_PACK),
                            (d, w) -> onBuy.run());
        }
        builder.show();
    }

    /** "Get more parties" from the drawer: explains the next pack and its price. */
    public static void offerPack(Context context, int used, int limit, String price, Runnable onBuy) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_party_more_title)
                .setMessage(context.getString(R.string.dialog_party_more_message,
                        used, limit, PartySlots.PER_PACK, price))
                .setNegativeButton(R.string.dialog_not_now, null)
                .setPositiveButton(context.getString(R.string.button_get_party_pack, PartySlots.PER_PACK),
                        (d, w) -> onBuy.run())
                .show();
    }

    @Nullable
    private static String errorFor(Context context, PartyNames.Check check) {
        switch (check) {
            case EMPTY:
                return context.getString(R.string.error_party_name_empty);
            case TOO_LONG:
                return context.getString(R.string.error_party_name_too_long, PartyNames.MAX_LENGTH);
            case DUPLICATE:
                return context.getString(R.string.error_party_name_duplicate);
            default:
                return null;
        }
    }
}
