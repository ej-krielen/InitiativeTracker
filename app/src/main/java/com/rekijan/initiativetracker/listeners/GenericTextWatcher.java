package com.rekijan.initiativetracker.listeners;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.character.model.DebuffModel;

/**
 * <p>Listener for EditText</p>
 * See {@link CharacterModel} and {@link com.rekijan.initiativetracker.character.adapter.CharacterAdapter} as well as {@link DebuffModel} and {@link com.rekijan.initiativetracker.character.adapter.DebuffAdapter}
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class GenericTextWatcher implements TextWatcher {
    private View view;
    private CharacterModel character;
    private DebuffModel debuff;

    public GenericTextWatcher(CharacterModel character, View view) {
        this.character = character;
        this.view = view;
    }

    public GenericTextWatcher(DebuffModel debuff, View view) {
        this.debuff = debuff;
        this.view = view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString();
        switch (view.getId()) {
            case R.id.character_initiative_editText:
                character.setInitiative(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.initiative_bonus_editText:
                character.setInitiativeBonus(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.fast_healing_editText:
                character.setFastHealing(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.regeneration_editText:
                character.setRegeneration(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.skills_editText:
                character.setSkills(text);
                break;
            case R.id.attack_routine_editText:
                character.setAttackRoutine(text);
                break;
            case R.id.ac_editText:
                character.setAc(text);
                break;
            case R.id.saves_editText:
                character.setSaves(text);
                break;
            case R.id.maneuvers_editText:
                character.setManeuvers(text);
                break;
            case R.id.max_hp_editText:
                character.setMaxHp(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_name_editText:
                character.setCharacterName(text);
                break;
            case R.id.notes_editText:
                character.setCharacterNotes(text);
                break;
            case R.id.debuff_name_editText:
                debuff.setName(text);
                break;
            case R.id.debuff_duration_editText:
                debuff.setDuration(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.debuff_description_editText:
                debuff.setDescription(text);
                break;
        }
    }
}