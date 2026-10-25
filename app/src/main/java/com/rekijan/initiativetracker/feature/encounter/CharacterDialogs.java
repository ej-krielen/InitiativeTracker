package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Delete characters, from the drawer: a checklist of the party's characters in initiative order;
 * the checked ones are removed when the user taps Delete. Long-pressing a card still removes a
 * single character.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class CharacterDialogs {

    private CharacterDialogs() {
    }

    /**
     * @param onDeleted Called after characters were removed, e.g. to fix the details pane.
     */
    public static void askDeleteCharacters(Context context, Encounter encounter, Runnable onDeleted) {
        if (encounter.size() == 0) {
            Toast.makeText(context, R.string.delete_characters_none, Toast.LENGTH_SHORT).show();
            return;
        }
        List<CharacterModel> characters = new ArrayList<>(encounter.getCharacters());
        String[] names = new String[characters.size()];
        for (int i = 0; i < names.length; i++) {
            String name = characters.get(i).getCharacterName();
            names[i] = TextUtils.isEmpty(name) ? context.getString(R.string.empty_character_name) : name;
        }
        boolean[] checked = new boolean[names.length];
        Dialogs.builder(context)
                .setTitle(R.string.nav_delete_characters)
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_party_delete_button, (d, w) -> {
                    List<CharacterModel> chosen = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) chosen.add(characters.get(i));
                    }
                    if (chosen.isEmpty()) return;
                    encounter.removeAll(chosen);
                    onDeleted.run();
                })
                .show();
    }
}
