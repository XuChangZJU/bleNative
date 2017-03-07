package com.martianLife.bleNative;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.martianLife.bleNative.result.Peripheral;

/**
 * Created by Administrator on 2017/1/20.
 */
public class BleEventListener extends HeadlessJsTaskService {
    @Override
    protected @Nullable
    HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        String event = intent.getStringExtra(BleService.EXTRA_EVENT);
        Log.w("BleEventListener", event);

        WritableMap writableMap = Arguments.createMap();

        switch (event) {
            case BleService.ACTION_SERVICES_DISCOVERED:
                Log.w("BleEventListener", "ACTION_SERVICES_DISCOVERED");
                Peripheral peripheral = intent.getParcelableExtra(BleService.EXTRA_VALUE);
                writableMap.putMap("peripheral", BleNative.constructPeripheralMap(peripheral));
                writableMap.putString("event", "serviceDiscovered");
                return new HeadlessJsTaskConfig(
                        "bleEvent",
                        writableMap,
                        10000);
            case BleService.ACTION_STATE_CHANGED:
                writableMap.putString("state", intent.getStringExtra(BleService.EXTRA_STATE));
                writableMap.putString("id", intent.getStringExtra(BleService.EXTRA_ADDRESS));
                writableMap.putString("event", "stateChanged");
                return new HeadlessJsTaskConfig(
                        "bleEvent",
                        writableMap,
                        10000);

            case BleService.ACTION_BOND_STATE_CHANGED:
                writableMap.putString("state", intent.getStringExtra(BleService.EXTRA_STATE));
                writableMap.putString("id", intent.getStringExtra(BleService.EXTRA_ADDRESS));
                writableMap.putString("event", "bondStateChanged");
                return new HeadlessJsTaskConfig(
                        "bleEvent",
                        writableMap,
                        10000);
            default:
                break;
        }

        return null;
    }
}
