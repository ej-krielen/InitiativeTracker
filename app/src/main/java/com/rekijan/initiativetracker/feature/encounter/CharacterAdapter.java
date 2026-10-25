package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.TextBinding;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Shows the initiative order: one card per character with initiative and name (editable in
 * place), hp (tap to change) and a summary of their (de)buffs. PCs get gold cards and NPCs
 * bordeaux ones; the top card, whose turn it is, gets a gold outline.
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
        final MaterialCardView cardView;
        final View markerView;
        final EditText initiativeEditText;
        final EditText nameEditText;
        final TextView typeTextView;
        final View hpBox;
        final TextView hpTextView;
        final TextView debuffsTextView;
        final View showDetailButton;

        CharacterViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            markerView = itemView.findViewById(R.id.character_marker_view);
            initiativeEditText = itemView.findViewById(R.id.character_initiative_editText);
            nameEditText = itemView.findViewById(R.id.character_name_editText);
            typeTextView = itemView.findViewById(R.id.character_type_textView);
            hpBox = itemView.findViewById(R.id.character_hp_box);
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

        bindColors(holder, context, character.isPC(), position == 0);

        TextBinding.bindInt(holder.initiativeEditText, character.getInitiative(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiative);
        TextBinding.bindText(holder.nameEditText, character.getCharacterName(), character::setCharacterName);

        holder.hpTextView.setText(hpText(holder.hpTextView, character));
        holder.hpBox.setOnClickListener(v -> HpDialog.show(context, encounter, character));

        List<DebuffModel> debuffs = character.getDebuffList();
        if (debuffs.isEmpty()) {
            holder.debuffsTextView.setVisibility(View.GONE);
        } else {
            holder.debuffsTextView.setVisibility(View.VISIBLE);
            holder.debuffsTextView.setText(debuffSummary(debuffs));
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

    /** PC or NPC tint, marker stripe and label; a gold outline on the character whose turn it is. */
    private static void bindColors(CharacterViewHolder holder, Context context, boolean isPc, boolean isActive) {
        int marker = ContextCompat.getColor(context, isPc ? R.color.rk_pc_marker : R.color.rk_npc_marker);
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context,
                isPc ? R.color.rk_pc_container : R.color.rk_npc_container));
        holder.markerView.setBackgroundColor(marker);
        holder.typeTextView.setText(isPc ? R.string.char_card_pc : R.string.char_card_npc);
        holder.typeTextView.setTextColor(marker);
        holder.cardView.setStrokeColor(
                MaterialColors.getColor(holder.cardView, com.google.android.material.R.attr.colorSecondary));
        holder.cardView.setStrokeWidth(isActive
                ? context.getResources().getDimensionPixelSize(R.dimen.active_card_stroke) : 0);
    }

    /** E.g. "24 / 32", with the max smaller and muted. */
    private static CharSequence hpText(TextView view, CharacterModel character) {
        SpannableStringBuilder sb = new SpannableStringBuilder(String.valueOf(character.getHp()));
        int start = sb.length();
        sb.append(" / ").append(String.valueOf(character.getMaxHp()));
        int muted = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant);
        sb.setSpan(new RelativeSizeSpan(0.65f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(muted), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /** E.g. "Haste (3) · Shaken (1)". */
    private static String debuffSummary(List<DebuffModel> debuffs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < debuffs.size(); i++) {
            DebuffModel d = debuffs.get(i);
            if (i > 0) sb.append(" · ");
            sb.append(d.getName())
                    .append(" (").append(d.getDuration()).append(")");
        }
        return sb.toString();
    }
}
