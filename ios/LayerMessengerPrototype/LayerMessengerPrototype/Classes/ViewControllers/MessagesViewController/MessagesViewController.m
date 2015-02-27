//
//  MessagesViewController.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "MessagesViewController.h"
#import "MessageCell.h"

@interface MessagesViewController () <UITableViewDataSource, UITableViewDelegate>

@end

@implementation MessagesViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    //[self setupLayerNotificationObservers];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

//- (void)setupLayerNotificationObservers
//{
//    // Register for Layer object change notifications
//    // For more information about Synchronization, check out https://developer.layer.com/docs/integration/ios#synchronization
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(didReceiveLayerObjectsDidChangeNotification:)
//                                                 name:LYRClientObjectsDidChangeNotification
//                                               object:nil];
//    
//    // Register for typing indicator notifications
//    // For more information about Typing Indicators, check out https://developer.layer.com/docs/integration/ios#typing-indicator
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(didReceiveTypingIndicator:)
//                                                 name:LYRConversationDidReceiveTypingIndicatorNotification
//                                               object:self.conversation];
//    
//    // Register for synchronization notifications
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(didReceiveLayerClientWillBeginSynchronizationNotification:)
//                                                 name:LYRClientWillBeginSynchronizationNotification
//                                               object:self.layerClient];
//    
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(didReceiveLayerClientDidFinishSynchronizationNotification:)
//                                                 name:LYRClientDidFinishSynchronizationNotification
//                                               object:self.layerClient];
//}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 44;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return 0;
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    MessageCell *cell = (MessageCell*)[tableView dequeueReusableCellWithIdentifier:@"MessageCell" forIndexPath:indexPath];

    
    return cell;
}

@end
