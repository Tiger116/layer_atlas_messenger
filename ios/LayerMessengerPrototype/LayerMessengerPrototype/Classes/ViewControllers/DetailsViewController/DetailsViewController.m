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
#import <Atlas.h>
#import "UsersDataSource.h"
#import "User.h"

typedef NS_ENUM(NSInteger, DetailsTableSection) {
    DetailsTableSectionTitle,
    DetailsTableSectionParticipants,
    DetailsTableSectionLeave,
    DetailsTableSectionCount,
};

@interface DetailsViewController () <UITextFieldDelegate>

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
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
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
            return self.conversation.participants.count + 1;
            
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
            if (indexPath.row < self.conversation.participants.count)
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
            if (indexPath.row < self.conversation.participants.count)
            {
                UsersDataSource* usersDataSource = [UsersDataSource sharedUsersDataSource];
                User* participant = [usersDataSource getUserForId:[self.participantIdentifiers objectAtIndex:indexPath.row]];
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



/*
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the specified item to be editable.
    return YES;
}
*/

/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    } else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/

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





@end
