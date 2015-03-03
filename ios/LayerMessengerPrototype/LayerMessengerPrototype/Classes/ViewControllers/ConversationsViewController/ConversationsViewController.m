//
//  ConversationsViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "ConversationsViewController.h"
#import "MessagesViewController.h"

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
    
    // Right navigation item
    UIBarButtonItem *composeButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemCompose target:self action:@selector(composeButtonTapped)];
    [self.navigationItem setRightBarButtonItem:composeButton];
    
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

#pragma mark - ATLConversationListViewControllerDataSource

/**
 Atlas - Returns a label that is used to represent the conversation. Atlas Messenger puts the name representing the `lastMessage.sentByUserID` property first in the string.
 */
- (NSString *)conversationListViewController:(ATLConversationListViewController *)conversationListViewController titleForConversation:(LYRConversation *)conversation
{
    
    NSMutableSet *participantIdentifiers = [conversation.participants mutableCopy];
    [participantIdentifiers minusSet:[NSSet setWithObject:self.layerClient.authenticatedUserID]];
    
    if (participantIdentifiers.count == 0) return @"Personal Conversation";
    
    return @"Other";
}

@end
