package com.martianLife.bleNative;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.*;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.martianLife.bleNative.result.Characteristic;
import com.martianLife.bleNative.result.Descriptor;
import com.martianLife.bleNative.result.Peripheral;
import com.martianLife.bleNative.util.BleAdvertisedData;
import com.martianLife.bleNative.util.BleConstantsConverter;
import com.martianLife.bleNative.util.BleUtil;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Administrator on 2017/2/13.
 */
@TargetApi(18)
public class BleService extends Service {
    private static final int MAX_PACKET_LENGTH = 20;

    public static final int MSG_SET = 1;
    public static final int MSG_START_SCAN = 2;
    public static final int MSG_STOP_SCAN = 3;
    public static final int MSG_CONNECT = 4;
    public static final int MSG_CONNECT_BOND = 5;
    public static final int MSG_IS_BOND = 6;
    public static final int MSG_REMOVE_BOND = 7;
    public static final int MSG_READ_DESCRIPTOR = 8;
    public static final int MSG_WRITE_DESCRIPTOR = 9;
    public static final int MSG_SET_CHARACTERISTIC_NOTIFICATION = 10;
    public static final int MSG_READ_CHARACTERISTIC = 11;
    public static final int MSG_WRITE_CHARACTERISTIC = 12;
    public static final int MSG_DISCONNECT = 13;
    public static final int MSG_GET_STATE = 14;

    public static final String MSG_EXTRA_UUIDS = "uuids";
    public static final String MSG_EXTRA_ID = "id";
    public static final String MSG_EXTRA_AUTO_CONNECT = "autoConnect";
    public static final String MSG_EXTRA_PERIPHERAL_ID = "peripheralId";
    public static final String MSG_EXTRA_SERVICE_UUID = "serviceUuid";
    public static final String MSG_EXTRA_CHARACTERISTIC_UUID = "characteristicUuid";
    public static final String MSG_EXTRA_DESCRIPTOR_UUID = "descriptorUuid";
    public static final String MSG_EXTRA_VALUE = "value";

    public static final String ACTION_STATE_GOT = "com.martianLife.bleNative.BleService.ACTION_STATE_GOT";
    public static final String ACTION_STATE_CHANGED = "com.martianLife.bleNative.BleService.ACTION_STATE_CHANGED";
    public static final String ACTION_BOND_STATE_CHANGED = "com.martianLife.bleNative.BleService.ACTION_BOND_STATE_CHANGED";
    public static final String ACTION_BOND_STATE_GOT = "com.martianLife.bleNative.BleService.ACTION_BOND_STATE_GOT";
    public static final String ACTION_CONNECTED = "com.martianLife.bleNative.BleService.ACTION_CONNECTED";
    public static final String ACTION_SERVICES_DISCOVERED = "com.martianLife.bleNative.BleService.ACTION_SERVICES_DISCOVERED";
    public static final String ACTION_DISCONNECTED = "com.martianLife.bleNative.BleService.ACTION_DISCONNECTED";
    public static final String ACTION_DESCRIPTOR_READ ="com.martianLife.bleNative.BleService.ACTION_DESCRIPTOR_READ";
    public static final String ACTION_DESCRIPTOR_WRITTEN ="com.martianLife.bleNative.BleService.ACTION_DESCRIPTOR_WRITTEN";
    public static final String ACTION_CHARACTERISTIC_READ ="com.martianLife.bleNative.BleService.ACTION_CHARACTERISTIC_READ";
    public static final String ACTION_CHARACTERISTIC_WRITTEN = "com.martianLife.bleNative.BleService.ACTION_CHARACTERISTIC_WRITTEN";
    public static final String ACTION_CHARACTERISTIC_CHANGED = "com.martianLife.bleNative.BleService.ACTION_CHARACTERISTIC_CHANGED";
    public static final String ACTION_PERIPHERAL_SCANNED = "com.martianLife.bleNative.BleService.ACTION_PERIPHERAL_SCANNED";
    public static final String ACTION_ERROR = "com.martianLife.bleNative.BleService.ACTION_ERROR";

    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_RSSI = "rssi";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_PERIPHERAL_ID = "peripheralId";
    public static final String EXTRA_SERVICE_UUID = "serviceUuid";
    public static final String EXTRA_CHARACTERISTIC_UUID = "characteristicUuid";
    public static final String EXTRA_DESCRIPTOR_UUID = "descriptorUuid";
    public static final String EXTRA_VALUE = "value";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_EVENT = "event";


    private static final String TAG = BleService.class.getSimpleName();

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private NotificationManagerCompat mNotificationManager;

    private int mBindedCount = 0;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mNotificationManager = NotificationManagerCompat.from(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mAdapter = bluetoothManager.getAdapter();
            if(mAdapter != null) {
                // 注册蓝牙状态监听
                // 增加蓝牙bond状态监听
                IntentFilter intent = new IntentFilter();
                intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mBleStateReceiver, intent);
            }
        }
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        unregisterReceiver(mBleStateReceiver);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "onBind");
        if (mBindedCount == 0) {
            mBindedCount ++;
            return mMessenger.getBinder();
        }
        else {
            Log.e(TAG, "同时只能绑定一个上层对象");
            return null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.w(TAG, "onUnbind");
        assert (mBindedCount == 1);
        mBindedCount --;
        // disable rebind
        return super.onUnbind(intent);
    }



    private static class IncomingHandler extends Handler {
        private final WeakReference<BleService> mReference;

        IncomingHandler(BleService service) {
            mReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BleService service = mReference.get();
            switch (msg.what) {
                case MSG_SET:
                    Boolean enable = (msg.arg1 == 1 ? true : false);
                    service.set(enable);
                    break;
                case MSG_CONNECT:
                    Bundle bundle = (Bundle) msg.obj;
                    service.connect(bundle);
                    break;
                case MSG_START_SCAN:
                    bundle = (Bundle) msg.obj;
                    service.startScan(bundle);
                    break;
                case MSG_STOP_SCAN:
                    service.stopScan();
                    break;
                case MSG_CONNECT_BOND:
                    bundle = (Bundle) msg.obj;
                    service.createBond(bundle);
                    break;
                case MSG_IS_BOND:
                    bundle = (Bundle) msg.obj;
                    Messenger messenger = msg.replyTo;
                    service.isBond(bundle);
                    break;
                case MSG_GET_STATE:;
                    service.getState();
                    break;
                case MSG_REMOVE_BOND:
                    bundle = (Bundle) msg.obj;
                    service.removeBond(bundle);
                    break;
                case MSG_DISCONNECT:
                    bundle = (Bundle) msg.obj;
                    service.disconnect(bundle);
                    break;
                case MSG_READ_DESCRIPTOR:
                    bundle = (Bundle) msg.obj;
                    service.readDescriptor(bundle);
                    break;
                case MSG_WRITE_DESCRIPTOR:
                    bundle = (Bundle) msg.obj;
                    service.writeDescriptor(bundle);
                    break;
                case MSG_SET_CHARACTERISTIC_NOTIFICATION:
                    bundle = (Bundle) msg.obj;
                    service.setCharacteristicNotification(bundle);
                    break;
                case MSG_READ_CHARACTERISTIC:
                    bundle = (Bundle) msg.obj;
                    service.readCharacteristic(bundle);
                    break;
                case MSG_WRITE_CHARACTERISTIC:
                    bundle = (Bundle) msg.obj;
                    service.writeCharacteristic(bundle);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    //////////////////////////////////////////////////////////////////

    private Map<String, BluetoothGatt> mGattMaps = new HashMap<>();
    private BluetoothAdapter mAdapter;
    private class LongValueWriter {
        public byte[] writingValue;
        public int writingCursor;
        public String writingPeripheralId;
        public String writingServiceUuid;
        public String writingCharacteristicUuid;
        public int writingSize;
    }
    private Map<String, LongValueWriter> mlvwMap = new HashMap<String, LongValueWriter>();

    private BroadcastReceiver mBleStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Intent intent2 = new Intent(ACTION_BOND_STATE_CHANGED);
                intent2.putExtra(EXTRA_ADDRESS, device.getAddress());
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.i("BleService", "正在配对......");
                        intent2.putExtra(EXTRA_STATE, BondState.BONDING);
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.i("BleService", "完成配对");
                        intent2.putExtra(EXTRA_STATE, BondState.BONDED);
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.i("BleService", "取消配对");
                        intent2.putExtra(EXTRA_STATE, BondState.UNBONDED);
                    default:
                        break;
                }
                sendBroadCastOrHeadlessTask(intent2);
            }
            else {
                assert (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action));
                int bleState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Intent intent2 = new Intent(ACTION_STATE_CHANGED);
                switch (bleState) {
                    case BluetoothAdapter.STATE_OFF:
                        intent2.putExtra(EXTRA_STATE, BleState.STATE_OFF.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        intent2.putExtra(EXTRA_STATE, BleState.STATE_TURNING_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_ON:
                        intent2.putExtra(EXTRA_STATE, BleState.STATE_ON.toString());
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        intent2.putExtra(EXTRA_STATE, BleState.STATE_TURNING_OFF.toString());
                        break;
                    default:
                        break;
                }
                sendBroadCastOrHeadlessTask(intent2);
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

                    Intent intent = new Intent(ACTION_CONNECTED);
                    intent.putExtra(EXTRA_ID, gatt.getDevice().getAddress());
                    sendBroadCastOrHeadlessTask(intent);

                    // 将这个gatt放入全局的map中
                    mGattMaps.put(gatt.getDevice().getAddress(), gatt);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // 断连时要把这个设备的longValueWriter清空
                    Iterator<LongValueWriter> iterator = BleService.this.mlvwMap.values().iterator();
                    while (iterator.hasNext()) {
                        LongValueWriter writer = iterator.next();
                        if (writer.writingPeripheralId.equals(getPeripheralId(gatt))) {
                            BleService.this.mlvwMap.remove(writer.writingCharacteristicUuid.concat(writer.writingPeripheralId));
                        }
                    }

                    Intent intent = new Intent(ACTION_DISCONNECTED);
                    intent.putExtra(EXTRA_ID, gatt.getDevice().getAddress());
                    sendBroadCastOrHeadlessTask(intent);

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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Peripheral peripheral = new Peripheral();
                peripheral.name = gatt.getDevice().getName();
                peripheral.address = gatt.getDevice().getAddress();
                peripheral.id = gatt.getDevice().getAddress();
                peripheral.services = new ArrayList<>();

                List<BluetoothGattService> services = gatt.getServices();
                Iterator<BluetoothGattService> iterator = services.iterator();
                while (iterator.hasNext()) {
                    BluetoothGattService service = iterator.next();

                    peripheral.services.add(constructBleService(service));
                }

                Intent intent = new Intent(ACTION_SERVICES_DISCOVERED);
                intent.putExtra(EXTRA_VALUE, peripheral);

                sendBroadCastOrHeadlessTask(intent);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);

                gatt.disconnect();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_DESCRIPTOR_READ);
                intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
                intent.putExtra(EXTRA_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                intent.putExtra(EXTRA_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
                intent.putExtra(EXTRA_DESCRIPTOR_UUID, descriptor.getUuid().toString());
                intent.putExtra(EXTRA_VALUE, descriptor.getValue());

                sendBroadCastOrHeadlessTask(intent);
            }
            else {
                onBleError(gatt, new RuntimeException("onDescriptorRead errur, status : " + status));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_DESCRIPTOR_WRITTEN);
                intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
                intent.putExtra(EXTRA_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
                intent.putExtra(EXTRA_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
                intent.putExtra(EXTRA_DESCRIPTOR_UUID, descriptor.getUuid().toString());
                intent.putExtra(EXTRA_VALUE, descriptor.getValue());

                sendBroadCastOrHeadlessTask(intent);
            }
            else {
                onBleError(gatt, new RuntimeException("onDescriptorWrite errur, status : " + status));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_CHARACTERISTIC_READ);
                intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
                intent.putExtra(EXTRA_SERVICE_UUID, characteristic.getService().getUuid().toString());
                intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                intent.putExtra(EXTRA_VALUE, characteristic.getValue());

                sendBroadCastOrHeadlessTask(intent);
            }
            else {
                onBleError(gatt, new RuntimeException("onCharacteristicRead errur, status : " + status));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                Log.i(TAG, "onCharacteristicWrite in " + System.currentTimeMillis() + " characteristicUuid：" + characteristic.getUuid().toString());
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    LongValueWriter writer = BleService.this.mlvwMap.get(characteristic.getUuid().toString().concat(getPeripheralId(gatt)));
                    if (writer != null) {
                        writer.writingCursor += writer.writingSize;
                        if (writer.writingCursor < writer.writingValue.length) {
                            // 还没写完，继续写

                            int sizeToWrite = Math.min(MAX_PACKET_LENGTH, writer.writingValue.length - writer.writingCursor);
                            byte[] value2 = new byte[sizeToWrite];
                            System.arraycopy(writer.writingValue, writer.writingCursor, value2, 0, sizeToWrite);
                            writer.writingSize = sizeToWrite;

                            BleService.this.writeCharacteristicInner(writer.writingPeripheralId, writer.writingServiceUuid, writer.writingCharacteristicUuid, value2);
                            return;
                        }
                        else {
                            // 写完了，把这个结点删除之
                            BleService.this.mlvwMap.remove(writer.writingCharacteristicUuid.concat(writer.writingPeripheralId));
                        }
                    }
                    Intent intent = new Intent(ACTION_CHARACTERISTIC_WRITTEN);
                    intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
                    intent.putExtra(EXTRA_SERVICE_UUID, characteristic.getService().getUuid().toString());
                    intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                    intent.putExtra(EXTRA_VALUE, characteristic.getValue());

                    sendBroadCastOrHeadlessTask(intent);
                }
                else {
                    String error = "onCharacteristicWrite errur, status : " + status;
                    LongValueWriter writer = BleService.this.mlvwMap.get(characteristic.getUuid().toString().concat(getPeripheralId(gatt)));
                    if (writer != null) {
                        BleService.this.mlvwMap.remove(writer.writingCharacteristicUuid.concat(writer.writingPeripheralId));
                        onBleError(null, new Exception(error));
                    }
                    onBleError(gatt, new RuntimeException(error));
                }
            }
            catch (Exception e) {
                LongValueWriter writer = BleService.this.mlvwMap.get(characteristic.getUuid().toString().concat(getPeripheralId(gatt)));
                if (writer != null) {
                    BleService.this.mlvwMap.remove(writer.writingCharacteristicUuid.concat(writer.writingPeripheralId));
                    onBleError(null, e);
                }
                onBleError(gatt, e);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                Log.i(TAG, "onCharacteristicChanged in " + System.currentTimeMillis() + " characteristicUuid：" + characteristic.getUuid().toString());
                Intent intent = new Intent(ACTION_CHARACTERISTIC_CHANGED);
                intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
                intent.putExtra(EXTRA_SERVICE_UUID, characteristic.getService().getUuid().toString());
                intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                intent.putExtra(EXTRA_VALUE, characteristic.getValue());

                sendBroadCastOrHeadlessTask(intent);
            }
            catch (Exception e) {
                onBleError(gatt, e);
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

                        Intent intent = new Intent(ACTION_PERIPHERAL_SCANNED);
                        intent.putExtra(EXTRA_ID, device.getAddress());
                        intent.putExtra(EXTRA_ADDRESS, device.getAddress());
                        intent.putExtra(EXTRA_NAME, deviceName);
                        intent.putExtra(EXTRA_RSSI, rssi);
                        sendBroadCastOrHeadlessTask(intent);
                    }
                    catch (Exception e) {
                        onBleError(null, e);
                    }
                }
            };

    private void sendEvent(String eventName,
                           @Nullable WritableMap params){
    }

    public class Event {
        String name;

        WritableMap params;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public WritableMap getParams() {
            return params;
        }

        public void setParams(WritableMap params) {
            this.params = params;
        }
    }

    //////////////////////////////////////////////////////////////////////////////

    private com.martianLife.bleNative.result.Service constructBleService(BluetoothGattService service) {
        com.martianLife.bleNative.result.Service service1 = new com.martianLife.bleNative.result.Service();
        service1.instanceId = service.getInstanceId();
        service1.uuid = service.getUuid().toString();
        service1.type = service.getType();
        service1.includedServices = new ArrayList<>();
        service1.characteristics = new ArrayList<>();
        Iterator<BluetoothGattCharacteristic> iterator2 = service.getCharacteristics().iterator();
        while (iterator2.hasNext()) {
            BluetoothGattCharacteristic characteristic = iterator2.next();
            service1.characteristics.add(constructBleCharacteristic(characteristic));
        }

        Iterator<BluetoothGattService> iterator = service.getIncludedServices().iterator();
        while (iterator.hasNext()) {
            BluetoothGattService service2 = iterator.next();

            service1.includedServices.add(constructBleService(service2));
        }

        return service1;
    }

    private Characteristic constructBleCharacteristic(BluetoothGattCharacteristic characteristic) {
        Characteristic characteristic1 = new Characteristic();
        characteristic1.value = characteristic.getValue();
        characteristic1.properties = characteristic.getProperties();
        characteristic1.writeType = characteristic.getWriteType();
        characteristic1.instanceId = characteristic.getInstanceId();
        characteristic1.permissions = characteristic.getPermissions();
        characteristic1.uuid = characteristic.getUuid().toString();
        characteristic1.descriptors = new ArrayList<>();

        Iterator<BluetoothGattDescriptor> iterator = characteristic.getDescriptors().iterator();
        while (iterator.hasNext()) {
            BluetoothGattDescriptor descriptor = iterator.next();
            characteristic1.descriptors.add(constructBleDescriptor(descriptor));
        }

        return characteristic1;
    }

    private Descriptor constructBleDescriptor(BluetoothGattDescriptor descriptor) {
        Descriptor descriptor1 = new Descriptor();
        descriptor1.permissions = descriptor.getPermissions();
        descriptor1.uuid = descriptor.getUuid().toString();
        descriptor1.value = descriptor.getValue();

        return descriptor1;
    }

    private String getPeripheralId(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress();
    }


    private void onBleError(BluetoothGatt gatt, Exception e) {
        Intent intent = new Intent(ACTION_ERROR);
        if(gatt != null) {
            intent.putExtra(EXTRA_PERIPHERAL_ID, getPeripheralId(gatt));
        }
        intent.putExtra(EXTRA_MESSAGE, e.getMessage());

        sendBroadCastOrHeadlessTask(intent);
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private void set(Boolean enable) {
        if(enable) {
            if(!mAdapter.enable()) {
                onBleError(null, new Exception("mAdapter enable返回失败"));
            }
        }
        else {
            if(!mAdapter.disable()) {
                onBleError(null, new Exception("mAdapter disable返回失败"));
            }
        }
    }

    private void startScan(Bundle bundle) {
        String [] uuids = bundle.getStringArray(MSG_EXTRA_UUIDS);
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

    private void stopScan() {
        mAdapter.stopLeScan(mLeScanCallback);
    }

    private void connect(Bundle bundle) {
        String id = bundle.getString(MSG_EXTRA_ID);
        Boolean autoConnect = bundle.getBoolean(MSG_EXTRA_AUTO_CONNECT);

        BluetoothDevice device = mAdapter.getRemoteDevice(id);
        BluetoothGatt bluetoothGatt = device.connectGatt(this, autoConnect, mGattCallback);
        refreshDeviceCache(bluetoothGatt);
    }

    private void createBond(Bundle bundle) {
        String id = bundle.getString(MSG_EXTRA_ID);

        BluetoothDevice device = mAdapter.getRemoteDevice(id);
        if (!device.createBond()) {
            onBleError(null, new Exception("与设备绑定时发生错误"));
        }
    }

    private void isBond(Bundle bundle) {
        String id = bundle.getString(MSG_EXTRA_ID);

        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iterator = bondedDevices.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice device = iterator.next();
            if (device.getAddress().equals(id)) {
                Intent intent = new Intent(ACTION_BOND_STATE_GOT);
                intent.putExtra(EXTRA_STATE, true);
                sendBroadCastOrHeadlessTask(intent);
                return;
            }
        }
        Intent intent = new Intent(ACTION_BOND_STATE_GOT);
        intent.putExtra(EXTRA_STATE, false);
        sendBroadCastOrHeadlessTask(intent);
    }

    private void removeBond(Bundle bundle) {
        try {
            String id = bundle.getString(MSG_EXTRA_ID);

            Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
            Iterator<BluetoothDevice> iterator = bondedDevices.iterator();
            while (iterator.hasNext()) {
                BluetoothDevice device = iterator.next();
                if (device.getAddress().equals(id)) {
                    Method m = device.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(device, (Object[]) null);
                    return;
                }
            }
            return;
        }
        catch (Exception e) {
            onBleError(null, e);
        }
    }

    private void getState() {
        Intent intent = new Intent(ACTION_STATE_GOT);
        switch (mAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                intent.putExtra("state", BleState.STATE_OFF.toString());
                break;
            case BluetoothAdapter.STATE_ON:
                intent.putExtra("state", BleState.STATE_ON.toString());
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                intent.putExtra("state", BleState.STATE_TURNING_ON.toString());
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                intent.putExtra("state", BleState.STATE_TURNING_OFF.toString());
                break;
            default:
                assert(false);
                return;
        }

        sendBroadCastOrHeadlessTask(intent);
    }

    private void disconnect(Bundle bundle) {
        String id = bundle.getString(MSG_EXTRA_ID);

        BluetoothGatt gatt = mGattMaps.get(id);
        gatt.disconnect();
    }

    private void readDescriptor(Bundle bundle) {
        String peripheralId = bundle.getString(MSG_EXTRA_PERIPHERAL_ID);
        String serviceUuId = bundle.getString(MSG_EXTRA_SERVICE_UUID);
        String characteristicUuid = bundle.getString(MSG_EXTRA_CHARACTERISTIC_UUID);
        String descriptorUuid = bundle.getString(MSG_EXTRA_DESCRIPTOR_UUID);

        BluetoothGatt gatt = mGattMaps.get(peripheralId);
        if(gatt.readDescriptor(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)).getDescriptor(UUID.fromString(descriptorUuid))) == false) {
            onBleError(null, new Exception("readDescriptor returns false"));
        }
    }

    private void writeDescriptor(Bundle bundle) {
        String peripheralId = bundle.getString(MSG_EXTRA_PERIPHERAL_ID);
        String serviceUuId = bundle.getString(MSG_EXTRA_SERVICE_UUID);
        String characteristicUuid = bundle.getString(MSG_EXTRA_CHARACTERISTIC_UUID);
        String descriptorUuid = bundle.getString(MSG_EXTRA_DESCRIPTOR_UUID);
        byte[] value = bundle.getByteArray(MSG_EXTRA_VALUE);

        BluetoothGatt gatt = mGattMaps.get(peripheralId);
        BluetoothGattDescriptor descriptor = gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)).getDescriptor(UUID.fromString(descriptorUuid));
        if(!descriptor.setValue(value) || !gatt.writeDescriptor(descriptor)) {
            onBleError(null, new Exception("writeDescriptor returns false"));
        }
    }

    private void setCharacteristicNotification(Bundle bundle) {
        String peripheralId = bundle.getString(MSG_EXTRA_PERIPHERAL_ID);
        String serviceUuId = bundle.getString(MSG_EXTRA_SERVICE_UUID);
        String characteristicUuid = bundle.getString(MSG_EXTRA_CHARACTERISTIC_UUID);
        Boolean enable = bundle.getBoolean(MSG_EXTRA_VALUE);

        BluetoothGatt gatt = mGattMaps.get(peripheralId);
        if(gatt.setCharacteristicNotification(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid)), enable) == false) {
            onBleError(null, new Exception("setCharacteristicNotification returns false"));
        }
    }

    private void readCharacteristic(Bundle bundle){
        String peripheralId = bundle.getString(MSG_EXTRA_PERIPHERAL_ID);
        String serviceUuId = bundle.getString(MSG_EXTRA_SERVICE_UUID);
        String characteristicUuid = bundle.getString(MSG_EXTRA_CHARACTERISTIC_UUID);

        BluetoothGatt gatt = mGattMaps.get(peripheralId);
        if(gatt.readCharacteristic(gatt.getService(UUID.fromString(serviceUuId)).getCharacteristic(UUID.fromString(characteristicUuid))) == false) {
            onBleError(null, new Exception("readCharacteristic returns false"));
        }
    }

    private void writeCharacteristicInner(String peripheralId, String serviceUuid, String characteristicUuid, byte[] value) {
        BluetoothGatt gatt = mGattMaps.get(peripheralId);
        BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUuid)).getCharacteristic(UUID.fromString(characteristicUuid));

        Log.i(TAG, "writeCharacteristic in " + System.currentTimeMillis() + " characteristicUuid：" + characteristicUuid);
        if(!characteristic.setValue(value) || !gatt.writeCharacteristic(characteristic)) {
            onBleError(null, new Exception("writeCharacteristic returns false"));
        }
    }

    public void writeCharacteristic(Bundle bundle) {
        String peripheralId = bundle.getString(MSG_EXTRA_PERIPHERAL_ID);
        String serviceUuId = bundle.getString(MSG_EXTRA_SERVICE_UUID);
        String characteristicUuid = bundle.getString(MSG_EXTRA_CHARACTERISTIC_UUID);
        try {
            byte[] value = bundle.getByteArray(MSG_EXTRA_VALUE);

            byte[] value2;
            if (value.length > MAX_PACKET_LENGTH) {
                // 如果写的字节过大，则分批次写
                LongValueWriter writer = new LongValueWriter();

                writer.writingValue = value;
                writer.writingCursor = 0;
                writer.writingCharacteristicUuid = characteristicUuid;
                writer.writingPeripheralId = peripheralId;
                writer.writingServiceUuid = serviceUuId;

                // 根据characteristicUuid+peripheralId来查找，应该不可能重复吧
                if(this.mlvwMap.get(characteristicUuid.concat(peripheralId)) != null) {
                    onBleError(null, new Exception("a new writing must wait for the writing long value success"));
                    return;
                }
                this.mlvwMap.put(characteristicUuid.concat(peripheralId), writer);

                int sizeToWrite = Math.min(MAX_PACKET_LENGTH, writer.writingValue.length - writer.writingCursor);
                value2 = new byte[sizeToWrite];

                System.arraycopy(writer.writingValue, writer.writingCursor, value2, 0, sizeToWrite);
                writer.writingSize = sizeToWrite;
            }
            else {
                value2 = value;
            }
            writeCharacteristicInner(peripheralId, serviceUuId, characteristicUuid, value2);
        }
        catch (Exception e) {
            LongValueWriter writer = this.mlvwMap.get(characteristicUuid.concat(peripheralId));
            if (writer != null) {
                this.mlvwMap.remove(writer.writingCharacteristicUuid.concat(writer.writingPeripheralId));
            }
            onBleError(null, e);
        }
    }
    
    private void sendBroadCastOrHeadlessTask(Intent intent) {
        if (mBindedCount > 0) {
            sendBroadcast(intent);
        }
        else {
            // 在一些必要的点，发起headlessTask
            switch (intent.getAction()) {
                case ACTION_SERVICES_DISCOVERED:
                case ACTION_STATE_CHANGED:
                case ACTION_BOND_STATE_CHANGED:
                    Intent intent2 = new Intent(this, BleEventListener.class);
                    intent2.putExtras(intent);
                    intent2.putExtra(EXTRA_EVENT, intent.getAction());
                    startService(intent2);
                    break;
                default:
                    break;
            }
        }
    }
}
