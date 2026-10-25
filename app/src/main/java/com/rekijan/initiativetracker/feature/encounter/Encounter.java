package com.rekijan.initiativetracker.feature.encounter;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The initiative order and round counter of the active party. The character at the top of the
 * list is the one whose turn it is. One character is marked "first in round"; when the turn comes
 * back to them a new round starts. Plain Java (no Android), so the turn rules are unit tested.
 * <p>
 * Changes to the order, the list or the round are passed to the {@link Listener}s, so open
 * screens can redraw.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class Encounter {

    /** Told after the order, the list or the round changed. */
    public interface Listener {
        void onEncounterChanged();
    }

    /** What happened when the turn moved on. */
    public static final class Turn {
        public final CharacterModel active;
        public final List<DebuffModel> expiredDebuffs;
        public final boolean newRound;

        Turn(CharacterModel active, List<DebuffModel> expiredDebuffs, boolean newRound) {
            this.active = active;
            this.expiredDebuffs = expiredDebuffs;
            this.newRound = newRound;
        }
    }

    private final List<CharacterModel> characters = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private int round;

    public Encounter(List<CharacterModel> characters, int round) {
        this.characters.addAll(characters);
        this.round = Math.max(0, round);
    }

    /**
     * Puts another party's characters and round counter in this fight, used when switching
     * parties. Open screens keep the same Encounter and are told to redraw.
     */
    public void replace(List<CharacterModel> newCharacters, int newRound) {
        characters.clear();
        characters.addAll(newCharacters);
        round = Math.max(0, newRound);
        notifyChanged();
    }

    public List<CharacterModel> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    public int size() {
        return characters.size();
    }

    public CharacterModel get(int position) {
        return characters.get(position);
    }

    public boolean isValidPosition(int position) {
        return position >= 0 && position < characters.size();
    }

    public void add(CharacterModel character) {
        characters.add(character);
        notifyChanged();
    }

    /** Adds several characters at the end, with one change notice (used by Table sync). */
    public void addAll(List<CharacterModel> newCharacters) {
        if (newCharacters.isEmpty()) return;
        characters.addAll(newCharacters);
        notifyChanged();
    }

    /**
     * Removes a character. When it was first in round, the next character in the order takes
     * that over, so the round counter keeps going up at the right moment.
     */
    public void remove(int position) {
        if (!isValidPosition(position)) return;
        CharacterModel removed = characters.remove(position);
        if (removed.isFirstInRound() && !characters.isEmpty()) {
            characters.get(position % characters.size()).setFirstInRound(true);
        }
        notifyChanged();
    }

    public void remove(CharacterModel character) {
        remove(characters.indexOf(character));
    }

    /**
     * Sorts high to low on initiative and makes the top character first in round.
     *
     * @return True when two or more characters share an initiative, so the user can check the order.
     */
    public boolean sortByInitiative() {
        characters.sort((a, b) -> Integer.compare(b.getInitiative(), a.getInitiative()));
        if (!characters.isEmpty()) setFirstInRoundSilently(0);
        notifyChanged();
        return hasDuplicateInitiative();
    }

    public boolean hasDuplicateInitiative() {
        Set<Integer> seen = new HashSet<>();
        for (CharacterModel c : characters) {
            if (!seen.add(c.getInitiative())) return true;
        }
        return false;
    }

    public void setFirstInRound(int position) {
        if (!isValidPosition(position)) return;
        setFirstInRoundSilently(position);
        notifyChanged();
    }

    /**
     * Moves the turn on: the top character goes to the bottom, the next one becomes active, their
     * (de)buffs tick down, and the round goes up when they are first in round.
     *
     * @return What happened, or null when there is nobody in the fight.
     */
    public Turn nextTurn() {
        if (characters.isEmpty()) return null;
        Collections.rotate(characters, -1);
        CharacterModel active = characters.get(0);
        List<DebuffModel> expired = active.tickDebuffs();
        boolean newRound = active.isFirstInRound();
        if (newRound) round++;
        notifyChanged();
        return new Turn(active, expired, newRound);
    }

    /** Swaps a character with the one above it; the top one swaps with the bottom one. */
    public void moveUp(int position) {
        if (!isValidPosition(position)) return;
        int other = position == 0 ? characters.size() - 1 : position - 1;
        Collections.swap(characters, position, other);
        notifyChanged();
    }

    /** Swaps a character with the one below it; the bottom one swaps with the top one. */
    public void moveDown(int position) {
        if (!isValidPosition(position)) return;
        int other = position == characters.size() - 1 ? 0 : position + 1;
        Collections.swap(characters, position, other);
        notifyChanged();
    }

    /**
     * Heals every PC to max hp. A PC with more hp than max (temporary hp) is left alone.
     *
     * @return The names of the PCs that were left alone, to tell the user.
     */
    public List<String> setPcsToMaxHp() {
        List<String> skipped = new ArrayList<>();
        for (CharacterModel c : characters) {
            if (!c.isPC()) continue;
            if (c.getMaxHp() >= c.getHp()) {
                c.setHp(c.getMaxHp());
            } else {
                skipped.add(c.getCharacterName());
            }
        }
        notifyChanged();
        return skipped;
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        round++;
        notifyChanged();
    }

    public void previousRound() {
        round = Math.max(0, round - 1);
        notifyChanged();
    }

    public void resetRound() {
        round = 1;
        notifyChanged();
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** Tells the listeners; call this after changing a character's hp or (de)buffs from outside. */
    public void notifyChanged() {
        for (Listener l : new ArrayList<>(listeners)) {
            l.onEncounterChanged();
        }
    }

    private void setFirstInRoundSilently(int position) {
        for (CharacterModel c : characters) {
            c.setFirstInRound(false);
        }
        characters.get(position).setFirstInRound(true);
    }
}
