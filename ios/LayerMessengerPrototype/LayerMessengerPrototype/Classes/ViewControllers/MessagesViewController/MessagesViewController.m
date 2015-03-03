//
//  MessagesViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "MessagesViewController.h"
#import "UsersDataSource.h"
#import "ParticipantsViewController.h"

@interface MessagesViewController () <ATLConversationViewControllerDataSource, ATLConversationViewControllerDelegate, ATLParticipantTableViewControllerDelegate>

@end

@implementation MessagesViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    //[self setupLayerNotificationObservers];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - ATLAddressBarControllerDelegate

/**
 Atlas - Informs the delegate that the user tapped the `addContacts` icon in the `ATLAddressBarViewController`. Atlas Messenger presents an `ATLParticipantPickerController`.
 */
- (void)addressBarViewController:(ATLAddressBarViewController *)addressBarViewController didTapAddContactsButton:(UIButton *)addContactsButton
{
    UsersDataSource *usersDataSource = [[UsersDataSource alloc] init];
    [usersDataSource getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        ParticipantsViewController *controller = [ParticipantsViewController participantTableViewControllerWithParticipants:users sortType:ATLParticipantPickerSortTypeFirstName];
        controller.delegate = self;
        controller.allowsMultipleSelection = NO;
        
        UINavigationController *navigationController =[[UINavigationController alloc] initWithRootViewController:controller];
        [self.navigationController presentViewController:navigationController animated:YES completion:nil];
        
    }];
}


@end
