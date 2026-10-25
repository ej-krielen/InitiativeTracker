package com.rekijan.initiativetracker.app;

import android.app.Application;

import com.rekijan.initiativetracker.core.data.EncounterStore;
import com.rekijan.initiativetracker.feature.character.CharacterDefaults;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;

import java.util.List;

/**
 * Holds the current fight for the whole app, so it survives screen rotation. It is loaded from
 * {@link EncounterStore} on first use (a fresh install starts with five PCs) and saved when the
 * app goes to the background. It also remembers whether Remove Ads was bought.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 16-5-2019
 */
public class InitiativeTrackerApp extends Application {

    private EncounterStore store;
    private Encounter encounter;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new EncounterStore(this);
    }

    public Encounter getEncounter() {
        if (encounter == null) {
            List<CharacterModel> characters = store.loadCharacters();
            if (characters == null) characters = CharacterDefaults.startingParty(this);
            encounter = new Encounter(characters, store.loadRound());
        }
        return encounter;
    }

    public void saveEncounter() {
        if (encounter != null) store.save(encounter);
    }

    /** @return True once Remove Ads was bought (set by BillingManager, restored on every start). */
    public boolean isAdsRemoved() {
        return store.isAdsRemoved();
    }

    public void setAdsRemoved(boolean removed) {
        store.setAdsRemoved(removed);
    }
}
