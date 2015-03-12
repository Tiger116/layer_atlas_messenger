//
//  ConversationsViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "ConversationsViewController.h"
#import "MessagesViewController.h"
#import <LayerKit/LayerKit.h>
#import "UsersDataSource.h"
#import "LoadingHUD.h"

@interface ConversationsViewController () <ATLConversationListViewControllerDataSource, ATLConversationListViewControllerDelegate>

@property (strong,nonatomic) NSOrderedSet* conversations;

@end

@implementation ConversationsViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.delegate = self;
    self.dataSource = self;
    self.allowsEditing = YES;
    
    // Left navigation item
    UIBarButtonItem *singOutButton = [[UIBarButtonItem alloc] initWithTitle:@"Sing out" style:UIBarButtonItemStylePlain target:self action:@selector(singOutButtonTapped)];
    [self.navigationItem setLeftBarButtonItem:singOutButton];
    
    // Right navigation item
    UIBarButtonItem *composeButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemCompose target:self action:@selector(composeButtonTapped)];
    [self.navigationItem setRightBarButtonItem:composeButton];
    
    [self.navigationController setNavigationBarHidden:NO];
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)composeButtonTapped
{
    [self presentControllerWithConversation:nil];
}

- (void)singOutButtonTapped
{
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    [self.layerClient deauthenticateWithCompletion:^(BOOL success, NSError *error) {
        if (!success) {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to deauthenticate with error: %@",error);
        } else {
            [hud hide:YES];
            [self.navigationController popToRootViewControllerAnimated:YES];
        }
    }];
}

#pragma mark - Conversation Selection

- (void)presentControllerWithConversation:(LYRConversation *)conversation
{
    BOOL shouldShowAddressBar = (conversation.participants.count > 2 || !conversation.participants.count);
    MessagesViewController *messagesViewController = [MessagesViewController conversationViewControllerWithLayerClient:self.layerClient];
    messagesViewController.displaysAddressBar = shouldShowAddressBar;
    messagesViewController.conversation = conversation;
    
    if (self.navigationController.topViewController == self) {
        [self.navigationController pushViewController:messagesViewController animated:YES];
    } else {
        NSMutableArray *viewControllers = [self.navigationController.viewControllers mutableCopy];
        NSUInteger listViewControllerIndex = [self.navigationController.viewControllers indexOfObject:self];
        NSRange replacementRange = NSMakeRange(listViewControllerIndex + 1, viewControllers.count - listViewControllerIndex - 1);
        [viewControllers replaceObjectsInRange:replacementRange withObjectsFromArray:@[messagesViewController]];
        [self.navigationController setViewControllers:viewControllers animated:YES];
    }
}

#pragma mark - ATLConversationListViewControllerDelegate

- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didSelectConversation:(LYRConversation *)conversation
{
    [self presentControllerWithConversation:conversation];
}

/**
 Atlas - Informs the delegate a conversation was deleted. Atlas Messenger does not need to react as the superclass will handle removing the conversation in response to a deletion.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didDeleteConversation:(LYRConversation *)conversation deletionMode:(LYRDeletionMode)deletionMode
{
    NSLog(@"Conversation Successsfully Deleted");
}

/**
 Atlas - Informs the delegate that a conversation deletion attempt failed. Atlas Messenger does not do anything in response.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didFailDeletingConversation:(LYRConversation *)conversation deletionMode:(LYRDeletionMode)deletionMode error:(NSError *)error
{
    NSLog(@"Conversation Deletion Failed with Error: %@", error);
}

/**
 Atlas - Informs the delegate that a search has been performed. Atlas messenger queries for, and returns objects conforming to the `ATLParticipant` protocol whose `fullName` property contains the search text.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didSearchForText:(NSString *)searchText completion:(void (^)(NSSet *))completion
{
    //TODO: Add search for conversation titles
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getUsersMatchingSearchText:searchText completion:^(NSSet *participants) {
        completion(participants);
    }];
}

#pragma mark - ATLConversationListViewControllerDataSource

/**
 Atlas - Returns a label that is used to represent the conversation. Atlas Messenger puts the name representing the `lastMessage.sentByUserID` property first in the string.
 */
- (NSString *)conversationListViewController:(ATLConversationListViewController *)conversationListViewController titleForConversation:(LYRConversation *)conversation
{
    // If we have a Conversation name in metadata, return it.
    NSString *conversationTitle = conversation.metadata[@"title"];
    if (conversationTitle.length) {
        return conversationTitle;
    }
    
    NSMutableSet *participantIdentifiers = [conversation.participants mutableCopy];
    [participantIdentifiers minusSet:[NSSet setWithObject:self.layerClient.authenticatedUserID]];
    
    if (participantIdentifiers.count == 0) return @"Personal Conversation";
    
    NSMutableSet* participants = [[UsersDataSource sharedUsersDataSource] getUsersForIds:participantIdentifiers];
    if (participants.count == 0) return @"No Matching Participants";
    if (participants.count == 1) return [[participants allObjects][0] fullName];
    
    NSMutableArray *firstNames = [NSMutableArray new];
    [participants enumerateObjectsUsingBlock:^(id obj, BOOL *stop) {
        id<ATLParticipant> participant = obj;
        if (participant.firstName) {
            // Put the last message sender's name first
            if ([conversation.lastMessage.sentByUserID isEqualToString:participant.participantIdentifier]) {
                [firstNames insertObject:participant.firstName atIndex:0];
            } else {
                [firstNames addObject:participant.firstName];
            }
        }
    }];
    NSString *firstNamesString = [firstNames componentsJoinedByString:@", "];
    return firstNamesString;

}

@end
