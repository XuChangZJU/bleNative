/**
 * Created by Administrator on 2016/3/3.
 */
'use strict'

var React = require('react-native');
var {
    View,
    Text,
    StyleSheet,
    ListView,
    TouchableHighlight,
    Animated,
    Platform
    } = React;

var Button = require('react-native-button');
var showError = require('./showError');
import {Actions} from 'react-native-router-flux';

const {bleNative, utils} = require("ble-native");
const {stringToByteArray,
    uuidToServiceName,
    uuidToCharacteristicName,
    uuidToDescriptorName,
    getClientConfigurationDescriptor,
    findNotificationProperty,
    findIndicateProperty
    } = utils;


var DescriptorItemCell = React.createClass({

    componentWillMount() {
        bleNative.on('descriptorRead', this.onDescriptorRead);
        bleNative.on('descriptorWritten', this.onDescriptorWritten);
        bleNative.on('descriptorChanged', this.onDescriptorChanged);
    },

    componentWillUnmount() {
        bleNative.removeListener('descriptorRead', this.onDescriptorRead);
        bleNative.removeListener('descriptorWritten', this.onDescriptorWritten);
        bleNative.removeListener('descriptorChanged', this.onDescriptorChanged);
    },

    getInitialState() {
        return {
            value: this.props.descriptor.value
        }
    },

    render: function() {
        const descriptor = this.props.descriptor;
        const descriptorName = uuidToDescriptorName(descriptor.uuid)
        var permissions = null, value = null;
        if(descriptor.permissions != null) {
            permissions = descriptor.permissions.toString();
        }
        if(this.state.value != null) {
            value = this.state.value.toString();
        }
        return (
            <View style={{flexDirection: 'column', paddingLeft: 15, backgroundColor: 'white'}}>
                <Text style={{fontSize: 12, color: 'black',flex: 1, fontWeight: 'bold'}}>{descriptorName}</Text>
                <Text style={{fontSize: 10, color: 'black',flex: 1}}>UUID: {descriptor.uuid}</Text>
                <Text style={{fontSize: 10, color: 'black',flex: 1}}>permissions: {permissions}</Text>
                <View style={{flexDirection: 'row', alignItems: 'center'}}>
                    <Text style={{fontSize: 10, color: 'black',flex: 3}}>value: {value}</Text>
                    <Button containerStyle={[styles.btnContainer, {flex: 1}]}
                            style={[styles.btn, {fontSize: 12}]}  onPress={this.readValue}>
                        读取
                    </Button>
                </View>
            </View>
        )
    },

    readValue: function() {
        bleNative.readDescriptor(
            this.props.peripheral.id,
            this.props.service.uuid,
            this.props.characteristic.uuid,
            this.props.descriptor.uuid,
            function(err) {
                showError(err);
            }
        )
    },

    onDescriptorRead(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid &&
            e.descriptorUuid == this.props.descriptor.uuid) {
            this.setState({
                value: e.value
            })
        }
    },
    onDescriptorChanged(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid &&
            e.descriptorUuid == this.props.descriptor.uuid) {
            this.setState({
                value: e.value
            })
        }
    },

    onDescriptorWritten(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid &&
            e.descriptorUuid == this.props.descriptor.uuid) {
            this.setState({
                value: e.value
            })
        }
    }
})

var CharacteristicItemCell = React.createClass({
    getInitialState: function() {
        return {
            isDescriptorListHide: true,
            enable: false,
            value: this.props.characteristic.value
        }
    },

    componentWillMount() {
        bleNative.on('characteristicRead', this._onCharacteristicRead);
        bleNative.on('characteristicWritten', this._onCharacteristicWritten);
        bleNative.on('characteristicChanged', this._onCharacteristicChanged);
        bleNative.on('descriptorWritten', this._onDescriptorWritten);
    },

    componentWillUnmount() {
        bleNative.removeListener('characteristicRead', this._onCharacteristicRead);
        bleNative.removeListener('characteristicWritten', this._onCharacteristicWritten);
        bleNative.removeListener('characteristicChanged', this._onCharacteristicChanged);
        bleNative.removeListener('descriptorWritten', this._onDescriptorWritten);
    },

    switchDescriptorVisibility: function() {
        this.setState({
            isDescriptorListHide: !this.state.isDescriptorListHide
        })
    },

    setCharacteristicNotification: function() {
        // ios和android的行为不完全一样
        if(Platform.OS === "ios") {
            bleNative.setCharacteristicNotification(
                this.props.peripheral.id,
                this.props.service.uuid,
                this.props.characteristic.uuid,
                function (err) {
                    showError(err);
                },
                {enable:!this.state.enable}
            );
            this.setState({enable:!this.state.enable});
        }
        else {
             var value = null;
             if(findNotificationProperty(this.props.characteristic.properties) !== undefined) {
                if(!this.state.enable) {
                     value = stringToByteArray(bleNative.ENABLE_NOTIFICATION_VALUE);
                 }
                 else {
                     value = stringToByteArray(bleNative.DISABLE_NOTIFICATION_VALUE);
                 }
             }
             else if(findIndicateProperty(this.props.characteristic.properties) !== undefined) {
                 if(!this.state.enable) {
                     value = stringToByteArray(bleNative.ENABLE_INDICATION_VALUE);
                 }
                 else {
                     value = stringToByteArray(bleNative.DISABLE_NOTIFICATION_VALUE);
                 }
             }
             if(value == null) {
                 showError("character without property notification or indicate could not set notification");
             }
             else {
                 bleNative.setCharacteristicNotification(
                     this.props.peripheral.id,
                     this.props.service.uuid,
                     this.props.characteristic.uuid,
                     function (err) {
                         showError(err);
                     },
                     {enable:!this.state.enable}
                 );

                 var cccd = getClientConfigurationDescriptor(this.props.characteristic.descriptors);
                 if (cccd !== undefined) {
                     bleNative.writeDescriptor(
                        this.props.peripheral.id,
                         this.props.service.uuid,
                         this.props.characteristic.uuid,
                         cccd.uuid,
                         value,
                         function(err) {
                             showError(err);
                         }
                     )
                 }
             }
        }

    },

    writeCharacteristic: function() {
        Actions.writeChar({
            id: this.props.peripheral.id,
            serviceUuid: this.props.service.uuid,
            characteristicUuid: this.props.characteristic.uuid,
            value:null //tips9:这里有改动: this.state.value
        });
    },

    readCharacteristic: function() {
        bleNative.readCharacteristic(
            this.props.peripheral.id,
            this.props.service.uuid,
            this.props.characteristic.uuid,
            function(err) {
                showError(err);
            }
        );
    },

    render: function() {
        const characteristic = this.props.characteristic;
        const characterName = uuidToCharacteristicName(characteristic.uuid);
        var writeType = null, properties = null, value = null;
        if(characteristic.writeType != null) {
            writeType = characteristic.writeType.toString();
        }
        if(characteristic.properties != null) {
            properties = characteristic.properties.toString();
        }
        if(this.state.value != null) {
            value = this.state.value.toString();
        }
        const setNotificationBtn = (
            <Button containerStyle={[styles.btnContainer, {width: 100}]}
                    style={[styles.btn, {fontSize: 14}]}  onPress={this.readCharacteristic}>
                read
            </Button>
        );
        const readCharacteristicBtn = (
            <Button containerStyle={[styles.btnContainer, {width: 100}]}
                    style={[styles.btn, {fontSize: 14}]}  onPress={this.setCharacteristicNotification}>
                notification {this.state.enable?'enabled':'disabled'}
            </Button>
        );
        const writeCharacteristicBtn = (
            <Button containerStyle={[styles.btnContainer, {width: 100}]}
                    style={[styles.btn, {fontSize: 14}]}  onPress={this.writeCharacteristic}>
                write
            </Button>
        )
        if(this.state.isDescriptorListHide) {
            return (
                <View style={{padding: 5, backgroundColor: 'aqua'}}>
                    <TouchableHighlight
                        onPress={this.switchDescriptorVisibility}
                        onShowUnderlay={this.props.onHighlight}
                        onHideUnderlay={this.props.onUnhighlight}
                        >
                        <View style={{flexDirection: 'column', paddingLeft: 15}}>
                            <Text style={{fontSize: 14, flex: 1, fontWeight: 'bold'}}>{characterName}</Text>
                            <Text style={{fontSize: 12, color: 'black',flex: 1}}>UUID: {characteristic.uuid}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>writeType: {writeType}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>properties: {properties}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>values: {value}</Text>
                            <View style={{flexDirection: 'row', justifyContent: 'flex-start'}}>
                                {setNotificationBtn}
                                {readCharacteristicBtn}
                                {writeCharacteristicBtn}
                            </View>
                        </View>
                    </TouchableHighlight>
                </View>
            );
        }
        else {
            var dataSource = new ListView.DataSource({
                rowHasChanged: (row1, row2) => row1 !== row2,
            });
            dataSource = dataSource.cloneWithRows(characteristic.descriptors);
            return (
                <View style={{padding: 5, backgroundColor: 'aqua'}}>
                    <TouchableHighlight
                        onPress={this.switchDescriptorVisibility}
                        onShowUnderlay={this.props.onHighlight}
                        onHideUnderlay={this.props.onUnhighlight}
                        >
                        <View style={{flexDirection: 'column', paddingLeft: 15}}>
                            <Text style={{fontSize: 14, flex: 1, fontWeight: 'bold'}}>{characterName}</Text>
                            <Text style={{fontSize: 12, color: 'black',flex: 1}}>UUID: {characteristic.uuid}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>writeType: {writeType}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>properties: {properties}</Text>
                            <Text
                                style={{fontSize: 12, color: 'black',flex: 1}}>values: {value}</Text>
                            <View style={{flexDirection: 'row', justifyContent: 'flex-start'}}>
                                {setNotificationBtn}
                                {readCharacteristicBtn}
                                {writeCharacteristicBtn}
                            </View>
                        </View>
                    </TouchableHighlight>
                    <ListView
                        ref="charList"
                        renderSeparator={this.renderSeparator}
                        renderRow={this.renderRow}
                        dataSource={dataSource}
                        automaticallyAdjustContentInsets={false}
                        keyboardDismissMode="on-drag"
                        keyboardShouldPersistTaps={true}
                        showsVerticalScrollIndicator={false}
                        />
                </View>
            );

        }
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

    renderRow: function(descriptor: Object,  sectionID, rowID, highlightRowFunc) {
        return (
            <DescriptorItemCell
                key={descriptor.uuid}
                onHighlight={() => highlightRowFunc(sectionID, rowID)}
                onUnhighlight={() => highlightRowFunc(null, null)}
                descriptor={descriptor}
                characteristic={this.props.characteristic}
                service={this.props.service}
                peripheral={this.props.peripheral}
                />
        );
    },

    _onCharacteristicRead(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid) {
            this.setState({
                value: e.value
            })
        }
    },

    _onCharacteristicWritten(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid) {
            this.setState({
                value: e.value
            })
        }
    },

    _onCharacteristicChanged(e) {
        if(e.peripheralId === this.props.peripheral.id &&
            e.serviceUuid == this.props.service.uuid &&
            e.characteristicUuid == this.props.characteristic.uuid) {
            this.setState({
                value: e.value
            })
        }
    },


    _onDescriptorWritten(e) {
        if(Platform.OS === "android") {
            var cccd = getClientConfigurationDescriptor(this.props.characteristic.descriptors);
            if(e.peripheralId === this.props.peripheral.id &&
                e.serviceUuid == this.props.service.uuid &&
                e.characteristicUuid == this.props.characteristic.uuid &&
                e.descriptorUuid == cccd.uuid) {

                var value = e.value;
                var enableIndicationValue = stringToByteArray(bleNative.ENABLE_INDICATION_VALUE);
                var enableNotificationValue = stringToByteArray(bleNative.ENABLE_NOTIFICATION_VALUE);
                if((value.length === enableIndicationValue.length && value.every(function (ele, index) {
                        return (enableIndicationValue[index] === ele);
                    }) === true) ||
                    (value.length === enableNotificationValue.length && value.every(function(ele, index) {
                        return (enableNotificationValue[index] === ele);
                    }))) {
                    this.setState({
                        enable: true
                    });
                }
                else {
                    this.setState({
                        enable: false
                    });
                }
            }
        }
    }

});

var ServiceItemCell = React.createClass({
    getInitialState: function() {
        return {
            isCharListHide: true
        }
    },


    switchCharVisibility: function() {
        this.setState({
            isCharListHide: !this.state.isCharListHide
        })
    },

    render: function() {
        const service = this.props.service;
        const serviceName = uuidToServiceName(service.uuid);


        if(this.state.isCharListHide) {
            return (
                <View style={{padding: 5, backgroundColor: 'aquamarine'}}>
                    <TouchableHighlight
                        onPress={this.switchCharVisibility}
                        onShowUnderlay={this.props.onHighlight}
                        onHideUnderlay={this.props.onUnhighlight}
                        >
                        <View style={{flexDirection: 'column', paddingLeft: 10}}>
                            <Text style={{fontSize: 16, flex: 1, fontWeight: 'bold'}}>{serviceName}</Text>
                            <Text style={{fontSize: 14, color: 'black',flex: 1}}>UUID: {service.uuid}</Text>
                            <Text style={{fontSize: 14, color: 'black',flex: 1}}>TYPE: {service.type}</Text>
                        </View>
                    </TouchableHighlight>
                </View>
            )
        }
        else {
            var dataSource = new ListView.DataSource({
                rowHasChanged: (row1, row2) => row1 !== row2,
            });
            dataSource = dataSource.cloneWithRows(service.characteristics);
            return (
                <View style={{padding: 5, backgroundColor: 'aquamarine'}}>
                    <TouchableHighlight
                        onPress={this.switchCharVisibility}
                        onShowUnderlay={this.props.onHighlight}
                        onHideUnderlay={this.props.onUnhighlight}
                        >
                        <View style={{flexDirection: 'column', paddingLeft: 10}}>
                            <Text style={{fontSize: 16, flex: 1, fontWeight: 'bold'}}>{serviceName}</Text>
                            <Text style={{fontSize: 14, color: 'black',flex: 1}}>UUID: {service.uuid}</Text>
                            <Text style={{fontSize: 14, color: 'black',flex: 1}}>TYPE: {service.type}</Text>
                        </View>
                    </TouchableHighlight>
                    <ListView
                        ref="charList"
                        renderSeparator={this.renderSeparator}
                        renderRow={this.renderRow}
                        dataSource={dataSource}
                        automaticallyAdjustContentInsets={false}
                        keyboardDismissMode="on-drag"
                        keyboardShouldPersistTaps={true}
                        showsVerticalScrollIndicator={false}
                        />
                </View>
            );
        }
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

    renderRow: function(characteristic: Object,  sectionID, rowID, highlightRowFunc) {
        return (
            <CharacteristicItemCell
                key={characteristic.uuid}
                onHighlight={() => highlightRowFunc(sectionID, rowID)}
                onUnhighlight={() => highlightRowFunc(null, null)}
                characteristic={characteristic}
                service={this.props.service}
                peripheral={this.props.peripheral}
                />
        );
    }
})

var PeripheralItem = React.createClass({

    componentWillUnmount: function() {
        bleNative.disconnect(this.props.peripheral, function(err){
            showError(err);
        })
    },

    render: function() {
        const peripheral = this.props.peripheral;

        var dataSource = new ListView.DataSource({
            rowHasChanged: (row1, row2) => row1 !== row2,
        });

        dataSource = dataSource.cloneWithRows(peripheral.services);
        var serviceView = (
            <ListView
                renderSeparator={this.renderSeparator}
                renderRow={this.renderRow}
                dataSource={dataSource}
                automaticallyAdjustContentInsets={false}
                keyboardDismissMode="on-drag"
                keyboardShouldPersistTaps={true}
                showsVerticalScrollIndicator={false}
                />);
        return (
            <View style={styles.container}>
                <View style={styles.body}>
                    {serviceView}
                </View>
                <View style={styles.bottomPanel}>
                    <View style={{flexDirection: 'column', flex: 1, paddingLeft: 10}}>
                        <Text style={{fontSize: 15, color: 'red', textAlign: 'center'}}>{peripheral.name}</Text>
                        <Text style={{fontSize: 12, color: 'red', textAlign: 'center'}}>Address: {peripheral.address}</Text>
                    </View>
                    <Button containerStyle={[styles.btnContainer, {flex: 1}]}
                            style={styles.btn}  onPress={this.disconnect}>
                        断连返回
                    </Button>
                </View>
            </View>
        )
    },

    disconnect: function() {
        Actions.pop();
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

    renderRow: function(service: Object,  sectionID, rowID, highlightRowFunc) {
        return (
            <ServiceItemCell
                key={service.uuid}
                onSelect={() => this.selectService(service)}
                onHighlight={() => highlightRowFunc(sectionID, rowID)}
                onUnhighlight={() => highlightRowFunc(null, null)}
                service={service}
                peripheral={this.props.peripheral}
                />
        );
    },

    selectService: function(service) {
        console.log(service);
    }
});



var styles = StyleSheet.create({
    container: {
        flex: 15,
        justifyContent: 'center',
        alignItems: 'stretch'
    },
    btnContainer: {
        overflow:'hidden',
        borderRadius:4,
        backgroundColor: 'burlywood'
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
    characteristicUuid: {
        fontSize: 12,
        color: 'red'
    }
});
module.exports = PeripheralItem;