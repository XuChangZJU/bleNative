package com.martianLife.bleNative.result;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2017/2/17.
 */
public class Descriptor implements Parcelable{
    public String uuid;
    public Integer permissions;
    public byte[] value;

    public Descriptor() {

    }

    public Descriptor(Parcel in) {
        uuid = in.readString();
        permissions = in.readInt();
        byte [] bytes = in.createByteArray();
        this.value = bytes;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uuid);
        parcel.writeInt(permissions);
        parcel.writeByteArray(value);
    }

    public static final Parcelable.Creator<Descriptor> CREATOR
            = new Parcelable.Creator<Descriptor>() {
        public Descriptor createFromParcel(Parcel in) {
            return new Descriptor(in);
        }

        public Descriptor[] newArray(int size) {
            return new Descriptor[size];
        }
    };
}
