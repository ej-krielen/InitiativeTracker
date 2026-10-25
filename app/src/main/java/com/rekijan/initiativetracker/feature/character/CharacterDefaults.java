package com.rekijan.initiativetracker.feature.character;

import android.content.Context;

import com.rekijan.initiativetracker.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates new characters with the default stat lines (AC 10, saves 10, CMB +0 CMD 10) in the
 * user's language, and the starting party of four PCs for a fresh install and a new party
 * (most groups have four or five players; adding one is easier than deleting one).
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public final class CharacterDefaults {

    private static final int DEFAULT_HP = 6;
    private static final int DEFAULT_AC_AND_SAVES = 10;
    private static final int STARTING_PARTY_SIZE = 4;

    private CharacterDefaults() {
    }

    public static CharacterModel newCharacter(Context context) {
        CharacterModel c = new CharacterModel();
        c.setAc(context.getString(R.string.standard_ac, DEFAULT_AC_AND_SAVES));
        c.setSaves(context.getString(R.string.standard_saves, DEFAULT_AC_AND_SAVES));
        c.setManeuvers(context.getString(R.string.maneuvers, 0, DEFAULT_AC_AND_SAVES));
        c.setHp(DEFAULT_HP);
        c.setMaxHp(DEFAULT_HP);
        return c;
    }

    public static List<CharacterModel> startingParty(Context context) {
        List<CharacterModel> party = new ArrayList<>();
        for (int i = 0; i < STARTING_PARTY_SIZE; i++) {
            CharacterModel c = newCharacter(context);
            c.setPC(true);
            party.add(c);
        }
        return party;
    }
}
