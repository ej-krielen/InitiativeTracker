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


        holder.characterInitiativeEditText.addTextChangedListener(new GenericTextWatcher(character, holder.characterInitiativeEditText));
        holder.characterNameEditText.addTextChangedListener(new GenericTextWatcher(character, holder.characterNameEditText));
        holder.characterNotesEditText.addTextChangedListener(new GenericTextWatcher(character, holder.characterNotesEditText));
        holder.characterHpEditText.addTextChangedListener(new GenericTextWatcher(character, holder.characterHpEditText));
        holder.characterDebuffTL.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffTL));
        holder.characterDebuffTC.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffTC));
        holder.characterDebuffTR.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffTR));
        holder.characterDebuffBL.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffBL));
        holder.characterDebuffBC.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffBC));
        holder.characterDebuffBR.addTextChangedListener(new GenericTextWatcher(character, holder.characterDebuffBR));

    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}