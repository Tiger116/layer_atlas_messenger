//
//  DetailsConversationTitleCell.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 06/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "DetailsConversationTitleCell.h"

@interface DetailsConversationTitleCell ()



@end

@implementation DetailsConversationTitleCell

- (void)awakeFromNib {
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

-(void) configureCellWithConversationName:(NSString*)title
{
    if (title)
    {
        self.titleTextField.text = title;
    }
    
}

-(void)setTextFieldDelegate:(id<UITextFieldDelegate>)delegate
{
    self.titleTextField.delegate = delegate;
}

@end
