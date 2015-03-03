//
//  AppDelegate.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Atlas.h>
@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) ATLConversationListViewController* conversationsViewController;
@property (strong, nonatomic) UINavigationController* navController;


@end

