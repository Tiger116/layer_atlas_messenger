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
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)handleTap:(UITapGestureRecognizer *)recognizer
{
    [self.view endEditing:YES];
}

- (void)leaveConversation
{
    NSSet *participants = [NSSet setWithObject:self.layerClient.authenticatedUserID];
    NSError *error;
    [self.conversation removeParticipants:participants error:&error];
    if (error) {
        NSLog(@"Error while leaving conversation: %@",error);
    } else {
        [self.navigationController popViewControllerAnimated:YES];
        [self.navigationController popViewControllerAnimated:YES];
    }
}

- (void)deleteConversation
{
    NSError *error;
    [self.conversation delete:LYRDeletionModeAllParticipants error:&error];
    if (error) {
        NSLog(@"Error while deleting conversation: %@", error);
    } else {
        [self.navigationController popViewControllerAnimated:YES];
        [self.navigationController popViewControllerAnimated:YES];
    }
}

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

- (NSMutableArray*) participantIdentifiersExcudingCurrentUser
{
    NSMutableArray* otherParticipants = [NSMutableArray new];
    for (NSString* userId in self.participantIdentifiers)
    {
        if (![userId isEqualToString:self.layerClient.authenticatedUserID]) {
            [otherParticipants addObject:userId];
        }
    }
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

- (void) tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch (indexPath.section) {
        case DetailsTableSectionTitle:
            [((DetailsConversationTitleCell*)cell) configureCellWithConversationName:self.conversation.metadata[@"title"]];
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

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    if ((indexPath.section == DetailsTableSectionParticipants) && (indexPath.row < [self participantIdentifiersExcudingCurrentUser].count)) {
        return YES;
    }
    return NO;
}

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

- (void)textFieldDidEndEditing:(UITextField *)textField
{
    NSString *title = [self.conversation.metadata valueForKey:@"title"];
    if (![textField.text isEqualToString:title]) {
        [self.conversation setValue:textField.text forMetadataAtKeyPath:@"title"];
        [self.delegate conversationTitleDidChange:nil];
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    if (textField.text.length > 0) {
        [self.conversation setValue:textField.text forMetadataAtKeyPath:@"title"];
    } else {
        [self.conversation deleteValueForMetadataAtKeyPath:@"title"];
    }
    [self.delegate conversationTitleDidChange:nil];
    [textField resignFirstResponder];
    return YES;
}

#pragma mark - ATLParticipantTableViewControllerDelegate

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

@end
