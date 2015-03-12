//
//  AuthenticationViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 04/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "AuthenticationViewController.h"
#import "AppDelegate.h"
#import "ConversationsViewController.h"
#import "LoadingHUD.h"
#import "RegistrationViewController.h"

@interface AuthenticationViewController ()

@property (strong, nonatomic) IBOutlet UITextField *usernameField;
@property (strong, nonatomic) IBOutlet UITextField *passwordField;
@property (weak, nonatomic) AppDelegate* appDelegate;

@end

@implementation AuthenticationViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.navigationController setNavigationBarHidden:YES];
    self.appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
    
    // Connect to Layer
    
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    hud.labelText = @"Loading";
    
    [self.appDelegate.layerClient connectWithCompletion:^(BOOL success, NSError *error) {
        if (!success) {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to connect to Layer: %@", error);
        } else {
            [hud hide:YES];
            if(self.appDelegate.layerClient.authenticatedUserID)
            {
                self.appDelegate.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:self.appDelegate.layerClient];
                [self.navigationController pushViewController:self.appDelegate.conversationsViewController animated:YES];
            }
        }
    }];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)signInButtonTapped:(UIButton *)sender
{
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    [self.appDelegate authenticateLayerWithUsername:self.usernameField.text andPassword:self.passwordField.text completion:^(BOOL success, NSError *error) {
        if (success) {
            [hud hide:YES];
            self.appDelegate.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:self.appDelegate.layerClient];
            [self.navigationController pushViewController:self.appDelegate.conversationsViewController animated:YES];
        } else {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed Authenticating Layer Client with error:%@", error);
        }
    }];
}

- (IBAction)registerButtonTapped:(UIButton *)sender
{
    RegistrationViewController* viewController = [[RegistrationViewController alloc] initWithNibName:@"RegistrationViewController" bundle:nil];
    [self presentViewController:viewController animated:YES completion:nil];
}

@end
