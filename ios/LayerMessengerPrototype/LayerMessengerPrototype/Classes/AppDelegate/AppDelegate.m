//
//  AppDelegate.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "AppDelegate.h"
#import <LayerKit/LayerKit.h>
#import <Parse/Parse.h>
#import "LoadingHUD.h"



static NSString *const LayerAppIDString = @"07b40518-aaaa-11e4-bceb-a25d000000f4";

NSString *const ConversationMetadataDidChangeNotification = @"ConversationMetadataDidChangeNotification";
NSString *const ConversationParticipantsDidChangeNotification = @"ConversationParticipantsDidChangeNotification";
NSString *const ConversationDidCreatedNotification = @"ConversationDidCreatedNotification";
NSString *const LayerClientDidFinishSynchronizationNotification = @"LayerClientDidFinishSynchronizationNotification";

NSString* const metadataTitleKey = @"title";
NSString* const metadataOwnerIdKey = @"owner";

@interface AppDelegate () <LYRClientDelegate>

@end

@implementation AppDelegate

/**
 *  Tells the delegate that the launch process is almost done and the app is almost ready to run. Method initializes Parse and LYRClient object, displays first view controller (AuthenticationViewController).
 *
 */
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Initialize Parse.
    [Parse setApplicationId:@"hE41H4TvIuyn1eiPMV8E7mSOFCxAM5sBnhv9b3D8"
                  clientKey:@"XTcDzrh0b2E299VsdeP7YqzuzBkSk0dUIIW2w6Gx"];
    
    //Initializes a LYRClient object
    NSUUID *appID = [[NSUUID alloc] initWithUUIDString:LayerAppIDString];
    self.layerClient = [LYRClient clientWithAppID:appID];
    self.layerClient.delegate = self;
    self.layerClient.autodownloadMIMETypes = [NSSet setWithObjects:ATLMIMETypeImageJPEGPreview, ATLMIMETypeTextPlain, nil];
    
    //Register for remote notifications
    // Checking if app is running iOS 8
    if ([application respondsToSelector:@selector(registerForRemoteNotifications)]) {
        // Register device for iOS8
        UIUserNotificationSettings *notificationSettings = [UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound categories:nil];
        [application registerUserNotificationSettings:notificationSettings];
        [application registerForRemoteNotifications];
    } else {
        // Register device for iOS7
        [application registerForRemoteNotificationTypes:UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeBadge];
    }
    
    //Initialize LoadingHUD style
//    [LoadingHUD setLabelColor:[UIColor blueColor]];
    
    self.authViewController = [[AuthenticationViewController alloc] initWithNibName:@"AuthenticationViewController" bundle:nil];
    self.navController = [[UINavigationController alloc] initWithRootViewController:self.authViewController];
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [self.window setRootViewController:self.navController];
    [self.window makeKeyAndVisible];
    
    return YES;
}

/**
 *  Tells the delegate that the app successfully registered with Apple Push Notification service (APNs).
 *
 *  Provided device token is submited to Layer.
 */
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSError *error;
    BOOL success = [self.layerClient updateRemoteNotificationDeviceToken:deviceToken error:&error];
    if (success) {
        NSLog(@"Application did register for remote notifications");
    } else {
        NSLog(@"Error updating Layer device token for push:%@", error);
    }
}

/**
 *  Tells the delegate that the running app received a remote notification.
 *
 *  If application wasn't in foreground method will present view controller with conversation where message in notification comes from.
 */
- (void) application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
{
    if (application.applicationState == UIApplicationStateInactive)
    {
        if (self.messagesViewController
            && [self.navController.viewControllers containsObject:self.messagesViewController]
            && [[self.messagesViewController.conversation.identifier absoluteString] isEqualToString:userInfo[@"layer"][@"conversation_identifier"]])
        {
            [self.navController popToViewController:self.messagesViewController animated:YES];
        }else
        {
            LYRQuery *query = [LYRQuery queryWithClass:[LYRConversation class]];
            query.predicate = [LYRPredicate predicateWithProperty:@"identifier" operator:LYRPredicateOperatorIsEqualTo value:userInfo[@"layer"][@"conversation_identifier"]];
            LYRConversation *conversation = [[self.layerClient executeQuery:query error:nil] firstObject];
            [self.conversationsViewController presentControllerWithConversation:conversation];
        }
    }
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    application.applicationIconBadgeNumber = 0;
}

#pragma mark - Layer Authentication Methods

/**
 *  Method requests authentication nonce from Layer. Then uses it, given username and password to get an identity token from Parse. Executes completion block when finished.
 *
 *  @param username   Username for Parse user.
 *  @param password   Password for Parse user.
 *  @param completion Block that will be executed in the end. If method finished succesfully block's parameters are (YES, nil), else - (NO, error)
 */
- (void)authenticateLayerWithUsername:(NSString *)username andPassword:(NSString*)password completion:(void (^)(BOOL success, NSError * error))completion
{
    if (self.layerClient.authenticatedUserID) {
        NSLog(@"Layer Authenticated as User %@", self.layerClient.authenticatedUserID);
        if (completion) completion(YES, nil);
        return;
    }
    
    // Request an authentication nonce from Layer
    [self.layerClient requestAuthenticationNonceWithCompletion:^(NSString *nonce, NSError *error) {
        NSLog(@"Authentication nonce %@", nonce);
        
        // Upon reciept of nonce, post to your backend and acquire a Layer identityToken
        if (nonce) {
            
            [PFUser logInWithUsernameInBackground:username password:password block:^(PFUser *user, NSError *error) {
                if (user)
                {
                    NSString *userID  = user.objectId;
                    [PFCloud callFunctionInBackground:@"generateToken"
                                       withParameters:@{@"nonce" : nonce,
                                                        @"userID" : userID}
                                                block:^(NSString *token, NSError *error) {
                        if (!error) {
                            // Send the Identity Token to Layer to authenticate the user
                            NSLog(@"Token:%@",token);
                            [self.layerClient authenticateWithIdentityToken:token completion:^(NSString *authenticatedUserID, NSError *error) {
                                if (!error) {
                                    NSLog(@"Parse User authenticated with Layer Identity Token");
                                    completion(YES,nil);
                                }
                                else{
                                    NSLog(@"Parse User failed to authenticate with token with error: %@", error);
                                    completion(NO,error);
                                }
                            }];
                        }
                        else{
                            NSLog(@"Parse Cloud function failed to be called to generate token with error: %@", error);
                            completion(NO,error);
                        }
                
                    }];
                }else{
                    NSLog(@"Parse User failed to log in with error: %@",error);
                    completion(NO,error);
                }
            }];
        }
    }];
}

#pragma - mark LYRClientDelegate Delegate Methods

/**
 *  Tells the delegate that objects associated with the client have changed due to local mutation or synchronization activities. Method identifies changes and posts notifications about them.
 *
 *  @param client  The client that received the changes.
 *  @param changes An array of `NSDictionary` objects, each one describing a change.
 */
- (void)layerClient:(LYRClient *)client objectsDidChange:(NSArray *)changes
{
    NSLog(@"Layer Client objects did change");
    for (NSDictionary *change in changes)
    {
        id changedObject = change[LYRObjectChangeObjectKey];
        if (![changedObject isKindOfClass:[LYRConversation class]]) continue;
        
        LYRObjectChangeType changeType = [change[LYRObjectChangeTypeKey] integerValue];
        NSString *changedProperty = change[LYRObjectChangePropertyKey];
        
        if (changeType == LYRObjectChangeTypeUpdate && [changedProperty isEqualToString:@"metadata"]) {
            [[NSNotificationCenter defaultCenter] postNotificationName:ConversationMetadataDidChangeNotification object:changedObject];
        }
        
        if (changeType == LYRObjectChangeTypeUpdate && [changedProperty isEqualToString:@"participants"]) {
            [[NSNotificationCenter defaultCenter] postNotificationName:ConversationParticipantsDidChangeNotification object:changedObject];
        }
        
        if (changeType == LYRObjectChangeTypeCreate) {
            [[NSNotificationCenter defaultCenter] postNotificationName:ConversationDidCreatedNotification object:changedObject];
        }
    }
}

- (void)layerClient:(LYRClient *)client didReceiveAuthenticationChallengeWithNonce:(NSString *)nonce
{
    NSLog(@"Layer Client did recieve authentication challenge with nonce: %@", nonce);
}

- (void)layerClient:(LYRClient *)client didAuthenticateAsUserID:(NSString *)userID
{
    NSLog(@"Layer Client did recieve authentication nonce");
}

- (void)layerClientDidDeauthenticate:(LYRClient *)client
{
    NSLog(@"Layer Client did deauthenticate");
}

- (void)layerClient:(LYRClient *)client didFinishSynchronizationWithChanges:(NSArray *)changes
{
    NSLog(@"Layer Client did finish sychronization");
    [[NSNotificationCenter defaultCenter] postNotificationName:LayerClientDidFinishSynchronizationNotification object:nil];
}

- (void)layerClient:(LYRClient *)client didFailSynchronizationWithError:(NSError *)error
{
    NSLog(@"Layer Client did fail synchronization with error: %@", error);
}

- (void)layerClient:(LYRClient *)client willAttemptToConnect:(NSUInteger)attemptNumber afterDelay:(NSTimeInterval)delayInterval maximumNumberOfAttempts:(NSUInteger)attemptLimit
{
    NSLog(@"Layer Client will attempt to connect");
}

- (void)layerClientDidConnect:(LYRClient *)client
{
    NSLog(@"Layer Client did connect");
}

- (void)layerClient:(LYRClient *)client didLoseConnectionWithError:(NSError *)error
{
    NSLog(@"Layer Client did lose connection with error: %@", error);
}

- (void)layerClientDidDisconnect:(LYRClient *)client
{
    NSLog(@"Layer Client did disconnect");
}


@end
