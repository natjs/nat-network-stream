//
//  NatStream.m
//
//  Created by huangyake on 17/1/7.
//  Copyright © 2017 Nat. All rights reserved.
//

#import "NatStream.h"

@interface NatStream ()

@end

@implementation NatStream
+ (NatStream *)singletonManger{
    static id manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (void)fetch:(NSDictionary *)params :(NatCallback)callback{
    //    __block NSInteger received = 0;
    __block NSHTTPURLResponse *httpResponse = nil;
    __block NSMutableDictionary * callbackRsp =[[NSMutableDictionary alloc] init];
    //    __block NSString *statusText = @"ERR_CONNECT_FAILED";
    
    NSString *method = [params objectForKey:@"method"];
    if ([NatStream isBlankString:method]) {
        // default HTTP method is GET
        method = @"GET";
    }
    NSString *urlStr = [params objectForKey:@"url"];
    if (!params || [NatStream isBlankString:urlStr]) {
        callback(@{@"error":@{@"code":@151040,@"msg":@"FETCH_INVALID_ARGUMENT"}},nil);
        
        return;
    }
    NSDictionary *headers = [params objectForKey:@"headers"];
    NSString *body = [params objectForKey:@"body"];
    NSString *type = [params objectForKey:@"type"];
    NSURL *url = [NSURL URLWithString:urlStr];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    [request setHTTPMethod:method];
    
    if ([type isEqual:@"text"]) {
        [request setValue:@"text/plain" forHTTPHeaderField:@"Content-Type"];
    }else if ([type isEqual:@"jsonp"]){
        [request setValue:@"application/javascript" forHTTPHeaderField:@"Content-Type"];
    }else{
        [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    }
    
    if ([params valueForKey:@"timeout"]){
        //ms
        [request setTimeoutInterval:([[params valueForKey:@"timeout"] floatValue])/1000];
    }
    
    for (NSString *header in headers) {
        NSString *value = [headers objectForKey:header];
        [request setValue:value forHTTPHeaderField:header];
    }
    
    
    [request setHTTPBody:[body dataUsingEncoding:NSUTF8StringEncoding]];
    //    [request seth]
    
    //    callback(nil,callbackRsp);
    NSOperationQueue *queue=[NSOperationQueue mainQueue];
    //    id<NatWorkProtocol> networkHandler = [WXHandlerFactory handlerForProtocol:@protocol(WXNetworkProtocol)];
    __block NSString *respEncode = nil;
    //    __weak typeof(self) weakSelf = self;
    [NSURLConnection sendAsynchronousRequest:request queue:queue completionHandler:^(NSURLResponse * _Nullable response, NSData * _Nullable data, NSError * _Nullable connectionError) {
        if (response == nil) {
            callback(@{@"error":@{@"code":@151050,@"msg":@"FETCH_NETWORK_ERROR"}},nil);
            return;
        }
        
        if (connectionError) {
            callback(@{@"error":@{@"code":@151000,@"msg":@"FETCH_INTERNAL_ERROR"}},nil);
            return;
        }
        
        httpResponse = (NSHTTPURLResponse *)response;
        NSLog(@"%@",response.description);
        [callbackRsp setObject:@(httpResponse.statusCode) forKey:@"status"];
        [callbackRsp setObject:httpResponse.allHeaderFields forKey:@"headers"];
        if (httpResponse.statusCode >= 200 && httpResponse.statusCode<300) {
            [callbackRsp setObject:@1 forKey:@"ok"];
        }else{
            [callbackRsp setObject:@0 forKey:@"ok"];
        }
        [callbackRsp setObject:[NatStream getStatusText:httpResponse.statusCode] forKey:@"statusText"];
        if (connectionError == nil) {
            respEncode = response.textEncodingName;
            NSString *responseData = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            
            if (responseData && responseData.length > 0) {
                [callbackRsp setObject:responseData forKey:@"data"];
            }
            callback(nil,callbackRsp);
            
            
        }else{
            callback(@{@"code":@(connectionError.code),@"msg":connectionError.domain},callbackRsp);
        }
    }];
    
}
+ (NSString*)getStatusText:(NSInteger)code
{
    switch (code) {
        case -1:
            return @"ERR_INVALID_REQUEST";
        case 100:
            return @"Continue";
            break;
        case 101:
            return @"Switching Protocol";
        case 102:
            return @"Processing";
            
        case 200:
            return @"OK";
        case 201:
            return @"Created";
        case 202:
            return @"Accepted";
        case 203:
            return @"Non-Authoritative Information";
        case 204:
            return @"No Content";
        case 205:
            return @" Reset Content";
        case 206:
            return @"Partial Content";
        case 207:
            return @"Multi-Status";
        case 208:
            return @"Already Reported";
        case 226:
            return @"IM Used";
            
        case 300:
            return @"Multiple Choices";
        case 301:
            return @"Moved Permanently";
        case 302:
            return @"Found";
        case 303:
            return @"See Other";
        case 304:
            return @"Not Modified";
        case 305:
            return @"Use Proxy";
        case 306:
            return @"Switch Proxy";
        case 307:
            return @"Temporary Redirect";
        case 308:
            return @"Permanent Redirect";
            
        case 400:
            return @"Bad Request";
        case 401:
            return @"Unauthorized";
        case 402:
            return @"Payment Required";
        case 403:
            return @"Forbidden";
        case 404:
            return @"Not Found";
        case 405:
            return @"Method Not Allowed";
        case 406:
            return @"Not Acceptable";
        case 407:
            return @"Proxy Authentication Required";
        case 408:
            return @"Request Timeout";
        case 409:
            return @"Conflict";
        case 410:
            return @"Gone";
        case 411:
            return @"Length Required";
        case 412:
            return @"Precondition Failed";
        case 413:
            return @"Payload Too Large";
        case 414:
            return @"URI Too Long";
        case 415:
            return @"Unsupported Media Type";
        case 416:
            return @"Range Not Satisfiable";
        case 417:
            return @"Expectation Failed";
        case 418:
            return @"I'm a teapot";
        case 421:
            return @"Misdirected Request";
        case 422:
            return @"Unprocessable Entity";
        case 423:
            return @"Locked";
        case 424:
            return @"Failed Dependency";
        case 426:
            return @"Upgrade Required";
        case 428:
            return @"Precondition Required";
        case 429:
            return @"Too Many Requests";
        case 431:
            return @"Request Header Fields Too Large";
        case 451:
            return @"Unavailable For Legal Reasons";
            
        case 500:
            return @"Internal Server Error";
        case 501:
            return @"Not Implemented";
        case 502:
            return @"Bad Gateway";
        case 503:
            return @"Service Unavailable";
        case 504:
            return @"Gateway Timeout";
        case 505:
            return @"HTTP Version Not Supported";
        case 506:
            return @"Variant Also Negotiates";
        case 507:
            return @"Insufficient Storage";
        case 508:
            return @"Loop Detected";
        case 510:
            return @"Not Extended";
        case 511:
            return @"Network Authentication Required";
        default:
            break;
    }
    
    return @"Unknown";
}

+ (BOOL)isBlankString:(NSString *)string {
    if (string == nil || string == NULL || [string isKindOfClass:[NSNull class]]) {
        return true;
    }
    if ([[string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] length] == 0) {
        return true;
    }
    
    return false;
}



@end
