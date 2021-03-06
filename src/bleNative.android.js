/**
 * Created by Administrator on 2016/3/2.
 */
'use strict'


const EventEmitter = require('events');

import {
    NativeModules,
    DeviceEventEmitter
} from "react-native";

const BleNativeAndroid = NativeModules.BleNative;


class BleNative extends EventEmitter{

    constructor() {
        super();
        this.onBondChanged = this.onBondChanged.bind(this);
        this.onStateChanged = this.onStateChanged.bind(this);
        this.onPeripheralScanned = this.onPeripheralScanned.bind(this);
        this.onPeripheralConnected = this.onPeripheralConnected.bind(this);
        this.onPeripheralDisconnected = this.onPeripheralDisconnected.bind(this);
        this.onServicesDiscovered = this.onServicesDiscovered.bind(this);
        this.onDescriptorRead = this.onDescriptorRead.bind(this);
        this.onDescriptorWritten = this.onDescriptorWritten.bind(this);
        this.onCharacteristicChanged = this.onCharacteristicChanged.bind(this);
        this.onCharacteristicRead = this.onCharacteristicRead.bind(this);
        this.onCharacteristicWritten = this.onCharacteristicWritten.bind(this);
        this.onBleError = this.onBleError.bind(this);
    }

    get STATE_ON() {
        return BleNativeAndroid.STATE_ON;
    }

    get STATE_OFF() {
        return BleNativeAndroid.STATE_OFF;
    }

    get STATE_TURNING_ON() {
        return BleNativeAndroid.STATE_TURNING_ON;
    }

    get STATE_TURNING_OFF() {
        return BleNativeAndroid.STATE_TURNING_OFF;
    }

    get STATE_UNSUPPORTED() {
        return BleNativeAndroid.STATE_UNSUPPORTED;
    }

    get DISABLE_NOTIFICATION_VALUE() {
        return BleNativeAndroid.DISABLE_NOTIFICATION_VALUE;
    }

    get ENABLE_INDICATION_VALUE() {
        return BleNativeAndroid.ENABLE_INDICATION_VALUE;
    }

    get ENABLE_NOTIFICATION_VALUE() {
        return BleNativeAndroid.ENABLE_NOTIFICATION_VALUE;
    }

    init() {
        DeviceEventEmitter.addListener('bondChanged', this.onBondChanged);
        DeviceEventEmitter.addListener('bleStateChanged', this.onStateChanged);
        DeviceEventEmitter.addListener('blePeripheralScanned', this.onPeripheralScanned);
        DeviceEventEmitter.addListener('blePeripheralConnected', this.onPeripheralConnected);
        DeviceEventEmitter.addListener('blePeripheralDisconnected', this.onPeripheralDisconnected);
        DeviceEventEmitter.addListener('bleServicesDiscovered', this.onServicesDiscovered);
        DeviceEventEmitter.addListener('bleDescriptorRead', this.onDescriptorRead);
        DeviceEventEmitter.addListener('bleDescriptorWritten', this.onDescriptorWritten);
        DeviceEventEmitter.addListener('bleCharacteristicRead', this.onCharacteristicRead);
        DeviceEventEmitter.addListener('bleCharacteristicWritten', this.onCharacteristicWritten);
        DeviceEventEmitter.addListener('bleCharacteristicChanged', this.onCharacteristicChanged);
        DeviceEventEmitter.addListener('bleError', this.onBleError);
        BleNativeAndroid.init().then(
            _state => {
                this.onStateChanged({state: _state})
            }
        );
    }


    destroy(stopService = true) {
        DeviceEventEmitter.removeAllListeners('bleStateChanged');
        DeviceEventEmitter.removeAllListeners('blePeripheralScanned');
        DeviceEventEmitter.removeAllListeners('blePeripheralConnected');
        DeviceEventEmitter.removeAllListeners('blePeripheralDisconnected');
        DeviceEventEmitter.removeAllListeners('bleServicesDiscovered');
        DeviceEventEmitter.removeAllListeners('bleDescriptorRead');
        DeviceEventEmitter.removeAllListeners('bleDescriptorWritten');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicRead');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicWritten');
        DeviceEventEmitter.removeAllListeners('bleCharacteristicChanged');
        DeviceEventEmitter.removeAllListeners('bleError');
        return BleNativeAndroid.destroy(stopService);
    }

    /**
     *
     * emit  event: "bondChanged"
     * emit  (object){
     *          id: "AA:BB:CC:DD:EE:FF",
     *          state: "bonding/bonded/none"
     *       }
     *
     */
    onBondChanged(e) {
        this.emit('bondChanged', e);
    }

    /**
     *
     * emit  event: "stateChanged"
     * emit  data:  (number):
     *              bleNative.STATE_ON,
     *              bleNative.STATE_OFF,
     *              bleNative.STATE_TURNING_ON,
     *              bleNative.STATE_TURNING_OFF,
     *              bleNative.STATE_UNSUPPORTED
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
     *                  id: "AA:BB:CC:DD:EE:00",
     *                  deviceName: "bleHealth",
     *                  rssi: 92,
     *                  address: "AA:BB:CC:DD:EE:00"
     *               }
     */
    onPeripheralScanned(e) {
        // e 即扫描到的对象信息
        this.emit('peripheralScanned', e);
    }

    /**
     *
     * emit  event: "peripheralConnected"
     * emit  data:  (object):
     *              {
     *                  id: "AA:BB:CC:DD:EE:00"
     *               }
     */
    onPeripheralConnected(e) {
        this.emit("peripheralConnected", e);
    }


    /**
     *
     * emit  event: "peripheralDisconnected"
     * emit  data:  (object):
     *              {
     *                  id: "AA:BB:CC:DD:EE:00"
     *               }
     */
    onPeripheralDisconnected(e) {
        this.emit("peripheralDisconnected", e);
    }


    /**
     * keep coincident with ios
     * emit  event: "finishDiscover"
     * emit  data:  (object):
     *              {
     *                  id: "AA:BB:CC:DD:EE:00",
     *                  deviceName: "bleHealth",
     *                  services: [
     *                      {
     *                          uuid: "",
     *                          instanceId: "",
     *                          type:  "PRIMARY",
     *                          includedServices: [
     *                              {
     *                                  // service object
     *                              }
     *                          ],
     *                          characteristics: [
     *                              {
     *                                  uuid: "",
     *                                  instanceId: "",
     *                                  permissions: [
     *                                      "permission_read",
     *                                      "permission_read_encrypted",
     *                                      "permission_read_encrypted_mitm",
     *                                      "permission_write",
     *                                      "permission_write_encrypted",
     *                                      "permission_write_encrypted_mitm",
     *                                      "permission_write_signed",
     *                                      "permission_write_signed_mitm"
     *                                  ],
     *                                  writeType:  "write_type_no_response",           // write_type_no_response/write_type_default/write_type_signed
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
     *                                  value: [1, 2, 3],
     *                                  descriptors: [
     *                                      {
     *                                          uuid: "",
     *                                          permissions: [
     *                                              "permission_read",
     *                                              "permission_read_encrypted",
     *                                              "permission_read_encrypted_mitm",
     *                                              "permission_write",
     *                                              "permission_write_encrypted",
     *                                              "permission_write_encrypted_mitm",
     *                                              "permission_write_signed",
     *                                              "permission_write_signed_mitm"
     *                                          ],
     *                                          value: [1, 2, 3]
     *                                      }
     *                                  ]
     *                              }
     *                          ]
     *                       }
     *                   ]
     *               }
     */
    onServicesDiscovered(e) {
        this.emit("finishDiscover", e);
    }

    /**
     *
     * emit  event: "descriptorRead"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptorUuid: ""
     *                  value: [1, 2, 3]
     *               }
     */
    onDescriptorRead(e) {
        this.emit("descriptorRead", e);
    }

    /**
     *
     * emit  event: "descriptorWritten"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  descriptorUuid: ""
     *                  value: [1, 2, 3]
     *               }
     */
    onDescriptorWritten(e) {
        this.emit("descriptorWritten", e);
    }

    /**
     *
     * emit  event: "characteristicRead"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  value: [1, 2, 3]
     *               }
     */
    onCharacteristicRead(e) {
        this.emit("characteristicRead", e);
    }

    /**
     *
     * emit  event: "characteristicWritten"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  value: [1, 2, 3]
     *               }
     */
    onCharacteristicWritten(e) {
        this.emit("characteristicWritten", e);
    }

    /**
     *
     * emit  event: "characteristicChanged"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  serviceUuid: "",
     *                  characteristicUuid: "",
     *                  value: [1, 2, 3]
     *               }
     */
    onCharacteristicChanged(e) {
        this.emit("characteristicChanged", e)
    }

    /**
     *
     * emit  event: "bleError"
     * emit  data:  (object):
     *              {
     *                  peripheralId: "AA:BB:CC:DD:EE:00",
     *                  message: ""
     *               }
     */
    onBleError(e) {
        this.emit("bleError", e);
    }

    /**
     *
     * @param filters               // not available now!
     *              {
     *                   uuids: ["", ""]            // only scan peripheral with services with specified uuids
     *              }
     */
    startScan(filters) {
        if(this._state != BleNativeAndroid.STATE_ON) {
            throw new Error("bleNative is not in STATE_ON, current state is" + this._state);
        }
        BleNativeAndroid.startScan(filters);
    }

    stopScan() {
        if(this._state != BleNativeAndroid.STATE_ON) {
            throw new Error("bleNative is not in STATE_ON, current state is" + this._state);
        }
        BleNativeAndroid.stopScan();
    }

    disconnect(peripheral, onError) {
        BleNativeAndroid.disconnect(peripheral);
    }

    setAdapter(enabled, onError) {
        BleNativeAndroid.set(enabled);
    }

    connect(peripheral, onError, options) {
        BleNativeAndroid.connect({id: peripheral.id}, options);
    }

    isBond(peripheral) {
        const result = BleNativeAndroid.isBond({ id: peripheral.id });
        console.warn(result);
        return result;
    }

    createBond(peripheral, onError) {
        BleNativeAndroid.createBond({ id: peripheral.id });
    }

    removeBond(peripheral, onError) {
        BleNativeAndroid.removeBond({ id: peripheral.id });
    }

    readDescriptor(peripheralId, serviceUuid, characteristicUuid, descriptorUuid,  onError) {
        BleNativeAndroid.readDescriptor({
                peripheralId,
                serviceUuid,
                characteristicUuid,
                descriptorUuid
            }
        );
    }

    writeDescriptor(peripheralId, serviceUuid, characteristicUuid, descriptorUuid, value, onError) {
        BleNativeAndroid.writeDescriptor({
                peripheralId,
                serviceUuid,
                characteristicUuid,
                descriptorUuid,
                value
            }
        );
    }

    readCharacteristic(peripheralId, serviceUuid, characteristicUuid, onError) {
        BleNativeAndroid.readCharacteristic({
                peripheralId,
                serviceUuid,
                characteristicUuid
            }
        );
    }

    writeCharacteristic(peripheralId, serviceUuid, characteristicUuid, value, onError) {
        BleNativeAndroid.writeCharacteristic({
                peripheralId,
                serviceUuid,
                characteristicUuid,
                value
            }
        );
    }

    /**
     *
     * @param peripheralId
     * @param serviceUuid
     * @param characteristicUuid
     * @param onError
     * @param options
     *              {
     *                  enable: true/false
     *               }
     */
    setCharacteristicNotification(peripheralId, serviceUuid, characteristicUuid, onError, options) {
        BleNativeAndroid.setCharacteristicNotification({
                peripheralId,
                serviceUuid,
                characteristicUuid,
                enable: options.enable
            });
    }
}

const bleNative = new BleNative();
module.exports = bleNative;
