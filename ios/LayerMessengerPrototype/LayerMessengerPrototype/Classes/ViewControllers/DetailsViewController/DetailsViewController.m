//
//  DetailsViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 06/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "DetailsViewController.h"
#import "DetailsConversationTitleCell.h"
#import "DetailsConversationMemberCell.h"
#import "DetailsLeaveConversationCell.h"
#import "UsersDataSource.h"
#import "User.h"
#import "ParticipantsViewController.h"
#import "LoadingHUD.h"
#import "AppDelegate.h"

typedef NS_ENUM(NSInteger, DetailsTableSection) {
    DetailsTableSectionTitle,
    DetailsTableSectionParticipants,
    DetailsTableSectionLeave,
    DetailsTableSectionCount,
};

@interface DetailsViewController () 

@property (nonatomic) NSMutableArray *participantIdentifiers;

@end

@implementation DetailsViewController

/**
 *  Called after the controller's view is loaded into memory.
 *
 *  Finishes view controller's initialization.
 */
- (void)viewDidLoad {
    [super viewDidLoad];
    self.title = @"Details";
    
    self.tableView.sectionHeaderHeight = 48.0f;
    self.tableView.sectionFooterHeight = 0.0f;
    self.tableView.rowHeight = 48.0f;
    
    self.participantIdentifiers = [self.conversation.participants.allObjects mutableCopy];
    
    [self.tableView registerClass:[ATLParticipantTableViewCell class] forCellReuseIdentifier:@"ATLParticipantTableViewCell"];
    
    UITapGestureRecognizer * tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
    [self.view addGestureRecognizer:tap];
    [tap setCancelsTouchesInView:NO];
    
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
 *  Methot will be called when user tappes to ('DetailsViewController *')self view. It will end editing of any text field to dismiss keyboard.
 *
 *  @param recognizer UITapGestureRecognizer object which recognized tap.
 */
- (void)handleTap:(UITapGestureRecognizer *)recognizer
{
    [self.view endEditing:YES];
}

/**
 *  Removes authenticated user from current conversation and returns to view controller with conversations ('ConversationsViewController').
 */
- (void)leaveConversation
{
    NSSet *participants = [NSSet setWithObject:self.layerClient.authenticatedUserID];
    NSError *error;
    [self.conversation removeParticipants:participants error:&error];
    if (error) {
        NSLog(@"Error while leaving conversation: %@",error);
    } else {
        AppDelegate* appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
        [self.navigationController popToViewController:appDelegate.conversationsViewController animated:YES];
    }
}

/**
 *  Deletes current conversation and returns to view controller with conversations ('ConversationsViewController').
 */
- (void)deleteConversation
{
    NSError *error;
    [self.conversation delete:LYRDeletionModeAllParticipants error:&error];
    if (error) {
        NSLog(@"Error while deleting conversation: %@", error);
    } else {
        AppDelegate* appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
        [self.navigationController popToViewController:appDelegate.conversationsViewController animated:YES];
    }
}

/**
 *  Downloads user list. And presents view controller with it ('ParticipantsViewController') to user to select participant.
 */
- (void)presentParticipantPicker
{
    LoadingHUD* hud = [LoadingHUD showHUDAddedTo:self.view animated:YES];
    UsersDataSource *usersDataSource = [UsersDataSource sharedUsersDataSource];
    [usersDataSource getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        if (error) {
            [hud hide:YES afterShowingText:@"Failed"];
            NSLog(@"Failed to dowload user list: %@",error);
        } else {
            [hud hide:YES];
            NSMutableSet* otherUsers = [usersDataSource usersFromUsers:users byExcudingUsers:[usersDataSource getUsersForIds:self.conversation.participants]];
            
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
 *  Creates 'NSMutableArray' object with participant's identifiers except authenticated user's identifier.
 *
 *  @return Created 'NSMutableArray' object.
 */
- (NSMutableArray*) participantIdentifiersExcudingCurrentUser
{
    NSMutableArray* otherParticipants = [self.participantIdentifiers mutableCopy];
    [otherParticipants removeObject:self.layerClient.authenticatedUserID];
    return otherParticipants;
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return DetailsTableSectionCount;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    switch (section) {
        case DetailsTableSectionTitle:
            return 1;
            
        case DetailsTableSectionParticipants:
            return [self participantIdentifiersExcudingCurrentUser].count + 1;
            
        case DetailsTableSectionLeave:
            return 1;
            
        default:
            return 0;
    }
    

}

/**
 *  Asks the data source for a cell to insert in a particular location of the table view.
 *
 *  Fetches previously created cell or creates new one.
 *
 *  @param tableView A table-view object requesting the cell.
 *  @param indexPath An index path locating a row in tableView.
 *
 *  @return 'DetailsConversationTitleCell' for cell with title, 'ATLParticipantTableViewCell' for cells with participants, 'DetailsConversationMemberCell' for cell-button "Add participant", 'DetailsLeaveConversationCell' for cell-button for leaving conversation.
 */
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch (indexPath.section) {
        case DetailsTableSectionTitle: {
            DetailsConversationTitleCell* cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsConversationTitleCell"];
            if (!cell)
            {
                [tableView registerNib:[UINib nibWithNibName:@"DetailsConversationTitleCell" bundle:nil] forCellReuseIdentifier:@"DetailsConversationTitleCell"];
                cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsConversationTitleCell"];
            }
            [cell setTextFieldDelegate:self];
            return cell;
        }
            
        case DetailsTableSectionParticipants: {
            if (indexPath.row < [self participantIdentifiersExcudingCurrentUser].count)
            {
                ATLParticipantTableViewCell *cell = [self.tableView dequeueReusableCellWithIdentifier:@"ATLParticipantTableViewCell" forIndexPath:indexPath];
                return cell;
            } else {
                DetailsConversationMemberCell* cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsConversationMemberCell"];
                if (!cell)
                {
                    [tableView registerNib:[UINib nibWithNibName:@"DetailsConversationMemberCell" bundle:nil] forCellReuseIdentifier:@"DetailsConversationMemberCell"];
                    cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsConversationMemberCell"];
                }
                return cell;
            }
            
        }
            
        case DetailsTableSectionLeave: {
            DetailsLeaveConversationCell* cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsLeaveConversationCell"];
            if (!cell)
            {
                [tableView registerNib:[UINib nibWithNibName:@"DetailsLeaveConversationCell" bundle:nil] forCellReuseIdentifier:@"DetailsLeaveConversationCell"];
                cell = [self.tableView dequeueReusableCellWithIdentifier:@"DetailsLeaveConversationCell"];
            }
            return cell;
        }
            
        default:
            return nil;
    }
}

/**
 *  Tells the delegate the table view is about to draw a cell for a particular row.
 *
 *  Configures cell's appearance.
 *
 *  @param tableView The table-view object informing the delegate of this impending event.
 *  @param cell      A table-view cell object that tableView is going to use when drawing the row.
 *  @param indexPath An index path locating the row in tableView.
 */
- (void) tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch (indexPath.section) {
        case DetailsTableSectionTitle:
            [((DetailsConversationTitleCell*)cell) configureCellWithConversationName:self.conversation.metadata[metadataTitleKey]];
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
            break;
            
        case DetailsTableSectionParticipants:
            if (indexPath.row < [self participantIdentifiersExcudingCurrentUser].count)
            {
                UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
                User* participant = [usersDataSource getUserForId:[[self participantIdentifiersExcudingCurrentUser] objectAtIndex:indexPath.row]];
                [((ATLParticipantTableViewCell*)cell) presentParticipant:participant withSortType:ATLParticipantPickerSortTypeFirstName shouldShowAvatarItem:YES];
                cell.selectionStyle = UITableViewCellSelectionStyleNone;
            }
            break;
            
        case DetailsTableSectionLeave:
            break;
            
        default:
            break;
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    switch ((DetailsTableSection)section) {
        case DetailsTableSectionTitle:
            return @"Conversation Name";
            
        case DetailsTableSectionParticipants:
            return @"Participants";
            
        default:
            return nil;
    }
}

/**
 *  Tells the delegate that the specified row is now selected.
 *
 *  If selected "Add participant" cell - calls 'presentParticipantPicker' method.
 *  If selected "Leave conversation" cell - leaves conversation or deletes one if user was last participant.
 *
 *  @param tableView A table-view object informing the delegate about the new row selection.
 *  @param indexPath An index path locating the new selected row in tableView.
 */
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch ((DetailsTableSection)indexPath.section) {
        case DetailsTableSectionParticipants:
            if (indexPath.row == [self participantIdentifiersExcudingCurrentUser].count) {
                [self presentParticipantPicker];
            }
            break;
            
        case DetailsTableSectionLeave:
            self.participantIdentifiers.count > 1 ? [self leaveConversation] : [self deleteConversation];
            break;
            
        default:
            break;
    }
}

/**
 *  Asks the data source to verify that the given row is editable.
 *
 *  Allows deleting only rows with participants and deleting can perform only owner of the conversation.
 *
 *  @param tableView The table-view object requesting this information.
 *  @param indexPath An index path locating a row in tableView.
 *
 *  @return YES if the row indicated by indexPath is editable; otherwise, NO.
 */
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (([self.layerClient.authenticatedUserID isEqualToString:self.conversation.metadata[metadataOwnerIdKey]])
            && (indexPath.section == DetailsTableSectionParticipants)
            && (indexPath.row < [self participantIdentifiersExcudingCurrentUser].count))
    {
        return YES;
    }
    return NO;
}

/**
 *  Asks the data source to commit the insertion or deletion of a specified row in the receiver.
 *
 *  Removes participant from the conversation and deletes his row.
 *
 *  @param tableView    The table-view object requesting the insertion or deletion.
 *  @param editingStyle The cell editing style corresponding to a insertion or deletion requested for the row specified by indexPath.
 *  @param indexPath    An index path locating the row in tableView.
 */
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete)
    {
        NSString *participantIdentifier = [self participantIdentifiersExcudingCurrentUser][indexPath.row];
        NSError *error;
        BOOL success = [self.conversation removeParticipants:[NSSet setWithObject:participantIdentifier] error:&error];
        if (!success) {
            NSLog(@"Error while removing participant: %@",error);
            return;
        }
        [self.participantIdentifiers removeObject:participantIdentifier];
        [self.tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationLeft];
    }
}

#pragma mark - UITextFieldDelegate

/**
 *  Tells the delegate that editing stopped for the specified text field.
 *
 *  Saves entered title in conversation's metadata and informs delegate about title's change.
 *
 *  @param textField The text field for which editing ended.
 */
- (void)textFieldDidEndEditing:(UITextField *)textField
{
    NSString *title = [self.conversation.metadata valueForKey:metadataTitleKey];
    if (![textField.text isEqualToString:title]) {
        [self.conversation setValue:textField.text forMetadataAtKeyPath:metadataTitleKey];
        [self.delegate conversationTitleDidChange];
    }
}

/**
 *  Asks the delegate if the text field should process the pressing of the return button.
 *
 *  Saves entered title in conversation's metadata and informs delegate about title's change.
 *
 *  @param textField The text field for which editing ended.
 */
- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    if (textField.text.length > 0) {
        [self.conversation setValue:textField.text forMetadataAtKeyPath:metadataTitleKey];
    } else {
        [self.conversation deleteValueForMetadataAtKeyPath:metadataTitleKey];
    }
    [self.delegate conversationTitleDidChange];
    [textField resignFirstResponder];
    return YES;
}

#pragma mark - ATLParticipantTableViewControllerDelegate

/**
 *  Informs the delegate that the user selected an participant. Delegate in turn, returns to 'DetailsViewController' and adds selected participant to the conversation.
 */
- (void)participantTableViewController:(ATLParticipantTableViewController *)participantTableViewController didSelectParticipant:(id<ATLParticipant>)participant
{
    [self.navigationController popViewControllerAnimated:YES];
    
    [self.participantIdentifiers addObject:participant.participantIdentifier];
    NSError *error;
    BOOL success = [self.conversation addParticipants:[NSSet setWithObject:participant.participantIdentifier] error:&error];
    if (!success) {
        NSLog(@"Error while adding participant: %@",error);
    }
    [self.tableView reloadData];
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


//#pragma mark - Conversation Configuration
//
//- (void)switchToConversationForParticipants
//{
//    NSSet *participants = [NSSet setWithArray:self.participantIdentifiers];
//    
//    LYRQuery *query = [LYRQuery queryWithClass:[LYRConversation class]];
//    query.predicate = [LYRPredicate predicateWithProperty:@"participants" operator:LYRPredicateOperatorIsEqualTo value:participants];
//    query.limit = 1;
//    
//    LYRConversation *conversation = [self.layerClient executeQuery:query error:nil].firstObject;
//    if (!conversation) {
//        conversation = [self.layerClient newConversationWithParticipants:participants options:nil error:nil];
//    }
//    [self.delegate conservationDidChange:conversation];
//    self.conversation = conversation;
//}

#pragma mark - Notifications

/**
 *  Adds ('DetailsViewController *')self as observer to different notifications.
 */
- (void)registerNotificationObservers
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(conversationMetadataDidChange:) name:ConversationMetadataDidChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(conversationParticipantsDidChange:) name:ConversationParticipantsDidChangeNotification object:nil];
}

/**
 *  Handles ConversationMetadataDidChangeNotification. Changes text field's text value for cell with conversation's title and informs the delegate that conversation's title was changed.
 *
 *  @param notification received notification.
 */
- (void)conversationMetadataDidChange:(NSNotification*)notification
{
    if (!self.conversation) return;
    if (!notification.object) return;
    if (![notification.object isEqual:self.conversation]) return;
    
    NSIndexPath *nameIndexPath = [NSIndexPath indexPathForRow:0 inSection:DetailsTableSectionTitle];
    DetailsConversationTitleCell *titleCell = (DetailsConversationTitleCell *)[self.tableView cellForRowAtIndexPath:nameIndexPath];
    if (!titleCell) return;
    if ([titleCell.titleTextField isFirstResponder]) return;
    
    [titleCell configureCellWithConversationName:self.conversation.metadata[metadataTitleKey]];
    [self.delegate conversationTitleDidChange];
}

/**
 *  Handles ConversationParticipantsDidChangeNotification. If user has been removed from the conversation then returns to previous view controller, else changes table's section with participants.
 *
 *  @param notification received notification.
 */
- (void)conversationParticipantsDidChange:(NSNotification*)notification
{
    if (!self.conversation) return;
    if (!notification.object) return;
    if (![notification.object isEqual:self.conversation]) return;
    
    if (self.conversation.participants.count == 0) {
        AppDelegate* appDelegate = (AppDelegate*)[[UIApplication sharedApplication] delegate];
        [self.navigationController popToViewController:appDelegate.conversationsViewController animated:YES];
        return;
    }
    
    if (self.conversation.participants.count > 2) {
        [self.conversation setValue:@"YES" forMetadataAtKeyPath:metadataIsGroupKey];
    }
    
    self.participantIdentifiers = [self.conversation.participants.allObjects mutableCopy];
    [self.tableView reloadData];
}

@end
