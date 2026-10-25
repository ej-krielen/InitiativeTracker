package com.rekijan.initiativetracker.feature.party;

import com.rekijan.initiativetracker.core.data.EncounterJson;
import com.rekijan.initiativetracker.feature.character.CharacterModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All the user's parties and which one is active. Creates, renames, duplicates, deletes and
 * switches parties, checks the names ({@link PartyNames}) and the party limit
 * ({@link PartySlots}), and loads and saves each party's characters and round counter through
 * {@link PartyStore}. There is always at least one party, and always one active.
 * <p>
 * Plain Java, so all party rules are unit tested. InitiativeTrackerApp puts the active party's
 * characters in the Encounter and saves them back here.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class PartyBook {

    private final PartyStore store;
    private final List<Party> parties = new ArrayList<>();
    private String activeId;
    private int packLevel;

    /**
     * @param fallbackName Name for a new first party, only used when the saved list is gone.
     */
    public PartyBook(PartyStore store, String fallbackName) {
        this.store = store;
        parties.addAll(store.loadParties());
        if (parties.isEmpty()) {
            parties.add(new Party(fallbackName));
            store.saveParties(parties);
        }
        activeId = store.loadActiveId();
        if (get(activeId) == null) setActive(parties.get(0).getId());
        packLevel = store.loadPackLevel();
    }

    /** @return The parties in the order they were made. Read-only. */
    public List<Party> getParties() {
        return Collections.unmodifiableList(parties);
    }

    public Party get(String id) {
        if (id == null) return null;
        for (Party p : parties) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    public Party getActive() {
        return get(activeId);
    }

    public String getActiveId() {
        return activeId;
    }

    /** Only stores which party is active; the app moves the characters over. */
    public void setActive(String id) {
        if (get(id) == null) return;
        activeId = id;
        store.saveActiveId(id);
    }

    // ---------------------------------------------------------------- Limit

    public int getPackLevel() {
        return packLevel;
    }

    /** Stores the highest pack Google Play reports as owned, so a refund lowers it again. */
    public void setPackLevel(int level) {
        packLevel = PartySlots.clampLevel(level);
        store.savePackLevel(packLevel);
    }

    public int getLimit() {
        return PartySlots.limit(packLevel);
    }

    public int getUsed() {
        return parties.size();
    }

    public boolean canCreate() {
        return PartySlots.canCreate(parties.size(), packLevel);
    }

    // ---------------------------------------------------------------- Changes

    /** @param excludeId The party being renamed, or null for a new one. */
    public PartyNames.Check checkName(String name, String excludeId) {
        return PartyNames.check(name, parties, excludeId);
    }

    /**
     * Makes a new party with the given characters and round 0. Does not make it active.
     *
     * @return The new party, or null when the limit is reached or the name is not allowed.
     */
    public Party create(String name, List<CharacterModel> characters) {
        if (!canCreate() || checkName(name, null) != PartyNames.Check.OK) return null;
        Party party = new Party(PartyNames.clean(name));
        parties.add(party);
        store.saveParties(parties);
        save(party.getId(), characters, 0);
        return party;
    }

    /** @return True when renamed; false when the party is unknown or the name is not allowed. */
    public boolean rename(String id, String name) {
        Party party = get(id);
        if (party == null || checkName(name, id) != PartyNames.Check.OK) return false;
        party.setName(PartyNames.clean(name));
        store.saveParties(parties);
        return true;
    }

    /**
     * Copies a party as saved: its characters and round counter, under the name "Name 2". Save
     * the active party first when copying that one. Does not make the copy active.
     *
     * @return The copy, or null when the limit is reached or the party is unknown.
     */
    public Party duplicate(String id) {
        Party original = get(id);
        if (original == null || !canCreate()) return null;
        Party copy = new Party(PartyNames.copyName(original.getName(), parties));
        parties.add(parties.indexOf(original) + 1, copy);
        store.saveParties(parties);
        String json = store.loadCharactersJson(id);
        if (json != null) store.saveFight(copy.getId(), json, store.loadRound(id));
        return copy;
    }

    /**
     * Deletes a party and its characters. The last party can't be deleted. When the active party
     * is deleted, the one above it (or else the new first one) becomes active.
     *
     * @return The id of the active party afterwards, or null when nothing was deleted.
     */
    public String delete(String id) {
        Party party = get(id);
        if (party == null || parties.size() <= 1) return null;
        int index = parties.indexOf(party);
        parties.remove(index);
        store.saveParties(parties);
        store.deleteFight(id);
        if (id.equals(activeId)) setActive(parties.get(Math.max(0, index - 1)).getId());
        return activeId;
    }

    // ---------------------------------------------------------------- Characters

    /** @return The party's saved characters, or null when none were saved (yet) or unreadable. */
    public List<CharacterModel> loadCharacters(String id) {
        return EncounterJson.fromJson(store.loadCharactersJson(id));
    }

    public int loadRound(String id) {
        return store.loadRound(id);
    }

    public void save(String id, List<CharacterModel> characters, int round) {
        if (get(id) == null) return;
        store.saveFight(id, EncounterJson.toJson(characters), Math.max(0, round));
    }

    /** @return How many characters are saved in the party, 0 when none. */
    public int characterCount(String id) {
        List<CharacterModel> characters = loadCharacters(id);
        return characters == null ? 0 : characters.size();
    }
}
