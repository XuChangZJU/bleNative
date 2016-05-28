/**
 * Created by Administrator on 2016/3/7.
 */
'use strict'

var React = require('react-native');

var {
    StyleSheet,
    Text,
    ToastAndroid,
    TouchableWithoutFeedback,
    Alert
    } = React;



module.exports = function(err) {
    //ToastAndroid.show(err, ToastAndroid.SHORT);
    Alert.alert('error',err.message);
}
