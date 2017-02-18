package com.martianLife.bleNative.result;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Administrator on 2017/2/17.
 */
public class Service implements Parcelable{
    public String uuid;
    public Integer instanceId;
    public Integer type;
    public  List<Characteristic> characteristics;
    public  List<Service> includedServices;

    public Service() {

    }

    public Service(Parcel in) {
        uuid = in.readString();
        instanceId = in.readInt();
        type = in.readInt();
        characteristics = in.readArrayList(Characteristic.class.getClassLoader());
        includedServices = in.readArrayList(Service.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uuid);
        parcel.writeInt(instanceId);
        parcel.writeInt(type);
        parcel.writeList(characteristics);
        parcel.writeList(includedServices);
    }

    public static final Parcelable.Creator<Service> CREATOR
            = new Parcelable.Creator<Service>() {
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        public Service[] newArray(int size) {
            return new Service[size];
        }
    };
}
