package com.five1mars.android.bleNative;

import android.annotation.TargetApi;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telecom.Call;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.*;

import com.five1mars.android.bleNative.util.*;

/**
 * Created by Administrator on 2016/2/28.
 */
@TargetApi(18)
public class BleNative extends ReactContextBaseJavaModule {

    private static final String TAG = BleNative.class.getSimpleName();

    private static final String STATE_UNSUPPORTED = "STATE_UNSUPPORTED";
    private static final String STATE_ADAPTER_DISABLED = "STATE_ADAPTER_DISABLED";
    private static final String STATE_OFF = "STATE_OFF";
    private static final String STATE_TURNING_ON = "STATE_TURNING_ON";
    private static final String STATE_ON = "STATE_ON";
    private static final String STATE_TURNING_OFF = "STATE_TURNING_OFF";

    private static final String EVENT_COMMON_ID = "id";
    private static final String EVENT_COMMON_UUID = "uuid";
    private static final String EVENT_COMMON_VALUE = "value";
    private static final String EVENT_COMMON_PERIPHERAL_ID = "peripheralId";
    private static final String EVENT_COMMON_SERVICE_UUID = "serviceUuid";
    private static final String EVENT_COMMON_CHARACTERISTIC_UUID = "characteristicUuid";
    private static final String EVENT_COMMON_DESCRIPTOR_UUID = "descriptorUuid";

    private static final String EVENT_STATE_CHANGE = "bleStateChanged";
    private static final String EVENT_STATE_CHANGE_PARAM_STATE = "state";

    private static final String EVENT_BLE_PERIPHERAL_SCANNED = "blePeripheralScanned";
    private static final String EVENT_BLE_PERIPHERAL_SCANNED_PARAM_DEVICE_NAME = "deviceName";
    private static final String EVENT_BLE_PERIPHERAL_SCANNED_PARAM_RSSI = "rssi";
    private static final String EVENT_BLE_PERIPHERAL_SCANNED_PARAM_ADDRESS = "address";

    private static final String EVENT_PERIPHERAL_CONNECTED = "blePeripheralConnected";
    private static final String EVENT_PERIPHERAL_DISCONNECTED = "blePeripheralDisconnected";

    private static final String EVENT_SERVICES_DISCOVERED = "bleServicesDiscovered";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_ADDRESS = "address";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_DEVICE_NAME = "deviceName";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_SERVICES = "services";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTICS = "characteristics";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_INCLUDED_SERVICES = "includedServices";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_INSTANCE_ID = "instanceId";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_TYPE = "type";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_INSTANCE_ID = "instanceId";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PERMISSIONS = "permissions";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_STRING_VALUE = "stringValue";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_WRITE_TYPE = "writeType";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PROPERTIES = "properties";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_DESCRIPTORS = "descriptors";
    private static final String EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_PERMISSIONS = "permissions";

    private static final String EVENT_DESCRIPTOR_READ = "bleDescriptorRead";

    private static final String EVENT_DESCRIPTOR_WRITE = "bleDescriptorWritten";

    private static final String EVENT_CHARACTERISTIC_READ = "bleCharacteristicRead";

    private static final String EVENT_CHARACTERISTIC_WRITE = "bleCharacteristicWritten";

    private static final String EVENT_CHARACTERISTIC_CHANGED = "bleCharacteristicChanged";


    private static final String EVENT_BLE_ERROR = "bleError";
    private static final String EVENT_BLE_ERROR_PARAM_MESSAGE = "message";

    private static final String PARAM_SCAN_UUIDS = "uuids";
    private static final String PARAM_CONNECT_ID = "id";
    private static final String PARAM_DISCONNECT_ID = "id";
    private static final String PARAM_COMMON_PERIPHERAL_ID = "peripheralId";
    private static final String PARAM_COMMON_SERVICE_UUID = "serviceUuid";
    private static final String PARAM_COMMON_CHARACTERISTIC_UUID = "characteristicUuid";
    private static final String PARAM_COMMON_DESCRIPTOR_UUID = "descriptorUuid";
    private static final String PARAM_COMMON_VALUE = "value";
    private static final String PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE = "enable";

    private enum BleState {
        STATE_UNSUPPORTED,
        STATE_ADAPTER_DISABLED,
        STATE_OFF,
        STATE_ON,
        STATE_TURNING_OFF,
        STATE_TURNING_ON,
    }

    private Map<String, BluetoothGatt> mGattMaps;
    private BluetoothAdapter mAdapter;

    private BroadcastReceiver mBleStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                WritableMap param = Arguments.createMap();
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        param.putString(EVENT_STATE_CHANGE_PARAM_STATE, BleState.STATE_OFF.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        param.putString(EVENT_STATE_CHANGE_PARAM_STATE, BleState.STATE_TURNING_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_ON:
                        param.putString(EVENT_STATE_CHANGE_PARAM_STATE, BleState.STATE_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        param.putString(EVENT_STATE_CHANGE_PARAM_STATE, BleState.STATE_TURNING_OFF.toString());
                        break;
                    default:
                        break;
                }
                sendEvent(EVENT_STATE_CHANGE, param);
            }
            catch (Exception e) {
                onBleError(null, e);
            }
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    try {
                        String deviceName = device.getName();
                        if (deviceName == null) {
                            final BleAdvertisedData badata;
                            try {
                                badata = BleUtil.parseAdertisedData(scanRecord);
                                deviceName = badata.getName();
                            } catch (UnsupportedEncodingException e) {
                                Log.w(TAG, "广播数据未能识别");
                            }
                        }

                        WritableMap param = Arguments.createMap();
                        param.putString(EVENT_COMMON_ID, device.getAddress());            // 安卓直接使用Address作为ID
                        param.putString(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_DEVICE_NAME, deviceName);
                        param.putInt(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_RSSI, rssi);
                        param.putString(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_ADDRESS, device.getAddress());

                        sendEvent(EVENT_BLE_PERIPHERAL_SCANNED, param);
                    }
                    catch (Exception e) {
                        onBleError(null, e);
                    }
                }
            };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection
                    boolean bSucc = gatt.discoverServices();
                    Log.i(TAG, "Attempting to start service discovery:" + bSucc);

                    WritableMap param = Arguments.createMap();
                    param.putString(EVENT_COMMON_ID, gatt.getDevice().getAddress());
                    sendEvent(EVENT_PERIPHERAL_CONNECTED, param);

                    // 将这个gatt放入全局的map中
                    mGattMaps.put(gatt.getDevice().getAddress(), gatt);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    WritableMap param = Arguments.createMap();
                    param.putString(EVENT_COMMON_ID, gatt.getDevice().getAddress());
                    sendEvent(EVENT_PERIPHERAL_DISCONNECTED, param);

                    mGattMaps.remove(gatt.getDevice().getAddress());
                    gatt.close();
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    WritableMap jsPeripheralObject = Arguments.createMap();
                    jsPeripheralObject.putString(EVENT_COMMON_ID, gatt.getDevice().getAddress());
                    jsPeripheralObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_ADDRESS, gatt.getDevice().getAddress());

                    jsPeripheralObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_DEVICE_NAME, gatt.getDevice().getName());
                    List<BluetoothGattService> services = gatt.getServices();
                    WritableArray jsServiceArray = Arguments.createArray();
                    Iterator<BluetoothGattService> iterator = services.iterator();
                    while (iterator.hasNext()) {
                        BluetoothGattService service = iterator.next();

                        jsServiceArray.pushMap(constructBleServiceObject(service));
                    }
                    jsPeripheralObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_SERVICES, jsServiceArray);

                    sendEvent(EVENT_SERVICES_DISCOVERED, jsPeripheralObject);
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);

                    gatt.disconnect();
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    WritableMap jsResult = Arguments.createMap();
                    jsResult.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
                    jsResult.putString(EVENT_COMMON_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_DESCRIPTOR_UUID, descriptor.getUuid().toString());
                    jsResult.putArray(EVENT_COMMON_VALUE, constructBleByteArray(descriptor.getValue()));

                    sendEvent(EVENT_DESCRIPTOR_READ, jsResult);
                }
                else {
                    onBleError(gatt, new RuntimeException("onDescriptorRead errur, status : " + status));
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    WritableMap jsResult = Arguments.createMap();
                    jsResult.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
                    jsResult.putString(EVENT_COMMON_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_DESCRIPTOR_UUID, descriptor.getUuid().toString());
                    jsResult.putArray(EVENT_COMMON_VALUE, constructBleByteArray(descriptor.getValue()));

                    sendEvent(EVENT_DESCRIPTOR_WRITE, jsResult);
                }
                else {
                    onBleError(gatt, new RuntimeException("onDescriptorWrite errur, status : " + status));
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    WritableMap jsResult = Arguments.createMap();
                    jsResult.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
                    jsResult.putString(EVENT_COMMON_SERVICE_UUID, characteristic.getService().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                    jsResult.putArray(EVENT_COMMON_VALUE, constructBleByteArray(characteristic.getValue()));

                    sendEvent(EVENT_CHARACTERISTIC_READ, jsResult);
                }
                else {
                    onBleError(gatt, new RuntimeException("onCharacteristicRead errur, status : " + status));
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    WritableMap jsResult = Arguments.createMap();
                    jsResult.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
                    jsResult.putString(EVENT_COMMON_SERVICE_UUID, characteristic.getService().getUuid().toString());
                    jsResult.putString(EVENT_COMMON_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                    jsResult.putArray(EVENT_COMMON_VALUE, constructBleByteArray(characteristic.getValue()));

                    sendEvent(EVENT_CHARACTERISTIC_WRITE, jsResult);
                }
                else {
                    onBleError(gatt, new RuntimeException("onCharacteristicWrite errur, status : " + status));
                }
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                WritableMap jsResult = Arguments.createMap();
                jsResult.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
                jsResult.putString(EVENT_COMMON_SERVICE_UUID, characteristic.getService().getUuid().toString());
                jsResult.putString(EVENT_COMMON_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                jsResult.putArray(EVENT_COMMON_VALUE, constructBleByteArray(characteristic.getValue()));

                sendEvent(EVENT_CHARACTERISTIC_CHANGED, jsResult);
            }
            catch (Exception e) {
                onBleError(gatt, e);
            }
        }
    };

    private WritableMap constructBleServiceObject(BluetoothGattService service) {
        WritableMap jsServiceObject = Arguments.createMap();
        jsServiceObject.putString(EVENT_COMMON_UUID, service.getUuid().toString());
        Iterator<BluetoothGattCharacteristic> iterator2 = service.getCharacteristics().iterator();
        WritableArray jsCharacteristicArray = Arguments.createArray();
        while (iterator2.hasNext()) {
            BluetoothGattCharacteristic characteristic = iterator2.next();

            jsCharacteristicArray.pushMap(constructBleCharacteristicObject(characteristic));
        }
        jsServiceObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTICS, jsCharacteristicArray);

        WritableArray jsIncludedServiceArray = Arguments.createArray();
        Iterator<BluetoothGattService> iterator = service.getIncludedServices().iterator();
        while (iterator.hasNext()) {
            BluetoothGattService service2 = iterator.next();

            jsIncludedServiceArray.pushMap(constructBleServiceObject(service2));
        }
        jsServiceObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_INCLUDED_SERVICES, jsIncludedServiceArray);
        jsServiceObject.putInt(EVENT_SERVICES_DISCOVERED_PARAM_INSTANCE_ID, service.getInstanceId());
        jsServiceObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_TYPE, BleConstantsConverter.getServiceType(service.getType()));

        return jsServiceObject;
    }

    private WritableMap constructBleCharacteristicObject(BluetoothGattCharacteristic characteristic) {
        WritableMap jsCharacteristicObject = Arguments.createMap();
        jsCharacteristicObject.putString(EVENT_COMMON_UUID, characteristic.getUuid().toString());
        jsCharacteristicObject.putInt(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_INSTANCE_ID, characteristic.getInstanceId());
        jsCharacteristicObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PERMISSIONS, BleConstantsConverter.getCharacteristicPermissions(characteristic.getPermissions()));
        jsCharacteristicObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_WRITE_TYPE, BleConstantsConverter.getCharacteristicWriteType(characteristic.getWriteType()));
        jsCharacteristicObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PROPERTIES, BleConstantsConverter.getCharacteristicProperties(characteristic.getProperties()));
        jsCharacteristicObject.putArray(EVENT_COMMON_VALUE, constructBleByteArray(characteristic.getValue()));

        Iterator<BluetoothGattDescriptor> iterator = characteristic.getDescriptors().iterator();
        WritableArray jsDescriptorsArray = Arguments.createArray();
        while(iterator.hasNext()) {
            BluetoothGattDescriptor descriptor = iterator.next();
            jsDescriptorsArray.pushMap(constructBleDescriptorObject(descriptor));
        }
        jsCharacteristicObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_DESCRIPTORS, jsDescriptorsArray);

        return jsCharacteristicObject;
    }

    private WritableMap constructBleDescriptorObject(BluetoothGattDescriptor descriptor) {
        WritableMap jsDescriptorObject = Arguments.createMap();
        jsDescriptorObject.putString(EVENT_COMMON_UUID, descriptor.getUuid().toString());
        jsDescriptorObject.putArray(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_PERMISSIONS, BleConstantsConverter.getDescriptorPermissions(descriptor.getPermissions()));
        byte[] value = descriptor.getValue();
       /* if(value == null) {
            jsDescriptorObject.putString(EVENT_COMMON_VALUE, null);
        }
        else if(descriptor.getUuid().equals(BleConstantsConverter.clientConfigrationDescriptorUuid)) {
            if(value.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                jsDescriptorObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_VALUE, BleConstantsConverter.descriptorValueNotificationAndIndicationDisabled);
            }
            else if(value.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                jsDescriptorObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_VALUE, BleConstantsConverter.descriptorValueIndicationEnabled);
            }
            else if(value.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                jsDescriptorObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_VALUE, BleConstantsConverter.descriptorValueNotificationEnabled);
            }
            else {
                jsDescriptorObject.putString(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_VALUE, BleConstantsConverter.descriptorValueUnrecognized);
            }
        }
        else */{
            jsDescriptorObject.putArray(EVENT_COMMON_VALUE, constructBleByteArray(value));
        }

        return jsDescriptorObject;
    }

    private WritableArray constructBleByteArray(byte[] value) {
        if(value == null)
            return null;

        WritableArray jsByteArray = Arguments.createArray();
        for(int i = 0; i < value.length; i ++) {
            jsByteArray.pushInt(value[i]);
        }

        return jsByteArray;
    }

    private byte[] decodeBleReadableArray(ReadableArray array) {
        byte[] value = new byte[array.size()];
        for(int i = 0; i < array.size(); i ++) {
            value[i] = (byte) array.getInt(i);
        }

        return value;
    }

    private String getPeripheralId(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BleNative(ReactApplicationContext reactContext) {
        super(reactContext);
        mGattMaps = new HashMap<>();
    }

    @Override
    public String getName() {
        return "BleNative";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> map = new HashMap<>();
        map.put("STATE_UNSUPPORTED", BleState.STATE_UNSUPPORTED.toString());
        map.put("STATE_ADAPTER_DISABLED", BleState.STATE_ADAPTER_DISABLED.toString());
        map.put("STATE_OFF", BleState.STATE_OFF.toString());
        map.put("STATE_TURNING_ON", BleState.STATE_TURNING_ON.toString());
        map.put("STATE_ON", BleState.STATE_ON.toString());
        map.put("STATE_TURNING_OFF", BleState.STATE_TURNING_OFF.toString());

        Log.i(TAG, bytesToString(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
        Log.i(TAG, bytesToString(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
        Log.i(TAG, bytesToString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));

        map.put("DISABLE_NOTIFICATION_VALUE", bytesToString(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
        map.put("ENABLE_INDICATION_VALUE", bytesToString(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
        map.put("ENABLE_NOTIFICATION_VALUE", bytesToString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
        return map;
    }


    @ReactMethod
    public void init(Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && getCurrentActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager bluetoothManager = (BluetoothManager) getCurrentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            mAdapter = bluetoothManager.getAdapter();
            if(mAdapter == null) {
                callback.invoke(BleState.STATE_UNSUPPORTED);
            }
            else {
                switch (mAdapter.getState()) {
                    case BluetoothAdapter.STATE_OFF:
                        callback.invoke(BleState.STATE_OFF.toString());
                        break;
                    case BluetoothAdapter.STATE_ON:
                        callback.invoke(BleState.STATE_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        callback.invoke(BleState.STATE_TURNING_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        callback.invoke(BleState.STATE_TURNING_OFF.toString());
                        break;
                    default:
                        break;
                }

                // 注册蓝牙状态监听
                getReactApplicationContext().registerReceiver(mBleStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            }

        }
        else {
            callback.invoke(BleState.STATE_UNSUPPORTED);
        }
    }

    @ReactMethod
    public void destroy() {
        getReactApplicationContext().unregisterReceiver(mBleStateReceiver);
    }

    @ReactMethod
    public void set(Boolean enable, Callback onError) {
        if(enable) {
            if(!mAdapter.enable()) {
                onError.invoke();
            }
        }
        else {
            if(!mAdapter.disable()) {
                onError.invoke();
            }
        }
    }


    @ReactMethod
    public void startScan(ReadableMap filter) {
        /*if(filter != null) {
            ReadableArray array = filter.getArray(PARAM_SCAN_UUIDS);
            UUID [] uuids = new UUID[array.size()];
            for(int i = 0; i < array.size(); i ++) {
                uuids[i] = UUID.fromString(array.getString(i));
            }

            mAdapter.startLeScan(uuids, mLeScanCallback);
        }
        else {*/
            mAdapter.startLeScan(mLeScanCallback);
        //}
    }

    @ReactMethod
    public void stopScan() {
        mAdapter.stopLeScan(mLeScanCallback);
    }

    @ReactMethod
    public void connect(ReadableMap param, Callback onError) {
        try {
            String id = param.getString(PARAM_CONNECT_ID);

            BluetoothDevice device = mAdapter.getRemoteDevice(id);
            BluetoothGatt bluetoothGatt = device.connectGatt(getReactApplicationContext(), false, mGattCallback);
            refreshDeviceCache(bluetoothGatt);
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void disconnect(ReadableMap param, Callback onError) {
        try{
            String id = param.getString(PARAM_DISCONNECT_ID);

            BluetoothGatt gatt = mGattMaps.get(id);
            gatt.disconnect();
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void readDescriptor(ReadableMap param, Callback onError) {
        try{
            String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
            String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
            String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
            String descriptorUuid = param.getString(PARAM_COMMON_DESCRIPTOR_UUID);

            BluetoothGatt gatt = mGattMaps.get(peripheralId);
            if(gatt.readDescriptor(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)).getDescriptor(UUID.fromString(descriptorUuid))) == false) {
                onError.invoke("readDescriptor returns false");
            }
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void writeDescriptor(ReadableMap param, Callback onError) {
        try{
            String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
            String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
            String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
            String descriptorUuid = param.getString(PARAM_COMMON_DESCRIPTOR_UUID);
            byte[] value = decodeBleReadableArray(param.getArray(PARAM_COMMON_VALUE));

            BluetoothGatt gatt = mGattMaps.get(peripheralId);
            BluetoothGattDescriptor descriptor = gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)).getDescriptor(UUID.fromString(descriptorUuid));
            if(!descriptor.setValue(value) || !gatt.writeDescriptor(descriptor)) {
                onError.invoke("writeDescriptor returns false");
            }
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void setCharacteristicNotification(ReadableMap param, Callback onError) {
        try {
            String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
            String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
            String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
            Boolean enable = param.getBoolean(PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE);

            BluetoothGatt gatt = mGattMaps.get(peripheralId);
            if(gatt.setCharacteristicNotification(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)), enable) == false) {
                onError.invoke("setCharacteristicNotification returns false");
            }

        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void readCharacteristic(ReadableMap param, Callback onError){
        try {
            String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
            String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
            String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);

            BluetoothGatt gatt = mGattMaps.get(peripheralId);
            if(gatt.readCharacteristic(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid))) == false) {
                onError.invoke("readCharacteristic returns false");
            }
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }


    @ReactMethod
    public void writeCharacteristic(ReadableMap param, Callback onError) {
        try {
            String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
            String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
            String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
            ReadableArray valueArray = param.getArray(PARAM_COMMON_VALUE);
            byte[] value = decodeBleReadableArray(valueArray);

            BluetoothGatt gatt = mGattMaps.get(peripheralId);
            BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid));
            if(!characteristic.setValue(value) || !gatt.writeCharacteristic(characteristic)) {
                onError.invoke("writeCharacteristic returns false");
            }
        }
        catch (Exception e) {
            onError.invoke(e.getMessage());
        }
    }

    private void onBleError(BluetoothGatt gatt, Exception e) {
        WritableMap param = Arguments.createMap();

        if(gatt != null) {
            param.putString(EVENT_COMMON_PERIPHERAL_ID, getPeripheralId(gatt));
        }
        param.putString(EVENT_BLE_ERROR_PARAM_MESSAGE, e.getMessage());

        sendEvent(EVENT_BLE_ERROR, param);
    }



    private void sendEvent(String eventName,
                           @Nullable WritableMap params) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    /*
   * 这个函数用来refresh掉缓存的service，以避免在dfu和normal模式之间切换时的service不一样
    */
    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }



    private static byte[] stringToBytes(String s) {
        char c[] = s.toCharArray();
        byte b[] = new byte[c.length];
        for(int i = 0; i < c.length; i ++){
            b[i] = (byte)c[i];
        }

        return b;
    }

    private static String bytesToString(byte[] b) {
        char[] c = new char[b.length];
        for(int i = 0; i < b.length; i ++) {
            c[i] = (char)b[i];
        }
        return new String(c);
    }
}
