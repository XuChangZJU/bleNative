package com.five1mars.android.bleNative.util;

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2015/4/16.
 */
public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}