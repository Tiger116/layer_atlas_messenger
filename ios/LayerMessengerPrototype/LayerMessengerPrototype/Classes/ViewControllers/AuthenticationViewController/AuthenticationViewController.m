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
#import "UsersDataSource.h"

@interface AuthenticationViewController () <UITextFieldDelegate>

@property (strong, nonatomic) IBOutlet UITextField *usernameField;
@property (strong, nonatomic) IBOutlet UITextField *passwordField;
@property (weak, nonatomic) AppDelegate* appDelegate;

@end

@implementation AuthenticationViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.navigationController setNavigationBarHidden:YES];
    self.appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
    self.usernameField.delegate = self;
    self.passwordField.delegate = self;
    
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
                [self signIn];
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
    hud.labelText = @"Signing in";
    [self.appDelegate authenticateLayerWithUsername:self.usernameField.text andPassword:self.passwordField.text completion:^(BOOL success, NSError *error) {
        if (success) {
            [hud hide:YES];
            [self signIn];
        } else {
            [hud hide:YES];
            NSLog(@"Failed Authenticating Layer Client with error:%@", error);
            if (error.code == 101) {
                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Authentication failed"
                                                                message:@"Invalid login credentials"
                                                               delegate:self
                                                      cancelButtonTitle:@"OK"
                                                      otherButtonTitles:nil];
                [alert show];
            }
        }
    }];
}

- (IBAction)registerButtonTapped:(UIButton *)sender
{
    RegistrationViewController* viewController = [[RegistrationViewController alloc] initWithNibName:@"RegistrationViewController" bundle:nil];
    [self presentViewController:viewController animated:YES completion:nil];
}

- (void) signIn
{
    //Initialize UsersDataSourse
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    hud.labelText = @"Signing in";
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        if (error) {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to download users from Parse:%@", error);
        } else {
            [hud hide:YES];
            self.appDelegate.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:self.appDelegate.layerClient];
            [self.navigationController pushViewController:self.appDelegate.conversationsViewController animated:YES];
        }
    }];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    
    UITouch *touch = [[event allTouches] anyObject];
    
    if (![[touch view] isKindOfClass:[UITextField class]]) {
        [self.view endEditing:YES];
    }
    [super touchesBegan:touches withEvent:event];
}

#pragma mark - UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    if(textField == self.usernameField){
        [self.passwordField becomeFirstResponder];
    }
    
    if (textField == self.passwordField) {
        [self signInButtonTapped:nil];
    }
    return YES;
}

@end
