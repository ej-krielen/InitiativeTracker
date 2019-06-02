package com.rekijan.initiativetracker.character.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the Debuffs
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 28-5-2019
 */
public class DebuffModel implements Parcelable {

    private String name;
    private int duration;
    private String description;

    public DebuffModel() {
        name = "";
        duration = 0;
        description = "";
    }

    public static final Parcelable.Creator<DebuffModel> CREATOR
            = new Parcelable.Creator<DebuffModel>() {
        public DebuffModel createFromParcel(Parcel in) {
            return new DebuffModel(in);
        }

        public DebuffModel[] newArray(int size) {
            return new DebuffModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(duration);
        dest.writeString(description);
    }

    private DebuffModel(Parcel in) {
        name = in.readString();
        duration = in.readInt();
        description = in.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}