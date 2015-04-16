//
//  SyncReporter.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 15/04/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <LayerKit/LayerKit.h>

@class SyncReporter;

@protocol SyncReporterDelegate <NSObject>

@optional
/**
 *  Tells delegate that synchronisation will begin.
 */
- (void)syncReporterWillBeginSyncing:(SyncReporter*)reporter;

/**
 *  Tells delegate that synchronisation did finish.
 */
- (void)syncReporterDidFinishSyncing:(SyncReporter*)reporter;
@end

@interface SyncReporter : NSObject

@property (nonatomic) BOOL done;

- (instancetype)initWithClient:(LYRClient *)client;

@property (nonatomic, weak) id<SyncReporterDelegate> delegate;

@end
