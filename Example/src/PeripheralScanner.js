/**
 * Created by Administrator on 2016/3/1.
 */
'use strict';

import React from 'react';
import {
    StyleSheet,
    Text,
    TouchableHighlight,
    TouchableNativeFeedback,
    View,
    ListView,
} from "react-native";

var Button = require('react-native-button');
var showError = require('./showError');
const {bleNative} = require('ble-native');

import {Actions} from 'react-native-router-flux';

var kLockServiceUUIDString = "d62a2015-7fac-a2a3-bec3-a68869e0f2bf";

const BleStateStrings = {
    STATE_ON: '蓝牙已开启',
    STATE_OFF: '蓝牙已关闭',
    STATE_TURNING_ON: '蓝牙正在打开',
    STATE_TURNING_OFF: '蓝牙正在关闭',
    STATE_UNSUPPORTED: '不支持蓝牙4.0'
}


var PeripheralItemCell = React.createClass({

    render: function() {
        const peripheral = this.props.peripheral;
        return (
            <View>
                <TouchableHighlight
                    onPress={this.props.onSelect}
                    onShowUnderlay={this.props.onHighlight}
                    onHideUnderlay={this.props.onUnhighlight}
                >
                    <View style={styles.row}>
                        <View style={styles.textContainer}>
                            <Text style={styles.peripheralName} numberOfLines={2}>
                                {peripheral.deviceName}
                            </Text>
                            <Text style={styles.peripheralId} numberOfLines={1}>
                                {peripheral.id}
                            </Text>
                            <Text style={styles.peripheralId} numberOfLines={1}>
                                {peripheral.rssi}
                            </Text>
                        </View>
                    </View>
                </TouchableHighlight>
            </View>
        );
    }
});

var PeripheralScanner = React.createClass({

    getInitialState: function() {
        return {
            bleState: 'unknown',
            scanning: false,
            scannedPeripherals: []
        }
    },

    componentWillMount: function() {
        bleNative.init();
        bleNative.on('stateChanged', this.onBleStateChanged);
        bleNative.on('peripheralScanned', this.onBlePeripheralScanned);
        bleNative.on('peripheralConnected', this.onPeripheralConnected);
        bleNative.on('finishDiscover',this.onFinishDiscover);
        bleNative.on('error', this.onError);
    },


    componentWillUnmount: function() {
        if(this.state.scanning === true) {
            this.switchScanState();
        }
        bleNative.removeListener('stateChanged', this.onBleStateChanged);
        bleNative.removeListener('peripheralScanned', this.onBlePeripheralScanned);
        bleNative.removeListener('peripheralConnected', this.onPeripheralConnected);
        bleNative.removeListener('finishDiscover',this.onFinishDiscover);
        bleNative.removeListener('error', this.onError);
        bleNative.destroy();
    },

    componentDidMount: function() {
        var state = bleNative.state;
        this.onBleStateChanged(state);
    },

    render: function() {
        var btnSwitchLabel, btnSwitchDisabled, btnSwitchScanLabel, btnSwitchScanDisabled;
        switch (this.state.bleState) {
            case bleNative.STATE_ON:
                btnSwitchLabel = '关闭蓝牙';
                btnSwitchDisabled = false;
                btnSwitchScanDisabled = false;
                break;
            case bleNative.STATE_OFF:
                btnSwitchLabel = '打开蓝牙';
                btnSwitchDisabled = false;
                btnSwitchScanDisabled = true;
                break;
            default:
                btnSwitchDisabled = true;
                btnSwitchScanDisabled = true;
        };

        if(this.state.scanning) {
            btnSwitchScanLabel = '停止扫描';
        }
        else {
            btnSwitchScanLabel = "开始扫描";
        }

        var scanResultView;
        if(this.state.scannedPeripherals.length === 0) {
            scanResultView = (
                <Text style={{fontSize: 30, color: 'red', textAlign: 'center', marginLeft: 15, marginRight: 15}}>
                    尚无扫描结果
                </Text>
            )
        }
        else {
            var dataSource = new ListView.DataSource({
                rowHasChanged: (row1, row2) => row1 !== row2,
            });

            dataSource = dataSource.cloneWithRows(this.state.scannedPeripherals);
            scanResultView = (
                <ListView
                    renderSeparator={this.renderSeparator}
                    renderRow={this.renderRow}
                    dataSource={dataSource}
                    automaticallyAdjustContentInsets={false}
                    keyboardDismissMode="on-drag"
                    keyboardShouldPersistTaps={true}
                    showsVerticalScrollIndicator={false}
                />
            )
        }
        return (
            <View style={styles.container}>
                <View style={styles.body}>
                    {scanResultView}
                </View>
                <View style={styles.bottomPanel}>
                    <Text style={{fontSize: 15, color: 'red', textAlign: 'center', marginLeft: 15, marginRight: 15}}>{BleStateStrings[this.state.bleState]}</Text>
                    <Button disabled={btnSwitchDisabled}
                            containerStyle={styles.btnContainer}
                            style={this.state.btn}
                            onPress={this.switchBleState}>
                        {btnSwitchLabel}
                    </Button>
                    <Button disabled={btnSwitchScanDisabled}
                            containerStyle={styles.btnContainer}
                            style={this.state.btn}
                            onPress={this.switchScanState}>
                        {btnSwitchScanLabel}
                    </Button>
                </View>
            </View>
        );
    },

    switchBleState: function() {
        var state = (this.state.bleState === bleNative.STATE_ON)? false: true;
        if(state === false && this.state.scanning === true) {
            this.switchScanState();
        }
        bleNative.setAdapter(state, function(err) {
            showError(err);
        })
    },

    switchScanState: function() {
        if(this.state.scanning) {
            bleNative.stopScan();
        }
        else {
            this.setState({
                scannedPeripherals: []
            });
            bleNative.startScan({
                uuids: []// uuids: [kLockServiceUUIDString]
            });
        }
        this.state.scanning = !this.state.scanning;
        this.onScanStateChanged(this.state.scanning);
    },

    onBleStateChanged: function(state) {
        this.setState({
            bleState: state
        });
        if(state !== bleNative.STATE_ON && this.state.scannedPeripherals.length > 0) {
            this.setState({
                scannedPeripherals: []
            });
        }
    },

    onScanStateChanged: function(scanning) {
        this.setState({
            scanning
        })
    },

    onBlePeripheralScanned: function(e) {
        // 去个重吧
        let existed = false;
        this.state.scannedPeripherals.forEach(function(ele, index) {
            if(ele.id === e.id) {
                existed = true;
            }
        });
        if(!existed) {
            this.state.scannedPeripherals.push(e);
            this.setState({
                scannedPeripherals: this.state.scannedPeripherals
            });
        }
    },

    onPeripheralConnected: function(e) {
        if(this.state.scanning === true) {
            this.switchScanState();
        }

    },

    onFinishDiscover: function(e) {
        Actions.item({peripheral: e});
    },

    onError: function(e) {
        showError(e);
    },


    renderSeparator: function(sectionID, rowID, adjacentRowHighlighted){
        var style = styles.rowSeparator;
        if (adjacentRowHighlighted) {
            style = [style, styles.rowSeparatorHide];
        }
        return (
            <View key={'SEP_' + sectionID + '_' + rowID}  style={style}/>
        );
    },

    renderRow: function(peripheral: Object,  sectionID, rowID, highlightRowFunc) {

        return (
            <PeripheralItemCell
                key={peripheral.id}
                onSelect={() => this.selectPeripheral(peripheral)}
                onHighlight={() => highlightRowFunc(sectionID, rowID)}
                onUnhighlight={() => highlightRowFunc(null, null)}
                peripheral={peripheral}
            />
        );
    },

    selectPeripheral: function(peripheral: Object) {
        bleNative.connect(peripheral,
            function(err) {
                showError(err);
            }, {
                isAutomaticDiscovering:true,
                autoConnect: false
            })
    }
})



var styles = StyleSheet.create({
    container: {
        flex: 15,
        justifyContent: 'center',
        alignItems: 'stretch'
    },
    btnContainer: {
        padding:10,
        height:45,
        overflow:'hidden',
        borderRadius:4,
        backgroundColor: 'white'
    },
    btn: {
        fontSize: 20,
        color: 'green'
    },
    bottomPanel: {
        flexDirection: 'row',
        justifyContent: 'flex-start',
        alignItems: 'center',
        flex: 1,
        backgroundColor: 'white'
    },
    body: {
        flex: 10,
        justifyContent: 'center',
        alignItems: 'stretch'
    },
    rowSeparator: {
        backgroundColor: 'rgba(0, 0, 0, 0.1)',
        height: 1,
        marginLeft: 4,
    },
    rowSeparatorHide: {
        opacity: 0.0,
    },
    textContainer: {
        flex: 1,
        alignItems: 'stretch'
    },
    textContainer2: {
        flex: 1,
        marginTop: 5,
        flexDirection: 'row',
        // alignItems: 'flex-end',
        justifyContent: 'flex-end'
    },
    peripheralName: {
        flex: 1,
        fontSize: 16,
        fontWeight: '500',
        marginBottom: 2
    },
    peripheralId: {
        color: '#999999',
        fontSize: 24
    }
});

module.exports = PeripheralScanner;