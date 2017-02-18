package com.martianLife.bleNative.result;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Administrator on 2017/2/17.
 */
public class Peripheral implements Parcelable{
    public String id;
    public String address;
    public String name;
    public List<Service> services;

    public Peripheral() {

    }

    public Peripheral(Parcel in) {
        id = in.readString();
        address = in.readString();
        name = in.readString();
        services = in.readArrayList(Service.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(address);
        parcel.writeString(name);
        parcel.writeList(services);
    }

    public static final Parcelable.Creator<Peripheral> CREATOR
            = new Parcelable.Creator<Peripheral>() {
        public Peripheral createFromParcel(Parcel in) {
            return new Peripheral(in);
        }

        public Peripheral[] newArray(int size) {
            return new Peripheral[size];
        }
    };
}
