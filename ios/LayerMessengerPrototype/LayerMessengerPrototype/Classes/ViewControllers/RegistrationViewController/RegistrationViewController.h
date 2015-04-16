//
//  RegistrationViewController.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 12/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol RegistrationViewControllerDelegate <NSObject>

@optional
/**
 *  Tells delegate that registration was successful.
 *
 *  @param username NSString with registered user's username.
 *  @param password NSString with registered user's password.
 */
-(void)registeredWithUsername:(NSString*)username andPassword:(NSString*)password;

@end

@interface RegistrationViewController : UITableViewController <UITextFieldDelegate>

@property (nonatomic, weak) id<RegistrationViewControllerDelegate> delegate;

@end
