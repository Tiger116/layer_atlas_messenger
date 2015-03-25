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

/**
 *  Returns singleton UsersDataSource object, creates one if it doesn't exist.
 *
 *  @return shared UsersDataSource object.
 */
+(instancetype) sharedUsersDataSource
{
    static dispatch_once_t onceToken;
    static UsersDataSource *usersDataSource = nil;
    dispatch_once(&onceToken,^{
        usersDataSource = [[self alloc] init];
    });
    return usersDataSource;
}

-(instancetype) init
{
    self = [super init];
    return self;
}

/**
 *  Download users 'asynchronously' and calls the given block with the results.
 *
 *  @param block Completion block with signature: `^(NSMutableSet *users, NSError *error)`. Will be called in the end with users set and nil as error if success, called with nil as users set and NSError object otherwise.
 */
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

/**
 *  Search in memory for users with given identifiers.
 *
 *  @param ids 'NSSet' object with user's identifiers for search.
 *
 *  @return 'NSMutableSet' object with found 'User' objects.
 */
-(NSMutableSet*) getUsersForIds:(NSSet*)ids
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

/**
 *  'Asynchronously' searches user for given id and calles given block with results. First searches in memory; if failes then requests user from Parse.
 *
 *  @param userId User's identifier for search.
 *  @param block  Completion block with signature: `^(User *user, NSError *error)`. Will be called in the end with user and nil as error if success, called with nil as user and NSError object otherwise.
 */
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

/**
 *  'Synchronously' searches user for given id. First searches in memory; if failes then requests user from Parse.
 *
 *  @param userId User's identifier for search.
 *
 *  @return Found 'User' object.
 */
-(User*) getUserForId:(NSString*)userId
{
    User* user = [self userFromMemoryForId:userId];
    if (user) {
        return user;
    }else{
        PFQuery *query = [PFQuery queryWithClassName:@"_User"];
        PFObject* object = [query getObjectWithId:userId];
        user = [self userFromPFObject:object];
        [self.users addObject:user];
        return user;
    }
}

/**
 *  Creates 'User' object from given 'PFObject' object.
 *
 *  @param object 'PFObject' object with data to create 'User' object.
 *
 *  @return Created 'User' object.
 */
-(User*) userFromPFObject:(PFObject*) object
{
    User* user = [[User alloc] initWithFirstName:object[@"firstName"] lastName:object[@"lastName"] userId:object.objectId];
    user.username = object[@"username"];
    return user;
}

/**
 *  Searches user in memory for given id.
 *
 *  @param userId User's identifier for search.
 *
 *  @return Found 'User' object or nil.
 */
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

/**
 *  Searhes users whose first or last name started with given 'NSString' object and calls the given block with results.
 *
 *  @param searchText 'NSString' object to search with.
 *  @param block      Completion block with signature ^(NSSet* participants).
 */
-(void) getUsersMatchingSearchText:(NSString*)searchText completion:(void(^)(NSSet *participants))block
{
    NSString *escapedSearchString = [NSRegularExpression escapedPatternForString:searchText];
    NSString *searchPattern = [NSString stringWithFormat:@".*\\b%@.*", escapedSearchString];
    NSPredicate *searchPredicate = [NSPredicate predicateWithFormat:@"fullName MATCHES[cd] %@", searchPattern];
    
    NSSet* matchedUsers = [self.users filteredSetUsingPredicate:searchPredicate];
    block(matchedUsers);
}

/**
 *  Creates new 'NSMutableSet' object with given 'NSSet' with users by excluding users from given 'NSSet' with users to exclude.
 *
 *  @param users          'NSSet' object with users to start with.
 *  @param excludingUsers 'NSSet' object with users to exclude.
 *
 *  @return created 'NSMutableSet' object.
 */
-(NSMutableSet*) usersFromUsers:(NSSet*)users byExcudingUsers:(NSSet*)excludingUsers
{
    NSMutableSet* otherUsers = [NSMutableSet new];
    for (User* user in users)
    {
        BOOL userShouldBeExcluded = NO;
        for (User* participant in excludingUsers)
        {
            if ([participant.participantIdentifier isEqualToString:user.participantIdentifier]) {
                userShouldBeExcluded = YES;
            }
        }
        if (!userShouldBeExcluded) {
            [otherUsers addObject:user];
        }
    }
    
    return otherUsers;
}

@end
