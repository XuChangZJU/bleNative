/**
 * Created by Administrator on 2016/3/7.
 */
'use strict'

import {
    Alert
    } from "react-native";



module.exports = function(err) {
    //ToastAndroid.show(err, ToastAndroid.SHORT);
    const message = (typeof err === "string" ? err : err.message);
    Alert.alert('error',err.message);
}
