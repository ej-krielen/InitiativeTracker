package com.rekijan.initiativetracker.feature.character;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.TextBinding;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Binds a character to the sheet layout (view_character_sheet): the Stats tab with the numbers,
 * the free-text stat lines and notes, and the Conditions tab with the (de)buffs. Every edit goes
 * straight into the character and is reported to the change callback.
 * <p>
 * Used by the GM's character details and by the player screen. The player sheet also shows name,
 * initiative and hp (the GM edits those on the card) and hides the PC switch, since a player's
 * own character is always a PC.
 * <p>
 * Add puts a new condition at the end and scrolls it into view, with its name field focused.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class CharacterSheet {

    public static final int TAB_STATS = 0;
    public static final int TAB_CONDITIONS = 1;

    private final View root;
    private final boolean playerMode;
    private final Runnable onChanged;
    private final TabLayout tabs;
    private final View statsPane;
    private final View conditionsPane;
    private final RecyclerView conditionList;
    private final TextView conditionsEmpty;
    private CharacterModel character;
    private DebuffAdapter adapter;

    /**
     * @param root       The inflated view_character_sheet.
     * @param playerMode True for the player's own character, false for the GM's details.
     * @param onChanged  Called after every edit, e.g. to redraw the encounter or save.
     */
    public CharacterSheet(View root, boolean playerMode, Runnable onChanged) {
        this.root = root;
        this.playerMode = playerMode;
        this.onChanged = onChanged;
        tabs = root.findViewById(R.id.sheet_tabs);
        statsPane = root.findViewById(R.id.sheet_stats_pane);
        conditionsPane = root.findViewById(R.id.sheet_conditions_pane);
        conditionList = root.findViewById(R.id.debuff_recyclerView);
        conditionsEmpty = root.findViewById(R.id.sheet_conditions_empty_textView);
        conditionList.setLayoutManager(new LinearLayoutManager(root.getContext()));

        root.findViewById(R.id.is_pc_switch).setVisibility(playerMode ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.sheet_player_rows).setVisibility(playerMode ? View.VISIBLE : View.GONE);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPane(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Nothing to do
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Nothing to do
            }
        });
        root.findViewById(R.id.clear_all_debuffs_button).setOnClickListener(v -> clearConditions());
        root.findViewById(R.id.add_debuff_button).setOnClickListener(v -> addCondition());
    }

    /** Shows this character on the sheet; call again to show it fresh (e.g. after a Sync). */
    public void bind(CharacterModel character) {
        this.character = character;

        CompoundButton pcSwitch = root.findViewById(R.id.is_pc_switch);
        pcSwitch.setOnCheckedChangeListener(null);
        pcSwitch.setChecked(character.isPC());
        pcSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            character.setPC(isChecked);
            onChanged.run();
        });

        if (playerMode) {
            bindText(R.id.sheet_name_editText, character.getCharacterName(), character::setCharacterName);
            TextBinding.bindInt(root.findViewById(R.id.sheet_initiative_editText), character.getInitiative(),
                    CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, value -> {
                        character.setInitiative(value);
                        onChanged.run();
                    });
            TextBinding.bindInt(root.findViewById(R.id.sheet_hp_editText), character.getHp(),
                    CharacterModel.MIN_HP, CharacterModel.MAX_HP, value -> {
                        character.setHp(value);
                        onChanged.run();
                    });
        }
        bindInt(R.id.initiative_bonus_editText, character.getInitiativeBonus(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiativeBonus);
        bindInt(R.id.max_hp_editText, character.getMaxHp(), 0, CharacterModel.MAX_HP, character::setMaxHp);
        bindInt(R.id.fast_healing_editText, character.getFastHealing(),
                0, CharacterModel.MAX_HEALING, character::setFastHealing);
        bindInt(R.id.regeneration_editText, character.getRegeneration(),
                0, CharacterModel.MAX_HEALING, character::setRegeneration);
        bindText(R.id.skills_editText, character.getSkills(), character::setSkills);
        bindText(R.id.attack_routine_editText, character.getAttackRoutine(), character::setAttackRoutine);
        bindText(R.id.ac_editText, character.getAc(), character::setAc);
        bindText(R.id.maneuvers_editText, character.getManeuvers(), character::setManeuvers);
        bindText(R.id.saves_editText, character.getSaves(), character::setSaves);
        bindText(R.id.notes_editText, character.getCharacterNotes(), character::setCharacterNotes);

        adapter = new DebuffAdapter(character, this::onConditionsChanged);
        conditionList.setAdapter(adapter);
        updateConditionCount();
    }

    /** @return The open tab, {@link #TAB_STATS} or {@link #TAB_CONDITIONS}, to keep it on rotation. */
    public int getSelectedTab() {
        return tabs.getSelectedTabPosition();
    }

    public void selectTab(int tab) {
        TabLayout.Tab t = tabs.getTabAt(tab);
        if (t != null) t.select();
        showPane(tab);
    }

    private void showPane(int tab) {
        statsPane.setVisibility(tab == TAB_STATS ? View.VISIBLE : View.GONE);
        conditionsPane.setVisibility(tab == TAB_CONDITIONS ? View.VISIBLE : View.GONE);
    }

    private void addCondition() {
        character.getDebuffList().add(new DebuffModel());
        int last = character.getDebuffList().size() - 1;
        adapter.notifyItemInserted(last);
        onConditionsChanged();
        // Scroll the new condition into view, then put the cursor in its name
        conditionList.post(() -> {
            conditionList.smoothScrollToPosition(last);
            conditionList.postDelayed(() -> {
                RecyclerView.ViewHolder holder = conditionList.findViewHolderForAdapterPosition(last);
                if (holder != null) holder.itemView.findViewById(R.id.debuff_name_editText).requestFocus();
            }, 300);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clearConditions() {
        character.getDebuffList().clear();
        adapter.notifyDataSetChanged();
        onConditionsChanged();
    }

    private void onConditionsChanged() {
        updateConditionCount();
        onChanged.run();
    }

    private void updateConditionCount() {
        int count = character.getDebuffList().size();
        TabLayout.Tab tab = tabs.getTabAt(TAB_CONDITIONS);
        if (tab != null) {
            tab.setText(count == 0 ? root.getContext().getString(R.string.sheet_tab_conditions)
                    : root.getContext().getString(R.string.sheet_tab_conditions_count, count));
        }
        conditionsEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void bindText(int id, String value, Consumer<String> setter) {
        EditText editText = root.findViewById(id);
        TextBinding.bindText(editText, value, text -> {
            setter.accept(text);
            onChanged.run();
        });
    }

    private void bindInt(int id, int value, int min, int max, IntConsumer setter) {
        TextBinding.bindInt(root.findViewById(id), value, min, max, number -> {
            setter.accept(number);
            onChanged.run();
        });
    }
}
