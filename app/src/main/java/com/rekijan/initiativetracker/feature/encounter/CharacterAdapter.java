package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.TextBinding;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Shows the initiative order: one card per character with initiative and name (editable in
 * place), hp (tap to change) and a summary of their (de)buffs.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private final Encounter encounter;
    private final IntConsumer onShowDetails;

    /**
     * @param onShowDetails Gets the position of the character whose details button was tapped.
     */
    public CharacterAdapter(Encounter encounter, IntConsumer onShowDetails) {
        this.encounter = encounter;
        this.onShowDetails = onShowDetails;
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder {
        final EditText initiativeEditText;
        final EditText nameEditText;
        final TextView hpTextView;
        final TextView debuffsTextView;
        final Button showDetailButton;

        CharacterViewHolder(View itemView) {
            super(itemView);
            initiativeEditText = itemView.findViewById(R.id.character_initiative_editText);
            nameEditText = itemView.findViewById(R.id.character_name_editText);
            hpTextView = itemView.findViewById(R.id.character_hp_textView);
            debuffsTextView = itemView.findViewById(R.id.character_debuffs_textView);
            showDetailButton = itemView.findViewById(R.id.show_character_detail_button);
        }
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_character, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        CharacterModel character = encounter.get(position);

        int color = ContextCompat.getColor(context, character.isPC() ? R.color.pcColor : R.color.npcColor);
        ((CardView) holder.itemView).setCardBackgroundColor(color);

        TextBinding.bindInt(holder.initiativeEditText, character.getInitiative(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiative);
        TextBinding.bindText(holder.nameEditText, character.getCharacterName(), character::setCharacterName);

        holder.hpTextView.setText(String.valueOf(character.getHp()));
        holder.hpTextView.setOnClickListener(v -> HpDialog.show(context, encounter, character));

        List<DebuffModel> debuffs = character.getDebuffList();
        if (debuffs.isEmpty()) {
            holder.debuffsTextView.setVisibility(View.GONE);
        } else {
            holder.debuffsTextView.setVisibility(View.VISIBLE);
            holder.debuffsTextView.setText(debuffSummary(context, debuffs));
        }

        holder.showDetailButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) onShowDetails.accept(pos);
        });
    }

    @Override
    public int getItemCount() {
        return encounter.size();
    }

    /** E.g. "(De)buffs: Haste (3), Shaken (1)". */
    private static String debuffSummary(Context context, List<DebuffModel> debuffs) {
        StringBuilder sb = new StringBuilder(context.getString(R.string.char_card_debuffs));
        for (int i = 0; i < debuffs.size(); i++) {
            DebuffModel d = debuffs.get(i);
            sb.append(i == 0 ? " " : ", ")
                    .append(d.getName())
                    .append(" (").append(d.getDuration()).append(")");
        }
        return sb.toString();
    }
}
