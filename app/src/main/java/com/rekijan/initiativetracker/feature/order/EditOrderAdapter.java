package com.rekijan.initiativetracker.feature.order;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

/**
 * A compact version of the initiative order to fix it by hand: up and down buttons per
 * character, and a marker on the one who is first in round.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.OrderViewHolder> {

    private final Encounter encounter;

    public EditOrderAdapter(Encounter encounter) {
        this.encounter = encounter;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView initiativeTextView;
        final TextView nameTextView;
        final Button upButton;
        final Button downButton;
        final ImageView firstInRoundImage;

        OrderViewHolder(View itemView) {
            super(itemView);
            initiativeTextView = itemView.findViewById(R.id.edit_order_initiative_textView);
            nameTextView = itemView.findViewById(R.id.edit_order_name_textView);
            upButton = itemView.findViewById(R.id.up_edit_order_button);
            downButton = itemView.findViewById(R.id.down_edit_order_button);
            firstInRoundImage = itemView.findViewById(R.id.first_in_round_imageView);
        }
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_order, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        CharacterModel character = encounter.get(position);

        int color = ContextCompat.getColor(context, character.isPC() ? R.color.pcColor : R.color.npcColor);
        ((CardView) holder.itemView).setCardBackgroundColor(color);

        holder.initiativeTextView.setText(String.valueOf(character.getInitiative()));
        holder.nameTextView.setText(character.getCharacterName());
        holder.firstInRoundImage.setVisibility(character.isFirstInRound() ? View.VISIBLE : View.INVISIBLE);

        holder.upButton.setOnClickListener(v -> encounter.moveUp(holder.getBindingAdapterPosition()));
        holder.downButton.setOnClickListener(v -> encounter.moveDown(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return encounter.size();
    }
}
