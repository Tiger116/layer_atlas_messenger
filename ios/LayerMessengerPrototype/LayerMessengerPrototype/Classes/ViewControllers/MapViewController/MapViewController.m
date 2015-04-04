//
//  MapViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 02/04/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "MapViewController.h"

@interface MapViewController ()

@property (strong, nonatomic) IBOutlet MKMapView *mapView;
@property (strong, nonatomic) IBOutlet UIButton *sendButton;
@property (nonatomic) MKPointAnnotation* pin;

@end

@implementation MapViewController

/**
 *  Called after the controller's view is loaded into memory. 
 *
 *  Finishes initializtion of view controller.
 */
- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.sendButton.hidden = YES;
    
    UILongPressGestureRecognizer *lpgr = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPress:)];
    lpgr.minimumPressDuration = 0.5;
    [self.mapView addGestureRecognizer:lpgr];
}

/**
 *  Notifies the view controller that its view is about to be added to a view hierarchy.
 *
 *  Adds provided pin to a map and presents provided location.
 *
 *  @param animated If YES, the view is being added to the window using an animation.
 */
- (void) viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    if (self.markedLocation)
    {
        [self placePin:self.markedLocation.coordinate];
        if (!self.locationToDisplay)
        {
            self.locationToDisplay = self.markedLocation;
        }
    }
    
    if (self.locationToDisplay)
    {
        MKCoordinateRegion viewRegion = MKCoordinateRegionMakeWithDistance(self.locationToDisplay.coordinate, 1000, 1000);
        [self.mapView setRegion:viewRegion animated:YES];
    }
}

/**
 *  Handler for long press gesture recognizer. 
 *
 *  Used to add a pin to a map.
 *
 *  @param gestureRecognizer UIGestureRecognizer object which recognized long press
 */
- (void)handleLongPress:(UIGestureRecognizer *)gestureRecognizer
{
    if (gestureRecognizer.state != UIGestureRecognizerStateBegan)
    {
        return;
    }
    CGPoint touchPoint = [gestureRecognizer locationInView:self.mapView];
    CLLocationCoordinate2D touchMapCoordinate = [self.mapView convertPoint:touchPoint toCoordinateFromView:self.mapView];
    [self placePin:touchMapCoordinate];
}

/**
 *  Method adds a pin to a map.
 *
 *  @param coordinate CLLocationCoordinate2D coordinates for a pin.
 */
- (void)placePin:(CLLocationCoordinate2D)coordinate
{
    if (self.pin) {
        [self.mapView removeAnnotation:self.pin];
    }
    self.pin = [[MKPointAnnotation alloc] init];
    self.pin.coordinate = coordinate;
    [self.mapView addAnnotation:self.pin];
    self.sendButton.hidden = NO;
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/**
 *  Dismisses view controller with map.
 *
 *  @param sender tapped UIButton.
 */
- (IBAction)closeButtonDidTapped:(UIButton *)sender
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

/**
 *  Tells the delegate to send pin's location.
 *
 *  @param sender tapped UIButton.
 */
- (IBAction)sendButtonDidTapped:(UIButton *)sender
{
    if ([self.delegate respondsToSelector:@selector(sendLocation:)]) {
        [self.delegate sendLocation:[[CLLocation alloc] initWithLatitude:self.pin.coordinate.latitude longitude:self.pin.coordinate.longitude]];
    }
    [self dismissViewControllerAnimated:YES completion:nil];
}

/**
 *  Used to change map type.
 *
 *  @param sender UISegmentedControl object which called method.
 */
- (IBAction)mapTypeDidChanged:(UISegmentedControl *)sender
{
    switch (sender.selectedSegmentIndex) {
        case 0:
            self.mapView.mapType = MKMapTypeStandard;
            break;
        case 1:
            self.mapView.mapType = MKMapTypeHybrid;
            break;
        case 2:
            self.mapView.mapType = MKMapTypeSatellite;
            break;
    }
}


@end
