//
//  DetailsViewController.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 06/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <LayerKit/LayerKit.h>

@protocol DetailsViewControllerDelegate <NSObject>

-(void) conversationTitleDidChange:(NSString*) newTitle;

@end

@interface DetailsViewController : UITableViewController

@property (nonatomic) LYRConversation *conversation;
@property (nonatomic, weak) id<DetailsViewControllerDelegate> delegate;
@property (nonatomic) LYRClient* layerClient;

@end
