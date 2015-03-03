//
//  User.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 03/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <Atlas.h>

@interface User : NSObject <ATLParticipant,ATLAvatarItem>

/**
 @abstract Username using to authenticate to Parse backend
 */
@property (nonatomic, strong) NSString* username;

/**
 @abstract Passsword using to authenticate to Parse backend
 */
@property (nonatomic, strong) NSString* password;

/**
 @abstract The first name of the participant as it should be presented in the user interface.
 */
@property (nonatomic, readonly) NSString *firstName;

/**
 @abstract The last name of the participant as it should be presented in the user interface.
 */
@property (nonatomic, readonly) NSString *lastName;

/**
 @abstract The full name of the participant as it should be presented in the user interface.
 */
@property (nonatomic, readonly) NSString *fullName;

/**
 @abstract The unique identifier of the participant as it should be used for Layer addressing.
 @discussion This identifier is issued by the Layer identity provider backend.
 */
@property (nonatomic, readonly) NSString *participantIdentifier;

-(instancetype) initWithFirstName:(NSString*)firstName lastName:(NSString*)lastName userId:(NSString*)userId;

/**
 @abstract Returns the avatar image of the receiver.
 */
@property (nonatomic, readonly) UIImage *avatarImage;

/**
 @abstract Returns the avatar initials of the receiver.
 */
@property (nonatomic, readonly) NSString *avatarInitials;

@end
