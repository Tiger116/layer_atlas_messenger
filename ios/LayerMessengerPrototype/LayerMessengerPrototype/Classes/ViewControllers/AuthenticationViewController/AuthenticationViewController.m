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

@interface AuthenticationViewController ()

@property (strong, nonatomic) IBOutlet UITextField *usernameField;
@property (strong, nonatomic) IBOutlet UITextField *passwordField;

@end

@implementation AuthenticationViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.navigationController setNavigationBarHidden:YES];
    // Do any additional setup after loading the view from its nib.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)signInButtonTapped:(UIButton *)sender
{
    AppDelegate* appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
    [appDelegate authenticateLayerWithUsername:self.usernameField.text andPassword:self.passwordField.text completion:^(BOOL success, NSError *error) {
        if (success) {
            appDelegate.conversationsViewController = [ConversationsViewController conversationListViewControllerWithLayerClient:appDelegate.layerClient];
            [self.navigationController pushViewController:appDelegate.conversationsViewController animated:YES];
        } else {
            NSLog(@"Failed Authenticating Layer Client with error:%@", error);
        }
    }];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
