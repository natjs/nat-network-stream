//
//  NatStream.h
//
//  Created by huangyake on 17/1/7.
//  Copyright © 2017 Nat. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol NatWorkProtocol <NSObject>

- (id)sendRequest:(NSURLRequest *)request withSendingData:(void (^)(int64_t bytesSent, int64_t totalBytes))sendDataCallback
     withResponse:(void (^)(NSURLResponse *response))responseCallback
  withReceiveData:(void (^)(NSData *data))receiveDataCallback
  withCompeletion:(void (^)(NSData *totalData, NSError *error))compeletionCallback;

@end


@interface NatStream : NSObject
typedef void (^NatCallback)(id error, id result);
+ (NatStream *)singletonManger;
//网络请求 参数
// method: String (GET | POST | PUT | PATCH | DELETE | HEAD, def: GET)
// url: String
// headers: Object(请求头)
//type: String (json | jsonp | text, def: json) (返回值格式)
// body: String(请求参数)
- (void)fetch:(NSDictionary *)params :(NatCallback)callback;

@end
