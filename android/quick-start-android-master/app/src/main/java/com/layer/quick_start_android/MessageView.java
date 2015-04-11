package com.layer.quick_start_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.layer.quick_start_android.layer_utils.MyProgressListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static com.layer.quick_start_android.LayerApplication.getUserNameById;
import static com.layer.quick_start_android.LayerApplication.layerClient;

/**
 * Takes a Layer Message object, formats the text and attaches it to a LinearLayout
 */
public class MessageView {

    //The sender and message views
//    private ImageView senderPhoto;
    private TextView senderTV;
    private TextView sendTime;
    private TextView messageTV;
    private View messageImageIndent;
    private RoundedCorners messageImage;
    private ImageView statusImage;

    private Context context;
    private Conversation conversation;

    //Takes the Layout parent object and message
    public MessageView(LinearLayout parent, LinearLayout meLayout, Message msg) {
        this.context = LayerApplication.getContext();
        this.conversation = msg.getConversation();
//        senderPhoto = (ImageView) meLayout.findViewById(R.id.message_user_photo);
        senderTV = (TextView) meLayout.findViewById(R.id.message_username);
        sendTime = (TextView) meLayout.findViewById(R.id.message_time);
        messageTV = (TextView) meLayout.findViewById(R.id.message_text);
        messageImageIndent = meLayout.findViewById(R.id.message_image_indent);
        messageImage = (RoundedCorners) meLayout.findViewById(R.id.message_image);
        messageImage.setRadius(10);
        statusImage = (ImageView) meLayout.findViewById(R.id.message_status);
        createStatusImage(msg);
        //Populates the text views
        craftMessage(msg);
        parent.addView(meLayout);
    }

    private void craftMessage(Message msg) {

        String senderTxt = msg.getSentByUserId();
        Drawable background;
        if (!layerClient.getAuthenticatedUserId().equals(senderTxt)) {
            if (conversation.getParticipants().size() > 2) {
                if (getUserNameById(senderTxt) != null)
                    senderTxt = getUserNameById(msg.getSentByUserId());
            } else
                senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_yellow);
            messageImageIndent.setVisibility(View.GONE);
        } else {
            senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_green);
            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.gravity = Gravity.END;
            messageTV.setLayoutParams(paramsText);
            messageImageIndent.setVisibility(View.INVISIBLE);
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
            time = new SimpleDateFormat("dd MMMM H:mm:ss", Locale.ENGLISH).format(msg.getReceivedAt());
        }
        sendTime.setText(time);

        //The message text
        String msgText = "";

        //Go through each part, and if it is text (which it should be by default), append it to the
        // message text
        List<MessagePart> parts = msg.getMessageParts();
        MyProgressListener listener = new MyProgressListener(messageImage);
        for (int i = 0; i < msg.getMessageParts().size(); i++) {
            //You can always set the mime type when creating a message part, by default the mime type
            // is initialized to plain text when the message part is created
            if (parts.get(i).getMimeType().equalsIgnoreCase("text/plain")) {
                try {
                    msgText += new String(parts.get(i).getData(), "UTF-8") + "\n";
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (!msgText.isEmpty()) {
                    messageTV.setVisibility(View.VISIBLE);
                    messageTV.setText(msgText);
                } else
                    messageTV.setVisibility(View.GONE);
            }
            if (parts.get(i).getMimeType().equalsIgnoreCase("image/jpeg")) {
                layerClient.registerProgressListener(parts.get(i), listener);
                if (parts.get(i).isContentReady()) {
                    byte[] imageArray = parts.get(i).getData();
                    if (imageArray != null) {
                        Log.d("SIZE", String.valueOf(imageArray.length));
                        Bitmap bm = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
                        messageImage.setImageBitmap(bm);
                    }
                }
                return;
            }
            if (parts.get(i).getMimeType().equalsIgnoreCase("image/jpeg+preview")) {
                layerClient.registerProgressListener(parts.get(i), listener);
                if (parts.get(i).isContentReady()) {
                    byte[] imageArray = parts.get(i).getData();
                    if (imageArray != null) {
                        Log.d("SIZE PREVIEW", String.valueOf(imageArray.length));
                        Bitmap bm = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
                        messageImage.setImageBitmap(bm);
                    }
                }
            }
            if (parts.get(i).getMimeType().equalsIgnoreCase("application/json+imageSize")) {
                byte[] imageArray = parts.get(i).getData();
                String json = new String(imageArray);
                Log.d("JSON", json);
                Gson gson = new Gson();
                ImageParams params = gson.fromJson(json, ImageParams.class);
//                if (params != null) {
//                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(params.getWidth(), params.getHeight());
//                    messageImage.setLayoutParams(layoutParams);
//                }
            }
//            layerClient.unregisterProgressListener(parts.get(i), listener);
        }
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
