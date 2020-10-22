package com.rekijan.initiativetracker.character.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.AppExtension;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.model.CharacterModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class, but minimal to just edit the order
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015 Creation of this file
 */
public class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.CharacterViewHolder> {

    // Field for the list of CharacterModels
    private ArrayList<CharacterModel> characters = new ArrayList<>();
    // Passing along the activity is needed to build and populate dialogs
    private Context context;

    // C'tor
    public EditOrderAdapter(Context context) {
        this.context = context;
    }

    public void add(CharacterModel character) {
        characters.add(character);
    }

    public void addAll(List<CharacterModel> characters) {
        this.characters.addAll(characters);
    }

    /**
     * Remove a character based on position, flag another with isFirstRound if the one being removed is currently flagged thus
     * @param position
     */
    public void remove(int position) {
        if (characters.size() > position)
            if (characters.get(position).isFirstRound()) {
                {
                    if (position-1 < 0) {
                        characters.get(characters.size()-1).setIsFirstRound(true);
                    } else {
                        characters.get(position-1).setIsFirstRound(true);
                    }
                }
                characters.remove(position);
            } else {
                characters.remove(position);
            }
        this.notifyDataSetChanged();
    }

    /**
     * Remove based on the character given
     * @param character
     */
    public void remove(CharacterModel character) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i) == character) {
                remove(i);
            }
        }
    }

    public ArrayList<CharacterModel> getList() {
        return characters;
    }

    public void removeAll() {
        characters.clear();
    }


    /* ViewHolder region */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView editOrderCardView;
        TextView editOrderInitiativeTextView;
        TextView editOrderNameTextView;
        Button editOrderUpButton;
        Button editOrderDownButton;

        CharacterViewHolder(View itemView) {
            super(itemView);
            editOrderCardView = itemView.findViewById(R.id.edit_order_cardView);
            editOrderInitiativeTextView = itemView.findViewById(R.id.edit_order_initiative_textView);
            editOrderNameTextView = itemView.findViewById(R.id.edit_order_name_textView);
            editOrderUpButton = itemView.findViewById(R.id.up_edit_order_button);
            editOrderDownButton = itemView.findViewById(R.id.down_edit_order_button);
        }
    }
    /* End of Viewholder region */


    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_order_card, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CharacterViewHolder holder, final int position) {

        final AppExtension app = (AppExtension) context.getApplicationContext();
        // Get the corresponding CharacterModel
        final CharacterModel character = characters.get(position);

        // Set background color depending on whether the character is a PC or NPC
        int color = character.isPC() ? context.getResources().getColor(R.color.pcColor) : context.getResources().getColor(R.color.npcColor);
        ((CardView) holder.itemView).setCardBackgroundColor(color);

        // Set values TextViews and EditTexts
        holder.editOrderInitiativeTextView.setText(String.valueOf(character.getInitiative()));

        if (character.getCharacterName() != null) {
            holder.editOrderNameTextView.setText(character.getCharacterName());
        }

        holder.editOrderUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.rotate(app.getCharacterAdapter().getList(), -1);
                Collections.rotate(characters, -1);
                notifyDataSetChanged();
            }
        });

        holder.editOrderDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Collections.rotate(app.getCharacterAdapter().getList(), 1);
                Collections.rotate(characters, 1);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}