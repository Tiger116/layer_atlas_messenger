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

/**
 *  Called after the controller's view is loaded into memory. 
    It finishes view controller's initialization and connects to Layer. If user is already authenticated it calls 'signIn' method, which proceeds to next view controller ('ConversationsViewController').
 */
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

/**
 *  Method will be called if "Sign in" button is tapped. It tries to authenticate with entered in TextFields username and password.
 *
 *  @param sender Button that was tapped.
 */
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

/**
 *  Method will be called if "Sign up" button is tapped. It presents view controller ('RegistrationViewController') for user to registrate.
 *
 *  @param sender Button that was tapped.
 */
- (IBAction)registerButtonTapped:(UIButton *)sender
{
    RegistrationViewController* viewController = [[RegistrationViewController alloc] initWithNibName:@"RegistrationViewController" bundle:nil];
    [self presentViewController:viewController animated:YES completion:nil];
}

/**
 *  Method initializes singleton 'UsersDataSourse' object and pushes view controllers with conversations ('ConversationsViewController').
 */
- (void) signIn
{
    //Initialize UsersDataSourse
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    hud.labelText = @"Signing in";
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        if (error)
        {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to download users from Parse:%@", error);
        } else
        {
            [hud hide:YES];
            self.appDelegate.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:self.appDelegate.layerClient];
            
            if (self.appDelegate.launchOptions && self.appDelegate.launchOptions[launchOptionsKeyForRemoteNotifications])
            {
                LYRQuery *query = [LYRQuery queryWithClass:[LYRConversation class]];
                query.predicate = [LYRPredicate predicateWithProperty:@"identifier"
                                                             operator:LYRPredicateOperatorIsEqualTo
                                                                value:self.appDelegate.launchOptions[launchOptionsKeyForRemoteNotifications][@"layer"][@"conversation_identifier"]];
                NSError *error;
                LYRConversation *conversation = [[self.appDelegate.layerClient executeQuery:query error:&error] firstObject];
                self.appDelegate.launchOptions = nil;
                
                if (!error)
                {
                    if (conversation)
                    {
                        self.appDelegate.messagesViewController = [MessagesViewController conversationViewControllerWithLayerClient:self.appDelegate.layerClient andConversation:conversation];
                        
                        NSMutableArray *viewControllers = [self.navigationController.viewControllers mutableCopy];
                        NSUInteger listViewControllerIndex = [self.navigationController.viewControllers indexOfObject:self];
                        NSRange replacementRange = NSMakeRange(listViewControllerIndex + 1, viewControllers.count - listViewControllerIndex - 1);
                        [viewControllers replaceObjectsInRange:replacementRange withObjectsFromArray:@[self.appDelegate.conversationsViewController, self.appDelegate.messagesViewController]];
                        [self.navigationController setViewControllers:viewControllers animated:YES];
                    } else
                    {
                        [self.navigationController pushViewController:self.appDelegate.conversationsViewController animated:YES];
                    }
                } else
                {
                    NSLog(@"Error querying conversation from notification: %@", error);
                }
                

                
            }else
            {
                [self.navigationController pushViewController:self.appDelegate.conversationsViewController animated:YES];
            }
            [self.navigationController setNavigationBarHidden:NO];
        }
    }];
}

/**
 *  Tells the receiver when one or more fingers touch down in a view or window. It will end editing of any text field to dismiss keyboard.
 *
 *  @param touches A set of UITouch instances that represent the touches for the starting phase of the event represented by event.
 *  @param event   An object representing the event to which the touches belong.
 */
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    
    UITouch *touch = [[event allTouches] anyObject];
    
    if (![[touch view] isKindOfClass:[UITextField class]]) {
        [self.view endEditing:YES];
    }
    [super touchesBegan:touches withEvent:event];
}

#pragma mark - UITextFieldDelegate

/**
 *  Asks the delegate if the text field should process the pressing of the return button.
 *  If return button was pressed in the username text field then password text field become active.
 *  If button was pressed in the password text field then it is considered as "Sign in" button was tapped.
 *
 *  @param textField The text field whose return button was pressed.
 *
 *  @return YES if the text field should implement its default behavior for the return button; otherwise, NO.
 */
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
