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
#import "MessagesViewController.h"
#import "ConversationsViewController.h"

extern NSString *const ConversationMetadataDidChangeNotification;
extern NSString *const ConversationParticipantsDidChangeNotification;
extern NSString *const ConversationDidCreatedNotification;
extern NSString *const LayerClientDidChangedObjectsNotification;

extern NSString* const metadataTitleKey;
extern NSString* const metadataOwnerIdKey;


extern NSString* const launchOptionsKeyForRemoteNotifications;

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) ConversationsViewController* conversationsViewController;
@property (strong, nonatomic) UINavigationController* navController;
@property (strong, nonatomic) AuthenticationViewController* authViewController;
@property (nonatomic,strong) MessagesViewController* messagesViewController;
@property (nonatomic) LYRClient *layerClient;
@property (nonatomic) NSDictionary* launchOptions;

- (void)authenticateLayerWithUsername:(NSString *)username andPassword:(NSString*)password completion:(void (^)(BOOL success, NSError * error))completion;

@end

