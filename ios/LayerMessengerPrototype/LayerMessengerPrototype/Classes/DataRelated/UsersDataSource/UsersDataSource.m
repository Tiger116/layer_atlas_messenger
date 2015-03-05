//
//  UsersDataSource.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 03/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "UsersDataSource.h"
#import <Parse/Parse.h>

@interface UsersDataSource ()

@property (strong, atomic) NSMutableSet* users;

@end

@implementation UsersDataSource

+(instancetype) sharedUsersDataSource
{
    static UsersDataSource *usersDataSource = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken,^{
        usersDataSource = [[self alloc] init];
    });
    return usersDataSource;
}

-(instancetype) init
{
    self = [super init];
    [self getAllUsersInBackgroundWithCompletion:^(NSMutableSet *users, NSError *error) {
        if (error) {
            NSLog(@"Error when retrieving users from Parse: %@",error);
        }
    }];
    return self;
}

-(void) getAllUsersInBackgroundWithCompletion:(void(^)(NSMutableSet* users, NSError* error))block
{
    PFQuery *query = [PFQuery queryWithClassName:@"_User"];
    [query findObjectsInBackgroundWithBlock:^(NSArray *objects, NSError *error) {
        if(!error)
        {
            self.users = [NSMutableSet new];
            for (PFObject* object in objects)
            {
                [self.users addObject:[self userFromPFObject:object]];
            }
            block(self.users,nil);
        }else{
            block(nil, error);
        }
    }];
}

-(NSMutableSet*) getUsersForIds:(NSMutableSet*)ids
{
    NSMutableSet* users = [NSMutableSet new];
    for (NSString* userId in ids)
    {
        for (User* user in self.users)
        {
            if ([userId isEqualToString:user.participantIdentifier]) {
                [users addObject:user];
            }
        }
    }
    return users;
}

-(void) getUserInBackgroundForId:(NSString*)userId withCompletion:(void(^)(User* user, NSError* error))block
{
    User* user = [self userFromMemoryForId:userId];
    if (user) {
        block(user,nil);
    }else{
        PFQuery *query = [PFQuery queryWithClassName:@"_User"];
        [query getObjectInBackgroundWithId:userId block:^(PFObject *object, NSError *error) {
            if(!error)
            {
                User* user = [self userFromPFObject:object];
                [self.users addObject:user];
                block(user,nil);
            }else{
                block(nil, error);
            }
        }];
    }
}

-(User*) getUserForId:(NSString*)userId
{
    User* user = [self userFromMemoryForId:userId];
    if (user) {
        return user;
    }else{
#warning Blocking operation on main thread
        PFQuery *query = [PFQuery queryWithClassName:@"_User"];
        PFObject* object = [query getObjectWithId:userId];
        user = [self userFromPFObject:object];
        [self.users addObject:user];
        return user;
    }
}

-(User*) userFromPFObject:(PFObject*) object
{
    User* user = [[User alloc] initWithFirstName:object[@"firstName"] lastName:object[@"lastName"] userId:object.objectId];
    user.username = object[@"username"];
    return user;
}

-(User*) userFromMemoryForId:(NSString*) userId
{
    for (User* user in self.users)
    {
        if ([userId isEqualToString:user.participantIdentifier]) {
            return user;
        }
    }
    return nil;
}

@end
