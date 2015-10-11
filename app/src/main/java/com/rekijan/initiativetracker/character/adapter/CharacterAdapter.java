package com.rekijan.initiativetracker.character.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;

import java.util.List;

/**
 * Custom ArrayAdapter for the Character class
 *
 * @author Erik-Jan Krielen ej.krielen@euphoria-it.nl
 * @since 17-9-2015 Creation of this file
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    public static class CharacterViewHolder extends RecyclerView.ViewHolder {

        CardView characterCardView;
        EditText initiativeEditText;


        CharacterViewHolder(View itemView) {
            super(itemView);
            characterCardView = (CardView)itemView.findViewById(R.id.character_cardView);
            initiativeEditText = (EditText)itemView.findViewById(R.id.initiative_editText);
        }
    }

    List<CharacterModel> characters;

    public CharacterAdapter(List<CharacterModel> characters){
        this.characters = characters;
    }

    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.character_card, parent, false);
        return new CharacterViewHolder (v);
    }

    @Override
    public void onBindViewHolder(CharacterViewHolder holder, int position) {
        holder.initiativeEditText.setText(String.valueOf(characters.get(position).getInitiative()));

    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}