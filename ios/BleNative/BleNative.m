//
//  BleNative.m
//  BleNative
//
//  Created by biliyuan on 16/4/25.
//  Copyright © 2016年 biliyuan. All rights reserved.
//
#import "BleNative.h"
#import "RCTEventDispatcher.h"
#import "RCTLog.h"
#import "RCTConvert.h"
//JS层监听的事件
NSString *EVENT_BLE_STATE_CHANGED = @"bleStateChanged";
NSString *EVENT_BLE_PERIPHERAL_SCANNED = @"blePeripheralScanned";
NSString *EVENT_BLE_PERIPHERAL_CONNECTED = @"blePeripheralConnected";
NSString *EVENT_BLE_PERIPHERAL_DISCONNECTED = @"blePeripheralDisconnected";

NSString *EVENT_BLE_PERIPHERAL_FAIL_TO_CONNECT = @"blePeripheralFailToConnect";
NSString *EVENT_BLE_SERVICES_DISCOVERED = @"bleServicesDiscovered";
NSString *EVENT_BLE_INCLUDED_SERVICES_DISCOVERED = @"bleIncludedServicesDiscovered";
NSString *EVENT_BLE_CHARACTERISTICS_DISCOVERED = @"bleCharacteristicsDiscovered";
NSString *EVENT_BLE_DESCRIPTORS_DISCOVERED = @"bleDescriptorsDiscovered";
NSString *EVENT_BLE_FINISH_DISCOVER = @"bleFinishDiscover";


NSString *EVENT_BLE_CHARACTERISTICS_READ = @"bleCharacteristicRead";
NSString *EVENT_BLE_CHARACTERISTICS_WRITTEN = @"bleCharacteristicWritten";
NSString *EVENT_BLE_CHARACTERISTICS_CHANGED = @"bleCharacteristicChanged";


NSString *EVENT_BLE_DESCRIPTORS_READ = @"bleDescriptorRead";
NSString *EVENT_BLE_DESCRIPTORS_WRITTEN = @"bleDescriptorWritten";
NSString *EVENT_BLE_DESCRIPTORS_CHANGED = @"bleDescriptorChanged";


NSString *EVENT_BLE_ERROR = @"bleError";



//层次结构
NSString *COMMON_PERIPHERAL_UUID = @"id";
NSString *COMMON_SERVICE_UUID = @"uuid";
NSString *COMMON_CHARACTERISTIC_UUID = @"uuid";
NSString *COMMON_DESCRIPTOR_UUID = @"uuid";
NSString *COMMON_SERVICES = @"services";
NSString *COMMON_INCLUDED_SERVICES = @"includedServices";
NSString *COMMON_CHARACTERISTICS = @"characteristics";
NSString *COMMON_DESCRIPTORS = @"descriptors";
NSString *COMMON_OPTIONS = @"options";
NSString *COMMON_VALUE = @"value";

//扁平结构
NSString *PARAM_COMMON_PERIPHERAL_UUID = @"peripheralId";
NSString *PARAM_COMMON_SERVICE_UUID = @"serviceUuid";
NSString *PARAM_COMMON_CHARACTERISTIC_UUID = @"characteristicUuid";
NSString *PARAM_COMMON_DESCRIPTOR_UUID = @"descriptorUuid";


//事件参数
//DidUpdateState
NSString *PARAM_STATE = @"state";

//PeripheralScanned
NSString *PARAM_DEVICE_NAME = @"deviceName";
NSString *PARAM_RSSI = @"rssi";
//PeripheralConnected

//ServicesDiscovered
NSString *PARAM_TYPE = @"type";

//CharacteristicsDiscovered
NSString *PARAM_CHARACTERISTIC_PROPERTIES = @"properties";
NSString *PARAM_CHARACTERISTIC_ISNOTIFYING = @"isNotifying";

//Error
NSString *ERROR_PARAM_MESSAGE = @"message";

//options
NSString *PARAM_SCAN_UUIDS = @"uuids";
NSString *PARAM_SCAN_ALLOWDUPLICATES = @"allowDuplicates";
NSString *PARAM_COMMON_VALUE = @"value";
NSString *PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE = @"enable";
NSString *PARAM_IS_AUTOMATIC = @"isAutomaticDiscovering";
NSString *PARAM_WRITE_CHARACTERISTIC_TYPE = @"type";

enum BleState {
  STATE_UNSUPPORTED,
  STATE_UNAUTHORIZED,
  STATE_OFF,
  STATE_ON,
  STATE_RESETTING,
  STATE_UNKNOWN
};


typedef struct {
    int total;
    int count;
}Pair;

@interface BleNative () <CBCentralManagerDelegate, CBPeripheralDelegate> {
    
    CBCentralManager    *centralManager;
    CBPeripheral        *centralPeripheral;
    BleConstantsConverter *converter;
    NSNumber            *isAutomaticDiscovering;
    NSMutableDictionary *discoverTreeDic;
  
}

@end




@implementation BleNative
{
    
}

RCT_EXPORT_MODULE()

@synthesize bridge = _bridge;

#pragma mark Initialization

- (instancetype)init {
    if (self = [super init]) {
    }
    isAutomaticDiscovering = @NO;
    converter = [[BleConstantsConverter alloc]init];
    discoverTreeDic = [[NSMutableDictionary alloc]init];
    return self;
}

//导出常量
- (NSDictionary *)constantsToExport {
    NSMutableDictionary *mutableDic = [[NSMutableDictionary alloc] init];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_UNSUPPORTED] forKey:@"STATE_UNSUPPORTED"];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_UNAUTHORIZED] forKey:@"STATE_UNAUTHORIZED"];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_OFF] forKey:@"STATE_OFF"];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_ON] forKey:@"STATE_ON"];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_RESETTING] forKey:@"STATE_RESETTING"];
    [mutableDic setValue:[NSString stringWithFormat:@"%d",STATE_UNKNOWN] forKey:@"STATE_UNKNOWN"];
    return [mutableDic copy];
}

//供JS层调用的方法
RCT_EXPORT_METHOD(setup) {
  dispatch_queue_t eventQueue = dispatch_queue_create("com.bly", DISPATCH_QUEUE_SERIAL);
  dispatch_set_target_queue(eventQueue, dispatch_get_main_queue());
  centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:eventQueue options:@{}];
}

RCT_EXPORT_METHOD(startScan:(NSDictionary *)filter) {
  NSArray *uuidsStringArray;
  NSMutableArray *uuidsMutableArray = [[NSMutableArray alloc]init];
  NSString *uuidString;
  NSMutableDictionary *scanOptions = [NSMutableDictionary dictionaryWithObject:@NO
                                                                        forKey:CBCentralManagerScanOptionAllowDuplicatesKey];
  if(filter != nil){
    uuidsStringArray = filter[PARAM_SCAN_UUIDS];
    BOOL allowDuplicates = filter[PARAM_SCAN_ALLOWDUPLICATES];
    if (allowDuplicates  == true) {
      [scanOptions setValue:@YES forKey:CBCentralManagerScanOptionAllowDuplicatesKey];
    }
    for(uuidString in uuidsStringArray){
      [uuidsMutableArray addObject:[CBUUID UUIDWithString:uuidString]];
    }
    [centralManager scanForPeripheralsWithServices:[uuidsMutableArray copy] options:scanOptions];
  }else{
    [centralManager scanForPeripheralsWithServices:nil options:scanOptions];
  }
}

RCT_EXPORT_METHOD(stopScan) {
  [centralManager stopScan];
}

RCT_EXPORT_METHOD(connect:(NSDictionary *)param ) {
  
  NSString *peripheralUuidString = param[COMMON_PERIPHERAL_UUID];

  NSDictionary *options = param[COMMON_OPTIONS];
  isAutomaticDiscovering = options[PARAM_IS_AUTOMATIC];

    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
    NSArray *peripherals = [NSArray array];
    peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
   centralPeripheral = peripherals[0];
  [centralPeripheral setDelegate:self];
  [centralManager connectPeripheral:centralPeripheral options:nil];
}

RCT_EXPORT_METHOD(disconnect:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[COMMON_PERIPHERAL_UUID];
//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];

  [centralManager cancelPeripheralConnection:centralPeripheral];
}

RCT_EXPORT_METHOD(discoverServices:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  [centralPeripheral discoverServices: nil];
}

RCT_EXPORT_METHOD(discoverIncludedServices:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    [centralPeripheral discoverIncludedServices:nil forService:service];
  }
}

RCT_EXPORT_METHOD(discoverCharacteristics:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    [centralPeripheral discoverCharacteristics:nil forService:service];
  }
}

RCT_EXPORT_METHOD(discoverDescriptors:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString  = param[PARAM_COMMON_CHARACTERISTIC_UUID];
//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    CBCharacteristic *characteristic;
    for(characteristic in characteristics) {
      if([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]){
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      [centralPeripheral discoverDescriptorsForCharacteristic:characteristic];
    }
  }
}

//设置notification是异步的,但是没有回调函数
RCT_EXPORT_METHOD(setCharacteristicNotification:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString = param[PARAM_COMMON_CHARACTERISTIC_UUID];
  NSDictionary *options = param[COMMON_OPTIONS];
  NSNumber *enable = options[PARAM_SET_CHARACTERISTIC_NOTIFICATION_ENABLE];

//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral= peripherals[0];
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    CBCharacteristic *characteristic;
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    for(characteristic in characteristics) {
      if ([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]) {
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      [centralPeripheral setNotifyValue:enable forCharacteristic:characteristic];
    }
  } else {
    RCTLogInfo(@"没有该service");
  }
}

RCT_EXPORT_METHOD(readCharacteristic:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString = param[PARAM_COMMON_CHARACTERISTIC_UUID];

//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];

  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    CBCharacteristic *characteristic;
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    for(characteristic in characteristics) {
      if ([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]) {
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      [centralPeripheral readValueForCharacteristic:characteristic];
    }
  }
}

RCT_EXPORT_METHOD(writeCharacteristic:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString = param[PARAM_COMMON_CHARACTERISTIC_UUID];
  NSArray *value = param[PARAM_COMMON_VALUE];
  NSDictionary *options = param[COMMON_OPTIONS];
  NSNumber *type = options[PARAM_WRITE_CHARACTERISTIC_TYPE];
  NSInteger writeType = [type integerValue];

//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    CBCharacteristic *characteristic;
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    for(characteristic in characteristics) {
      if ([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]) {
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      Byte bytes[value.count];
      NSNumber *num;
      for(int i=0;i<value.count;i++){
        num = value[i];
        bytes[i] = (Byte) [num intValue];
      }
      NSData *data = [[NSData alloc] initWithBytes:bytes length:value.count];
      [centralPeripheral writeValue:data forCharacteristic:characteristic type:writeType];

    }
  }
}

RCT_EXPORT_METHOD(readDescriptor:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString = param[PARAM_COMMON_CHARACTERISTIC_UUID];
  NSString *descriptorUuidString = param[PARAM_COMMON_DESCRIPTOR_UUID];

//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];

  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    CBCharacteristic *characteristic;
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    for(characteristic in characteristics) {
      if ([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]) {
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      CBDescriptor *descriptor;
      NSArray *descriptors = characteristic.descriptors;
      BOOL descriptorExist = false;
      for(descriptor in descriptors){
        if ([descriptor.UUID.UUIDString isEqualToString:descriptorUuidString]) {
          descriptorExist = true;
          break;
        }
      }
      if (descriptorExist == true) {
        [centralPeripheral readValueForDescriptor:descriptor];
      }
    }
  }
}


RCT_EXPORT_METHOD(writeDescriptor:(NSDictionary *)param) {
  NSString *peripheralUuidString = param[PARAM_COMMON_PERIPHERAL_UUID];
  NSString *serviceUuidString = param[PARAM_COMMON_SERVICE_UUID];
  NSString *characteristicUuidString = param[PARAM_COMMON_CHARACTERISTIC_UUID];
  NSString *descriptorUuidString = param[PARAM_COMMON_DESCRIPTOR_UUID];
  NSArray *value = param[PARAM_COMMON_VALUE];

//  NSArray *uuidsArray	= [NSArray arrayWithObjects:[CBUUID UUIDWithString:peripheralUuidString], nil];
    NSUUID *proximityUUID = [[NSUUID alloc] initWithUUIDString:peripheralUuidString];
    NSArray *uuidsArray	= [NSArray arrayWithObjects:proximityUUID,nil];
  NSArray *peripherals = [NSArray array];
  peripherals = [centralManager retrievePeripheralsWithIdentifiers:uuidsArray];
  centralPeripheral = peripherals[0];
  
  NSArray *services = centralPeripheral.services;
  CBService *service;
  BOOL serviceExist = false;
  for(service in services) {
    if ([service.UUID.UUIDString isEqualToString:serviceUuidString]) {
      serviceExist = true;
      break;
    }
  }
  if (serviceExist == true) {
    CBCharacteristic *characteristic;
    NSArray *characteristics = service.characteristics;
    BOOL characteristicExist = false;
    for(characteristic in characteristics) {
      if ([characteristic.UUID.UUIDString isEqualToString:characteristicUuidString]) {
        characteristicExist = true;
        break;
      }
    }
    if (characteristicExist == true) {
      CBDescriptor *descriptor;
      NSArray *descriptors = characteristic.descriptors;
      BOOL descriptorExist = false;
      for(descriptor in descriptors){
        if ([descriptor.UUID.UUIDString isEqualToString:descriptorUuidString]) {
          descriptorExist = true;
          break;
        }
      }
      if (descriptorExist == true) {
        Byte bytes[value.count];
        NSNumber *num;
        for(int i=0;i<value.count;i++){
          num = value[i];
          bytes[i] = (Byte) [num intValue];
        }
        NSData *data = [[NSData alloc] initWithBytes:bytes length:value.count];
        [centralPeripheral writeValue:data forDescriptor:descriptor];
      }
    }
  }
}



/****************************************************************************/
/*								蓝牙回调函数                                  */
/****************************************************************************/

- (void) centralManagerDidUpdateState:(CBCentralManager *)central {
  NSMutableDictionary * paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:[self NSStringForCBCentralManagerState:[central state]] forKey:PARAM_STATE];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_STATE_CHANGED body:[paramDic copy]];
}

- (NSString *) NSStringForCBCentralManagerState:(CBCentralManagerState)state {
  int stateInt;
  switch (state) {
    case CBCentralManagerStateResetting:
      stateInt = STATE_RESETTING;
      break;
    case CBCentralManagerStateUnsupported:
      stateInt = STATE_UNSUPPORTED;
      break;
    case CBCentralManagerStateUnauthorized:
      stateInt = STATE_UNAUTHORIZED;
      break;
    case CBCentralManagerStatePoweredOff:
      stateInt = STATE_OFF;
      break;
    case CBCentralManagerStatePoweredOn:
      stateInt = STATE_ON;
      break;
    case CBCentralManagerStateUnknown:
    default:
      stateInt = STATE_UNKNOWN;
  }
  return [NSString stringWithFormat:@"%d",stateInt];
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral
     advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI {
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:COMMON_PERIPHERAL_UUID];
  [paramDic setValue:peripheral.name forKey:PARAM_DEVICE_NAME];
  [paramDic setValue:RSSI forKey:PARAM_RSSI];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_PERIPHERAL_SCANNED body:[paramDic copy]];
}

- (void) centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
  if (isAutomaticDiscovering) {
    [peripheral discoverServices:nil];
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:COMMON_PERIPHERAL_UUID];
  [paramDic setValue:peripheral.name forKey:PARAM_DEVICE_NAME];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_PERIPHERAL_CONNECTED body:[paramDic copy]];
}

- (void) centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:COMMON_PERIPHERAL_UUID];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_PERIPHERAL_FAIL_TO_CONNECT body:[paramDic copy]];
  [self onBleError:peripheral error:error];
}

- (void) centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:COMMON_PERIPHERAL_UUID];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_PERIPHERAL_DISCONNECTED body:[paramDic copy]];
}

- (void) peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  NSArray *services = [peripheral services];
  //TODO:当services为空的情况需要测试
  if (!services || ![services count]) {
      [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_FINISH_DISCOVER body:[self constructTotalInformation:peripheral]];
  } else {
      //这种写法有问题吗？
      Pair pair;
      pair.total = (int)[services count];
      pair.count = 0;
      NSValue *pairValue = [NSValue valueWithBytes:&pair objCType:@encode(Pair)];
      [discoverTreeDic setValue:pairValue forKey:@"peripheral"];
      
  }
  NSMutableArray *serviceObjects = [[NSMutableArray alloc]init];
  CBService *service;
  for(service in services){
    [serviceObjects addObject:[self constructBleServiceObject:service]];
    if (isAutomaticDiscovering) {
      [peripheral discoverCharacteristics:nil forService:service];
    }
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue:[serviceObjects copy] forKey:COMMON_SERVICES];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_SERVICES_DISCOVERED body:[paramDic copy]];
}

- (void) peripheral:(CBPeripheral *)peripheral didDiscoverIncludedServicesForService:(nonnull CBService *)service error:(nullable NSError *)error {
  CBService *includedService;
  NSArray *includedServices = service.includedServices;
  //TODO:includedServices为空需要被测试
  if (!includedServices || ![includedServices count]) {
      //不用做
  } else {
      //需要测试
      [self changePairKey:@"peripheral" addTotal:(int)[includedServices count] addCount:0];
  }
  NSMutableArray *serviceObjects = [[NSMutableArray alloc]init];
  for(includedService in includedServices){
    [serviceObjects addObject:[self constructBleServiceObject:includedService]];
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue:peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue:service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue:[serviceObjects copy] forKey:COMMON_SERVICES];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_INCLUDED_SERVICES_DISCOVERED body:[paramDic copy]];
}

- (void) peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
  //TODO:error时的情况需要测试
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  NSArray *characteristics = [service characteristics];
  //TODO:当characteristics为空的情况需要测试
  if (!characteristics || ![characteristics count]) {
      //这里改变了count进行判断
      BOOL isPeripheralCompleted = [self changePairKey:@"peripheral" addTotal:0 addCount:1];
      if(isPeripheralCompleted){
          [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_FINISH_DISCOVER body:[self constructTotalInformation:peripheral]];
      }
  } else {
      //需要测试
      Pair pair;
      pair.total = (int)[characteristics count];
      pair.count = 0;
      //这里有疑问是不是使用
      //NSValue *pairValue = [NSValue value:&pair withObjCType:@encode(Pair)];
      NSValue *pairValue = [NSValue valueWithBytes:&pair objCType:@encode(Pair)];
      [discoverTreeDic setValue:pairValue forKey:service.UUID.UUIDString];
  }
  NSMutableArray *characteristicObjects = [[NSMutableArray alloc]init];
  CBCharacteristic *characteristic;
  for (characteristic in characteristics) {
    [characteristicObjects addObject:[self constructBleCharacteristicObject:characteristic]];
    if (isAutomaticDiscovering) {
      [peripheral discoverDescriptorsForCharacteristic:characteristic];
    }
  }
  
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: service.peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: [characteristicObjects copy] forKey:COMMON_CHARACTERISTICS];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_CHARACTERISTICS_DISCOVERED body:[paramDic copy]];
}

- (void) peripheral:(CBPeripheral *)peripheral didDiscoverDescriptorsForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
  
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  NSArray *descriptors = [characteristic descriptors];
  //不管有没有descriptor，其父亲service都要加1
  BOOL isServiceCompleted = [self changePairKey:characteristic.service.UUID.UUIDString addTotal:0 addCount:1];
  if(isServiceCompleted){
      BOOL isPeripheralCompleted = [self changePairKey:@"peripheral" addTotal:0 addCount:1];
      if (isPeripheralCompleted) {
          [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_FINISH_DISCOVER body:[self constructTotalInformation:peripheral]];
      }
  }
  NSMutableArray *descriptorObjects = [[NSMutableArray alloc]init];
  CBDescriptor *descriptor;
  for (descriptor in descriptors) {
    [descriptorObjects addObject:[self constructBleDescriptorObject:descriptor]];
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: characteristic.service.peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: characteristic.service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: characteristic.UUID.UUIDString forKey:PARAM_COMMON_CHARACTERISTIC_UUID];
  [paramDic setValue: [descriptorObjects copy] forKey:COMMON_DESCRIPTORS];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_DESCRIPTORS_DISCOVERED body:[paramDic copy]];
}

- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(nonnull CBCharacteristic *)characteristic error:(nullable NSError *)error {
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: characteristic.service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: characteristic.UUID.UUIDString forKey:PARAM_COMMON_CHARACTERISTIC_UUID];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_CHARACTERISTICS_WRITTEN body:[paramDic copy]];
}

- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(nonnull CBCharacteristic *)characteristic error:(nullable NSError *)error {
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: characteristic.service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: characteristic.UUID.UUIDString forKey:PARAM_COMMON_CHARACTERISTIC_UUID];
  [paramDic setValue: [self constructBleByteArrayToIntArray:characteristic.value] forKey:COMMON_VALUE];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_CHARACTERISTICS_CHANGED body:[paramDic copy]];
  //为了提示读
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_CHARACTERISTICS_READ body:[paramDic copy]];
}

- (void) peripheral:(CBPeripheral *)peripheral didWriteValueForDescriptor:(nonnull CBDescriptor *)descriptor error:(nullable NSError *)error {
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: descriptor.characteristic.service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: descriptor.characteristic.UUID.UUIDString forKey:PARAM_COMMON_CHARACTERISTIC_UUID];
  [paramDic setValue: descriptor.UUID.UUIDString forKey:PARAM_COMMON_DESCRIPTOR_UUID];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_DESCRIPTORS_WRITTEN body:[paramDic copy]];
}


- (void) peripheral:(CBPeripheral *)peripheral didUpdateValueForDescriptor:(nonnull CBDescriptor *)descriptor error:(nullable NSError *)error {
  if (error != nil) {
    [self onBleError:peripheral error:error];
    return ;
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  [paramDic setValue: descriptor.characteristic.service.UUID.UUIDString forKey:PARAM_COMMON_SERVICE_UUID];
  [paramDic setValue: descriptor.characteristic.UUID.UUIDString forKey:PARAM_COMMON_CHARACTERISTIC_UUID];
  [paramDic setValue: descriptor.UUID.UUIDString forKey:PARAM_COMMON_DESCRIPTOR_UUID];
  [paramDic setValue: descriptor.value forKey:COMMON_VALUE];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_DESCRIPTORS_CHANGED body:[paramDic copy]];
  //为了提示读
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_DESCRIPTORS_READ body:[paramDic copy]];
  
}
/****************************************************************************/
/*								               Tools                        */
/****************************************************************************/

- (void) onBleError:(CBPeripheral *)peripheral error:(NSError *)error {
  NSMutableDictionary * paramDic = [[NSMutableDictionary alloc]init];
  if (peripheral != nil) {
    [paramDic setValue:peripheral.identifier.UUIDString forKey:PARAM_COMMON_PERIPHERAL_UUID];
  }
  NSString *errorString = [error localizedDescription];
  [paramDic setValue:errorString forKey:ERROR_PARAM_MESSAGE];
  [self.bridge.eventDispatcher sendDeviceEventWithName:EVENT_BLE_ERROR body:[paramDic copy]];
}

- (NSDictionary *)constructBleServiceObject:(CBService *)service {
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: service.UUID.UUIDString forKey:COMMON_SERVICE_UUID];
  NSString *type = @"PRIMARY";
  if(!service.isPrimary){
    type = @"SECONDARY";
  }
  [paramDic setValue: type forKey:PARAM_TYPE];
  
  return [paramDic copy];
}

- (NSDictionary *)constructBleCharacteristicObject:(CBCharacteristic *)characteristic {
  NSString *notifying = @"false";
  if (characteristic.isNotifying) {
    notifying = @"true";
  }
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: characteristic.UUID.UUIDString forKey:COMMON_CHARACTERISTIC_UUID];
  [paramDic setValue: [converter getCharacteristicProperties:characteristic.properties] forKey: PARAM_CHARACTERISTIC_PROPERTIES];
  [paramDic setValue:notifying forKey:PARAM_CHARACTERISTIC_ISNOTIFYING];
  [paramDic setValue: characteristic.value forKey:COMMON_VALUE];
  return [paramDic copy];
}

- (NSDictionary *)constructBleDescriptorObject:(CBDescriptor *)descriptor {
  NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
  [paramDic setValue: descriptor.UUID.UUIDString forKey:COMMON_DESCRIPTOR_UUID];
  //Todo这里要看下value的样子[peripheral readValueForDescriptor:descriptor];是异步的
  [paramDic setValue: descriptor.value forKey:COMMON_VALUE];
  return [paramDic copy];
}

- (NSArray *)constructBleByteArrayToIntArray:(NSData *)data {
  NSMutableArray *result = [[NSMutableArray alloc]init];
  Byte *bytes = (Byte *)[data bytes];
  for (int i=0; i<data.length; i++) {
  }
  NSNumber *num;
  for (int i=0; i<data.length; i++) {
    num = [NSNumber numberWithInt:(int)bytes[i]];
    [result addObject:num];
  }
  return [result copy];
}
- (BOOL)changePairKey:(NSString *)key addTotal:(int) total addCount:(int) count {
    NSValue *pairValue = discoverTreeDic[key];
    Pair pair;
    [pairValue getValue:&pair];
    pair.total = pair.total+total;
    pair.count = pair.count+count;
    NSValue *newValue = [NSValue valueWithBytes:&pair objCType:@encode(Pair)];
    [discoverTreeDic setValue:newValue forKey:key];
    if (pair.total == pair.count) {
        return true;
    }
    return false;
}

- (NSDictionary *)constructTotalInformation:(CBPeripheral *)peripheral {
    NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
    NSMutableArray *serviceObject = [[NSMutableArray alloc]init];
    
    NSArray *services = peripheral.services;
    CBService *service;
    for (service in services) {
        [serviceObject addObject:[self constructBleServiceObjectAll:service]];
    }
    [paramDic setValue:peripheral.identifier.UUIDString forKey:COMMON_PERIPHERAL_UUID];
    [paramDic setValue:peripheral.name forKey:PARAM_DEVICE_NAME];
    [paramDic setValue:[serviceObject copy] forKey:COMMON_SERVICES];
    NSLog(@"%@",paramDic);
    return [paramDic copy];
}

//循环构造
- (NSDictionary *)constructBleServiceObjectAll:(CBService *)service {
    NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
    NSMutableArray *characteristicObject = [[NSMutableArray alloc]init];
    NSMutableArray *includedServicesObject = [[NSMutableArray alloc]init];

    NSArray *characteristics = service.characteristics;
    CBCharacteristic *characteristic;
    for(characteristic in characteristics) {
        [characteristicObject addObject:[self constructBleCharacteristicObjectAll:characteristic]];
    }
    
    NSArray *includedServices = service.includedServices;
    CBService *includedService;
    for(includedService in includedServices) {
        [includedServicesObject addObject:[self constructBleServiceObjectAll:service]];
    }
    
    [paramDic setValue: service.UUID.UUIDString forKey:COMMON_SERVICE_UUID];
    [paramDic setValue: [characteristicObject copy] forKey:COMMON_CHARACTERISTICS];
    [paramDic setValue: [includedServicesObject copy] forKey:COMMON_INCLUDED_SERVICES];
    NSString *type = @"PRIMARY";
    if(!service.isPrimary){
        type = @"SECONDARY";
    }
    [paramDic setValue: type forKey:PARAM_TYPE];
    
    
    return [paramDic copy];
}

- (NSDictionary *)constructBleCharacteristicObjectAll:(CBCharacteristic *)characteristic {
    NSString *notifying = @"false";
    if (characteristic.isNotifying) {
        notifying = @"true";
    }
    NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
    NSMutableArray *descriptorObject =  [[NSMutableArray alloc]init];
    
    NSArray *descriptors = characteristic.descriptors;
    CBDescriptor *descriptor;
    for(descriptor in descriptors){
        [descriptorObject addObject:[self constructBleDescriptorObject:descriptor]];
    }
    [paramDic setValue: characteristic.UUID.UUIDString forKey:COMMON_CHARACTERISTIC_UUID];
    [paramDic setValue: [converter getCharacteristicProperties:characteristic.properties] forKey: PARAM_CHARACTERISTIC_PROPERTIES];
    [paramDic setValue: notifying forKey:PARAM_CHARACTERISTIC_ISNOTIFYING];
    [paramDic setValue: characteristic.value forKey:COMMON_VALUE];
    [paramDic setValue: [descriptorObject copy] forKey:COMMON_DESCRIPTORS];
    return [paramDic copy];
}

- (NSDictionary *)constructBleDescriptorObjectAll:(CBDescriptor *)descriptor {
    NSMutableDictionary *paramDic = [[NSMutableDictionary alloc]init];
    [paramDic setValue: descriptor.UUID.UUIDString forKey:COMMON_DESCRIPTOR_UUID];
    //Todo这里要看下value的样子[peripheral readValueForDescriptor:descriptor];好像是异步的
    [paramDic setValue: descriptor.value forKey:COMMON_VALUE];
    return [paramDic copy];
}



@end
