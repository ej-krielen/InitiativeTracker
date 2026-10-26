package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the app shows when the turn moves on (which conditions counted down, as a snackbar;
 * remove a condition that ended? add fast healing or regeneration? cap hp at max?) and the
 * questions around sorting (sort at all? reset the round counter?).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class TurnDialogs {

    private TurnDialogs() {
    }

    /** Asks the questions that belong to the start of a character's turn. */
    public static void onTurnStarted(Context context, Encounter encounter, Encounter.Turn turn) {
        CharacterModel active = turn.active;
        // One question per spell, for all its targets at once; other conditions one by one
        Map<String, List<Encounter.Expired>> spells = new LinkedHashMap<>();
        for (Encounter.Expired e : turn.expired) {
            String spellId = e.condition.getSpellId();
            if (spellId == null) {
                askRemoveExpiredDebuff(context, encounter, e.character, e.condition);
            } else {
                List<Encounter.Expired> group = spells.get(spellId);
                if (group == null) spells.put(spellId, group = new ArrayList<>());
                group.add(e);
            }
        }
        for (List<Encounter.Expired> group : spells.values()) {
            askRemoveExpiredSpell(context, encounter, group);
        }
        if (active.getFastHealing() > 0) {
            askAutomaticHealing(context, encounter, active, active.getFastHealing(),
                    R.string.auto_healing_dialog_fast_healing_title, R.string.auto_healing_dialog_fast_healing_message);
        }
        if (active.getRegeneration() > 0) {
            askAutomaticHealing(context, encounter, active, active.getRegeneration(),
                    R.string.auto_healing_dialog_regeneration_title, R.string.auto_healing_dialog_regeneration_message);
        }
    }

    /**
     * Shows which conditions went down a round, as a snackbar at the bottom ("Mira's turn ·
     * Haste 3 → 2, Bless 2 → 1 (Rekijan, Tor, Ash)"), so it is seen without stopping play. A
     * spell the active character cast is shown once, with its targets. Nothing shows when no
     * condition has a duration. A condition that ended also gets a dialog from {@link #onTurnStarted}.
     *
     * @param root   A view in the screen, for the snackbar.
     * @param anchor The view to show it above (the ad bar), or null.
     */
    public static void showTicks(View root, @Nullable View anchor, Encounter.Turn turn) {
        if (turn.ticks.isEmpty()) return;
        Context context = root.getContext();
        String who = nameOf(context, turn.active);
        List<String> parts = new ArrayList<>();
        // Spell ticks with the same rounds are joined: spell id + rounds -> the names of its targets
        Map<String, List<String>> spellTargets = new LinkedHashMap<>();
        Map<String, Encounter.Tick> spellTicks = new LinkedHashMap<>();
        for (Encounter.Tick tick : turn.ticks) {
            if (tick.spellId == null) {
                parts.add(context.getString(R.string.turn_tick, conditionName(context, tick.name), tick.from, tick.to));
                continue;
            }
            String key = tick.spellId + ":" + tick.from + ":" + tick.to;
            List<String> targets = spellTargets.get(key);
            if (targets == null) {
                spellTargets.put(key, targets = new ArrayList<>());
                spellTicks.put(key, tick);
            }
            targets.add(tick.target == null ? who : nameOf(context, tick.target));
        }
        for (Map.Entry<String, Encounter.Tick> entry : spellTicks.entrySet()) {
            Encounter.Tick tick = entry.getValue();
            parts.add(context.getString(R.string.turn_tick_spell, conditionName(context, tick.name), tick.from, tick.to,
                    TextUtils.join(", ", spellTargets.get(entry.getKey()))));
        }
        Snackbar snackbar = Snackbar.make(root, context.getString(R.string.turn_ticks, who, TextUtils.join(", ", parts)),
                Snackbar.LENGTH_LONG);
        if (anchor != null && anchor.getVisibility() == View.VISIBLE) snackbar.setAnchorView(anchor);
        snackbar.show();
    }

    /**
     * Asks before sorting by initiative, since it undoes any order set by hand. After sorting it
     * asks about the round counter.
     */
    public static void askSort(Context context, Encounter encounter) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_sort_title)
                .setMessage(R.string.dialog_sort_message)
                .setPositiveButton(R.string.dialog_sort_ok, (d, w) ->
                        askRoundReset(context, encounter, encounter.sortByInitiative()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * Asks to reset the round counter to 1 after sorting, with an extra warning when two
     * characters have the same initiative.
     */
    public static void askRoundReset(Context context, Encounter encounter, boolean duplicateInitiative) {
        String message = context.getString(R.string.dialog_reset_round_counter);
        if (duplicateInitiative) message += context.getString(R.string.dialog_reset_round_double_init);
        Dialogs.builder(context)
                .setTitle(R.string.dialog_reset_round_counter_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> encounter.resetRound())
                .setNegativeButton(R.string.dialog_reset_round_counter_cancel, null)
                .show();
    }

    private static void askRemoveExpiredDebuff(Context context, Encounter encounter,
                                               CharacterModel character, DebuffModel debuff) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_debuff_expired_title)
                .setMessage(context.getString(R.string.debuff_expired, debuff.getName()))
                .setPositiveButton(R.string.dialog_debuff_remove, (d, w) -> {
                    character.getDebuffList().remove(debuff);
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_debuff_keep, null)
                .show();
    }

    /** "Bless ended on Rekijan, Tor and Ash": Remove takes it off all of them. */
    private static void askRemoveExpiredSpell(Context context, Encounter encounter, List<Encounter.Expired> group) {
        List<String> names = new ArrayList<>();
        for (Encounter.Expired e : group) names.add(nameOf(context, e.character));
        String spell = conditionName(context, group.get(0).condition.getName());
        Dialogs.builder(context)
                .setTitle(R.string.dialog_spell_expired_title)
                .setMessage(context.getString(R.string.spell_expired, spell, TextUtils.join(", ", names)))
                .setPositiveButton(R.string.dialog_debuff_remove, (d, w) -> {
                    for (Encounter.Expired e : group) e.character.getDebuffList().remove(e.condition);
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_debuff_keep, null)
                .show();
    }

    static String nameOf(Context context, CharacterModel character) {
        String name = character.getCharacterName();
        return TextUtils.isEmpty(name) ? context.getString(R.string.empty_character_name) : name;
    }

    private static String conditionName(Context context, String name) {
        return TextUtils.isEmpty(name) ? context.getString(R.string.approval_condition_unnamed) : name;
    }

    private static void askAutomaticHealing(Context context, Encounter encounter, CharacterModel character,
                                            int healing, int titleRes, int messageRes) {
        Dialogs.builder(context)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.heal(healing);
                    encounter.notifyChanged();
                    if (character.isAboveMaxHp()) askCapAtMaxHp(context, encounter, character);
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private static void askCapAtMaxHp(Context context, Encounter encounter, CharacterModel character) {
        Dialogs.builder(context)
                .setTitle(R.string.auto_healing_dialog_max_exceeded_title)
                .setMessage(R.string.auto_healing_dialog_max_exceeded_message)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.capHpAtMaxHp();
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }
}
