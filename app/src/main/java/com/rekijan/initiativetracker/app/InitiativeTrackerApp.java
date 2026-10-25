package com.rekijan.initiativetracker.app;

import android.app.Application;
import android.content.Context;

import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.core.data.EncounterStore;
import com.rekijan.initiativetracker.core.data.KeyValueStore;
import com.rekijan.initiativetracker.core.data.PrefsKeyValueStore;
import com.rekijan.initiativetracker.feature.character.CharacterDefaults;
import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.encounter.Encounter;
import com.rekijan.initiativetracker.feature.party.Party;
import com.rekijan.initiativetracker.feature.party.PartyBook;
import com.rekijan.initiativetracker.feature.party.PartyStore;

import java.util.List;

/**
 * Holds the saved parties ({@link PartyBook}) and the fight of the active party for the whole
 * app, so it survives screen rotation. The active party is the {@link Encounter}: it is loaded on
 * first use (a party without saved characters starts with four PCs), saved when the app goes to
 * the background, and swapped in place when the user switches parties. On the first start of 2.0
 * the 1.x save becomes the first party (see PartyStore). It also remembers whether Remove Ads was
 * bought.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 16-5-2019
 */
public class InitiativeTrackerApp extends Application {

    private EncounterStore store;
    private PartyBook parties;
    private Encounter encounter;

    @Override
    public void onCreate() {
        super.onCreate();
        KeyValueStore values = new PrefsKeyValueStore(
                getSharedPreferences(EncounterStore.PREFS_NAME, Context.MODE_PRIVATE));
        store = new EncounterStore(values);
        String firstName = getString(R.string.party_first_name);
        parties = new PartyBook(new PartyStore(values, firstName), firstName);
    }

    public PartyBook getParties() {
        return parties;
    }

    public Encounter getEncounter() {
        if (encounter == null) {
            String id = parties.getActiveId();
            encounter = new Encounter(charactersOf(id), parties.loadRound(id));
        }
        return encounter;
    }

    public void saveEncounter() {
        if (encounter != null) {
            parties.save(parties.getActiveId(), encounter.getCharacters(), encounter.getRound());
        }
    }

    /** Saves the active party, then puts the other party's characters and round in the fight. */
    public void switchParty(String id) {
        if (parties.get(id) == null || id.equals(parties.getActiveId())) return;
        saveEncounter();
        parties.setActive(id);
        getEncounter().replace(charactersOf(id), parties.loadRound(id));
    }

    /** @return The new party (four PCs, round 0), or null when the limit or the name says no. */
    public Party createParty(String name) {
        return parties.create(name, CharacterDefaults.startingParty(this));
    }

    /** @return The copy, or null when the limit is reached. */
    public Party duplicateParty(String id) {
        if (id.equals(parties.getActiveId())) saveEncounter();
        return parties.duplicate(id);
    }

    /**
     * Deletes a party. When it was the active one, the fight switches to the party that became
     * active, without saving the deleted one first.
     *
     * @return True when deleted (the last party can't be).
     */
    public boolean deleteParty(String id) {
        boolean wasActive = id.equals(parties.getActiveId());
        String newActive = parties.delete(id);
        if (newActive == null) return false;
        if (wasActive) getEncounter().replace(charactersOf(newActive), parties.loadRound(newActive));
        return true;
    }

    /** @return How many characters the party has; the active one is counted live. */
    public int characterCount(String id) {
        if (id.equals(parties.getActiveId()) && encounter != null) return encounter.size();
        return parties.characterCount(id);
    }

    /** @return True once Remove Ads was bought (set by BillingManager, restored on every start). */
    public boolean isAdsRemoved() {
        return store.isAdsRemoved();
    }

    public void setAdsRemoved(boolean removed) {
        store.setAdsRemoved(removed);
    }

    private List<CharacterModel> charactersOf(String id) {
        List<CharacterModel> characters = parties.loadCharacters(id);
        return characters != null ? characters : CharacterDefaults.startingParty(this);
    }
}
