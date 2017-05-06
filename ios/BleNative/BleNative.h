
#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>
#import <React/RCTConvert.h>
#else // back compatibility for RN version < 0.40
#import "RCTBridgeModule.h"
#import "RCTEventDispatcher.h"
#import "RCTLog.h"
#import "RCTConvert.h"
#endif


#import <CoreBluetooth/CoreBluetooth.h>
#import "BleConstantsConverter.h"

@interface BleNative : NSObject <RCTBridgeModule>

@end
