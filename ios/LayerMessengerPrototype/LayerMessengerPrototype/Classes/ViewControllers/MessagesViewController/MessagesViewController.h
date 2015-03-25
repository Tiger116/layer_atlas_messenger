//
//  MessagesViewController.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <LayerKit/LayerKit.h>
#import <Atlas.h>

@interface MessagesViewController : ATLConversationViewController <ATLConversationViewControllerDataSource, ATLConversationViewControllerDelegate, ATLParticipantTableViewControllerDelegate>

+ (instancetype) conversationViewControllerWithLayerClient:(LYRClient *)layerClient andConversation:(LYRConversation *)conversation;

@end
