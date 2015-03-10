//
//  DetailsConversationTitleCell.m
//  LayerMessengerPrototype
//
//  Created by Шамро Артур on 06/03/15.
//  Copyright (c) 2015 Шамро Артур. All rights reserved.
//

#import "DetailsConversationTitleCell.h"

@interface DetailsConversationTitleCell ()

@property (strong, nonatomic) IBOutlet UITextField *titleTextField;

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
    if (title) {
        //self.titleTextField.textColor = [UIColor blackColor];
        self.titleTextField.text = title;
    } else {
        //self.titleTextField.textColor = [UIColor grayColor];
        //self.titleTextField.text = @"Enter conversation name";
    }
    
}

-(void)setTextFieldDelegate:(id<UITextFieldDelegate>)delegate
{
    self.titleTextField.delegate = delegate;
}

//- (IBAction)titleEditingDidEnd:(UITextField *)sender
//{
//    if (sender.text)
//    {
//        <#statements#>
//    }
//}

@end
