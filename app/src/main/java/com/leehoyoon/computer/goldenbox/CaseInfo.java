package com.leehoyoon.computer.goldenbox;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

public class CaseInfo implements Parcelable {
    private Location start;
    private Location destination;
    private ArrayList<HashMap<String, Double>> route;

    public CaseInfo(Location start, Location destination,ArrayList<HashMap<String, Double>> route){
        this.start = start;
        this.destination = destination;
        this.route = route;
    }

    protected CaseInfo(Parcel in) {
        start = in.readParcelable(Location.class.getClassLoader());
        destination = in.readParcelable(Location.class.getClassLoader());
        route = (ArrayList<HashMap<String, Double>>)in.readSerializable();
    }

    public static final Creator<CaseInfo> CREATOR = new Creator<CaseInfo>() {
        @Override
        public CaseInfo createFromParcel(Parcel in) {
            return new CaseInfo(in);
        }

        @Override
        public CaseInfo[] newArray(int size) {
            return new CaseInfo[size];
        }
    };

    public Location getStart() {
        return start;
    }

    public Location getDestination() {
        return destination;
    }

    public ArrayList<HashMap<String, Double>> getRoute() {
        return route;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(start, flags);
        dest.writeParcelable(destination, flags);
        dest.writeSerializable(route);
    }
}
/*
class Type implements Parcelable{
    HashMap<String, Double> hashMap;

    public Type(HashMap<String, Double> hashMap){
        this.hashMap = hashMap;
    }

    protected Type(Parcel in) {
        hashMap = (HashMap<String, Double>)in.readSerializable();
    }

    public static final Creator<Type> CREATOR = new Creator<Type>() {
        @Override
        public Type createFromParcel(Parcel in) {
            return new Type(in);
        }

        @Override
        public Type[] newArray(int size) {
            return new Type[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(hashMap);
    }
}*/
