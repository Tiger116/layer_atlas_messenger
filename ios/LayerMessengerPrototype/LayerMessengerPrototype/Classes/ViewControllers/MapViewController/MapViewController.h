//
//  MapViewController.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 02/04/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MapKit/MapKit.h>

@protocol MapViewControllerDelegate <NSObject>

/**
 *  Tells the delegate to send provided location.
 *
 *  @param location Location to send.
 */
-(void) sendLocation:(CLLocation *)location;

@end

@interface MapViewController : UIViewController

@property (nonatomic) CLLocation* locationToDisplay;
@property (nonatomic) CLLocation* markedLocation;
@property (nonatomic, weak) id<MapViewControllerDelegate> delegate;

@end
