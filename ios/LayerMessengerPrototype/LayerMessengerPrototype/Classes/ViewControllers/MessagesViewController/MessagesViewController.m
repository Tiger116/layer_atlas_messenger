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

@interface MessagesViewController () 

@end

@implementation MessagesViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.dataSource = self;
    self.delegate = self;
    
    [self configureUserInterfaceAttributes];
    //[self setupLayerNotificationObservers];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)configureUserInterfaceAttributes
{
    [[ATLIncomingMessageCollectionViewCell appearance] setBubbleViewColor:ATLLightGrayColor()];
    [[ATLIncomingMessageCollectionViewCell appearance] setMessageTextColor:[UIColor blackColor]];
    [[ATLIncomingMessageCollectionViewCell appearance] setMessageLinkTextColor:ATLBlueColor()];
    
    [[ATLOutgoingMessageCollectionViewCell appearance] setBubbleViewColor:ATLBlueColor()];
    [[ATLOutgoingMessageCollectionViewCell appearance] setMessageTextColor:[UIColor whiteColor]];
    [[ATLOutgoingMessageCollectionViewCell appearance] setMessageLinkTextColor:[UIColor whiteColor]];
}

#pragma mark - ATLConversationViewControllerDataSource

- (id<ATLParticipant>)conversationViewController:(ATLConversationViewController *)conversationViewController participantForIdentifier:(NSString *)participantIdentifier
{
    if (participantIdentifier) {
        UsersDataSource* userDataSource = [UsersDataSource new];
        return [userDataSource getUserForId:participantIdentifier];
    }
    return nil;
}

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
 Atlas - Returns an `NSAttributedString` object for given recipient state. The state string will only be displayed below the latest message that was sent by the currently authenticated user.
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

#pragma mark - ATLParticipantTableViewControllerDelegate

/**
 Atlas - Informs the delegate that the user selected an participant. Atlas Messenger in turn, informs the `ATLAddressBarViewController` of the selection.
 */
- (void)participantTableViewController:(ATLParticipantTableViewController *)participantTableViewController didSelectParticipant:(id<ATLParticipant>)participant
{
    [self.addressBarController selectParticipant:participant];
    [self.navigationController popViewControllerAnimated:YES];
}

/**
 Atlas - Informs the delegate that the user is searching for participants. Atlas Messengers queries for participants whose `fullName` property contains the give search string.
 */
- (void)participantTableViewController:(ATLParticipantTableViewController *)participantTableViewController didSearchWithString:(NSString *)searchText completion:(void (^)(NSSet *))completion
{
#warning Not implemented method
//    [self.participantDataSource participantsMatchingSearchText:searchText completion:^(NSSet *participants) {
//        completion(participants);
//    }];
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
        [self.navigationController pushViewController:controller animated:YES];
        
    }];
}

/**
 Atlas - Informs the delegate that the user is searching for participants. Atlas Messengers queries for participants whose `fullName` property contains the given search string.
 */
- (void)addressBarViewController:(ATLAddressBarViewController *)addressBarViewController searchForParticipantsMatchingText:(NSString *)searchText completion:(void (^)(NSArray *participants))completion
{
#warning Not implemented method
//    [self.participantDataSource participantsMatchingSearchText:searchText completion:^(NSSet *participants) {
//        completion([participants allObjects]);
//    }];
}

/**
 Atlas - Informs the delegate that the user tapped on the `ATLAddressBarViewController` while it was disabled. Atlas Messenger presents an `ATLConversationDetailViewController` in response.
 */
- (void)addressBarViewControllerDidSelectWhileDisabled:(ATLAddressBarViewController *)addressBarViewController
{
#warning Not implemented method
//    [self detailsButtonTapped];
}


@end