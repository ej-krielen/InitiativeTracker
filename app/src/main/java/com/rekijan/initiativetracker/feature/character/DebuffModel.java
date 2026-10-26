package com.rekijan.initiativetracker.feature.character;

import com.rekijan.initiativetracker.core.util.NumberInput;

/**
 * A (de)buff on a character: a name, a duration in rounds and a description. A duration of 0
 * means it has no timer (or it ran out and the user chose to keep it).
 * <p>
 * A condition from a cast spell also knows its caster and its spell: it counts down when the
 * caster's turn starts instead of its own character's ({@link #hasCaster}). The caster is kept by
 * {@link CharacterModel#getStableId()}, with the name for showing it (a player only gets the
 * name). All conditions of one cast share a spell id, so the spell can be ended on every target
 * at once. Without a caster (all saves from before spells) nothing changes.
 * <p>
 * The field names are part of the JSON saved by 1.x, so they must stay the same.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-5-2019
 */
public class DebuffModel {

    public static final int MAX_DURATION = 9999;

    private String name;
    private int duration;
    private String description;
    /** Stable id of the character who cast this; null when it isn't a spell. */
    private String casterId;
    /** The caster's name when cast, for showing it; also what a player sees. */
    private String casterName;
    /** Shared by every condition of one cast; null when it isn't a spell. */
    private String spellId;

    public DebuffModel() {
        name = "";
        duration = 0;
        description = "";
    }

    public DebuffModel(String name, int duration, String description) {
        this.name = name;
        setDuration(duration);
        this.description = description;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    /** Sets the duration in rounds, kept between 0 and {@link #MAX_DURATION}. */
    public void setDuration(int duration) {
        this.duration = NumberInput.clamp(duration, 0, MAX_DURATION);
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Lowers the duration by one round, never below 0.
     *
     * @return True when it reached 0 just now.
     */
    public boolean tick() {
        int left = duration - 1;
        setDuration(left);
        return left == 0;
    }

    /** @return True when this counts down on its caster's turn, not on its own character's. */
    public boolean hasCaster() {
        return casterId != null && !casterId.isEmpty();
    }

    /** Marks this as cast by a character, as part of one spell. */
    public void setCaster(String casterId, String casterName, String spellId) {
        this.casterId = casterId;
        this.casterName = casterName;
        this.spellId = spellId;
    }

    /** Takes over the caster and spell of another condition (e.g. the GM's own copy of it). */
    public void copyCasterFrom(DebuffModel other) {
        setCaster(other.casterId, other.casterName, other.spellId);
    }

    public String getCasterId() {
        return casterId;
    }

    /** Lets the spell count on another character's turn: the spell card that took the caster's place. */
    public void setCasterId(String casterId) {
        this.casterId = casterId;
    }

    public String getCasterName() {
        return casterName == null ? "" : casterName;
    }

    public void setCasterName(String casterName) {
        this.casterName = casterName;
    }

    public String getSpellId() {
        return spellId;
    }
}
