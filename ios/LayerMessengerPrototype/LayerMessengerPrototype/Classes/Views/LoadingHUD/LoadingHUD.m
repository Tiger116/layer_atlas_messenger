//
//  LoadingHUD.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 12/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "LoadingHUD.h"

@interface LoadingHUD ()

@property (nonatomic, weak) UIView* view;

@end

@implementation LoadingHUD

/**
 *  Creates a new HUD, adds it to given view and shows it.
 *
 *  @param view     The view that the HUD will be added to
 *  @param animated If set to YES the HUD will appear using the current animationType. If set to NO the HUD will not use animations while appearing.
 *
 *  @return A reference to the created HUD.
 */
+ (MB_INSTANCETYPE)showHUDAddedTo:(UIView *)view animated:(BOOL)animated
{
    LoadingHUD* hud = [super showHUDAddedTo:view animated:animated];
    hud.view = view;
    if (_labelColor) {
        hud.labelColor = [[self class] labelColor];
    }
    return hud;
}

static NSNumber* _delayForShowingText;
+(NSNumber*) delayForShowingText
{
    if (!_delayForShowingText) {
        _delayForShowingText = [NSNumber numberWithDouble:1.0];
    }
    return _delayForShowingText;
}
+(void) setDelayForShowingText:(NSNumber*)delay;
{
    _delayForShowingText = delay;
}

static UIColor* _labelColor;
+(UIColor*) labelColor
{
    return _labelColor;
}
+(void) setLabelColor:(UIColor*)color
{
    _labelColor = color;
}

/**
 * Displays given text for preset '_delayForShowingText' time, then hides HUD
 *
 *  @param animated If set to YES the HUD will disappear using the current animationType. If set to NO the HUD will not use animations while disappearing.
 *  @param text     The text which will be displayed before HUD hides.
 */
-(void) hide:(BOOL)animated afterShowingText:(NSString*)text
{
    [self hide:YES];
    MBProgressHUD* hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
    hud.mode = MBProgressHUDModeText;
    hud.labelText = text;
    [hud hide:animated afterDelay:[[self class] delayForShowingText].doubleValue];
}



@end
