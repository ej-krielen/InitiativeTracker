package com.rekijan.initiativetracker.party;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the Party
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 7-2-2016
 */
public class PartyModel implements Parcelable{

    private long id;
    private long party_id;

    public PartyModel () {

    }

    public static final Creator<PartyModel> CREATOR = new Creator<PartyModel>() {
        @Override
        public PartyModel createFromParcel(Parcel in) {
            return new PartyModel(in);
        }

        @Override
        public PartyModel[] newArray(int size) {
            return new PartyModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(party_id);
    }

    private PartyModel(Parcel in) {
        id = in.readLong();
        party_id = in.readLong();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getParty_id() {
        return party_id;
    }

    public void setParty_id(long party_id) {
        this.party_id = party_id;
    }
}