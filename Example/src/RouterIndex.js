/**
 * Created by Administrator on 2016/3/3.
 */
'use strict'
import React from 'react';
import {
    Navigator
} from "react-native";

import {Scene, Router, Modal} from 'react-native-router-flux';
var PeripheralScanner = require("./PeripheralScanner");
var PeripheralItem = require("./PeripheralItem");
var CharacteristicWriter = require("./CharacteristicWriter");

var RouterIndex = React.createClass({
    render: function() {
        return (
            <Router hideNavBar={true}>
                <Scene key="modal" component={Modal} >
                    <Scene key="root" hideNavBar={true}>
                        <Scene key="scanner" component={PeripheralScanner} initial={true} hideNavBar={true}/>
                        <Scene key="item" component={PeripheralItem} title="蓝牙设备信息" hideNavBar={true}/>
                    </Scene>
                    <Scene key="writeChar" component={CharacteristicWriter}/>
                </Scene>
            </Router>
        );
    }
})


module.exports = RouterIndex;
