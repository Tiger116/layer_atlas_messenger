package com.layer.quick_start_android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static com.layer.quick_start_android.LayerApplication.layerClient;

/**
 * Takes a Layer Message object, formats the text and attaches it to a LinearLayout
 */
public class MessageView {

    //The sender and message views
    private ImageView senderPhoto;
    private TextView senderTV;
    private TextView sendTime;
    private TextView messageTV;
    private ImageView statusImage;

    private Context context;
    private Conversation conversation;

    //Takes the Layout parent object and message
    public MessageView(LinearLayout parent, LinearLayout meLayout, Message msg) {
        this.context = LayerApplication.getContext();
        this.conversation = msg.getConversation();
        senderPhoto = (ImageView) meLayout.findViewById(R.id.message_user_photo);
        senderTV = (TextView) meLayout.findViewById(R.id.message_username);
        sendTime = (TextView) meLayout.findViewById(R.id.message_time);
        statusImage = (ImageView) meLayout.findViewById(R.id.message_status);
        createStatusImage(msg);
        messageTV = (TextView) meLayout.findViewById(R.id.message_text);
        //Populates the text views
        craftMessage(msg);
        parent.addView(meLayout);
    }

    private void craftMessage(Message msg) {

        //The User ID
        String senderTxt = msg.getSentByUserId();
        Drawable background;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (layerClient.getAuthenticatedUserId().equals(senderTxt)) {
            senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_green);
            params.gravity = Gravity.END;
            messageTV.setLayoutParams(params);
        } else {
            if (conversation.getParticipants().size() <= 2)
                senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_yellow);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            messageTV.setBackground(background);
        } else {
            messageTV.setBackgroundDrawable(background);
        }
        if (senderTxt.isEmpty()) {
            senderTV.setVisibility(View.GONE);
        } else {
            senderTV.setText(senderTxt);
            senderTV.setVisibility(View.VISIBLE);
        }

        //Add the timestamp
        String time = "";
        if (msg.getSentAt() != null) {
            time = new SimpleDateFormat("dd MMMM H:mm:ss").format(msg.getReceivedAt());
        }
        sendTime.setText(time);

        //The message text
        String msgText = "";

        //Go through each part, and if it is text (which it should be by default), append it to the
        // message text
        List<MessagePart> parts = msg.getMessageParts();
        for (int i = 0; i < msg.getMessageParts().size(); i++) {

            //You can always set the mime type when creating a message part, by default the mime type
            // is initialized to plain text when the message part is created
            if (parts.get(i).getMimeType().equalsIgnoreCase("text/plain")) {
                try {
                    msgText += new String(parts.get(i).getData(), "UTF-8") + "\n";
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        messageTV.setText(msgText);
    }

    //Checks the recipient status of the message (based on all participants)
    private Message.RecipientStatus getMessageStatus(Message msg) {

        //If we didn't send the message, we already know the status - we have read it
        if (!msg.getSentByUserId().equalsIgnoreCase(layerClient.getAuthenticatedUserId()))
            return Message.RecipientStatus.READ;

        //Assume the message has been sent
        Message.RecipientStatus status = Message.RecipientStatus.SENT;

        //Go through each user to check the status, in this case we check each user and prioritize so
        // that we return the highest status: Sent -> Delivered -> Read
        for (int i = 0; i < conversation.getParticipants().size(); i++) {

            //Don't check the status of the current user
            String participant = conversation.getParticipants().get(i);
            if (participant.equalsIgnoreCase(layerClient.getAuthenticatedUserId()))
                continue;

            if (status == Message.RecipientStatus.SENT) {

                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.DELIVERED)
                    status = Message.RecipientStatus.DELIVERED;

                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.READ)
                    return Message.RecipientStatus.READ;

            } else if (status == Message.RecipientStatus.DELIVERED) {
                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.READ)
                    return Message.RecipientStatus.READ;
            }
        }
        return status;
    }

    //Sets the status image based on whether other users in the conversation have received or read
    //the message
    private void createStatusImage(Message msg) {

        switch (getMessageStatus(msg)) {

            case SENT:
                statusImage.setImageResource(R.drawable.sent);
                break;

            case DELIVERED:
                statusImage.setImageResource(R.drawable.delivered);
                break;

            case READ:
                statusImage.setImageResource(R.drawable.read);
                break;
        }

//        //Have the icon fill the space vertically
//        statusImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
    }

}
