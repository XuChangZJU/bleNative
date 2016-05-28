//
//  BleConstantsConverter.h
//  BleNative
//
//  Created by biliyuan on 16/4/29.
//  Copyright © 2016年 Facebook. All rights reserved.
//
#import <UIKit/UIKit.h>

@interface BleConstantsConverter : NSObject
- (NSArray *)getAllStringsByBitmask:(NSInteger)param array:(NSArray *)array;
- (NSArray *)getCharacteristicProperties:(NSInteger)properties;
- (NSArray *)getCharacteristicPermissions:(NSInteger)permissions;
@end
