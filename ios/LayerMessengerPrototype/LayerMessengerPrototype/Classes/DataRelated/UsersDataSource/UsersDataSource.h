//
//  UsersDataSource.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 03/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "User.h"

@interface UsersDataSource : NSObject

+(instancetype) sharedUsersDataSource;
-(void) getAllUsersInBackgroundWithCompletion:(void(^)(NSMutableSet* users, NSError* error))block;
-(void) getUserInBackgroundForId:(NSString*)userId withCompletion:(void(^)(User* user, NSError* error))block;
-(User*) getUserForId:(NSString*)userId;
-(NSMutableSet*) getUsersForIds:(NSSet*)ids;
-(void) getUsersMatchingSearchText:(NSString*)searchText completion:(void(^)(NSSet *participants))block;
-(NSMutableSet*) usersFromUsers:(NSSet*)users byExcudingUsers:(NSSet*)excludingUsers;


@end
