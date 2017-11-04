package com.rekijan.initiativetracker.listeners;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;

/**
 * <p>Listener for EditText</p>
 * See {@link CharacterModel} and {@link com.rekijan.initiativetracker.character.adapter.CharacterAdapter}
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class GenericTextWatcher implements TextWatcher {
    private View view;
    private CharacterModel character;

    public GenericTextWatcher(CharacterModel character, View view) {
        this.character = character;
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
            case R.id.character_name_editText:
                character.setCharacterName(text);
                break;
            case R.id.character_notes_editText:
                character.setCharacterNotes(text);
                break;
            case R.id.character_hp_editText:
                character.setHp(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_TL_editText:
                character.setDebuffTL(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_TC_editText:
                character.setDebuffTC(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_TR_editText:
                character.setDebuffTR(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_BL_editText:
                character.setDebuffBL(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_BC_editText:
                character.setDebuffBC(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
            case R.id.character_debuff_BR_editText:
                character.setDebuffBR(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text));
                break;
        }
    }
}