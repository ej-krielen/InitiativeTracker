package com.rekijan.initiativetracker.character.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;
import com.rekijan.initiativetracker.listeners.GenericTextWatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class
 *
 * @author Erik-Jan Krielen ej.krielen@euphoria-it.nl
 * @since 17-9-2015 Creation of this file
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    // Field for the list of CharacterModels
    ArrayList<CharacterModel> characters = new ArrayList<>();

    public void add(CharacterModel character) {
        characters.add(character);
    }

    public void addAll(List<CharacterModel> characters) {
        this.characters.addAll(characters);
    }

    public void remove(int position) {
        if (characters.size() > position) {
            characters.remove(position);
        }
    }

    public ArrayList<CharacterModel> getList() {
        return characters;
    }

    public void sortInitiative() {
        Collections.sort(characters, new Comparator<CharacterModel>() {
            public int compare(CharacterModel o1, CharacterModel o2) {
                return o2.getInitiative() - o1.getInitiative();
            }
        });
        notifyDataSetChanged();
    }


    /* ViewHolder region */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView characterCardView;
        EditText characterInitiativeEditText;
        EditText characterNameEditText;
        EditText characterNotesEditText;
        EditText characterHpEditText;
        EditText characterDebuffTL;
        EditText characterDebuffTC;
        EditText characterDebuffTR;
        EditText characterDebuffBL;
        EditText characterDebuffBC;
        EditText characterDebuffBR;

        CharacterViewHolder(View itemView) {
            super(itemView);
            characterCardView = (CardView) itemView.findViewById(R.id.character_cardView);
            characterInitiativeEditText = (EditText) itemView.findViewById(R.id.character_initiative_editText);
            characterNameEditText = (EditText) itemView.findViewById(R.id.character_name_editText);
            characterNotesEditText = (EditText) itemView.findViewById(R.id.character_notes_editText);
            characterHpEditText = (EditText) itemView.findViewById(R.id.character_hp_editText);
            characterDebuffTL = (EditText) itemView.findViewById(R.id.character_debuff_TL_editText);
            characterDebuffTC = (EditText) itemView.findViewById(R.id.character_debuff_TC_editText);
            characterDebuffTR = (EditText) itemView.findViewById(R.id.character_debuff_TR_editText);
            characterDebuffBL = (EditText) itemView.findViewById(R.id.character_debuff_BL_editText);
            characterDebuffBC = (EditText) itemView.findViewById(R.id.character_debuff_BC_editText);
            characterDebuffBR = (EditText) itemView.findViewById(R.id.character_debuff_BR_editText);
        }
    }
    /* End of Viewholder region */


    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.character_card, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CharacterViewHolder holder, int position) {
                //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldInitiativeWatcher = (GenericTextWatcher)holder.characterInitiativeEditText.getTag();
        if (oldInitiativeWatcher != null) {
            holder.characterInitiativeEditText.removeTextChangedListener(oldInitiativeWatcher);
        }
        GenericTextWatcher oldNameWatcher = (GenericTextWatcher)holder.characterNameEditText.getTag();
        if (oldNameWatcher != null) {
            holder.characterNameEditText.removeTextChangedListener(oldNameWatcher);
        }
        GenericTextWatcher oldNoteWatcher = (GenericTextWatcher)holder.characterNotesEditText.getTag();
        if (oldNoteWatcher != null) {
            holder.characterNotesEditText.removeTextChangedListener(oldNoteWatcher);
        }
        GenericTextWatcher oldHPWatcher = (GenericTextWatcher)holder.characterHpEditText.getTag();
        if (oldHPWatcher != null) {
            holder.characterHpEditText.removeTextChangedListener(oldHPWatcher);
        }
        GenericTextWatcher oldDebuffTLWatcher = (GenericTextWatcher)holder.characterDebuffTL.getTag();
        if (oldDebuffTLWatcher != null) {
            holder.characterDebuffTL.removeTextChangedListener(oldDebuffTLWatcher);
        }
        GenericTextWatcher oldDebuffTCWatcher = (GenericTextWatcher)holder.characterDebuffTC.getTag();
        if (oldDebuffTCWatcher != null) {
            holder.characterDebuffTC.removeTextChangedListener(oldDebuffTCWatcher);
        }
        GenericTextWatcher oldDebuffTRWatcher = (GenericTextWatcher)holder.characterDebuffTR.getTag();
        if (oldDebuffTRWatcher != null) {
            holder.characterDebuffTR.removeTextChangedListener(oldDebuffTRWatcher);
        }
        GenericTextWatcher oldDebuffBLWatcher = (GenericTextWatcher)holder.characterDebuffBL.getTag();
        if (oldDebuffBLWatcher != null) {
            holder.characterDebuffBL.removeTextChangedListener(oldDebuffBLWatcher);
        }
        GenericTextWatcher oldDebuffBCWatcher = (GenericTextWatcher)holder.characterDebuffBC.getTag();
        if (oldDebuffBCWatcher != null) {
            holder.characterDebuffBC.removeTextChangedListener(oldDebuffBCWatcher);
        }
        GenericTextWatcher oldDebuffBRWatcher = (GenericTextWatcher)holder.characterDebuffBR.getTag();
        if (oldDebuffBRWatcher != null) {
            holder.characterDebuffBR.removeTextChangedListener(oldDebuffBRWatcher);
        }

        final CharacterModel character = characters.get(position);

        holder.characterInitiativeEditText.setText(String.valueOf(character.getInitiative()));

        if (character.getCharacterName() != null) {
            holder.characterNameEditText.setText(character.getCharacterName());
        }

        if (character.getCharacterNotes() != null) {
            holder.characterNotesEditText.setText(character.getCharacterNotes());
        }

        holder.characterHpEditText.setText(String.valueOf(character.getHp()));

        holder.characterDebuffTL.setText(String.valueOf(character.getDebuffTL()));
        holder.characterDebuffTC.setText(String.valueOf(character.getDebuffTC()));
        holder.characterDebuffTR.setText(String.valueOf(character.getDebuffTR()));
        holder.characterDebuffBL.setText(String.valueOf(character.getDebuffBL()));
        holder.characterDebuffBC.setText(String.valueOf(character.getDebuffBC()));
        holder.characterDebuffBR.setText(String.valueOf(character.getDebuffBR()));

        //Add new text watchers
        GenericTextWatcher newInitiativeWatcher = new GenericTextWatcher(character, holder.characterInitiativeEditText);
        holder.characterInitiativeEditText.setTag(newInitiativeWatcher);
        holder.characterInitiativeEditText.addTextChangedListener(newInitiativeWatcher);

        GenericTextWatcher newNameWatcher = new GenericTextWatcher(character, holder.characterNameEditText);
        holder.characterNameEditText.setTag(newNameWatcher);
        holder.characterNameEditText.addTextChangedListener(newNameWatcher);

        GenericTextWatcher newNotesWatcher = new GenericTextWatcher(character, holder.characterNotesEditText);
        holder.characterNotesEditText.setTag(newNotesWatcher);
        holder.characterNotesEditText.addTextChangedListener(newNotesWatcher);

        GenericTextWatcher newHPWatcher = new GenericTextWatcher(character, holder.characterHpEditText);
        holder.characterHpEditText.setTag(newHPWatcher);
        holder.characterHpEditText.addTextChangedListener(newHPWatcher);

        GenericTextWatcher newDebuffTLWatcher = new GenericTextWatcher(character, holder.characterDebuffTL);
        holder.characterDebuffTL.setTag(newDebuffTLWatcher);
        holder.characterDebuffTL.addTextChangedListener(newDebuffTLWatcher);

        GenericTextWatcher newDebuffTCWatcher = new GenericTextWatcher(character, holder.characterDebuffTC);
        holder.characterDebuffTC.setTag(newDebuffTCWatcher);
        holder.characterDebuffTC.addTextChangedListener(newDebuffTCWatcher);

        GenericTextWatcher newDebuffTRWatcher = new GenericTextWatcher(character, holder.characterDebuffTR);
        holder.characterDebuffTR.setTag(newDebuffTRWatcher);
        holder.characterDebuffTR.addTextChangedListener(newDebuffTRWatcher);

        GenericTextWatcher newDebuffBLWatcher = new GenericTextWatcher(character, holder.characterDebuffBL);
        holder.characterDebuffBL.setTag(newDebuffBLWatcher);
        holder.characterDebuffBL.addTextChangedListener(newDebuffBLWatcher);

        GenericTextWatcher newDebuffBCWatcher = new GenericTextWatcher(character, holder.characterDebuffBC);
        holder.characterDebuffBC.setTag(newDebuffBCWatcher);
        holder.characterDebuffBC.addTextChangedListener(newDebuffBCWatcher);

        GenericTextWatcher newDebuffBRWatcher = new GenericTextWatcher(character, holder.characterDebuffBR);
        holder.characterDebuffBR.setTag(newDebuffBRWatcher);
        holder.characterDebuffBR.addTextChangedListener(newDebuffBRWatcher);
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}