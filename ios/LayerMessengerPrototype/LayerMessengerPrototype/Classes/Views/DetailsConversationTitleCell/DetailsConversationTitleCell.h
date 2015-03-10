//
//  DetailsConversationTitleCell.h
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 06/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DetailsConversationTitleCell : UITableViewCell

-(void) configureCellWithConversationName:(NSString*)title;
-(void)setTextFieldDelegate:(id<UITextFieldDelegate>)delegate;

@end
