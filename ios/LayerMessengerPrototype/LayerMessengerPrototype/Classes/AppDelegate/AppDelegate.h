//
//  AppDelegate.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Atlas.h>
#import "AuthenticationViewController.h"
@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) ATLConversationListViewController* conversationsViewController;
@property (strong, nonatomic) UINavigationController* navController;
@property (strong, nonatomic) AuthenticationViewController* authViewController;
@property (nonatomic) LYRClient *layerClient;

- (void)authenticateLayerWithUsername:(NSString *)username andPassword:(NSString*)password completion:(void (^)(BOOL success, NSError * error))completion;

@end

