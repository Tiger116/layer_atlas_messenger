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
#import <EventKitUI/EventKitUI.h>
#import "MapViewController.h"
#import <ATLLocationManager.h>
#import "ImageViewController.h"

static NSDateFormatter *DateFormatter()
{
    static NSDateFormatter *dateFormatter;
    if (!dateFormatter) {
        dateFormatter = [[NSDateFormatter alloc] init];
        dateFormatter.dateFormat = @"MMM dd, yyyy,"; // Nov 29, 2013,
    }
    return dateFormatter;
}
static NSDateFormatter *TimeFormatter()
{
    static NSDateFormatter *dateFormatter;
    if (!dateFormatter) {
        dateFormatter = [[NSDateFormatter alloc] init];
        dateFormatter.timeStyle = NSDateFormatterShortStyle;
    }
    return dateFormatter;
}


@interface MessagesViewController () <DetailsViewControllerDelegate, EKEventEditViewDelegate, CLLocationManagerDelegate, MapViewControllerDelegate>

@property (nonatomic, strong) UIBarButtonItem* detailsButton;
@property (nonatomic) BOOL shouldShareLocation;
@property (nonatomic) ATLLocationManager *locationManager;
@property (nonatomic) MapViewController* mapViewController;

@end

@implementation MessagesViewController

+ (instancetype) conversationViewControllerWithLayerClient:(LYRClient *)layerClient andConversation:(LYRConversation *)conversation
{
    BOOL shouldShowAddressBar = (conversation.participants.count > 2 || !conversation.participants.count);
    MessagesViewController* controller = [self conversationViewControllerWithLayerClient:layerClient];
    controller.displaysAddressBar = shouldShowAddressBar;
    controller.conversation = conversation;
    return controller;
}

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

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
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

- (void) viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    if (self.conversation && !self.addressBarController.addressBarView.addressBarTextView.isFirstResponder) {
        [self.view becomeFirstResponder];
    }
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

/**
 *  Presents view controller for creating calendar event at prodided date.
 *
 *  @param date Dare for event.
 */
- (void)createEventWithDate:(NSDate*)date andPresentViewController:store
{
    EKEvent *event = [EKEvent eventWithEventStore:store];
    
    event.calendar = [store defaultCalendarForNewEvents];
    
    NSCalendar *calendar = [NSCalendar currentCalendar];
    NSDateComponents *components = [[NSDateComponents alloc] init];
    event.startDate = date;
    components.hour = 1;
    event.endDate = [calendar dateByAddingComponents:components
                                              toDate:event.startDate
                                             options:0];
    
    EKEventEditViewController *controller = [[EKEventEditViewController alloc] init];
    controller.event = event;
    controller.eventStore = store;
    controller.editViewDelegate = self;
    
    [self presentViewController:controller animated:YES completion:nil];
}

- (void)presentImageViewControllerWithMessage:(LYRMessage *)message
{
    ImageViewController *imageViewController = [[ImageViewController alloc] initWithMessage:message];
    UINavigationController *controller = [[UINavigationController alloc] initWithRootViewController:imageViewController];
    [self.navigationController presentViewController:controller animated:YES completion:nil];
}

#pragma mark - Map Related

/**
 *  Method is called when user presses a button with arrow to send a location.
 */
- (void)sendLocationMessage
{
    self.shouldShareLocation = YES;
    
    if ([self.locationManager locationServicesEnabled])
    {
        [self.locationManager startUpdatingLocation];
    } else
    {
        [self presentMapWithLocationToDisplay:nil andWithMarkedLocation:nil];
    }

}

/**
 *  Tells the delegate that new location data is available.
 *
 *  It presents (MapViewController *) view controller with map.
 *
 *  @param manager   The location manager object that generated the update event.
 *  @param locations An array of CLLocation objects containing the location data.
 */
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    [self.locationManager stopUpdatingLocation];
    if (!self.shouldShareLocation) return;
    if (locations.firstObject)
    {
        self.shouldShareLocation = NO;
        [self presentMapWithLocationToDisplay:locations.firstObject andWithMarkedLocation:nil];
    }
}

/**
 *  Tells the delegate that the location manager was unable to retrieve a location value.
 *
 *  Still presents (MapViewController *) view controller with map.
 *
 *  @param manager The location manager object that was unable to retrieve the location.
 *  @param error   The error object containing the reason the location or heading could not be retrieved.
 */
- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    NSLog(@"Error updating location: %@",error);
    [self presentMapWithLocationToDisplay:nil andWithMarkedLocation:nil];
}

/**
 *  Method is using to present map to user.
 *
 *  @param locationToDisplay Location which will be presented after map appears. (Can be 'nil').
 *  @param markedLocation    Location of pin in the map. (Can be 'nil').
 */
- (void) presentMapWithLocationToDisplay:(CLLocation*)locationToDisplay andWithMarkedLocation:(CLLocation*)markedLocation
{
    MapViewController* mapViewController = [[MapViewController alloc] initWithNibName:@"MapViewController" bundle:nil];
    mapViewController.delegate = self;
    mapViewController.locationToDisplay = locationToDisplay;
    mapViewController.markedLocation = markedLocation;
    [self presentViewController:mapViewController animated:YES completion:nil];
}

#pragma mark - MapViewControllerDelegate

- (void)sendLocation:(CLLocation *)location
{
    [self sendMessageWithLocation:location];
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
    NSDateFormatter *dateFormatter = DateFormatter();
    NSDateFormatter *timeFormatter = TimeFormatter();
    
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

/**
 *  Informs the delegate that a message was selected.
 */
- (void)conversationViewController:(ATLConversationViewController *)viewController didSelectMessage:(LYRMessage *)message
{
    LYRMessagePart *JPEGMessagePart = ATLMessagePartForMIMEType(message, ATLMIMETypeImageJPEG);
    if (JPEGMessagePart) {
        [self presentImageViewControllerWithMessage:message];
        return;
    }
    
    LYRMessagePart *PNGMessagePart = ATLMessagePartForMIMEType(message, ATLMIMETypeImagePNG);
    if (PNGMessagePart) {
        [self presentImageViewControllerWithMessage:message];
    }
    
    LYRMessagePart *locationMessagePart = ATLMessagePartForMIMEType(message, ATLMIMETypeLocation);
    if (locationMessagePart)
    {
        NSDictionary *dictionary = [NSJSONSerialization JSONObjectWithData:locationMessagePart.data
                                                                   options:NSJSONReadingAllowFragments
                                                                     error:nil];
        double latitude = [dictionary[ATLLocationLatitudeKey] doubleValue];
        double longitude = [dictionary[ATLLocationLongitudeKey] doubleValue];
        CLLocation *location = [[CLLocation alloc] initWithLatitude:latitude longitude:longitude];
        [self presentMapWithLocationToDisplay:nil andWithMarkedLocation:location];
    }
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

#pragma mark - EKEventEditViewDelegate

- (void)eventEditViewController:(EKEventEditViewController *)controller didCompleteWithAction:(EKEventEditViewAction)action
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - Accessors

- (void)setConversation:(LYRConversation *)conversation
{
    [super setConversation:conversation];
    [self configureTitle];
}

- (CLLocationManager *) locationManager
{
    if (!_locationManager)
    {
        _locationManager = [[ATLLocationManager alloc] init];
        _locationManager.delegate = self;
        if ([_locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)])
        {
            [_locationManager requestWhenInUseAuthorization];
        }
    }
    return _locationManager;
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
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(userDidTapDate:) name:LMPUserDidTapDateNotification object:nil];
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
    [self configureTitle];
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
 *  Handles LMPUserDidTapDateNotification
 *
 *  Requests access to calendar and if granted calls 'createEventWithDate:andPresentViewController:' method to create a calendar event.
 *
 *  @param notification received notification.
 */
- (void)userDidTapDate:(NSNotification *)notification
{
    EKEventStore *store = [[EKEventStore alloc] init];
    [store requestAccessToEntityType:EKEntityTypeEvent
                          completion:^(BOOL granted, NSError *error) {
                              if (granted)
                              {
                                  dispatch_async(dispatch_get_main_queue(), ^{
                                      [self createEventWithDate:(NSDate*)notification.object andPresentViewController:store];
                                  });
                              }
    }];
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
