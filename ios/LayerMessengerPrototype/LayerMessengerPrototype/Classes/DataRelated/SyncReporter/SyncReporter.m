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

//+(instancetype)sharedSyncReporterWithLayerClient:(LYRClient *)layerClient
//{
//    static dispatch_once_t onceToken;
//    static SyncReporter *syncReporter = nil;
//    dispatch_once(&onceToken,^{
//        syncReporter = [[self alloc] initWithClient:layerClient];
//    });
//    return syncReporter;
//}

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
