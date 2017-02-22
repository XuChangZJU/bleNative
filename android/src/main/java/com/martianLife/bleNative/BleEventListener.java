package com.five1mars.android.bleNative;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

/**
 * Created by Administrator on 2017/1/20.
 */
public class BleEventListener extends HeadlessJsTaskService {
    @Override
    protected @Nullable
    HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Log.w("BleEventListener", "getTaskConfig");
        Bundle extras = intent.getExtras();
        BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        WritableMap param = Arguments.createMap();
        param.putString("id", device.getAddress());
        if (extras != null) {
            return new HeadlessJsTaskConfig(
                    "bleConnected",
                    param,
                    8000);
        }
        return null;
    }
}
