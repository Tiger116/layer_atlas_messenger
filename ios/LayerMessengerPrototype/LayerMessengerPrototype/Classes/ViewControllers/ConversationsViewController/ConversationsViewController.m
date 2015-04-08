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
#import "AppDelegate.h"

@interface ConversationsViewController () <ATLConversationListViewControllerDataSource, ATLConversationListViewControllerDelegate>

//Can't remember why it's here. Probably it isn't used.
//@property (strong,nonatomic) NSOrderedSet* conversations;

/**
 *  Used to remove loading spinner if synchronization with Layer is finished.
 */
@property (atomic) BOOL synchronizationIsFinished;

@property (nonatomic) CGPoint contentOffsetForTableOnly;
@property (nonatomic) LoadingHUD *syncHud;

@end

@implementation ConversationsViewController

/**
 *  Called after the controller's view is loaded into memory.
 *
 *  Finishes view controller's initialization.
 */
- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.delegate = self;
    self.dataSource = self;
    self.allowsEditing = YES;
    self.deletionModes = [NSArray new];
    self.synchronizationIsFinished = NO;
    [self registerNotificationObservers];
    
    // Left navigation item
    UIBarButtonItem *singOutButton = [[UIBarButtonItem alloc] initWithTitle:@"Sign out" style:UIBarButtonItemStylePlain target:self action:@selector(signOutButtonTapped)];
    [self.navigationItem setLeftBarButtonItem:singOutButton];
    
    // Right navigation item
    UIBarButtonItem *composeButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemCompose target:self action:@selector(composeButtonTapped)];
    [self.navigationItem setRightBarButtonItem:composeButton];
    
}

- (void) viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
}

/**
 *  Notifies the view controller that its view was added to a view hierarchy.
 *
 *  Presents refreshing spinner until data finished loading.
 *
 *  @param animated If YES, the view was added to the window using an animation.
 */
- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    if (!self.synchronizationIsFinished)
    {
        self.syncHud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
        self.syncHud.labelText = @"Synchronisation";
    }
    
//    self.contentOffsetForTableOnly = self.tableView.contentOffset;
//    
//    if (!self.synchronizationIsFinished)
//    {
//        [self.refreshControl beginRefreshing];
//        [self.tableView setContentOffset:CGPointMake(self.contentOffsetForTableOnly.x,
//                                                     self.contentOffsetForTableOnly.y - self.refreshControl.frame.size.height
//                                                     - self.searchDisplayController.searchBar.frame.size.height)
//                                animated:YES];
//        if (self.synchronizationIsFinished)
//        {
//            [self.tableView setContentOffset:CGPointMake(self.contentOffsetForTableOnly.x,
//                                                         self.contentOffsetForTableOnly.y)
//                                    animated:YES];
//            [self.refreshControl endRefreshing];
//        }
//    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

/**
 *  Method will be called if "Compose" button is tapped.
 */
- (void)composeButtonTapped
{
    [self presentControllerWithConversation:nil];
}

/**
 *  Method will be called if "Sign out" button is tapped. It deauthenticates user and returns to AuthenticationViewController if succes.
 */
- (void)signOutButtonTapped
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

/**
 *  Creates view controller for given conversation ('MessagesViewController') and pushed it into navigation controller.
 *
 *  @param conversation 'LYRConversation' object to initialize created view controller.
 */
- (void)presentControllerWithConversation:(LYRConversation *)conversation
{
    AppDelegate* appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
    appDelegate.messagesViewController = [MessagesViewController conversationViewControllerWithLayerClient:self.layerClient andConversation:conversation];
    
    if (self.navigationController.topViewController == self) {
        [self.navigationController pushViewController:appDelegate.messagesViewController animated:YES];
    } else {
        NSMutableArray *viewControllers = [self.navigationController.viewControllers mutableCopy];
        NSUInteger listViewControllerIndex = [self.navigationController.viewControllers indexOfObject:self];
        NSRange replacementRange = NSMakeRange(listViewControllerIndex + 1, viewControllers.count - listViewControllerIndex - 1);
        [viewControllers replaceObjectsInRange:replacementRange withObjectsFromArray:@[appDelegate.messagesViewController]];
        [self.navigationController setViewControllers:viewControllers animated:YES];
    }
}

#pragma mark - ATLConversationListViewControllerDelegate

- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didSelectConversation:(LYRConversation *)conversation
{
    [self presentControllerWithConversation:conversation];
}

/**
 *  Informs the delegate a conversation was deleted. Delegate does not need to react as the superclass will handle removing the conversation in response to a deletion.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didDeleteConversation:(LYRConversation *)conversation deletionMode:(LYRDeletionMode)deletionMode
{
    NSLog(@"Conversation Successsfully Deleted");
}

/**
 *  Informs the delegate that a conversation deletion attempt failed. Delegate does not do anything in response.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didFailDeletingConversation:(LYRConversation *)conversation deletionMode:(LYRDeletionMode)deletionMode error:(NSError *)error
{
    NSLog(@"Conversation Deletion Failed with Error: %@", error);
}

/**
 *  Informs the delegate that a search has been performed. It queries for, and returns objects conforming to the `ATLParticipant` protocol whose `fullName` property contains the search text.
 */
- (void)conversationListViewController:(ATLConversationListViewController *)conversationListViewController didSearchForText:(NSString *)searchText completion:(void (^)(NSSet *))completion
{
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getUsersMatchingSearchText:searchText completion:^(NSSet *participants) {
        completion(participants);
    }];
}

#pragma mark - ATLConversationListViewControllerDataSource

/**
 *  Returns a label that is used to represent the conversation.
 *  Conversation title - if it is stored in metadata.
 *  "Personal Conversation" - if conversation haven't other participants except authenticated user.
 *  Other participant's full name - if there is only one other participant.
 *  "Group" - if there are more then one other participants.
 */
- (NSString *)conversationListViewController:(ATLConversationListViewController *)conversationListViewController titleForConversation:(LYRConversation *)conversation
{
    // If we have a Conversation name in metadata, return it.
    NSString *conversationTitle = conversation.metadata[metadataTitleKey];
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

#pragma mark - Notifications
/**
 *  Adds ('ConversationViewController *')self as observer to different notifications.
 */
- (void)registerNotificationObservers
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(synchronizationDidFinished:) name:LayerClientDidFinishSynchronizationNotification object:nil];
}

/**
 *  Handles LayerClientDidFinishSynchronizationNotification.
 *
 *  Hides refreshing spinner.
 *
 *  @param notification received notification.
 */
- (void) synchronizationDidFinished:(NSNotification*) notification
{
    if (!self.synchronizationIsFinished)
    {
        self.synchronizationIsFinished = YES;
        [self.syncHud hide:YES];
    }
}

#pragma mark - UITableViewDataSource

/**
 *  Asks the data source to verify that the given row is editable.
 *
 *  Denies deleting conversations.
 *
 *  @param tableView The table-view object requesting this information.
 *  @param indexPath An index path locating a row in tableView.
 *
 *  @return YES if the row indicated by indexPath is editable; otherwise, NO.
 */
- (BOOL) tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    return NO;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    NSInteger result = [super tableView:tableView numberOfRowsInSection:section];
    if ((!self.synchronizationIsFinished) && (result > 0))
    {
        self.synchronizationIsFinished = YES;
        [self.syncHud hide:YES];
    }
    return result;
}

@end
