/**
 * Created by Administrator on 2016/3/8.
 */
'use strict'

import React from 'react';
import {
    View,
    Text,
    TextInput,
    StyleSheet,
    ListView,
    TouchableHighlight,
    Animated
    } from "react-native";

var showError = require('./showError');
var Button = require('react-native-button');
import {Actions} from 'react-native-router-flux';


var {bleNative} = require("ble-native");


const CharacteristicWriter = React.createClass({


    _generateInputByte: function(index) {

        var _onChangeText = function (text) {
            this.setState({
                dirtyValue: text,
                index: index
            })
        };

        var _onEndEditing = function() {
            const {
                dirtyValue,
                index
                } = this.state;
            if(dirtyValue != undefined) {
                if(dirtyValue == "") {
                    var value;
                    if(index === (this.state.value.length-1)) {
                        // 如果是最后一个字节则删除之
                        value = this.state.value.slice(0, index);
                    }
                    else {
                        value = this.state.value.slice();
                        value[index] = 0;
                    }
                    this.setState({
                        value,
                        dirtyValue: undefined,
                        index: undefined
                    })
                }
                else {
                    var newValue = parseInt(dirtyValue, 16);
                    if (!isNaN(newValue)) {
                        var value2 = this.state.value.slice();
                        value2[index] = newValue;
                        this.setState({
                            value: value2,
                            dirtyValue: undefined,
                            index: undefined
                        })
                    }
                    else {
                        this.setState({
                            dirtyValue: undefined,
                            index: undefined
                        })
                    }
                }
            }
        }

        _onChangeText = _onChangeText.bind(this);
        _onEndEditing = _onEndEditing.bind(this);

        var value = null, backgroundColor = 'aqua';
        if(this.state.value !== null) {
            if(this.state.value[index] !== undefined) {
                value = this.state.value[index].toString(16);
            }
            if((this.props.value === null && this.state.value[index] !== undefined) ||
                (this.props.value != null && this.state.value[index] !== this.props.value[index])) {
                backgroundColor = 'beige';
            }
        }

        return(
            <TextInput
                ref={index.toString()}
                maxLength={2}
                keyboardType={'numeric'}
                key={index}
                style={{width: 40, height: 40, backgroundColor: backgroundColor, borderColor: 'gray', margin: 2, borderWidth: 1, fontSize: 15}}
                defaultValue={value}
                onChangeText={_onChangeText}
                onEndEditing={_onEndEditing}
                />
        )
    },

    getInitialState() {
        return {
            value: this.props.value !== null ? this.props.value.concat(): []
        }
    },

    _onCharacteristicWritten(e) {
        this.setState({
            value: e.value
        });
    },

    _onCharacteristicChanged(e) {
        this.setState({
            value: e.value
        });
    },

    componentWillMount() {
        bleNative.on('characteristicWrite', this._onCharacteristicWritten);
        bleNative.on('characteristicChanged', this._onCharacteristicChanged);
    },

    componentWillUnmount() {
        bleNative.removeListener('characteristicWrite', this._onCharacteristicWritten);
        bleNative.removeListener('characteristicChanged', this._onCharacteristicChanged);
    },

    writeChar() {
        bleNative.writeCharacteristic(
            this.props.id,
            this.props.serviceUuid,
            this.props.characteristicUuid,
            this.state.value,
            function(err) {
                showError(err);
            },
            {type:0}
        );
    },

    render: function() {
        var inputBytes = [], isCharDirty = false;
        for(let i = 0; i < 25; i ++){
            inputBytes.push(this._generateInputByte(i));
        }
        if(this.props.value === null) {
            if(this.state.value.length === 0) {
                isCharDirty = false;
            }
            else {
                isCharDirty = true;
            }
        }
        else {
            if(this.props.value.length > this.state.value.length && this.props.value.every(function(value, index){
                    if(value === this.state.value[index]){
                        return true;
                    }
                    else {
                        return false
                    }
                }, this) == false) {
                isCharDirty = true;
            }
            else if(this.props.value.length <= this.state.value.length && this.state.value.every(function(value, index){
                    if(value === this.props.value[index]){
                        return true;
                    }
                    else {
                        return false
                    }
                }, this) == false) {
                isCharDirty = true;
            }
        }

        return (
            <View style={[styles.container,{backgroundColor:'rgba(52,52,52,0.5)'}]}>
                <View style={{width:250,height:250,justifyContent: 'center',
        alignItems: 'center',backgroundColor:'white',
        padding: 50}}>
                    <View style={{flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', width: 240}}>
                        {inputBytes}
                    </View>
                    <View style={{flexDirection: 'row', alignItems: 'center'}}>
                        <Button containerStyle={[styles.btnContainer, {width: 120}]}
                                style={[styles.btn]}  onPress={Actions.pop}>
                            关闭
                        </Button>
                        <Button containerStyle={[styles.btnContainer, {width: 120}]}
                                style={[styles.btn]}  onPress={this.writeChar} disabled={!isCharDirty} styleDisabled={styles.btnDisabled}>
                            写入
                        </Button>
                    </View>
                </View>
            </View>
        )
    }
});

var styles = StyleSheet.create({
    container: {
        position: 'absolute',
        top:0,
        bottom:0,
        left:0,
        right:0,
        backgroundColor:'transparent',
        justifyContent: 'center',
        alignItems: 'center'
    },
    btnContainer: {
        margin: 10,
        overflow:'hidden',
        borderRadius:4,
        backgroundColor: 'burlywood'
    },
    btn: {
        fontSize: 20,
        color: 'green'
    },
    btnDisabled: {
        fontSize: 20,
        color: 'gray'
    }
});


module.exports = CharacteristicWriter;

