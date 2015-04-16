//
//  SyncReporter.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 15/04/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "SyncReporter.h"

@interface SyncReporter ()
@property (nonatomic) LYRClient *client;
@end

@implementation SyncReporter

/**
 *  Initializes SyncReporter object with LYRClient object and registers as observer to notifications about LYRClient's synchronisation state.
 *
 *  @param client LYRClient object which synchronisation state needed to report.
 *
 *  @return Initialized SyncReporter object
 */
- (instancetype)initWithClient:(LYRClient *)client
{
    self = [super init];
    if (self) {
        _client = client;
        _done = NO;
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveWillBeginSynchronizationNotification:) name:LYRClientWillBeginSynchronizationNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didReceiveDidFinishSynchronizationNotification:) name:LYRClientDidFinishSynchronizationNotification object:nil];
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

/**
 *  Handles LYRClientWillBeginSynchronizationNotification and if it is first synchronisation tells delegate that synchronisation will begin.
 *
 *  @param notification Received NSNotification.
 */
- (void)didReceiveWillBeginSynchronizationNotification:(NSNotification *)notification
{
    NSLog(@"Synchronisation will begin");
    if (!self.done)
    {
        if (self.delegate && [self.delegate respondsToSelector:@selector(syncReporterWillBeginSyncing:)])
        {
            [self.delegate syncReporterWillBeginSyncing:self];
        }
    }
}

/**
 *  Handles LYRClientDidFinishSynchronizationNotification and if it was first synchronisation tells delegate that synchronisation did finish.
 *
 *  @param notification Received NSNotification.
 */
- (void)didReceiveDidFinishSynchronizationNotification:(NSNotification *)notification
{
    NSLog(@"Synchronisation did finish");
    if (!self.done)
    {
        self.done = YES;
        if (self.delegate && [self.delegate respondsToSelector:@selector(syncReporterDidFinishSyncing:)])
        {
            [self.delegate syncReporterDidFinishSyncing:self];
        }
    }
}

@end
