//
//  UsersDataSource.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 03/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "UsersDataSource.h"
#import <Parse/Parse.h>

@implementation UsersDataSource

//+(instancetype) sharedUsersDataSource
//{
//    static UsersDataSource *usersDataSource = nil;
//    static dispatch_once_t onceToken;
//    dispatch_once(&onceToken,^{
//        usersDataSource = [[self alloc] init];
//    });
//    return usersDataSource;
//}

-(void) getAllUsersInBackgroundWithCompletion:(void(^)(NSMutableSet* users, NSError* error))block
{
    PFQuery *query = [PFQuery queryWithClassName:@"_User"];
    [query findObjectsInBackgroundWithBlock:^(NSArray *objects, NSError *error) {
        if(!error)
        {
            NSMutableSet* users = [NSMutableSet new];
            for (PFObject* object in objects)
            {
                [users addObject:[self userFromPFObject:object]];
            }
            block(users,nil);
        }else{
            block(nil, error);
        }
    }];
}

-(void) getUserInBackgroundForId:(NSString*)userId withCompletion:(void(^)(User* user, NSError* error))block
{
    PFQuery *query = [PFQuery queryWithClassName:@"_User"];
    [query getObjectInBackgroundWithId:userId block:^(PFObject *object, NSError *error) {
        if(!error)
        {
            User* user = [self userFromPFObject:object];
            block(user,nil);
        }else{
            block(nil, error);
        }
    }];
}

-(User*) getUserForId:(NSString*)userId
{
    PFQuery *query = [PFQuery queryWithClassName:@"_User"];
    PFObject* object = [query getObjectWithId:userId];
    return [self userFromPFObject:object];
}

-(User*) userFromPFObject:(PFObject*) object
{
    User* user = [[User alloc] initWithFirstName:object[@"firstName"] lastName:object[@"lastName"] userId:object.objectId];
    user.username = object[@"username"];
    return user;
}

@end
