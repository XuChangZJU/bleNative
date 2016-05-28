//
//  BleConstantsConverter.m
//  BleNative
//
//  Created by biliyuan on 16/4/29.
//  Copyright © 2016年 Facebook. All rights reserved.
//

#import "BleConstantsConverter.h"
#import <Foundation/Foundation.h>






 
@interface BleConstantsConverter (){
  NSArray *BLE_CHARACTERISTIC_PROPERITIES;
  NSArray *BLE_CHARACTERISTIC_PERMISSIONS;
  }
@end

@implementation BleConstantsConverter

- (instancetype)init {
  if (self = [super init]) {
    
  }
  BLE_CHARACTERISTIC_PROPERITIES = [NSArray arrayWithObjects:
                                    @"property_broadcast",
                                    @"property_read",
                                    @"property_write_no_response",
                                    @"property_write",
                                    @"property_notify",
                                    @"property_indicate",
                                    @"property_signed_write",
                                    @"property_extended_props",
                                    @"property_notify_encryption_required",
                                    @"property_indicate_encryption_required",nil];
//  CBAttributePermissionsReadable					= 0x01,
//  CBAttributePermissionsWriteable					= 0x02,
//  CBAttributePermissionsReadEncryptionRequired	= 0x04,
//  CBAttributePermissionsWriteEncryptionRequired	= 0x08

  BLE_CHARACTERISTIC_PERMISSIONS = [NSArray arrayWithObjects:
                                    @"permission_read",
                                    @"permission_write",
                                    @"permission_read_encrypted",
                                    @"permission_write_encrypted",nil];
  return self;
}



- (NSArray *)getAllStringsByBitmask:(NSInteger)param array:(NSArray *)array {
  NSMutableArray *result = [[NSMutableArray alloc]init];
  NSString *paramString16 = [NSString stringWithFormat:@"%lx",param];
  NSString *paramString10 = [self turn16to10:paramString16];
  NSString *paramString2 = [self turn10to2:paramString10];
  for (int i=0;i<paramString2.length;i++) {
    if([paramString2 characterAtIndex:paramString2.length-i-1]=='0'){
      continue;
    }else{
      [result addObject:array[i]];
    }
  }
  NSLog(@"%@",result);
  return [result copy];
}

- (NSString *) turn16to10:(NSString *)str{
  int sum = 0;
  for (int i = 0; i < str.length; i++) {
    sum *= 16;
    char c = [str characterAtIndex:i] ;
    if (c >= 'A') {
      sum += c - 'A' + 10;
    }else{
      sum += c - '0';
    }
  }
  return [NSString stringWithFormat:@"%d",sum];
}

- (NSString *) turn10to2:(NSString *)str{
  int num = [str intValue];
  
  NSMutableString * result = [[NSMutableString alloc]init];
  while (num > 0) {
    NSString * reminder = [NSString stringWithFormat:@"%d",num % 2];
    [result insertString:reminder atIndex:0];
    num = num / 2;
  }
  return [result copy];
}
- (NSArray *)getCharacteristicProperties:(NSInteger)properties {
  return [self getAllStringsByBitmask:properties array:BLE_CHARACTERISTIC_PROPERITIES];
}

- (NSArray *)getCharacteristicPermissions:(NSInteger)permissions {
  return [self getAllStringsByBitmask:permissions array:BLE_CHARACTERISTIC_PROPERITIES];
}
@end