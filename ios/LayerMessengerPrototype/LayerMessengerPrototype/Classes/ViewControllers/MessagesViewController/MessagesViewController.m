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
#import "DetailsViewController.h"
#import "LoadingHUD.h"
#import "AppDelegate.h"

@interface MessagesViewController () <DetailsViewControllerDelegate>

@property (nonatomic, strong) UIBarButtonItem* detailsButton;

@end

@implementation MessagesViewController

/**
 *  Called after the controller's view is loaded into memory.
 *
 *  Finishes view controller's initialization.
 */
- (void)viewDidLoad {
    [super viewDidLoad];
    self.dataSource = self;
    self.delegate = self;
    
    self.detailsButton = [[UIBarButtonItem alloc] initWithTitle:@"Details" style:UIBarButtonItemStylePlain target:self action:@selector(detailsButtonTapped)];
    if (self.conversation)
    {
        [self.navigationItem setRightBarButtonItem:self.detailsButton];
    }
    
    [self configureUserInterfaceAttributes];
    [self registerNotificationObservers];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/**
 *  Notifies the view controller that its view is about to be added to a view hierarchy.
 *
 *  Reloads title before view appears.
 *
 *  @param animated If YES, the view is being added to the window using an animation.
 */
-(void) viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    [self configureTitle];
}

/**
 *  Sets view controller's title with conversation title stored in ('LYRConversation *') self.converastion's metadata if it is there, else sets with default title created by method 'defaultTitle'.
 */
- (void)configureTitle
{
    if ([self.conversation.metadata valueForKey:metadataTitleKey])
    {
        NSString *conversationTitle = [self.conversation.metadata valueForKey:metadataTitleKey];
        if (conversationTitle.length)
        {
            self.title = conversationTitle;
        } else {
            self.title = [self defaultTitle];
        }
    } else {
            self.title = [self defaultTitle];
    }
}

/**
 *  Creates title for ('LYRConversation *') self.conversation. 
 *  "New Message" - if conversation doesn't exist. 
 *  "Personal" - if conversation haven't other participants except authenticated user. 
 *  Other participant's first name - if there is only one other participant. 
 *  "Group" - if there are more then one other participants.
 *
 *  @return Returns created ('NSString *') title.
 */
- (NSString *)defaultTitle
{
    if (!self.conversation) {
        return @"New Message";
    }
    
    NSMutableSet *otherParticipantIDs = [self.conversation.participants mutableCopy];
    if (self.layerClient.authenticatedUserID) [otherParticipantIDs removeObject:self.layerClient.authenticatedUserID];
    
    if (otherParticipantIDs.count == 0) {
        return @"Personal";
    } else if (otherParticipantIDs.count == 1) {
        NSString *otherParticipantID = [otherParticipantIDs anyObject];
        id<ATLParticipant> participant = [self conversationViewController:self participantForIdentifier:otherParticipantID];
        return participant ? participant.firstName : @"Message";
    } else if (otherParticipantIDs.count > 1) {
        NSUInteger participantCount = 0;
        id<ATLParticipant> knownParticipant;
        for (NSString *participantIdentifier in otherParticipantIDs) {
            id<ATLParticipant> participant = [self conversationViewController:self participantForIdentifier:participantIdentifier];
            if (participant) {
                participantCount += 1;
                knownParticipant = participant;
            }
        }
        if (participantCount == 1) {
            return knownParticipant.firstName;
        } else if (participantCount > 1) {
            return @"Group";
        }
    }
    return @"Message";
}

/**
 *  Configures mesagge's appearance.
 */
- (void)configureUserInterfaceAttributes
{
    [[ATLIncomingMessageCollectionViewCell appearance] setBubbleViewColor:ATLLightGrayColor()];
    [[ATLIncomingMessageCollectionViewCell appearance] setMessageTextColor:[UIColor blackColor]];
    [[ATLIncomingMessageCollectionViewCell appearance] setMessageLinkTextColor:ATLBlueColor()];
    
    [[ATLOutgoingMessageCollectionViewCell appearance] setBubbleViewColor:ATLBlueColor()];
    [[ATLOutgoingMessageCollectionViewCell appearance] setMessageTextColor:[UIColor whiteColor]];
    [[ATLOutgoingMessageCollectionViewCell appearance] setMessageLinkTextColor:[UIColor whiteColor]];
}

/**
 *  Called if "Details" button is tapped. It creates and pushes into navigation controller ('DetailsViewController') view controller for conversation's details.
 */
-(void) detailsButtonTapped
{
    DetailsViewController* detailsViewController = [[DetailsViewController alloc] initWithNibName:@"DetailsViewController" bundle:nil];
    [detailsViewController setConversation:self.conversation];
    [detailsViewController setDelegate:self];
    [detailsViewController setLayerClient:self.layerClient];
    [self.navigationController pushViewController:detailsViewController animated:YES];
}

#pragma mark - ATLConversationViewControllerDataSource

/**
 *  Asks the data source for an object conforming to the `ATLParticipant` protocol for a given identifier.
 *
 *  @param conversationViewController The `ATLConversationViewController` requesting the object.
 *  @param participantIdentifier      The participant identifier.
 *
 *  @return  An object conforming to the `ATLParticipant` protocol.
 */
- (id<ATLParticipant>)conversationViewController:(ATLConversationViewController *)conversationViewController participantForIdentifier:(NSString *)participantIdentifier
{
    if (participantIdentifier) {
        UsersDataSource* userDataSource = [UsersDataSource sharedUsersDataSource];
        return [userDataSource getUserForId:participantIdentifier];
    }
    return nil;
}

/**
 *  Asks the data source for an `NSAttributedString` representation of a given date.
 *
 *  @param conversationViewController The `ATLConversationViewController` requesting the string.
 *  @param date                        The `NSDate` object to be displayed as a string.
 *
 *  @return  `NSAttributedString` representing the given date.
 */
- (NSAttributedString *)conversationViewController:(ATLConversationViewController *)conversationViewController attributedStringForDisplayOfDate:(NSDate *)date
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.dateFormat = @"MMM dd, yyyy,"; // Nov 29, 2013,
    NSDateFormatter *timeFormatter = [[NSDateFormatter alloc] init];
    timeFormatter.timeStyle = NSDateFormatterShortStyle;
    
    NSString *dateString = [dateFormatter stringFromDate:date];
    NSString *timeString = [timeFormatter stringFromDate:date];
    
    NSMutableAttributedString *dateAttributedString = [[NSMutableAttributedString alloc] initWithString:[NSString stringWithFormat:@"%@ %@", dateString, timeString]];
    [dateAttributedString addAttribute:NSForegroundColorAttributeName value:[UIColor grayColor] range:NSMakeRange(0, dateAttributedString.length)];
    [dateAttributedString addAttribute:NSFontAttributeName value:[UIFont systemFontOfSize:11] range:NSMakeRange(0, dateAttributedString.length)];
    [dateAttributedString addAttribute:NSFontAttributeName value:[UIFont boldSystemFontOfSize:11] range:NSMakeRange(0, dateString.length)];
    return dateAttributedString;
    
}

/**
 *  Asks the data source for an `NSAttributedString` representation of a given `LYRRecipientStatus`.
 *
 *  @param conversationViewController The `ATLConversationViewController` requesting the string.
 *  @param recipientStatus            The `LYRRecipientStatus` object to be displayed as a question string.
 *
 *  @return `NSAttributedString` representing the give recipient status.
 */
- (NSAttributedString *)conversationViewController:(ATLConversationViewController *)conversationViewController attributedStringForDisplayOfRecipientStatus:(NSDictionary *)recipientStatus
{
    NSMutableDictionary *mutableRecipientStatus = [recipientStatus mutableCopy];
    if ([mutableRecipientStatus valueForKey:self.layerClient.authenticatedUserID]) {
        [mutableRecipientStatus removeObjectForKey:self.layerClient.authenticatedUserID];
    }
    
    NSString *statusString = [NSString new];
    if (mutableRecipientStatus.count > 1) {
        __block NSUInteger readCount = 0;
        __block BOOL delivered;
        __block BOOL sent;
        [mutableRecipientStatus enumerateKeysAndObjectsUsingBlock:^(NSString *userID, NSNumber *statusNumber, BOOL *stop) {
            LYRRecipientStatus status = statusNumber.integerValue;
            switch (status) {
                case LYRRecipientStatusInvalid:
                    break;
                case LYRRecipientStatusSent:
                    sent = YES;
                    break;
                case LYRRecipientStatusDelivered:
                    delivered = YES;
                    break;
                case LYRRecipientStatusRead:
                    NSLog(@"Read");
                    readCount += 1;
                    break;
            }
        }];
        if (readCount) {
            NSString *participantString = readCount > 1 ? @"Participants" : @"Participant";
            statusString = [NSString stringWithFormat:@"Read by %lu %@", (unsigned long)readCount, participantString];
        } else if (delivered) {
            statusString = @"Delivered";
        } else if (sent) {
            statusString = @"Sent";
        }
    } else {
        __block NSString *blockStatusString = [NSString new];
        [mutableRecipientStatus enumerateKeysAndObjectsUsingBlock:^(NSString *userID, NSNumber *statusNumber, BOOL *stop) {
            if ([userID isEqualToString:self.layerClient.authenticatedUserID]) return;
            LYRRecipientStatus status = statusNumber.integerValue;
            switch (status) {
                case LYRRecipientStatusInvalid:
                    blockStatusString = @"Not Sent";
                case LYRRecipientStatusSent:
                    blockStatusString = @"Sent";
                case LYRRecipientStatusDelivered:
                    blockStatusString = @"Delivered";
                    break;
                case LYRRecipientStatusRead:
                    blockStatusString = @"Read";
                    break;
            }
        }];
        statusString = blockStatusString;
    }
    return [[NSAttributedString alloc] initWithString:statusString attributes:@{NSFontAttributeName : [UIFont boldSystemFontOfSize:11]}];
}

/**
 *  Asks the data source to provide a conversation for a set of participants.
 *
 *  It disables delivery receipts if there are more than five participants and creates new conversation with given participants.
 *
 *  @param viewController The `ATLConversationViewController` requesting the conversation.
 *  @param participants   A set of objects conforming to `ATLParticipant`.
 *
 *  @return  A conversation that will be used by the conversation view controller.
 */
- (LYRConversation *)conversationViewController:(ATLConversationViewController *)viewController conversationWithParticipants:(NSSet *)participants
{
    NSSet *participantIdentifiers = [participants valueForKey:@"participantIdentifier"];
    BOOL deliveryReceiptsEnabled = participants.count <= 5;
    NSDictionary *options = @{LYRConversationOptionsDeliveryReceiptsEnabledKey: @(deliveryReceiptsEnabled)};
    return [self.layerClient newConversationWithParticipants:participantIdentifiers options:options error:nil];
}

#pragma mark - ATLParticipantTableViewControllerDelegate

/**
 *  Informs the delegate that the user selected an participant. Delegate in turn, informs the `ATLAddressBarViewController` of the selection and return to 'MessagesViewController'
 */
- (void)participantTableViewController:(ATLParticipantTableViewController *)participantTableViewController didSelectParticipant:(id<ATLParticipant>)participant
{
    [self.addressBarController selectParticipant:participant];
    [self.navigationController popViewControllerAnimated:YES];
}

/**
 *  Informs the delegate that the user is searching for participants. Delegate queries for participants whose `fullName` property contains the give search string.
 */
- (void)participantTableViewController:(ATLParticipantTableViewController *)participantTableViewController didSearchWithString:(NSString *)searchText completion:(void (^)(NSSet *))completion
{
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getUsersMatchingSearchText:searchText completion:^(NSSet* matchedUsers)
     {
         NSMutableSet* usersToDisplay = [NSMutableSet new];
         for (User* participant in participantTableViewController.participants)
         {
             for (User* matchedUser in matchedUsers)
             {
                 if ([participant.participantIdentifier isEqualToString:matchedUser.participantIdentifier]) {
                     [usersToDisplay addObject:matchedUser];
                 }
             }
         }
         completion(usersToDisplay);
     }];
}

#pragma mark - ATLAddressBarControllerDelegate

/**
 *  Informs the delegate that the user tapped the `addContacts` icon in the `ATLAddressBarViewController`. Delegate downloads list of users and presents an `ParticipantsViewController`.
 */
- (void)addressBarViewController:(ATLAddressBarViewController *)addressBarViewController didTapAddContactsButton:(UIButton *)addContactsButton
{
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    UsersDataSource *usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        if (error) {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to download user list: %@",error);
        } else {
            [hud hide:YES];
            NSMutableSet* otherUsers = [usersDataSource usersFromUsers:users byExcudingUsers:addressBarViewController.selectedParticipants.set];
            
            [[NSOperationQueue mainQueue] addOperationWithBlock:^{
                ParticipantsViewController *controller = [ParticipantsViewController participantTableViewControllerWithParticipants:otherUsers sortType:ATLParticipantPickerSortTypeFirstName];
                controller.delegate = self;
                controller.allowsMultipleSelection = NO;
                [self.navigationController pushViewController:controller animated:YES];
            }];
        }
        
        
    }];
}

/**
 *  Informs the delegate that the user is searching for participants. Delegate queries for participants whose `fullName` property contains the given search string.
 */
- (void)addressBarViewController:(ATLAddressBarViewController *)addressBarViewController searchForParticipantsMatchingText:(NSString *)searchText completion:(void (^)(NSArray *participants))completion
{
    UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getUsersMatchingSearchText:searchText completion:^(NSSet *participants) {
        completion([participants allObjects]);
    }];
}

/**
 *  Informs the delegate that the user tapped on the `ATLAddressBarViewController` while it was disabled. Delegate presents an `DetailsViewController` in response.
 */
- (void)addressBarViewControllerDidSelectWhileDisabled:(ATLAddressBarViewController *)addressBarViewController
{
    [self detailsButtonTapped];
}

#pragma mark - DetailsViewControllerDelegate

/**
 *  Informs the delegate that conversation's title was changed. Delegate reloads view controller's title in response.
 */
-(void) conversationTitleDidChange
{
    [self configureTitle];
}

//-(void) conservationDidChange:(LYRConversation*)conversation
//{
//    self.conversation = conversation;
//    [self configureTitle];
//}

#pragma mark - Accessors

- (void)setConversation:(LYRConversation *)conversation
{
    [super setConversation:conversation];
    [self configureTitle];
}

#pragma mark - Notifications

/**
 *  Adds ('MessagesViewController *')self as observer to different notifications.
 */
- (void)registerNotificationObservers
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(conversationMetadataDidChange:) name:ConversationMetadataDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(conversationParticipantsDidChange:) name:ConversationParticipantsDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(userDidTapLink:) name:ATLUserDidTapLinkNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(conversationDidCreated:) name:ConversationDidCreatedNotification object:nil];
}

/**
 *  Handles ConversationMetadataDidChangeNotification. Configures title for ('MessagesViewController *')self by calling 'configureTitle' method.
 *
 *  @param notification received notification.
 */
- (void)conversationMetadataDidChange:(NSNotification*) notification
{
    if (!self.conversation) return;
    if (!notification.object) return;
    if (![notification.object isEqual:self.conversation]) return;
    
    [self configureTitle];
}

/**
 *  Handles ConversationParticipantsDidChangeNotification. Returns to previous view controller if user has been removed from the conversation.
 *
 *  @param notification received notification.
 */
- (void)conversationParticipantsDidChange:(NSNotification*) notification
{
    if (self.conversation.participants.count == 0) {
        [self.navigationController popViewControllerAnimated:YES];
        return;
    }
}

/**
 *  Handles ATLUserDidTapLinkNotification. Opens given in notification's object URL.
 *
 *  @param notification received notification.
 */
- (void)userDidTapLink:(NSNotification *)notification
{
    [[UIApplication sharedApplication] openURL:notification.object];
}

/**
 *  Handles ConversationDidCreatedNotification. Sets "Details" button and stores conversation creator's identifier in conversation's metadata.
 *
 *  @param notification received notification.
 */
- (void)conversationDidCreated:(NSNotification*) notification
{
    [self.navigationItem setRightBarButtonItem:self.detailsButton];
    [((LYRConversation*)notification.object) setValue:self.layerClient.authenticatedUserID forMetadataAtKeyPath:metadataOwnerIdKey];
}

@end
