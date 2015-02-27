//
//  MessagesViewController.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 20/02/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <LayerKit/LayerKit.h>

@interface MessagesViewController : UIViewController

@property (nonatomic) LYRClient *layerClient;
@property (nonatomic, strong) LYRConversation* conversation;
@property (strong, nonatomic) IBOutlet UITableView *tableView;
@property (strong, nonatomic) IBOutlet UITextView *textView;

@end
