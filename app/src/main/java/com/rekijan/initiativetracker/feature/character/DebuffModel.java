package com.rekijan.initiativetracker.feature.character;

import com.rekijan.initiativetracker.core.util.NumberInput;

/**
 * A (de)buff on a character: a name, a duration in rounds and a description. A duration of 0
 * means it has no timer (or it ran out and the user chose to keep it).
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
}
