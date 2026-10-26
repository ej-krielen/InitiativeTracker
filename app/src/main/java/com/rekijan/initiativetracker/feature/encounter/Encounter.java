package com.rekijan.initiativetracker.feature.encounter;

import com.rekijan.initiativetracker.feature.character.CharacterModel;
import com.rekijan.initiativetracker.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The initiative order and round counter of the active party. The character at the top of the
 * list is the one whose turn it is. One character is marked "first in round"; when the turn comes
 * back to them a new round starts. Plain Java (no Android), so the turn rules are unit tested.
 * <p>
 * Changes to the order, the list or the round are passed to the {@link Listener}s, so open
 * screens can redraw.
 * <p>
 * Spells: a condition with a caster counts down when its caster's turn starts, on every target.
 * When a caster leaves the fight while their spells still hold targets, each spell gets a spell
 * card in the caster's place, so it keeps counting on the same initiative; the screen then asks
 * which of them go on ({@link #takeCastersGone}). A spell card goes away once its spell ended.
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
        /** The conditions that reached 0 this turn: the active character's own and its spells'. */
        public final List<Expired> expired;
        /** The conditions with a duration that went down a round, the active character's own first. */
        public final List<Tick> ticks;
        public final boolean newRound;

        Turn(CharacterModel active, List<Expired> expired, List<Tick> ticks, boolean newRound) {
            this.active = active;
            this.expired = expired;
            this.ticks = ticks;
            this.newRound = newRound;
        }
    }

    /** One condition that went down a round at the start of a turn. */
    public static final class Tick {
        public final String name;
        public final int from;
        public final int to;
        /** Who has it, when that is not the active character (a spell the active one cast); else null. */
        public final CharacterModel target;
        /** The spell it belongs to, or null. */
        public final String spellId;

        Tick(String name, int from, int to, CharacterModel target, String spellId) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.target = target;
            this.spellId = spellId;
        }
    }

    /** A condition that reached 0, and who has it. */
    public static final class Expired {
        public final CharacterModel character;
        public final DebuffModel condition;

        Expired(CharacterModel character, DebuffModel condition) {
            this.character = character;
            this.condition = condition;
        }
    }

    /** A caster left the fight; their spells go on as these spell cards until the GM says otherwise. */
    public static final class CasterGone {
        public final String casterName;
        public final List<CharacterModel> spellCards;

        CasterGone(String casterName, List<CharacterModel> spellCards) {
            this.casterName = casterName;
            this.spellCards = spellCards;
        }
    }

    private final List<CharacterModel> characters = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<CasterGone> castersGone = new ArrayList<>();
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
        castersGone.clear();
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
     * that over, so the round counter keeps going up at the right moment. See {@link #removeAll}
     * for casters and spell cards.
     */
    public void remove(int position) {
        if (!isValidPosition(position)) return;
        removeAll(Collections.singletonList(characters.get(position)));
    }

    public void remove(CharacterModel character) {
        remove(characters.indexOf(character));
    }

    /**
     * Removes several characters (Delete characters in the drawer), with one change notice.
     * A caster whose spells still hold targets that stay is replaced by one spell card per spell,
     * on the same spot, and reported by {@link #takeCastersGone}. A spell card that is removed
     * ends its spell on every target.
     */
    public void removeAll(Collection<CharacterModel> toRemove) {
        List<CharacterModel> leaving = new ArrayList<>();
        for (CharacterModel c : toRemove) {
            if (characters.contains(c) && !leaving.contains(c)) leaving.add(c);
        }
        if (leaving.isEmpty()) return;
        for (CharacterModel c : leaving) {
            if (c.isSpellCard()) removeSpellsOf(c.getStableId());
        }
        for (CharacterModel c : leaving) {
            int position = characters.indexOf(c);
            List<CharacterModel> cards = c.isSpellCard()
                    ? Collections.emptyList() : spellCardsFor(c, leaving);
            if (cards.isEmpty()) {
                removeSilently(position);
                continue;
            }
            characters.remove(position);
            characters.addAll(position, cards);
            cards.get(0).setFirstInRound(c.isFirstInRound());
            castersGone.add(new CasterGone(c.getCharacterName(), cards));
        }
        notifyChanged();
    }

    /**
     * @return The casters that left the fight since the last call, with the spell cards that
     * took their place; the list is emptied, so each one is asked about once.
     */
    public List<CasterGone> takeCastersGone() {
        List<CasterGone> taken = new ArrayList<>(castersGone);
        castersGone.clear();
        return taken;
    }

    /** @return True when a condition in the fight was cast by this character (or spell card). */
    public boolean hasSpellsOf(CharacterModel caster) {
        String id = caster.getStableId();
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (id.equals(d.getCasterId())) return true;
            }
        }
        return false;
    }

    /**
     * One spell card per spell the caster cast on characters that stay, with the caster's
     * initiative and the spell's name. Those conditions now count on the card's turn.
     */
    private List<CharacterModel> spellCardsFor(CharacterModel caster, List<CharacterModel> leaving) {
        String id = caster.getStableId();
        Map<String, CharacterModel> cards = new LinkedHashMap<>();
        for (CharacterModel c : characters) {
            if (leaving.contains(c)) continue;
            for (DebuffModel d : c.getDebuffList()) {
                if (!id.equals(d.getCasterId())) continue;
                // A spell from before spell ids would have none; one card per name then
                String key = d.getSpellId() != null ? d.getSpellId() : "name:" + d.getName();
                CharacterModel card = cards.get(key);
                if (card == null) {
                    card = new CharacterModel();
                    card.setCharacterName(d.getName());
                    card.setInitiative(caster.getInitiative());
                    card.setSpellCardCaster(caster.getCharacterName());
                    cards.put(key, card);
                }
                d.setCasterId(card.getStableId());
            }
        }
        return new ArrayList<>(cards.values());
    }

    /** Takes every condition cast by this id off every character. */
    private void removeSpellsOf(String casterId) {
        for (CharacterModel c : characters) {
            c.getDebuffList().removeIf(d -> casterId.equals(d.getCasterId()));
        }
    }

    /**
     * Spell cards whose spells all ended go away. Not the one at the top: it is having its turn,
     * so it goes when the turn moves on ({@link #nextTurn}).
     */
    private void removeEndedSpellCards() {
        for (int i = characters.size() - 1; i >= 1; i--) {
            CharacterModel c = characters.get(i);
            if (c.isSpellCard() && !hasSpellsOf(c)) removeSilently(i);
        }
    }

    private void removeSilently(int position) {
        CharacterModel removed = characters.remove(position);
        if (removed.isFirstInRound() && !characters.isEmpty()) {
            characters.get(position % characters.size()).setFirstInRound(true);
        }
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
     * Moves the turn on: the top character goes to the bottom, the next one becomes active, and
     * the round goes up when they are first in round. Then these conditions with a duration count
     * down (reported in Turn.ticks): the active character's own, except spells, and on every
     * character the spells the active one cast. A spell whose caster isn't in the fight counts on
     * its own character's turn, so it still ends. A spell card at the top whose spells all ended
     * leaves instead of going to the bottom.
     *
     * @return What happened, or null when there is nobody in the fight.
     */
    public Turn nextTurn() {
        if (characters.isEmpty()) return null;
        CharacterModel top = characters.get(0);
        boolean roundStarted = false;
        if (characters.size() > 1 && top.isSpellCard() && !hasSpellsOf(top)) {
            characters.remove(0);
            // Its round already started on its turn; the next one takes over first in round from now on
            roundStarted = top.isFirstInRound();
            if (roundStarted) characters.get(0).setFirstInRound(true);
        } else {
            Collections.rotate(characters, -1);
        }
        CharacterModel active = characters.get(0);

        Set<String> present = new HashSet<>();
        for (CharacterModel c : characters) present.add(c.getStableId());
        List<Expired> expired = new ArrayList<>();
        List<Tick> ticks = new ArrayList<>();
        tickConditions(active, active, present, expired, ticks);
        for (CharacterModel c : characters) {
            if (c != active) tickConditions(c, active, present, expired, ticks);
        }

        boolean newRound = active.isFirstInRound() && !roundStarted;
        if (newRound) round++;
        notifyChanged();
        return new Turn(active, expired, ticks, newRound);
    }

    /** Counts down the conditions of {@code target} that count on {@code active}'s turn. */
    private static void tickConditions(CharacterModel target, CharacterModel active, Set<String> present,
                                       List<Expired> expired, List<Tick> ticks) {
        for (DebuffModel d : target.getDebuffList()) {
            // Conditions without a duration (0) don't count down, so they aren't reported
            if (d.getDuration() <= 0) continue;
            boolean onThisTurn = d.hasCaster() && present.contains(d.getCasterId())
                    ? d.getCasterId().equals(active.getStableId())
                    : target == active;
            if (!onThisTurn) continue;
            int from = d.getDuration();
            if (d.tick()) expired.add(new Expired(target, d));
            ticks.add(new Tick(d.getName(), from, d.getDuration(), target == active ? null : target, d.getSpellId()));
        }
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

    /**
     * Tells the listeners; call this after changing a character's hp or conditions from outside.
     * Spell cards whose spell just ended are removed first.
     */
    public void notifyChanged() {
        removeEndedSpellCards();
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
