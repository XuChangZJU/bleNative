package com.five1mars.android.bleNative.util;

import android.support.annotation.Nullable;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;

import java.util.UUID;

/**
 * Created by Administrator on 2016/3/3.
 *
 * 本函数用于将android ble的枚举整数值转化成为javascript所习惯的readable string
 * 本函数中的字符串定义参照的是android sdk apilevel 23
 * BluetoothGattDescriptor类和BluetoothGattCharacteristic类的定义
 *
 * by xc
 */
public class BleConstantsConverter {
    public static final UUID clientConfigrationDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String descriptorValueNotificationEnabled = "Notification Enabled";
    public static final String descriptorValueIndicationEnabled = "Indication Enabled";
    public static final String descriptorValueNotificationAndIndicationDisabled = "Notification and Indication Disabled";
    public static final String descriptorValueUnrecognized = "Unrecognized";

    private static final String[] BLE_SERVICE_TYPES = {"PRIMARY", "SECONDARY"};
    private static final String[] BLE_CHARACTERISTIC_PERMISSIONS = {
            "permission_read",
            "permission_read_encrypted",
            "permission_read_encrypted_mitm",
            "permission_write",
            "permission_write_encrypted",
            "permission_write_encrypted_mitm",
            "permission_write_signed",
            "permission_write_signed_mitm"
    };
    private static final String[] BLE_CHARACTERISTIC_PROPERITIES = {
            "property_broadcast",
            "property_read",
            "property_write_no_response",
            "property_write",
            "property_notify",
            "property_indicate",
            "property_signed_write",
            "property_extended_props"
    };
    private static final String[] BLE_CHARACTERISTIC_WRITE_TYPES = {
            "write_type_no_response",
            "write_type_default",
            "write_type_signed"
    };
    private static final String[] BLE_DESCRIPTOR_PERMISSIONS = {
            "permission_read",
            "permission_read_encrypted",
            "permission_read_encrypted_mitm",
            "permission_write",
            "permission_write_encrypted",
            "permission_write_encrypted_mitm",
            "permission_write_signed",
            "permission_write_signed_mitm"
    };


    public static String getServiceType(int type) {
        return BLE_SERVICE_TYPES[type];
    }

    public static WritableArray getCharacteristicPermissions(int permissions) {
        return getAllStringsByBitmask(permissions, BLE_CHARACTERISTIC_PERMISSIONS);
    }


    public static WritableArray getCharacteristicProperties(int properties) {
        return getAllStringsByBitmask(properties, BLE_CHARACTERISTIC_PROPERITIES);
    }

    public static String getCharacteristicWriteType(int writeType) {
        return getFirstStringByBitmask(writeType, BLE_CHARACTERISTIC_WRITE_TYPES);
    }

    public static WritableArray getDescriptorPermissions(int permissions) {
        return getAllStringsByBitmask(permissions, BLE_DESCRIPTOR_PERMISSIONS);
    }


    private static String getFirstStringByBitmask(int mask, String[] values) {
        int i = 0;
        if(mask == 0)
            return null;
        while(((mask >>> i) & 0x0001) == 0) {
            i++;
        }
        return values[i];
    }


    private static WritableArray getAllStringsByBitmask(int mask, String[] values) {
        WritableArray array = Arguments.createArray();
        int i = 0;
        if(mask == 0)
            return array;

        while((mask >>> i) != 0) {
            if(((mask >>> i) & 0x0001) != 0)
                array.pushString(values[i]);
            i++;
        }
        return array;
    }
}
