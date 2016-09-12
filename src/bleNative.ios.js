/**
 * Created by Administrator on 2016/3/2.
 */
'use strict';

var debug = require('debug')('native-ble');

const EventEmitter = require('events');


import {
    NativeModules,
    DeviceEventEmitter
} from "react-native";


const BleNativeIOS = NativeModules.BleNative;



class BleNative extends EventEmitter{

    constructor() {
        super();
        this.onStateChanged = this.onStateChanged.bind(this);
        this.onPeripheralScanned = this.onPeripheralScanned.bind(this);
        this.onPeripheralConnected = this.onPeripheralConnected.bind(this);
        this.onPeripheralDisconnected = this.onPeripheralDisconnected.bind(this);

        this.onPeripheralFailToConnect = this.onPeripheralFailToConnect.bind(this);
        this.onServicesDiscovered = this.onServicesDiscovered.bind(this);
        this.onCharacteristicsDiscovered = this.onCharacteristicsDiscovered.bind(this);
        this.onDescriptorsDiscovered = this.onDescriptorsDiscovered.bind(this);
        this.onFinishDiscover = this.onFinishDiscover.bind(this);

        this.onCharacteristicChanged = this.onCharacteristicChanged.bind(this);
        this.onCharacteristicRead = this.onCharacteristicRead.bind(this);
        this.onCharacteristicWritten = this.onCharacteristicWritten.bind(this);

        this.onDescriptorRead = this.onDescriptorRead.bind(this);
        this.onDescriptorChanged = this.onDescriptorChanged.bind(this);
        this.onDescriptorWritten = this.onDescriptorWritten.bind(this);

        this.onBleError = this.onBleError.bind(this);
    }

    get STATE_UNSUPPORTED() {
        return BleNativeIOS.STATE_UNSUPPORTED;
    }

    get STATE_UNAUTHORIZED() {
        return BleNativeIOS.STATE_UNAUTHORIZED;
    }

    get STATE_OFF() {
        return BleNativeIOS.STATE_OFF;
    }

    get STATE_ON() {
        return BleNativeIOS.STATE_ON;
    }

    get STATE_RESETTING() {
        return BleNativeIOS.STATE_RESETTING;
    }

    get STATE_UNKNOWN() {
        return BleNativeIOS.STATE_UNKNOWN;
    }

    //tips5:未使用这些方法
    // get DISABLE_NOTIFICATION_VALUE() {
    // }
    //
    // get ENABLE_INDICATION_VALUE() {
    // }
    //
    // get ENABLE_NOTIFICATION_VALUE() {
    // }

    init() {
        DeviceEventEmitter.addListener('bleStateChanged', this.onStateChanged);
        DeviceEventEmitter.addListener('blePeripheralScanned', this.onPeripheralScanned);
        DeviceEventEmitter.addListener('blePeripheralConnected', this.onPeripheralConnected);
        DeviceEventEmitter.addListener('blePeripheralDisconnected',this.onPeripheralDisconnected);

        //tips6:多出来的事件
        DeviceEventEmitter.addListener('blePeripheralFailToConnect',this.onPeripheralFailToConnect);
        DeviceEventEmitter.addListener('bleServicesDiscovered', this.onServicesDiscovered);
        DeviceEventEmitter.addListener('bleIncludedServicesDiscovered',this.onIncludedServicesDiscovered);
        DeviceEventEmitter.addListener('bleCharacteristicsDiscovered',this.onCharacteristicsDiscovered);
        DeviceEventEmitter.addListener('bleDescriptorsDiscovered', this.onDescriptorsDiscovered);
        DeviceEventEmitter.addListener('bleFinishDiscover', this.onFinishDiscover);

        DeviceEventEmitter.addListener('bleCharacteristicChanged', this.onCharacteristicChanged);
        DeviceEventEmitter.addListener('bleCharacteristicRead', this.onCharacteristicRead);
        DeviceEventEmitter.addListener('bleCharacteristicWritten', this.onCharacteristicWritten);

        DeviceEventEmitter.addListener('bleDescriptorChanged', this.onDescriptorChanged);
        DeviceEventEmitter.addListener('bleDescriptorRead', this.onDescriptorRead);
        DeviceEventEmitter.addListener('bleDescriptorWritten', this.onDescriptorWritten);

        DeviceEventEmitter.addListener('bleError', this.onBleError);

        BleNativeIOS.setup();
    }

    destroy() {
        DeviceEventEmitter.removeAllListeners('bleStateChanged');
        DeviceEventEmitter.removeAllListeners('blePeripheralScanned');
        DeviceEventEmitter.removeAllListeners('blePeripheralConnected');
        DeviceEventEmitter.removeAllListeners('blePeripheralDisconnected');

        //tips6:多出来的事件
        DeviceEventEmitter.removeAllListeners('blePeripheralFailToConnect');
        DeviceEventEmitter.removeAllListeners('bleServicesDiscovered');
        DeviceEventEmitter.removeAllListeners('bleIncludedServicesDiscovered');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicsDiscovered');
        DeviceEventEmitter.removeAllListeners('bleDescriptorsDiscovered');
        DeviceEventEmitter.removeAllListeners('bleFinishDiscover');

        DeviceEventEmitter.removeAllListeners('bleCharacteristicChanged');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicRead');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicWritten');

        DeviceEventEmitter.removeAllListeners('bleDescriptorChanged');
        DeviceEventEmitter.removeAllListeners('bleDescriptorRead');
        DeviceEventEmitter.removeAllListeners('bleDescriptorWritten');

        DeviceEventEmitter.removeAllListeners('bleError');
        return;
    }

    /**
     *
     * emit  event: "stateChanged"
     * emit  data:  state(number):
     *              bleNative.STATE_UNSUPPORTED,
     *              bleNative.STATE_UNAUTHORIZED,
     *              bleNative.STATE_OFF,
     *              bleNative.STATE_ON,
     *              bleNative.STATE_RESETTING
     *              bleNative.STATE_UNKNOWN
     */
    onStateChanged(e) {
        this._state = e.state;
        this.emit('stateChanged', e.state);
    }


    /**
     *
     * emit  event: "peripheralScanned"
     * emit  data:  (object):
     *              {
     *                  id: "",            //peripheralId
     *                  deviceName: "bleHealth",
     *                  rssi: 92
     *               }
     */
    onPeripheralScanned(e) {
        this.emit('peripheralScanned', e);
    }

    /**
     *
     * emit  event: "peripheralConnected"
     * emit  data:  (object):
     *              {
     *                  id: "",           //peripheralId
     *                  deviceName: "bleHealth",
     *               }
     */
    onPeripheralConnected(e) {
        this.emit('peripheralConnected', e);
    }

    /**
     *
     * emit  event: "peripheralFailToConnect"
     * emit  data:  (object):
     *              {
     *                  id: ""           //peripheralId
     *               }
     */
    onPeripheralFailToConnect(e) {
        this.emit('peripheralFailToConnect',e);
    }

    /**
     *
     * emit  event: "peripheralDisconnected"
     * emit  data:  (object):
     *              {
     *                  id: ""           //peripheralId
     *               }
     */
    onPeripheralDisconnected(e) {
        this.emit('peripheralDisconnected', e);
    }

    /**
     *
     * emit  event: "servicesDiscovered"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  deviceName: "bleHealth",
     *                  services: [
     *                      {
     *                          uuid: "",               // serviceUuid
     *                          type: "PRIMARY",        // PRIMARY/SECONDARY
     *                       }
     *                   ]
     *               }
     */
    onServicesDiscovered(e) {
        this.emit('servicesDiscovered', e);
    }

    /**
     *
     * emit  event: "includedServicesDiscovered"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  services: [
     *                      {
     *                          uuid: "",               // serviceUuid
     *                          type: "PRIMARY",        // PRIMARY/SECONDARY
     *                       }
     *                   ]
     *               }
     */
    onIncludedServicesDiscovered(e) {
        this.emit('includedServicesDiscovered',e);
    }

    /**
     *
     * emit  event: "characteristicsDiscovered"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristics: [
     *                      {
     *                          uuid: "",              // characteristicUuid
     *                          properties: [],        // property_broadcast/property_read/property_write_no_response/property_write
     *                                                 // property_notify/property_indicate/property_signed_write/property_extended_props
     *                                                 // property_notify_encryption_required/property_indicate_encryption_required
     *                          isNotifying: "true"    //true/false
     *                          value: 0
     *                       }
     *                   ]
     *               }
     */
    onCharacteristicsDiscovered(e) {
        this.emit('characteristicsDiscovered', e);
    }

    /**
     *
     * emit  event: "descriptorsDiscovered"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptors: [
     *                      {
     *                          uuid: "",              //descriptorUuid
     *                          value: 0
     *                       }
     *                   ]
     *               }
     */
    onDescriptorsDiscovered(e) {
        this.emit('descriptorsDiscovered', e);
    }

    /**
     * emit  event: "finishDiscover"
     * emit  data:  (object):
     *              {
     *                  id: "",                    //peripheralId
     *                  deviceName: "bleHealth",
     *                  services: [
     *                      {
     *                          uuid: "",          //serviceId
     *                          type:  "PRIMARY",  //PRIMARY/SECONDARY
     *                          includedServices: [
     *                              {
     *                                  // service object
     *                              }
     *                          ],
     *                          characteristics: [
     *                              {
     *                                  uuid: "",   //characteristicId
     *                                  properties: [
     *                                      "property_broadcast",
     *                                      "property_read",
     *                                      "property_write_no_response",
     *                                      "property_write",
     *                                      "property_notify",
     *                                      "property_indicate",
     *                                      "property_signed_write",
     *                                      "property_extended_props"
     *                                  ],
     *                                  value: [0,1,2],
     *                                  descriptors: [
     *                                      {
     *                                          uuid: "",   //descriptorId
     *                                          value: [1, 2, 3]
     *                                      }
     *                                  ]
     *                              }
     *                          ]
     *                       }
     *                   ]
     *               }
     */
    onFinishDiscover(e) {
        this.emit('finishDiscover',e);
    }

    /**
     *
     * emit  event: "characteristicRead"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  value: [1,2,3]
     *               }
     */
    onCharacteristicRead(e) {
        this.emit('characteristicRead', e);
    }

    /**
     *
     * emit  event: "characteristicChanged"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  value: [1,2,3]
     *               }
     */
    onCharacteristicChanged(e) {
        this.emit('characteristicChanged', e)
    }

    /**
     *
     * emit  event: "characteristicWritten"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: ""
     *               }
     */
    onCharacteristicWritten(e) {
        this.emit('characteristicWritten', e);
    }

    /**
     *
     * emit  event: "descriptorRead"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptorUuid: "",
     *                  value: [1,2,3]
     *               }
     */
    onDescriptorRead(e) {
        this.emit('descriptorRead', e);
    }

    /**
     *
     * emit  event: "descriptorChanged"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptorUuid: "",
     *                  value: [1,2,3]
     *               }
     */
    onDescriptorChanged(e) {
        this.emit('descriptorChanged', e);
    }

    /**
     *
     * emit  event: "descriptorWritten"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "" ,
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptorUuid: ""
     *               }
     */
    onDescriptorWritten(e) {
        this.emit('descriptorWritten', e);
    }

    /**
     *
     * emit  event: "error"
     * emit  data:  (object):
     *              {
     *                  peripheralId: ""
     *                  message: "Reading is not permitted"
     *               }
     */
    onBleError(e) {
        this.emit('error', e);
    }

    //tips2:打开蓝牙,ios需要用户手动打开
    setAdapter(enabled, onError) {
        throw new Error("ios does not support to this.");
    }
    
    startScan(filters) {
        if(this._state !== BleNativeIOS.STATE_ON) {
            throw new Error("bleNative is not in STATE_ON, current state is" + this._state);
        }
        BleNativeIOS.startScan(filters);
    }

    stopScan() {
        if(this._state !== BleNativeIOS.STATE_ON) {
            throw new Error("bleNative is not in STATE_ON, current state is" + this._state);
        }
        BleNativeIOS.stopScan();
    }

    //options是一个字典,key:isAutomaticDiscovering value:true/false
    connect(peripheral, onError, options) {
        //todo判断参数是否为空
        if(peripheral.id !== null){
            BleNativeIOS.connect({id: peripheral.id,
                options:options
            });
        } else {
            throw new Error("参数不能为空");
        }
    }

    disconnect(peripheral, onError) {
        BleNativeIOS.disconnect(peripheral);
    }
    //tips4:安卓不提供以下接口 ios在调用这些接口时,如果发生error,有专门的事件来处理
    discoverServices(peripheralId) {
        BleNativeIOS.discoverServices({peripheralId});
    }

    discoverIncludedServices(peripheralId, serviceUuid) {
        BleNativeIOS.discoverIncludedServices({
            peripheralId,
            serviceUuid
        });
    }

    discoverCharacteristics(peripheralId, serviceUuid) {
        BleNativeIOS.discoverCharacteristics({
            peripheralId,
            serviceUuid
        });
    }

    discoverDescriptors(peripheralId, serviceUuid, characteristicUuid) {
        BleNativeIOS.discoverCharacteristics({
            peripheralId,
            serviceUuid,
            characteristicUuid
        });
    }
    //tips4:安卓不提供以上接口
    //options是一个字典,key:enable value:true/false
    setCharacteristicNotification(peripheralId, serviceUuid, characteristicUuid, onError, options) {
        BleNativeIOS.setCharacteristicNotification({
            peripheralId,
            serviceUuid,
            characteristicUuid,
            options
        });
    }

    readCharacteristic(peripheralId, serviceUuid, characteristicUuid, onError) {
        BleNativeIOS.readCharacteristic({
                peripheralId,
                serviceUuid,
                characteristicUuid
        });
    }
    //options是一个字典,key:type value:0/1(0:CBCharacteristicWriteWithResponse,1:CBCharacteristicWriteWithoutResponse)
    writeCharacteristic(peripheralId, serviceUuid, characteristicUuid, value,  onError, options) {
        BleNativeIOS.writeCharacteristic({
                peripheralId,
                serviceUuid,
                characteristicUuid,
                value,
                options
            }
        );
    }

    readDescriptor(peripheralId, serviceUuid, characteristicUuid, descriptorUuid,  onError) {
        BleNativeIOS.readDescriptor({
            peripheralId,
            serviceUuid,
            characteristicUuid,
            descriptorUuid
        });
    }

    writeDescriptor(peripheralId, serviceUuid, characteristicUuid, descriptorUuid, value, onError) {
        BleNativeIOS.writeDescriptor({
            peripheralId,
            serviceUuid,
            characteristicUuid,
            descriptorUuid,
            value
        });
    }
}

const bleNative = new BleNative();
module.exports = bleNative;
