//
//  RegistrationViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 12/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "RegistrationViewController.h"
#import "RegistrationInputCell.h"
#import "RegistrationButtonCell.h"

typedef NS_ENUM(NSInteger, RegistrationTableSection)
{
    RegistrationTableSectionUsername,
    RegistrationTableSectionPassword,
    RegistrationTableSectionUserInfo,
    RegistrationTableSectionButtons,
    RegistrationTableSectionCount,
};

@interface RegistrationViewController ()

@end

@implementation RegistrationViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.tableView.sectionHeaderHeight = 48.0f;
    self.tableView.sectionFooterHeight = 0.0f;
    self.tableView.rowHeight = 48.0f;
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return RegistrationTableSectionCount;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    switch (section) {
        case RegistrationTableSectionUsername:
            return 1;
        case RegistrationTableSectionPassword:
            return 2;
        case RegistrationTableSectionUserInfo:
            return 2;
        case RegistrationTableSectionButtons:
            return 2;
        default:
            return 0;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    switch (indexPath.section) {
        case RegistrationTableSectionUsername:
        case RegistrationTableSectionPassword:
        case RegistrationTableSectionUserInfo:{
            RegistrationInputCell* cell = [self.tableView dequeueReusableCellWithIdentifier:@"RegistrationInputCell"];
            if(!cell)
           {
               [tableView registerNib:[UINib nibWithNibName:@"RegistrationInputCell" bundle:nil] forCellReuseIdentifier:@"RegistrationInputCell"];
               cell = [self.tableView dequeueReusableCellWithIdentifier:@"RegistrationInputCell"];
           }
            return cell;
        }
        case RegistrationTableSectionButtons:{
            RegistrationButtonCell* cell = [self.tableView dequeueReusableCellWithIdentifier:@"RegistrationButtonCell"];
            if(!cell)
            {
                [tableView registerNib:[UINib nibWithNibName:@"RegistrationButtonCell" bundle:nil] forCellReuseIdentifier:@"RegistrationButtonCell"];
                cell = [self.tableView dequeueReusableCellWithIdentifier:@"RegistrationButtonCell"];
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
        case RegistrationTableSectionUsername:
            ((RegistrationInputCell*)cell).inputText.placeholder = @"Username";
            ((RegistrationInputCell*)cell).selectionStyle = UITableViewCellSelectionStyleNone;
            break;
        case RegistrationTableSectionPassword:{
            ((RegistrationInputCell*)cell).selectionStyle = UITableViewCellSelectionStyleNone;
            switch (indexPath.row) {
                case 0:
                    ((RegistrationInputCell*)cell).inputText.placeholder = @"Password";
                    ((RegistrationInputCell*)cell).inputText.secureTextEntry = YES;
                    break;
                case 1:
                    ((RegistrationInputCell*)cell).inputText.placeholder = @"Confirm password";
                    ((RegistrationInputCell*)cell).inputText.secureTextEntry = YES;
                    break;
                default:
                    break;
            }
            break;
        }
        case RegistrationTableSectionUserInfo:{
            ((RegistrationInputCell*)cell).selectionStyle = UITableViewCellSelectionStyleNone;
            switch (indexPath.row) {
                case 0:
                    ((RegistrationInputCell*)cell).inputText.placeholder = @"First name";
                    break;
                case 1:
                    ((RegistrationInputCell*)cell).inputText.placeholder = @"Last name";
                    break;
                default:
                    break;
            }
            break;
        }
        case RegistrationTableSectionButtons:
            switch (indexPath.row) {
                case 0:
                    ((RegistrationButtonCell*)cell).label.textColor = [UIColor colorWithRed:0/255.0f green:174/255.0f blue:243/255.0f alpha:1.0f];
                    ((RegistrationButtonCell*)cell).label.text = @"OK";
                    break;
                case 1:
                    ((RegistrationButtonCell*)cell).label.textColor = [UIColor redColor];
                    ((RegistrationButtonCell*)cell).label.text = @"Cancel";
                    break;
                default:
                    break;
            }
            break;
        default:
            break;
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    switch ((RegistrationTableSection)section) {
        case RegistrationTableSectionUsername:
            return @"Username";
        case RegistrationTableSectionPassword:
            return @"Password";
        case RegistrationTableSectionUserInfo:
            return @"User info";
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

/*
#pragma mark - Table view delegate

// In a xib-based application, navigation from a table can be handled in -tableView:didSelectRowAtIndexPath:
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    // Navigation logic may go here, for example:
    // Create the next view controller.
    <#DetailViewController#> *detailViewController = [[<#DetailViewController#> alloc] initWithNibName:<#@"Nib name"#> bundle:nil];
    
    // Pass the selected object to the new view controller.
    
    // Push the view controller.
    [self.navigationController pushViewController:detailViewController animated:YES];
}
*/


@end
