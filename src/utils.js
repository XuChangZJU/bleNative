/**
 * Created by Administrator on 2016/3/11.
 */
'use strict'

function stringToByteArray(str) {
    var bytes = [];

    for (var i = 0; i < str.length; ++i) {
        bytes.push(str.charCodeAt(i));
    }

    return bytes;
}


function byteArrayToShort(first, second) {
    var value = 0;
    value |= (first & 0xff);
    value |= ((second << 8) & 0xff00);
    return  value;
}

function byteArrayToInt(first, second, third, fourth) {
    var value = 0;
    value |= (first & 0xff);
    value |= ((second << 8) & 0xff00);
    value |= ((third << 16) & 0xff0000);
    value |= ((fourth << 24) & 0xff000000);
    return value;
}

function intToByteArray(int) {
    return [(int & 0xff), ((int >> 8) & 0xff), ((int >> 16) & 0xff), ((int >> 24) & 0xff)];
}

function shortToByteArray(short) {
    return [(short & 0xff), ((short >> 8) & 0xff)];
}


function uuidToServiceName(uuid) {
    return "Unknown Service";
}



function uuidToCharacteristicName(uuid) {
    return "Unknown Characteristic";
}

function uuidToDescriptorName(uuid) {
    return "Unknown Descriptor";
}


const cccDescriptorUuid = '2902';

function getClientConfigurationDescriptor(descriptors) {
    return descriptors.find(
        function(ele, index) {
            if(ele.uuid.slice(4, 8) === cccDescriptorUuid) {
                return true;
            }
            else {
                return false;
            }
        }
    )
}

function findNotificationProperty(properties) {
    return properties.find(
        function(ele, index) {
            if(ele.includes('notify') !== -1) {
                return true;
            }
            else{
                return false;
            }
        }
    )
}


function findIndicateProperty(properties)  {
    return properties.find(
        function(ele, index) {
            if(ele.includes('indicate') !== -1) {
                return true;
            }
            else{
                return false;
            }
        }
    )
}

module.exports = {
    stringToByteArray,
    byteArrayToShort,
    byteArrayToInt,
    intToByteArray,
    shortToByteArray,


    uuidToServiceName,
    uuidToCharacteristicName,
    uuidToDescriptorName,
    getClientConfigurationDescriptor,
    findNotificationProperty,
    findIndicateProperty
}
