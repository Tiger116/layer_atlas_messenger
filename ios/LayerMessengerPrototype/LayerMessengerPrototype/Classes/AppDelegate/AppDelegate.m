//
//  AppDelegate.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "AppDelegate.h"
#import <LayerKit/LayerKit.h>
#import "ConversationsViewController.h"
#import <Parse/Parse.h>


static NSString *const LayerAppIDString = @"07b40518-aaaa-11e4-bceb-a25d000000f4";

#if TARGET_IPHONE_SIMULATOR
// If on simulator set the user ID to Simulator and participant to Device
NSString *const LQSCurrentUserID = @"iOS_Simulator";
#else
// If on device set the user ID to Device and participant to Simulator
NSString *const LQSCurrentUserID = @"iOS_Device";
#endif

@interface AppDelegate () <LYRClientDelegate>

@property (nonatomic) LYRClient *layerClient;

@end

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Initialize window.
    
    
    // Initialize Parse.
    [Parse setApplicationId:@"hE41H4TvIuyn1eiPMV8E7mSOFCxAM5sBnhv9b3D8"
                  clientKey:@"XTcDzrh0b2E299VsdeP7YqzuzBkSk0dUIIW2w6Gx"];
    
    // Initializes a LYRClient object
    NSUUID *appID = [[NSUUID alloc] initWithUUIDString:LayerAppIDString];
    self.layerClient = [LYRClient clientWithAppID:appID];
    self.layerClient.delegate = self;
    
    // Connect to Layer
    [self.layerClient connectWithCompletion:^(BOOL success, NSError *error) {
        if (!success) {
            NSLog(@"Failed to connect to Layer: %@", error);
        } else {
            [self authenticateLayerWithUserID:LQSCurrentUserID completion:^(BOOL success, NSError *error) {
                if (!success) {
                    NSLog(@"Failed Authenticating Layer Client with error:%@", error);
                }else
                {
                    self.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:self.layerClient];
                    self.navController = [[UINavigationController alloc] initWithRootViewController:self.conversationsViewController];
                    
                    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
                    [self.window setRootViewController:self.navController];
                    [self.window makeKeyAndVisible];
                }
            }];
        }
    }];
    
    return YES;
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

- (void)authenticateLayerWithUserID:(NSString *)userID completion:(void (^)(BOOL success, NSError * error))completion
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
            
            [PFUser logInWithUsernameInBackground:@"iOS_Simulator" password:@"86903" block:^(PFUser *user, NSError *error) {
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
                }
            }];
        }
    }];
}

- (void)requestIdentityTokenForUserID:(NSString *)userID nonce:(NSString *)nonce completion:(void(^)(NSString *identityToken, NSError *error))completion
{
    
}

#pragma - mark LYRClientDelegate Delegate Methods

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
