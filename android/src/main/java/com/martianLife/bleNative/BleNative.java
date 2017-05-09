package com.martianLife.bleNative;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.martianLife.bleNative.result.Characteristic;
import com.martianLife.bleNative.result.Descriptor;
import com.martianLife.bleNative.result.Peripheral;
import com.martianLife.bleNative.result.Service;
import com.martianLife.bleNative.util.BleConstantsConverter;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Created by Administrator on 2016/2/28.
 */
@TargetApi(18)
public class BleNative extends ReactContextBaseJavaModule {

    private static final String TAG = BleNative.class.getSimpleName();

    private static final int MAX_PACKET_LENGTH = 20;
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
    private static final String EVENT_BOND_CHANGE = "bondChanged";
    private static final String EVENT_BOND_CHANGE_PARAM_STATE = "state";
    private static final String EVENT_BOND_CHANGE_PARAM_ID = "id";

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
    private static final String PARAM_AUTO_CONNECT = "autoConnect";
    private static final String PARAM_DISCONNECT_ID = "id";
    private static final String PARAM_COMMON_PERIPHERAL_ID = "peripheralId";
    private static final String PARAM_COMMON_SERVICE_UUID = "serviceUuid";
    private static final String PARAM_COMMON_CHARACTERISTIC_UUID = "characteristicUuid";
    private static final String PARAM_COMMON_DESCRIPTOR_UUID = "descriptorUuid";
    private static final String PARAM_COMMON_VALUE = "value";
    private static final String PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE = "enable";

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private Messenger mRemoteBleService;
    private Promise mPromise = null;

    public BleNative(ReactApplicationContext reactContext) {
        super(reactContext);
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
        map.put("STATE_BIND_FAILURE", BleState.STATE_BIND_FAILURE.toString());


        map.put("DISABLE_NOTIFICATION_VALUE", bytesToString(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
        map.put("ENABLE_INDICATION_VALUE", bytesToString(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
        map.put("ENABLE_NOTIFICATION_VALUE", bytesToString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
        return map;
    }

    private BroadcastReceiver mBleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            WritableMap writableMap = Arguments.createMap();
            switch (action) {
                case BleService.ACTION_STATE_GOT:
                    if (mPromise != null) {
                        mPromise.resolve(intent.getStringExtra(BleService.EXTRA_STATE));
                        mPromise = null;
                    }
                    break;
                case BleService.ACTION_STATE_CHANGED:
                    writableMap.putString(EVENT_STATE_CHANGE_PARAM_STATE, intent.getStringExtra(BleService.EXTRA_STATE));
                    sendEvent(EVENT_STATE_CHANGE, writableMap);
                    break;
                case BleService.ACTION_BOND_STATE_CHANGED:
                    writableMap.putString(EVENT_BOND_CHANGE_PARAM_STATE, intent.getStringExtra(BleService.EXTRA_STATE));
                    writableMap.putString(EVENT_BOND_CHANGE_PARAM_ID, intent.getStringExtra(BleService.EXTRA_ADDRESS));
                    sendEvent(EVENT_BOND_CHANGE, writableMap);
                    break;
                case BleService.ACTION_BOND_STATE_GOT:
                    if (mPromise != null) {
                        mPromise.resolve(intent.getBooleanExtra(BleService.EXTRA_STATE, false));
                        mPromise = null;
                    }
                    break;
                case BleService.ACTION_CONNECTED:
                    writableMap.putString(EVENT_BOND_CHANGE_PARAM_ID, intent.getStringExtra(BleService.EXTRA_ADDRESS));
                    sendEvent(EVENT_BOND_CHANGE, writableMap);
                    break;
                case BleService.ACTION_SERVICES_DISCOVERED:
                    Peripheral peripheral = intent.getParcelableExtra(BleService.EXTRA_VALUE);
                    sendEvent(EVENT_SERVICES_DISCOVERED, constructPeripheralMap(peripheral));
                    break;
                case BleService.ACTION_DISCONNECTED:
                    writableMap.putString(EVENT_COMMON_ID, intent.getStringExtra(BleService.EXTRA_ID));
                    sendEvent(EVENT_PERIPHERAL_DISCONNECTED, writableMap);
                    break;
                case BleService.ACTION_DESCRIPTOR_READ:
                    writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID));
                    writableMap.putString(EVENT_COMMON_SERVICE_UUID, intent.getStringExtra(BleService.EXTRA_SERVICE_UUID));
                    writableMap.putString(EVENT_COMMON_CHARACTERISTIC_UUID, intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID));
                    writableMap.putString(EVENT_COMMON_DESCRIPTOR_UUID, intent.getStringExtra(BleService.EXTRA_DESCRIPTOR_UUID));
                    writableMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(intent.getByteArrayExtra(BleService.EXTRA_VALUE)));

                    sendEvent(EVENT_DESCRIPTOR_READ, writableMap);
                    break;
                case BleService.ACTION_DESCRIPTOR_WRITTEN:
                    writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID));
                    writableMap.putString(EVENT_COMMON_SERVICE_UUID, intent.getStringExtra(BleService.EXTRA_SERVICE_UUID));
                    writableMap.putString(EVENT_COMMON_CHARACTERISTIC_UUID, intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID));
                    writableMap.putString(EVENT_COMMON_DESCRIPTOR_UUID, intent.getStringExtra(BleService.EXTRA_DESCRIPTOR_UUID));
                    writableMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(intent.getByteArrayExtra(BleService.EXTRA_VALUE)));

                    sendEvent(EVENT_DESCRIPTOR_WRITE, writableMap);
                    break;
                case BleService.ACTION_CHARACTERISTIC_READ:
                    writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID));
                    writableMap.putString(EVENT_COMMON_SERVICE_UUID, intent.getStringExtra(BleService.EXTRA_SERVICE_UUID));
                    writableMap.putString(EVENT_COMMON_CHARACTERISTIC_UUID, intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID));
                    writableMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(intent.getByteArrayExtra(BleService.EXTRA_VALUE)));

                    sendEvent(EVENT_CHARACTERISTIC_READ, writableMap);
                    break;
                case BleService.ACTION_CHARACTERISTIC_WRITTEN:
                    writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID));
                    writableMap.putString(EVENT_COMMON_SERVICE_UUID, intent.getStringExtra(BleService.EXTRA_SERVICE_UUID));
                    writableMap.putString(EVENT_COMMON_CHARACTERISTIC_UUID, intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID));
                    writableMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(intent.getByteArrayExtra(BleService.EXTRA_VALUE)));

                    sendEvent(EVENT_CHARACTERISTIC_WRITE, writableMap);
                    break;
                case BleService.ACTION_CHARACTERISTIC_CHANGED:
                    writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID));
                    writableMap.putString(EVENT_COMMON_SERVICE_UUID, intent.getStringExtra(BleService.EXTRA_SERVICE_UUID));
                    writableMap.putString(EVENT_COMMON_CHARACTERISTIC_UUID, intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID));
                    writableMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(intent.getByteArrayExtra(BleService.EXTRA_VALUE)));

                    sendEvent(EVENT_CHARACTERISTIC_CHANGED, writableMap);
                    break;
                case BleService.ACTION_PERIPHERAL_SCANNED:
                    writableMap.putString(EVENT_COMMON_ID, intent.getStringExtra(BleService.EXTRA_ID));
                    writableMap.putString(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_ADDRESS, intent.getStringExtra(BleService.EXTRA_ADDRESS));
                    writableMap.putString(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_DEVICE_NAME, intent.getStringExtra(BleService.EXTRA_NAME));
                    writableMap.putInt(EVENT_BLE_PERIPHERAL_SCANNED_PARAM_RSSI, intent.getIntExtra(BleService.EXTRA_RSSI, 0));

                    sendEvent(EVENT_BLE_PERIPHERAL_SCANNED, writableMap);
                    break;
                case BleService.ACTION_ERROR:
                    String peripheralId = intent.getStringExtra(BleService.EXTRA_PERIPHERAL_ID);
                    if (peripheralId != null) {
                        writableMap.putString(EVENT_COMMON_PERIPHERAL_ID, peripheralId);
                    }
                    writableMap.putString(EVENT_BLE_ERROR_PARAM_MESSAGE, intent.getStringExtra(BleService.EXTRA_MESSAGE));
                    sendEvent(EVENT_BLE_ERROR, writableMap);
                    break;
                default:
                    break;
            }
        }
    };

    private void registerBleReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_STATE_GOT);
        intentFilter.addAction(BleService.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BleService.ACTION_STATE_CHANGED);
        intentFilter.addAction(BleService.ACTION_BOND_STATE_GOT);
        intentFilter.addAction(BleService.ACTION_CONNECTED);
        intentFilter.addAction(BleService.ACTION_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_DESCRIPTOR_READ);
        intentFilter.addAction(BleService.ACTION_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_READ);
        intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_WRITTEN);
        intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_CHANGED);
        intentFilter.addAction(BleService.ACTION_PERIPHERAL_SCANNED);
        intentFilter.addAction(BleService.ACTION_ERROR);

        getReactApplicationContext().registerReceiver(mBleServiceReceiver, intentFilter);
    }

    private void unregisterBleReceiver() {
        getReactApplicationContext().unregisterReceiver(mBleServiceReceiver);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.w(TAG, "onServiceConnected");
            if (service != null) {
                mRemoteBleService = new Messenger(service);

                registerBleReceiver();
                Message message = Message.obtain(null, BleService.MSG_GET_STATE);
                try {
                    mRemoteBleService.send(message);
                } catch (RemoteException e) {
                    if (mPromise != null) {
                        mPromise.reject(e);
                        mPromise = null;
                    }
                }
            }
            else {
                if (mPromise != null) {
                    mPromise.resolve(BleState.STATE_BIND_FAILURE.toString());
                    mPromise = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.w(TAG, "onServiceDisconnected");
            unregisterBleReceiver();
            mRemoteBleService = null;
        }
    };

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getReactApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @ReactMethod
    public void init(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && getReactApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager bluetoothManager = (BluetoothManager) getReactApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bluetoothManager.getAdapter();
            if(adapter == null) {
                Log.w(TAG, "init-unsupported");
                promise.resolve(BleState.STATE_UNSUPPORTED.toString());
                return;
            }
            else {
                mPromise = promise;
            }
            Log.w(TAG, "init");
            if (!isServiceRunning(BleService.class)) {
                Log.w(TAG, "init-startService");
                getReactApplicationContext().startService(new Intent(getReactApplicationContext(), BleService.class));
            }
            Intent intent = new Intent(getReactApplicationContext(), BleService.class);
            getReactApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        }
        else {
            promise.resolve(BleState.STATE_UNSUPPORTED.toString());
        }
    }

    @ReactMethod
    public void destroy(boolean stopService) {
        if (stopService) {
            Log.w(TAG, "destroy-stopService");
            getReactApplicationContext().unbindService(mConnection);
            getReactApplicationContext().stopService(new Intent(getReactApplicationContext(), BleService.class));
        }
        else {
            Log.w(TAG, "destroy-unbindService");
            getReactApplicationContext().unbindService(mConnection);
        }
    }

    @ReactMethod
    public void set(Boolean enable) {
        Message message = Message.obtain(null, BleService.MSG_SET, enable ? 1 : 0, 0);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }


    @ReactMethod
    public void startScan(ReadableMap filter) {
        Bundle bundle = new Bundle();
        if(filter != null) {
            ReadableArray array = filter.getArray(PARAM_SCAN_UUIDS);
            ArrayList<String> arrayList = new ArrayList<>();
            for(int i = 0; i < array.size(); i ++) {
                arrayList.add(array.getString(i));
            }
            bundle.putStringArrayList(BleService.MSG_EXTRA_UUIDS, arrayList);
        }
        Message message = Message.obtain(null, BleService.MSG_START_SCAN, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void stopScan() {
        Message message = Message.obtain(null, BleService.MSG_STOP_SCAN);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void connect(ReadableMap param, ReadableMap options) {
        String id = param.getString(PARAM_CONNECT_ID);
        boolean autoConnect = (options != null) ? options.getBoolean(PARAM_AUTO_CONNECT) : false;

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_ID, id);
        bundle.putBoolean(BleService.MSG_EXTRA_AUTO_CONNECT, autoConnect);
        Message message = Message.obtain(null, BleService.MSG_CONNECT, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void createBond(ReadableMap param) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            onBleError("本设备版本不支持绑定外设");
            return;
        }
        String id = param.getString(PARAM_CONNECT_ID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_ID, id);
        Message message = Message.obtain(null, BleService.MSG_CONNECT_BOND, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void isBond(ReadableMap param, Promise promise) {
        String id = param.getString(PARAM_CONNECT_ID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_ID, id);
        Message message = Message.obtain(null, BleService.MSG_IS_BOND, bundle);
        try {
            mPromise = promise;
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void removeBond(ReadableMap param) {
        String id = param.getString(PARAM_CONNECT_ID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_ID, id);
        Message message = Message.obtain(null, BleService.MSG_REMOVE_BOND, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void disconnect(ReadableMap param) {
        String id = param.getString(PARAM_CONNECT_ID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_ID, id);
        Message message = Message.obtain(null, BleService.MSG_DISCONNECT, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void readDescriptor(ReadableMap param) {
        String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
        String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
        String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
        String descriptorUuid = param.getString(PARAM_COMMON_DESCRIPTOR_UUID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_PERIPHERAL_ID, peripheralId);
        bundle.putString(BleService.MSG_EXTRA_SERVICE_UUID, serviceUuId);
        bundle.putString(BleService.MSG_EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        bundle.putString(BleService.MSG_EXTRA_DESCRIPTOR_UUID, descriptorUuid);
        Message message = Message.obtain(null, BleService.MSG_READ_DESCRIPTOR, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void writeDescriptor(ReadableMap param) {
        String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
        String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
        String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
        String descriptorUuid = param.getString(PARAM_COMMON_DESCRIPTOR_UUID);
        byte[] value = decodeBleReadableArray(param.getArray(PARAM_COMMON_VALUE));

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_PERIPHERAL_ID, peripheralId);
        bundle.putString(BleService.MSG_EXTRA_SERVICE_UUID, serviceUuId);
        bundle.putString(BleService.MSG_EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        bundle.putString(BleService.MSG_EXTRA_DESCRIPTOR_UUID, descriptorUuid);
        bundle.putByteArray(BleService.MSG_EXTRA_VALUE, value);
        Message message = Message.obtain(null, BleService.MSG_WRITE_DESCRIPTOR, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void setCharacteristicNotification(ReadableMap param) {
        String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
        String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
        String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
        Boolean enable = param.getBoolean(PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_PERIPHERAL_ID, peripheralId);
        bundle.putString(BleService.MSG_EXTRA_SERVICE_UUID, serviceUuId);
        bundle.putString(BleService.MSG_EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        bundle.putBoolean(BleService.MSG_EXTRA_VALUE, enable);
        Message message = Message.obtain(null, BleService.MSG_SET_CHARACTERISTIC_NOTIFICATION, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    @ReactMethod
    public void readCharacteristic(ReadableMap param){
        String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
        String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
        String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_PERIPHERAL_ID, peripheralId);
        bundle.putString(BleService.MSG_EXTRA_SERVICE_UUID, serviceUuId);
        bundle.putString(BleService.MSG_EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        Message message = Message.obtain(null, BleService.MSG_READ_CHARACTERISTIC, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }


    @ReactMethod
    public void writeCharacteristic(ReadableMap param) {
        String peripheralId = param.getString(PARAM_COMMON_PERIPHERAL_ID);
        String serviceUuId = param.getString(PARAM_COMMON_SERVICE_UUID);
        String characteristicUuid = param.getString(PARAM_COMMON_CHARACTERISTIC_UUID);
        ReadableArray valueArray = param.getArray(PARAM_COMMON_VALUE);
        byte[] value = decodeBleReadableArray(valueArray);

        Bundle bundle = new Bundle();
        bundle.putString(BleService.MSG_EXTRA_PERIPHERAL_ID, peripheralId);
        bundle.putString(BleService.MSG_EXTRA_SERVICE_UUID, serviceUuId);
        bundle.putString(BleService.MSG_EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        bundle.putByteArray(BleService.MSG_EXTRA_VALUE, value);
        Message message = Message.obtain(null, BleService.MSG_WRITE_CHARACTERISTIC, bundle);
        try {
            mRemoteBleService.send(message);
        } catch (RemoteException e) {
            onBleError(e.getMessage());
        }
    }

    private void sendEvent(String eventName,
                           @Nullable WritableMap params) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    public static WritableMap constructPeripheralMap(Peripheral peripheral) {
        WritableMap peripheralMap = Arguments.createMap();
        peripheralMap.putString(EVENT_COMMON_ID, peripheral.id);
        peripheralMap.putString(EVENT_SERVICES_DISCOVERED_PARAM_ADDRESS, peripheral.address);
        peripheralMap.putString(EVENT_SERVICES_DISCOVERED_PARAM_DEVICE_NAME, peripheral.name);

        WritableArray serviceArray = Arguments.createArray();
        Iterator<Service> iterator = peripheral.services.iterator();
        while (iterator.hasNext()) {
            serviceArray.pushMap(constructServiceMap(iterator.next()));
        }
        peripheralMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_SERVICES, serviceArray);

        return peripheralMap;
    }

    private static WritableMap constructServiceMap(Service service) {
        WritableMap serviceMap = Arguments.createMap();
        serviceMap.putString(EVENT_COMMON_UUID, service.uuid);
        serviceMap.putInt(EVENT_SERVICES_DISCOVERED_PARAM_INSTANCE_ID, service.instanceId);
        serviceMap.putString(EVENT_SERVICES_DISCOVERED_PARAM_TYPE, BleConstantsConverter.getServiceType(service.type));

        WritableArray includedServiceArray = Arguments.createArray();
        Iterator<Service> iterator = service.includedServices.iterator();
        while (iterator.hasNext()) {
            includedServiceArray.pushMap(constructServiceMap(iterator.next()));
        }
        serviceMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_INCLUDED_SERVICES, includedServiceArray);

        WritableArray characteristicArray = Arguments.createArray();
        Iterator<Characteristic> iterator2 = service.characteristics.iterator();
        while (iterator2.hasNext()) {
            characteristicArray.pushMap(constructCharacteristicMap(iterator2.next()));
        }
        serviceMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTICS, characteristicArray);

        return serviceMap;
    }

    private static WritableMap constructCharacteristicMap(Characteristic characteristic) {
        WritableMap characteristicMap = Arguments.createMap();
        characteristicMap.putString(EVENT_COMMON_UUID, characteristic.uuid);
        characteristicMap.putInt(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_INSTANCE_ID, characteristic.instanceId);
        characteristicMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PERMISSIONS, BleConstantsConverter.getCharacteristicPermissions(characteristic.permissions));

        characteristicMap.putString(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_WRITE_TYPE, BleConstantsConverter.getCharacteristicWriteType(characteristic.writeType));
        characteristicMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_PROPERTIES, BleConstantsConverter.getCharacteristicProperties(characteristic.properties));
        characteristicMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(characteristic.value));

        WritableArray descriptorArray = Arguments.createArray();
        Iterator<Descriptor> iterator = characteristic.descriptors.iterator();
        while (iterator.hasNext()) {
            descriptorArray.pushMap(constructDescriptorMap(iterator.next()));
        }
        characteristicMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_CHARACTERISTIC_DESCRIPTORS, descriptorArray);

        return characteristicMap;
    }

    private static WritableMap constructDescriptorMap(Descriptor descriptor) {
        WritableMap descriptorMap = Arguments.createMap();
        descriptorMap.putString(EVENT_COMMON_UUID, descriptor.uuid);
        descriptorMap.putArray(EVENT_SERVICES_DISCOVERED_PARAM_DESCRIPTOR_PERMISSIONS, BleConstantsConverter.getDescriptorPermissions(descriptor.permissions));
        descriptorMap.putArray(EVENT_COMMON_VALUE, constructBleByteArray(descriptor.value));

        return descriptorMap;
    }

    private static WritableArray constructBleByteArray(byte[] value) {
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

    private static String bytesToString(byte[] b) {
        char[] c = new char[b.length];
        for(int i = 0; i < b.length; i ++) {
            c[i] = (char)b[i];
        }
        return new String(c);
    }


    private void onBleError(String message) {
        WritableMap param = Arguments.createMap();

        param.putString(EVENT_BLE_ERROR_PARAM_MESSAGE, message);

        sendEvent(EVENT_BLE_ERROR, param);
    }
}
