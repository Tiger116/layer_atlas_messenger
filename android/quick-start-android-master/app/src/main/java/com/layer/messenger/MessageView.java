package com.layer.messenger;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.layer.messenger.activities.MapActivity;
import com.layer.messenger.layer_utils.MyProgressListener;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.layer.messenger.LayerApplication.getUserNameById;
import static com.layer.messenger.LayerApplication.layerClient;

/**
 * Takes a Layer Message object, formats the text and attaches it to a LinearLayout
 */
public class MessageView {

    private boolean isImage;
    //The sender and message views
//    private ImageView senderPhoto;
    private TextView senderTV;
    private TextView sendTime;
    private TextView messageTV;
    private View messageImageIndent;
    private RoundedCorners messageImage;
    private ImageView statusImage;
    private LinearLayout locationLayout;
    private ScrollView conversationScroll;

    private Context context;
    private Message message;

    private File imageFile;
    private List<HashMap<Double, Double>> locations;

    //Takes the Layout parent object and message
    public MessageView(LinearLayout parent, LinearLayout meLayout, final Message msg) {
        this.context = LayerApplication.getContext();
        this.message = msg;
        this.conversationScroll = (ScrollView) parent.getParent();
        File directory = new File(context.getExternalCacheDir() + File.separator + layerClient.getAuthenticatedUserId() + File.separator + msg.getConversation().getId().getLastPathSegment());
        directory.mkdirs();
        imageFile = new File(directory, msg.getId().getLastPathSegment() + ".jpeg");
        locations = new ArrayList<>();
//        senderPhoto = (ImageView) meLayout.findViewById(R.id.message_user_photo);
        senderTV = (TextView) meLayout.findViewById(R.id.message_username);
        sendTime = (TextView) meLayout.findViewById(R.id.message_time);
        messageTV = (TextView) meLayout.findViewById(R.id.message_text);
        messageImageIndent = meLayout.findViewById(R.id.message_image_indent);
        messageImage = (RoundedCorners) meLayout.findViewById(R.id.message_image);
        locationLayout = (LinearLayout) meLayout.findViewById(R.id.location_layout);
        locationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!locations.isEmpty()) {
                    Intent intent = new Intent(context, MapActivity.class);
                    double[] longitude = new double[locations.size()];
                    double[] latitude = new double[locations.size()];
                    for (int i = 0; i < locations.size(); i++) {
//                        Log.d(this.toString(), "" + locations.get(i).get("lat"));
                        latitude[i] = locations.get(i).get("lat");
                        longitude[i] = locations.get(i).get("lon");
                    }
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
        messageImage.setRadius(10);
        messageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
        statusImage = (ImageView) meLayout.findViewById(R.id.message_status);

        createStatusImage(msg);

        //Populates the text views
        craftMessage(msg);

        parent.addView(meLayout);
    }

    private void craftMessage(Message msg) {

        String senderTxt = msg.getSender().getUserId();
        Drawable background;
        if (!layerClient.getAuthenticatedUserId().equals(senderTxt)) {
            if (message.getConversation().getParticipants().size() > 2) {
                if (getUserNameById(senderTxt) != null)
                    senderTxt = getUserNameById(msg.getSender().getUserId());
            } else
                senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_yellow);
            messageImageIndent.setVisibility(View.GONE);
        } else {
            senderTxt = "";
            background = context.getResources().getDrawable(R.drawable.bubble_green);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END;
            messageTV.setLayoutParams(params);
            locationLayout.setLayoutParams(params);
            messageImageIndent.setVisibility(View.INVISIBLE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            messageTV.setBackground(background);
            locationLayout.setBackground(background);
        } else {
            messageTV.setBackgroundDrawable(background);
            locationLayout.setBackgroundDrawable(background);
        }
        if (senderTxt.isEmpty()) {
            senderTV.setVisibility(View.GONE);
        } else {
            senderTV.setText(senderTxt);
            senderTV.setVisibility(View.VISIBLE);
        }

        //The message text
        String msgText = "";
        //Go through each part, and if it is text (which it should be by default), append it to the
        // message text
        List<MessagePart> parts = msg.getMessageParts();
        if (parts != null) {
            for (MessagePart part : parts) {
                //You can always set the mime type when creating a message part, by default the mime type
                // is initialized to plain text when the message part is created
                switch (part.getMimeType()) {
                    case "text/plain":
                        try {
                            msgText += new String(part.getData(), "UTF-8") + "\n";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "image/jpeg":
                        isImage = true;
                        if (imageFile.exists())
                            if (imageFile.length() < part.getSize()) {
                                if (part.isContentReady()) {
                                    byte[] imageArray = part.getData();
                                    if (imageArray != null) {
                                        byteArrayToFile(imageArray, imageFile);
                                    }
                                } else {
                                    Log.d("Download part", String.valueOf(part.getId()));
                                    MyProgressListener listener = new MyProgressListener();
                                    part.download(listener);
                                }
                            }
                        break;
                    case "image/jpeg+preview":
                        isImage = true;
                        if (part.isContentReady()) {
                            if (!imageFile.exists()) {
                                byte[] imagePreviewArray = part.getData();
                                if (imagePreviewArray != null) {
                                    byteArrayToFile(imagePreviewArray, imageFile);
                                }
                            }
                        }
                        break;
//                    case "application/json+imageSize":
//                        byte[] jsonArray = part.getData();
//                        if (jsonArray != null) {
//                            String json = new String(jsonArray);
//                            Log.d("JSON", json);
//                            Gson gson = new Gson();
//                            ImageParams params = gson.fromJson(json, ImageParams.class);
//                        }
//                        break;
                    case "text/location":
                        isImage = false;
                        locationLayout.setVisibility(View.VISIBLE);
                        HashMap<Double, Double> location;
                        byte[] data = part.getData();
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                        ObjectInputStream objectInputStream;
                        try {
                            objectInputStream = new ObjectInputStream(inputStream);
                            location = (HashMap<Double, Double>) objectInputStream.readObject();
                            locations.add(location);
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }

//                Add the timestamp
            String time;
            if (msg.getSentAt() != null) {
                time = new SimpleDateFormat("dd MMMM H:mm:ss", Locale.ENGLISH).format(msg.getSentAt());
            } else {
                statusImage.setVisibility(View.INVISIBLE);
                time = "Loading... Please wait";
            }
            sendTime.setText(time);

            if (!msgText.isEmpty()) {
                messageTV.setVisibility(View.VISIBLE);
                messageTV.setText(msgText);
            } else {
                messageTV.setVisibility(View.GONE);
                if (isImage) {
                    if (imageFile.exists()) {
                        Picasso.with(context).invalidate(imageFile);
                        Picasso.with(context).load(imageFile).error(R.drawable.image_not_available).placeholder(R.drawable.loading).fit().centerInside().into(messageImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                scrollDown();
                            }

                            @Override
                            public void onError() {
                                scrollDown();
                            }

                            private void scrollDown() {
                                conversationScroll.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        conversationScroll.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                            }
                        });
//                        messageImage.setImageURI(Uri.fromFile(imageFile));
                    } else {
                        Log.d("Image not loaded", imageFile.getName());
                        Picasso.with(context).load(R.drawable.loading).into(messageImage);
                    }
                }
            }
        }
    }

    private void openImage() {
        if (imageFile.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(imageFile), "image/*");
            context.startActivity(intent);
        }
    }

    //Checks the recipient status of the message (based on all participants)
    private Message.RecipientStatus getMessageStatus(Message msg) {

        //If we didn't send the message, we already know the status - we have read it
        if (!msg.getSender().getUserId().equalsIgnoreCase(layerClient.getAuthenticatedUserId()))
            return Message.RecipientStatus.READ;

        //Assume the message has been sent
        Message.RecipientStatus status = Message.RecipientStatus.SENT;

        //Go through each user to check the status, in this case we check each user and prioritize so
        // that we return the highest status: Sent -> Delivered -> Read
        for (int i = 0; i < message.getConversation().getParticipants().size(); i++) {

            //Don't check the status of the current user
            String participant = message.getConversation().getParticipants().get(i);
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
    }

    private void byteArrayToFile(byte[] array, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
