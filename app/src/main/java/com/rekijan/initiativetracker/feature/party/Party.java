package com.rekijan.initiativetracker.feature.party;

import java.util.UUID;

/**
 * A saved party: a name and a fixed id. Its characters and round counter are saved under that id
 * by {@link PartyStore}, so renaming never moves data. The id is random, so a party can later be
 * synced between devices without clashing.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 25-10-2026
 */
public class Party {

    private String id;
    private String name;

    /** For Gson. */
    @SuppressWarnings("unused")
    private Party() {
    }

    /** A new party with a fresh id. */
    public Party(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Party(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }
}
