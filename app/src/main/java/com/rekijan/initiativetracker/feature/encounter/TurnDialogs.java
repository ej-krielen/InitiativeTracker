package com.rekijan.initiativetracker.feature.encounter;

import android.content.Context;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.util.Dialogs;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

/**
 * The questions the app asks when the turn moves on (remove an expired (de)buff? add fast
 * healing or regeneration? cap hp at max?) and after sorting (reset the round counter?).
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
        for (DebuffModel debuff : turn.expiredDebuffs) {
            askRemoveExpiredDebuff(context, encounter, active, debuff);
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
