package com.rekijan.initiativetracker.feature.spell;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.ArrayList;
import java.util.List;

/**
 * The spell dialogs on the GM's encounter: Cast spell (caster, spell, targets), which spells of a
 * caster who left the fight keep going, and ending a spell on all its targets. The rules are in
 * {@link Spells} and {@link Encounter}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2026
 */
public final class SpellDialogs {

    private SpellDialogs() {
    }

    /**
     * Cast spell. The caster is the character whose turn it is unless changed; the caster is also
     * in the target list, unticked, for spells on themselves. The active character's row is
     * outlined in gold. The Cast button counts the ticked targets ("Cast on 3"); pressing it checks
     * the name, the duration (empty, or 1 to 9999) and that there is a target, and shows what's wrong.
     */
    public static void showCast(Context context, Encounter encounter) {
        List<CharacterModel> combatants = Spells.combatants(encounter);
        if (combatants.isEmpty()) {
            Toast.makeText(context, R.string.spell_need_characters, Toast.LENGTH_SHORT).show();
            return;
        }
        CharacterModel active = encounter.get(0);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_cast_spell, null);
        TextInputLayout nameLayout = view.findViewById(R.id.spell_name_layout);
        TextView nameInput = view.findViewById(R.id.spell_name_editText);
        TextInputLayout durationLayout = view.findViewById(R.id.spell_duration_layout);
        TextView durationInput = view.findViewById(R.id.spell_duration_editText);
        TextView descriptionInput = view.findViewById(R.id.spell_description_editText);
        TextView targetsError = view.findViewById(R.id.spell_targets_error);
        ViewGroup targetsContainer = view.findViewById(R.id.spell_targets_container);

        // Caster: every combatant, the active one first in line by default
        List<String> casterNames = new ArrayList<>();
        int casterIndex = 0;
        for (int i = 0; i < combatants.size(); i++) {
            CharacterModel c = combatants.get(i);
            casterNames.add(c == active
                    ? context.getString(R.string.spell_caster_active, name(context, c)) : name(context, c));
            if (c == active) casterIndex = i;
        }
        int[] caster = {casterIndex};
        MaterialAutoCompleteTextView casterDropdown = view.findViewById(R.id.spell_caster_dropdown);
        casterDropdown.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, casterNames));
        casterDropdown.setText(casterNames.get(casterIndex), false);
        casterDropdown.setOnItemClickListener((parent, v, position, id) -> caster[0] = position);

        List<MaterialCheckBox> boxes = new ArrayList<>();
        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.action_cast_spell)
                .setIcon(R.drawable.ic_spellbook)
                .setView(view)
                .setPositiveButton(R.string.spell_cast, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        Runnable updateCastLabel = () -> {
            int count = 0;
            for (MaterialCheckBox b : boxes) if (b.isChecked()) count++;
            if (count > 0) targetsError.setVisibility(View.GONE);
            View positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive instanceof TextView) {
                ((TextView) positive).setText(count == 0 ? context.getString(R.string.spell_cast)
                        : context.getResources().getQuantityString(R.plurals.spell_cast_on, count, count));
            }
        };

        LayoutInflater inflater = LayoutInflater.from(context);
        for (CharacterModel c : combatants) {
            View row = inflater.inflate(R.layout.item_spell_target, targetsContainer, false);
            MaterialCheckBox box = row.findViewById(R.id.spell_target_checkBox);
            box.setText(name(context, c));
            box.setOnCheckedChangeListener((b, checked) -> updateCastLabel.run());
            TextView type = row.findViewById(R.id.spell_target_type);
            type.setText(c.isPC() ? R.string.char_card_pc : R.string.char_card_npc);
            type.setTextColor(ContextCompat.getColor(context, c.isPC() ? R.color.rk_pc_marker : R.color.rk_npc_marker));
            if (c == active) {
                row.findViewById(R.id.spell_target_active).setVisibility(View.VISIBLE);
                row.setBackgroundResource(R.drawable.bg_spell_target_active);
            }
            boxes.add(box);
            targetsContainer.addView(row);
        }

        // "3 rnds", "1 rnd": the suffix follows the number
        durationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                durationLayout.setError(null);
                durationLayout.setErrorEnabled(false);
                durationLayout.setSuffixText(suffix(context, Spells.parseDuration(s.toString())));
            }
        });
        durationLayout.setSuffixText(suffix(context, 0));
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                nameLayout.setError(null);
                nameLayout.setErrorEnabled(false);
            }
        });

        dialog.setOnShowListener(d -> {
            updateCastLabel.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String spellName = nameInput.getText().toString().trim();
                int rounds = Spells.parseDuration(durationInput.getText().toString());
                List<CharacterModel> targets = new ArrayList<>();
                for (int i = 0; i < boxes.size(); i++) {
                    if (boxes.get(i).isChecked()) targets.add(combatants.get(i));
                }
                boolean ok = true;
                if (spellName.isEmpty()) {
                    nameLayout.setError(context.getString(R.string.spell_error_name));
                    ok = false;
                }
                if (rounds < 0) {
                    durationLayout.setError(context.getString(R.string.spell_error_duration, DebuffModel.MAX_DURATION));
                    ok = false;
                }
                if (targets.isEmpty()) {
                    targetsError.setVisibility(View.VISIBLE);
                    ok = false;
                }
                if (!ok) return;
                Spells.cast(encounter, combatants.get(caster[0]), targets, spellName, rounds,
                        descriptionInput.getText().toString());
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    /**
     * A caster left the fight: tick the spells that keep going on their initiative as spell cards
     * (all ticked to start with, so nothing is lost by just closing it); the rest end.
     */
    public static void askCasterGone(Context context, Encounter encounter, Encounter.CasterGone gone) {
        String caster = gone.casterName.isEmpty() ? context.getString(R.string.empty_character_name) : gone.casterName;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_caster_gone, null);
        ((TextView) view.findViewById(R.id.caster_gone_message)).setText(
                context.getString(R.string.spell_caster_gone_message, caster));
        ViewGroup list = view.findViewById(R.id.caster_gone_list);
        List<MaterialCheckBox> boxes = new ArrayList<>();
        List<String> spellIds = new ArrayList<>();
        for (CharacterModel card : gone.spellCards) {
            String spellId = Spells.spellOfCard(encounter, card);
            MaterialCheckBox box = (MaterialCheckBox) inflater.inflate(R.layout.item_approval_change, list, false);
            box.setText(context.getString(R.string.spell_caster_gone_item, name(context, card),
                    targetNames(context, encounter, spellId)));
            box.setChecked(true);
            list.addView(box);
            boxes.add(box);
            spellIds.add(spellId);
        }
        Dialogs.builder(context)
                .setTitle(context.getString(R.string.spell_caster_gone_title, caster))
                .setView(view)
                .setPositiveButton(R.string.spell_caster_gone_done, (d, w) -> {
                    for (int i = 0; i < boxes.size(); i++) {
                        if (!boxes.get(i).isChecked()) Spells.end(encounter, spellIds.get(i));
                    }
                })
                .show();
    }

    /**
     * Ends a spell on every target, after asking ("Bless comes off Rekijan, Tor and Ash"). Used
     * from a spell condition on the character details, and for a spell card in the order.
     */
    public static void askEnd(Context context, Encounter encounter, String spellId, String spellName,
                              boolean fromSpellCard, @Nullable Runnable afterEnd) {
        String spell = spellName.isEmpty() ? context.getString(R.string.approval_condition_unnamed) : spellName;
        String targets = targetNames(context, encounter, spellId);
        Dialogs.builder(context)
                .setTitle(context.getString(R.string.spell_end_title, spell))
                .setMessage(context.getString(fromSpellCard ? R.string.spell_end_card_message : R.string.spell_end_message,
                        spell, targets))
                .setPositiveButton(R.string.spell_end, (d, w) -> {
                    Spells.end(encounter, spellId);
                    if (afterEnd != null) afterEnd.run();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /** @return "Rekijan, Tor, Ash": who has this spell. */
    public static String targetNames(Context context, Encounter encounter, String spellId) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : Spells.targetsOf(encounter, spellId)) names.add(name(context, c));
        return TextUtils.join(", ", names);
    }

    private static String suffix(Context context, int rounds) {
        return context.getResources().getQuantityString(R.plurals.spell_rounds_suffix, rounds == 1 ? 1 : 2);
    }

    private static String name(Context context, CharacterModel c) {
        String name = c.getCharacterName();
        return TextUtils.isEmpty(name) ? context.getString(R.string.empty_character_name) : name;
    }
}
