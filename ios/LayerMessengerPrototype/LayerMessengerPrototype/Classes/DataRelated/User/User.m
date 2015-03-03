//
//  User.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 03/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "User.h"

@implementation User

-(instancetype) initWithFirstName:(NSString*)firstName lastName:(NSString*)lastName userId:(NSString*)userId
{
    self = [super init];
    if(!self) return nil;
    _firstName = firstName;
    _lastName = lastName;
    _participantIdentifier = userId;
    _fullName = [NSString stringWithFormat:@"%@ %@",firstName, lastName];
    _avatarInitials = [firstName stringByPaddingToLength:1 withString:@"" startingAtIndex:0];
    return self;
}

@end
