/**
 * Created by Administrator on 2016/3/3.
 */
'use strict'
var React = require('react-native');
var {
    Navigator
    } = React;

import {Router, Route, Schema, Animations} from 'react-native-router-flux';
var PeripheralScanner = require("./PeripheralScanner");
var PeripheralItem = require("./PeripheralItem");
var CharacteristicWriter = require("./CharacteristicWriter");

var RouterIndex = React.createClass({
    render: function() {
        return (
            <Router hideNavBar={true}>
                <Schema name="default" sceneConfig={Navigator.SceneConfigs.FloatFromRight}/>
                <Schema name="modal" sceneConfig={Navigator.SceneConfigs.FloatFromBottom}/>
                <Schema name="withoutAnimation"/>

                <Route name="scanner" component={PeripheralScanner} initial={true} wrapRouter={true} hideNavBar={true}/>
                <Route name="item" component={PeripheralItem} title="蓝牙设备信息" hideNavBar={true}/>
                <Route name="writeChar" component={CharacteristicWriter} type="modal"/>
            </Router>
        );
    }
})


module.exports = RouterIndex;
