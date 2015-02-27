//
//  ConversationsViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "ConversationsViewController.h"
#import "MessagesViewController.h"
#import "ConversationCell.h"

@interface ConversationsViewController ()

@property (strong,nonatomic) NSOrderedSet* conversations;

@end

@implementation ConversationsViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)appDidConnectedToLayer
{
    self.conversations = [self getAllConversations];
    [self.tableView reloadData];
}

- (NSOrderedSet*) getAllConversations
{
    // Fetches all LYRConversation objects
    LYRQuery *query = [LYRQuery queryWithClass:[LYRConversation class]];
    
    NSError *error;
    NSOrderedSet *conversations = [self.layerClient executeQuery:query error:&error];
    if (!error) {
        NSLog(@"%tu conversations", conversations.count);
        if (conversations.count > 0)
        {
            return conversations;
        } else {
            NSString* otherUser = @"otherUser";
            LYRConversation* conversation = [self.layerClient newConversationWithParticipants:[NSSet setWithArray:@[otherUser]] options:nil error:&error];
            [conversation setValue:otherUser forMetadataAtKeyPath:@"title"];
            return [NSOrderedSet orderedSetWithArray:@[conversation]];
        }
        
    } else {
        NSLog(@"Query failed with error %@", error);
        return nil;
    }
}

#pragma mark - Table view data source

//- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
//#warning Potentially incomplete method implementation.
//    // Return the number of sections.
//    return 0;
//}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 44;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.conversations count];
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
     ConversationCell *cell = (ConversationCell*)[tableView dequeueReusableCellWithIdentifier:@"ConversationCell" forIndexPath:indexPath];
    cell.conversationTitle.text = ((LYRConversation*)self.conversations[indexPath.row]).metadata[@"title"];
    
    return cell;
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

/*
// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}
*/

/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/


#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:@"messagesSegue"])
    {
        MessagesViewController* destinationVievController = (MessagesViewController*)[segue destinationViewController];
        [destinationVievController setLayerClient:self.layerClient];
        ConversationCell* cell = ((ConversationCell*)sender);
        [destinationVievController setConversation:[self.conversations objectAtIndex:[self.tableView indexPathForCell:cell].row]];
    }
}


@end
