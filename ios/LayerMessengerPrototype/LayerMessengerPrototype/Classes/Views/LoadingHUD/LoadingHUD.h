//
//  LoadingHUD.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 12/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "MBProgressHUD.h"

@interface LoadingHUD : MBProgressHUD

+ (MB_INSTANCETYPE)showHUDAddedTo:(UIView *)view animated:(BOOL)animated;

+(NSNumber*) delayForShowingText;
+(void) setDelayForShowingText:(NSNumber*)delay;

+(UIColor*) labelColor;
+(void) setLabelColor:(UIColor*)color;

-(void) hide:(BOOL)animated afterShowingText:(NSString*)text;

@end
