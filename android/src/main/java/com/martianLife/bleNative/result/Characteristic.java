package com.martianLife.bleNative.result;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Administrator on 2017/2/17.
 */
public class Characteristic implements Parcelable{

    public Characteristic() {

    }

    public Characteristic(Parcel in) {
        uuid = in.readString();
        instanceId = in.readInt();
        permissions = in.readInt();
        writeType = in.readInt();
        properties = in.readInt();
        byte [] bytes = in.createByteArray();
        this.value = bytes;
        descriptors = in.readArrayList(Descriptor.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uuid);
        parcel.writeInt(instanceId);
        parcel.writeInt(permissions);
        parcel.writeInt(writeType);
        parcel.writeInt(properties);
        parcel.writeByteArray(value);
        parcel.writeList(descriptors);
    }

    public static final Parcelable.Creator<Characteristic> CREATOR
            = new Parcelable.Creator<Characteristic>() {
        public Characteristic createFromParcel(Parcel in) {
            return new Characteristic(in);
        }

        public Characteristic[] newArray(int size) {
            return new Characteristic[size];
        }
    };

    public String uuid;
    public Integer instanceId;
    public Integer permissions;
    public Integer writeType;
    public Integer properties;
    public byte[] value;
    public List<Descriptor> descriptors;
}
