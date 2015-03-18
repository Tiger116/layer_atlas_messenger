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
#import <Parse/Parse.h>
#import "User.h"

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

/**
 *  Called after the controller's view is loaded into memory.
 *  It finishes view controller's initialization.
 */
- (void)viewDidLoad {
    [super viewDidLoad];
    self.tableView.sectionHeaderHeight = 48.0f;
    self.tableView.sectionFooterHeight = 0.0f;
    self.tableView.rowHeight = 48.0f;
    
    UITapGestureRecognizer * tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
    [self.view addGestureRecognizer:tap];
    [tap setCancelsTouchesInView:NO];
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/**
 *  Methot will be called when user tappes to RegistrationViewController's view. It will end editing of any text field to dismiss keyboard.
 *
 *  @param recognizer UITapGestureRecognizer object which recognized tap.
 */
- (void)handleTap:(UITapGestureRecognizer *)recognizer
{
    [self.view endEditing:YES];
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

/**
 *  Asks the data source for a cell to insert in a particular location of the table view.
 *
 *  Fetches previously created cell or creates new one.
 *
 *  @param tableView A table-view object requesting the cell.
 *  @param indexPath An index path locating a row in tableView.
 *
 *  @return 'RegistrationInputCell' for cells to user's input, 'RegistrationButtonCell' for "OK" and "Cancel" buttons.
 */
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
        case RegistrationTableSectionButtons:{
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
        }
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

#pragma mark - Table view delegate

/**
 *  Tells the delegate that the specified row is now selected.
 *
 *  Calls 'validateInputInTableView' method to check user's input for correctness. If success registers new user in Parse and returns to view controller with sign-in form ('AuthenticationViewController'). If registration fails show alertView with failure reason.
 *
 *  @param tableView A table-view object informing the delegate about the new row selection.
 *  @param indexPath An index path locating the new selected row in tableView.
 */
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.section == RegistrationTableSectionButtons)
    {
        switch (indexPath.row) {
            case 0:{
                User* user;
                if ((user = [self validateInputInTableView:tableView]))
                {
                    PFUser *parseUser = [PFUser user];
                    parseUser.username = user.username;
                    parseUser.password = user.password;
                    parseUser[@"firstName"] = user.firstName;
                    parseUser[@"lastName"] = user.lastName;
                    [parseUser signUpInBackgroundWithBlock:^(BOOL succeeded, NSError *error) {
                        if (succeeded) {
                            [self dismissViewControllerAnimated:YES completion:nil];
                        } else {
                            NSLog(@"Failed to create new user: %@",error);
                            [tableView deselectRowAtIndexPath:indexPath animated:YES];
                            if (error.code == 202) {
                                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Registration failed"
                                                                                message:[NSString stringWithFormat:@"Username \"%@\" already taken",user.username]
                                                                               delegate:self
                                                                      cancelButtonTitle:@"OK"
                                                                      otherButtonTitles:nil];
                                [alert show];
                            } else {
                                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Registration failed"
                                                                                message:@"Uknown error"
                                                                               delegate:self
                                                                      cancelButtonTitle:@"OK"
                                                                      otherButtonTitles:nil];
                                [alert show];
                            }
                            
                        }
                    }];
                }else{
                    [tableView deselectRowAtIndexPath:indexPath animated:YES];
                }
                break;
            }
            case 1:{
                [self dismissViewControllerAnimated:YES completion:nil];
                break;
            }
                
            default:
                break;
        }
    }
}

#pragma mark - Input validation

/**
 *  Checks text fields in this RegistrationViewController's tableViews's cells for folowing conditions:
 *  Text fileds are not empty;
 *  Entered passwords are equal.
 *
 *
 *  @param tableView UITableView object where cell will be gotten from.
 *
 *  @return User object with entered information if success. Nil in other case.
 */
- (User*) validateInputInTableView:(UITableView*)tableView
{
    NSString* username;
    NSString* password;
    NSString* firstName;
    NSString* lastName;
    //Username
    RegistrationInputCell* cell = (RegistrationInputCell*)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0
                                                                                                              inSection:RegistrationTableSectionUsername]];
    if ([self textFieldIsEmpty:cell.inputText]){
        [self showAlertWithMessage:@"Username is empty"];
        return nil;
    }
    username = cell.inputText.text;
    
    //Password
    cell = (RegistrationInputCell*)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0
                                                                                        inSection:RegistrationTableSectionPassword]];
    RegistrationInputCell* confirmationCell = (RegistrationInputCell*)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:1
                                                                                                                          inSection:RegistrationTableSectionPassword]];
    if ([self textFieldIsEmpty:cell.inputText]){
        [self showAlertWithMessage:@"Password is empty"];
        return nil;
    }
    if ([self textFieldIsEmpty:confirmationCell.inputText]){
        [self showAlertWithMessage:@"Confirmation password is empty"];
        return nil;
    }
    if (! [cell.inputText.text isEqualToString:confirmationCell.inputText.text]){
        [self showAlertWithMessage:@"Passwords are not equal"];
        return nil;
    }
    password = cell.inputText.text;
    
    //First name
    cell = (RegistrationInputCell*)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0
                                                                                       inSection:RegistrationTableSectionUserInfo]];
    if ([self textFieldIsEmpty:cell.inputText]){
        [self showAlertWithMessage:@"First name is empty"];
        return nil;
    }
    firstName = cell.inputText.text;
    
    //Last name
    cell = (RegistrationInputCell*)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:1
                                                                                       inSection:RegistrationTableSectionUserInfo]];
    if ([self textFieldIsEmpty:cell.inputText]){
        [self showAlertWithMessage:@"Last name is empty"];
        return nil;
    }
    lastName = cell.inputText.text;
    
    User* user = [[User alloc] initWithFirstName:firstName lastName:lastName userId:nil];
    user.username = username;
    user.password = password;
    return user;
}

/**
 *  Checks if given UITextField object's text is empty or contains only spaces.
 *
 *  @param textField Object that will be checked.
 *
 *  @return YES if given object is null, or objects's text is null, or text is empty or contains only spaces. NO in other cases.
 */
- (BOOL) textFieldIsEmpty:(UITextField*)textField
{
    if (!textField || !textField.text || ([textField.text stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@" "]].length == 0)) {
        return YES;
    }
    return NO;
}

/**
 *  Creates and shows UIAlertView object with "Validation error" title, given message and one "OK" button.
 *
 *  @param message Message that will be displayed in UIAlertView.
 */
- (void) showAlertWithMessage:(NSString*)message
{
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Validation error"
                                                    message:message
                                                   delegate:self
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
}



@end
