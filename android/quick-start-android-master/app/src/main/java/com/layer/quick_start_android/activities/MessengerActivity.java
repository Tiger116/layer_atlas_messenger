package com.layer.quick_start_android.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.layer.quick_start_android.ImageParams;
import com.layer.quick_start_android.LayerApplication;
import com.layer.quick_start_android.MessageView;
import com.layer.quick_start_android.MyAutoCompleteTextView;
import com.layer.quick_start_android.R;
import com.layer.quick_start_android.layer_utils.ConversationViewController;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static com.layer.quick_start_android.LayerApplication.conversationView;
import static com.layer.quick_start_android.LayerApplication.getImageURI;
import static com.layer.quick_start_android.LayerApplication.getUserIdByName;
import static com.layer.quick_start_android.LayerApplication.getUserNameById;
import static com.layer.quick_start_android.LayerApplication.layerClient;
import static com.layer.quick_start_android.LayerApplication.setImageURI;

public class MessengerActivity extends ActionBarActivity implements View.OnClickListener, TextWatcher, LayerTypingIndicatorListener {
    private static final int REQUEST_LOAD_IMAGE = 2;
    private static final int REQUEST_OPEN_CAMERA = 3;
    private static final int REQUEST_MAP_MARKERS = 4;

    //List of all users currently typing
    private ArrayList<String> typingUsers;
    //GUI elements
    private Button sendButton;
    private ScrollView conversationScroll;
    private LinearLayout conversationLayout;
    private TextView typingIndicator;
    private MyAutoCompleteTextView usersView;
    private EditText userInput;
    private ImageButton attach_button;
    private ArrayAdapter<String> myAutoCompleteAdapter;
    private String parameter = null;
    //All messages
    private Hashtable<String, MessageView> allMessages;
    private PopupWindow popUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        LayerApplication.setCurrentActivity(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle.getString(getString(R.string.conversation_id_key)) != null)
            parameter = bundle.getString(getString(R.string.conversation_id_key));
        else if (bundle.getString(getString(R.string.participant_key)) != null)
            parameter = bundle.getString(getString(R.string.participant_key));
        if (conversationView == null)
            conversationView = new ConversationViewController(parameter);
        else
            conversationView.setConversation(parameter);
        if (layerClient != null) {
            layerClient.registerTypingIndicator(this);
        }

        //Cache off gui objects
        sendButton = (Button) findViewById(R.id.send);
        userInput = (EditText) findViewById(R.id.input);
        attach_button = (ImageButton) findViewById(R.id.attachment);
        conversationScroll = (ScrollView) findViewById(R.id.scrollView);
        conversationLayout = (LinearLayout) findViewById(R.id.conversation);
        typingIndicator = (TextView) findViewById(R.id.typingIndicator);

        //List of users that are typing which is used with LayerTypingIndicatorListener
        typingUsers = new ArrayList<>();
        //Capture user input
        sendButton.setOnClickListener(this);
        attach_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPopUp((View) conversationScroll.getParent());
            }
        });
        userInput.setText("");
        userInput.addTextChangedListener(this);

        usersView = (MyAutoCompleteTextView) findViewById(R.id.participants_text);
        usersView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString();
                addUser(getUserIdByName(item));
            }
        });
        ArrayList<String> availableUserNames = getAvailableUserNames();
        myAutoCompleteAdapter = new ArrayAdapter<>(MessengerActivity.this, android.R.layout.simple_dropdown_item_1line, availableUserNames);
        usersView.setTokenizer(new MyAutoCompleteTextView.CommaTokenizer());
        usersView.setAdapter(myAutoCompleteAdapter);
        usersView.addTextChangedListener(new TextWatcher() {
            private String user = null;
            private String deleteUser = null;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!s.toString().isEmpty() &&
                        conversationView.getConversation().getParticipants().contains(LayerApplication.getUserIdByName(s.toString())))
                    user = Arrays.asList(s.toString().split(", ")).get(0);
                else
                    user = null;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> textUsers = new ArrayList<>(Arrays.asList(s.toString().split(", ")));
                if (textUsers.contains(""))
                    textUsers.removeAll(Collections.singleton(""));
                if (!s.toString().isEmpty() && count == 0) {
                    if ((conversationView.getConversation().getParticipants().size() - 1) > textUsers.size()) {
                        for (String participant : conversationView.getConversation().getParticipants()) {
                            if (!textUsers.contains(getUserNameById(participant)) && !participant.equals(layerClient.getAuthenticatedUserId())) {
                                myAutoCompleteAdapter.add(getUserNameById(participant));
                                deleteUser = participant;
                                drawConversation();
                            }
                        }
                    }
                } else if (user != null) {
                    addBubble(user);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (deleteUser != null) {
                    conversationView.getConversation().removeParticipants(deleteUser);
                    deleteUser = null;
                    drawConversation();
                }
            }
        });

        //If there is an active conversation, draw it
        drawConversation();
    }

    private void createPopUp(final View parent) {

        if (popUp == null) {
            View popUpView = getLayoutInflater().inflate(R.layout.popup_attach, null);

            popUp = new PopupWindow(popUpView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
            popUp.setOutsideTouchable(true);


            LinearLayout picturesLayout = (LinearLayout) popUpView.findViewById(R.id.pictures);
            picturesLayout.setOnClickListener(onPopUpItemClick);
            LinearLayout photoLayout = (LinearLayout) popUpView.findViewById(R.id.take_photo);
            photoLayout.setOnClickListener(onPopUpItemClick);
            LinearLayout lastImageLayout = (LinearLayout) popUpView.findViewById(R.id.last_image);
            lastImageLayout.setOnClickListener(onPopUpItemClick);
            LinearLayout mapLayout = (LinearLayout) popUpView.findViewById(R.id.map_marker);
            mapLayout.setOnClickListener(onPopUpItemClick);

            popUp.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    parent.setAlpha(1.0f);
                }
            });
            popUp.setBackgroundDrawable(new BitmapDrawable(null, ""));
        }
        parent.setAlpha(0.3f);
        popUp.showAtLocation(parent, Gravity.CENTER, 0, 0);
    }

    View.OnClickListener onPopUpItemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.pictures:
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQUEST_LOAD_IMAGE);
                    break;
                case R.id.take_photo:
                    File phoroFile = new File(getExternalFilesDir(null) + File.separator + layerClient.getAuthenticatedUserId(), "layer-photo.jpg");
                    setImageURI(Uri.fromFile(phoroFile));
                    Log.d("Messenger", getImageURI().toString());
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getImageURI());
                    startActivityForResult(cameraIntent, REQUEST_OPEN_CAMERA);
                    break;
                case R.id.last_image:
                    if (getImageURI() != null) {
                        createImageMessage(getImageURI());
                    } else
                        Toast.makeText(MessengerActivity.this, "You don't have last sent image", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.map_marker:
                    Intent mapIntent = new Intent(MessengerActivity.this, MapActivity.class);
                    startActivityForResult(mapIntent, REQUEST_MAP_MARKERS);
                    break;
            }
            if (popUp != null)
                popUp.dismiss();
        }
    };

    //Create a new message and send it
    private void sendButtonClicked() {

        //Check to see if there is an active conversation between the pre-defined participants
        if (conversationView.getConversation() == null) {
            conversationView.setConversation(parameter);
        }
        if (!userInput.getText().toString().isEmpty())
            sendMessage(userInput.getText().toString());
        //Clears the text input field
        userInput.setText("");
    }

    private void sendMessage(String text) {

        //Put the user's text into a message part, which has a MIME type of "text/plain" by default
        MessagePart messagePart = layerClient.newMessagePart(text);

        //Creates and returns a new message object with the given conversation and array of message parts
        Message message = layerClient.newMessage(Arrays.asList(messagePart));

        //Formats the push notification that the other participants will receive
        Map<String, String> metadata = new HashMap<>();
        metadata.put("layer-push-message", ParseUser.getCurrentUser().getUsername() + ": " + text);
        message.setMetadata(metadata);

        //Sends the message
        if (conversationView.getConversation() != null)
            conversationView.getConversation().send(message);
    }

    //Redraws the conversation window in the GUI
    public void drawConversation() {
        //Only proceed if there is a valid conversation
        if (conversationView.getConversation() != null) {
            if (conversationView.getConversation().isDeleted())
                finish();

            List<String> participantIdList = conversationView.getConversation().getParticipants();

            if (participantIdList.isEmpty())
                finish();
            String title;
            if (conversationView.getConversation().getMetadata().get(getString(R.string.title_label)) != null)
                title = conversationView.getConversation().getMetadata().get(getString(R.string.title_label)).toString();
            else {
                List<String> userNames = new ArrayList<>();
                for (String userId : participantIdList) {
                    String userName = getUserNameById(userId);
                    if (userName == null)
                        userName = userId;
                    userNames.add(userName);
                }
                title = userNames.toString();
            }
            setTitle(title);

            //Clear the GUI first and empty the list of stored messages
            conversationLayout.removeAllViews();
            allMessages = new Hashtable<>();

            //Grab all the messages from the conversation and add them to the GUI
            List<Message> allMsgs = layerClient.getMessages(conversationView.getConversation());
            for (Message msg : allMsgs) {
                addMessageToView(msg);
            }

            //After redrawing, force the scroll view to the bottom (most recent message)
            conversationScroll.post(new Runnable() {
                @Override
                public void run() {
                    conversationScroll.fullScroll(View.FOCUS_DOWN);
                }
            });

            for (String userId : participantIdList) {
                if (!userId.equals(layerClient.getAuthenticatedUserId())) {
                    String userName = getUserNameById(userId);
                    if (userName != null)
                        addBubble(userName);
                    else
                        addBubble(userId);
                }
            }
            userInput.requestFocus();
        } else
            finish();
    }

    //Creates a GUI element (header and body) for each Message
    private void addMessageToView(Message msg) {
        usersView.setText("");
        //Make sure the message is valid
        if (msg == null)
            return;

        //Once the message has been displayed, we mark it as read
        //NOTE: the sender of a message CANNOT mark their own message as read
        if (!msg.getSentByUserId().equalsIgnoreCase(layerClient.getAuthenticatedUserId()))
            msg.markAsRead();

        //Grab the message id
        String msgId = msg.getId().toString();

        //If we have already added this message to the GUI, skip it
        if (!allMessages.contains(msgId)) {
            //Build the GUI element and save it
            LinearLayout messageLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.message, conversationLayout, false);
            MessageView msgView = new MessageView(conversationLayout, messageLayout, msg);
            allMessages.put(msgId, msgView);
        }
    }

    //================================================================================
    // View.OnClickListener methods
    //================================================================================

    public void onClick(View v) {
        //When the "send" button is clicked, grab the ongoing conversation (or create it) and send the message
        if (v == sendButton) {
            sendButtonClicked();
        }
    }

    private void addUser(String userId) {
        conversationView.getConversation().addParticipants(userId);
        String name = getUserNameById(userId);
        if (name == null)
            name = userId;
        myAutoCompleteAdapter.remove(name);
        addBubble(name);
    }

    private ArrayList<String> getAvailableUserNames() {
        ArrayList<String> users = new ArrayList<>();
        if (conversationView.getConversation() != null)
            for (ParseObject obj : LayerApplication.getParseUsers()) {
                if (!conversationView.getConversation().getParticipants().contains(obj.getObjectId()))
                    users.add(((ParseUser) obj).getUsername());
            }
        return users;
    }

    private void addBubble(String userName) {
        if (!usersView.getText().toString().contains(userName)) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            TextView tv = createContactTextView(userName);
            BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(tv);

            bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
            sb.append(userName);
            sb.setSpan(new ImageSpan(bd), 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(", ");

            usersView.setMovementMethod(LinkMovementMethod.getInstance());
            usersView.append(sb);
        }
    }

    public TextView createContactTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.white));
        tv.setTextSize(14);
        tv.setBackgroundResource(R.drawable.btn);
        return tv;
    }

    public Object convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(getResources(), viewBmp);
    }

    //onResume is called on App Start and when the app is brought to the foreground
    protected void onResume() {
        super.onResume();

        LayerApplication.setCurrentActivity(this);

        //Every time the app is brought to the foreground, register the typing indicator
        if (layerClient != null && conversationView != null)
            layerClient.registerTypingIndicator(this);
    }

    //onPause is called when the app is sent to the background
    protected void onPause() {
        super.onPause();

        LayerApplication.setCurrentActivity(null);

        //When the app is moved to the background, unregister the typing indicator
        if (layerClient != null && conversationView != null)
            layerClient.unregisterTypingIndicator(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_messenger, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
//            case android.R.id.home:
//                setResult(RESULT_OK);
//                finish();
//                return true;
            case R.id.action_add_user:
                Intent intentAdd = new Intent(MessengerActivity.this, UsersActivity.class);
                startActivityForResult(intentAdd, MainActivity.requestCodeUsers);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //================================================================================
    // TextWatcher methods
    //================================================================================

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        //After the user has changed some text, we notify other participants that they are typing
        if (conversationView.getConversation() != null)
            conversationView.getConversation().send(LayerTypingIndicatorListener.TypingIndicator.STARTED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.requestCodeUsers:
                if (resultCode == RESULT_OK) {
                    String participantId = data.getExtras().getString(getString(R.string.participant_key));
                    String participantName = getUserNameById(participantId);
                    if (!usersView.getText().toString().contains(participantName) && !usersView.getText().toString().contains(layerClient.getAuthenticatedUserId())) {
                        addUser(participantName);
                    }
                }
                break;
            case REQUEST_OPEN_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (getImageURI() != null)
                        createImageMessage(getImageURI());
                }
            case REQUEST_LOAD_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    setImageURI(data.getData());
                    createImageMessage(getImageURI());
                }
                break;
            case REQUEST_MAP_MARKERS:
                if (data != null) {
                    List<MessagePart> parts = new ArrayList<>();
                    double[] latitudes = data.getExtras().getDoubleArray("latitude");
                    double[] longitudes = data.getExtras().getDoubleArray("longitude");
                    Log.d(this.toString(), Arrays.toString(latitudes));
                    Log.d(this.toString(), Arrays.toString(longitudes));
                    for (int i = 0; i < latitudes.length; i++) {
                        HashMap location = new HashMap<String, String>();
                        location.put("lat", latitudes[i]);
                        location.put("lon", longitudes[i]);

//                        Convert the location to data
                        ByteArrayOutputStream locationData = new ByteArrayOutputStream();
                        ObjectOutputStream outputStream;
                        try {
                            outputStream = new ObjectOutputStream(locationData);
                            outputStream.writeObject(location);
                            MessagePart locationPart = layerClient.newMessagePart("text/location", locationData.toByteArray());
                            parts.add(locationPart);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!parts.isEmpty()) {
                        Message message = layerClient.newMessage(parts);
                        conversationView.getConversation().send(message);
                    }
                }
                break;
        }
    }

    private void createImageMessage(Uri selectedImage) {
        Bitmap bitmap;
        ByteArrayOutputStream outputStream;
        InputStream inputStream;
        int arraySize;
        byte[] imageArray;
        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            inputStream = cr.openInputStream(selectedImage);
            bitmap = BitmapFactory.decodeStream(inputStream);
            outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            imageArray = outputStream.toByteArray();

            if (imageArray != null) {
                arraySize = imageArray.length;
                Log.d("SEND IMAGE", "length original: " + arraySize);
                Log.d("SEND IMAGE", "sizes original: " + bitmap.getWidth() + " x " + bitmap.getHeight());
                MessagePart jpeg = layerClient.newMessagePart("image/jpeg", imageArray);
                ImageParams params = new ImageParams(bitmap);
                Gson gson = new Gson();
                String json = gson.toJson(params);
                Log.d("SEND IMAGE", "json: " + json);
                MessagePart jsonPart = layerClient.newMessagePart("application/json+imageSize", json.getBytes());
                final int reqSize = 1024 * 128;
                int inSampleSize = 2;
                while (arraySize > reqSize) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inSampleSize = calculateInSampleSize(imageArray.length, reqSize);
                    options.inSampleSize = inSampleSize;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    inputStream = cr.openInputStream(selectedImage);
                    outputStream.reset();
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    arraySize = outputStream.toByteArray().length;
                    inSampleSize += 2;
                }
                Log.d("SEND IMAGE", "length preview: " + arraySize);
                Log.d("SEND IMAGE", "sizes preview: " + bitmap.getWidth() + " x " + bitmap.getHeight());
                MessagePart jpegPreview = layerClient.newMessagePart("image/jpeg+preview", outputStream.toByteArray());
                Message message = layerClient.newMessage(jpeg, jpegPreview, jsonPart);
                conversationView.getConversation().send(message);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

//    private int calculateInSampleSize(long size, long reqSize) {
//        if (size > reqSize) {
//            int inSampleSize = 2;
//
//            while ((size / inSampleSize) > reqSize) {
//                inSampleSize += 2;
//            }
//            return inSampleSize;
//        } else
//            return 1;
//    }

    //================================================================================
    // LayerTypingIndicatorListener methods
    //================================================================================

    @Override
    public void onTypingIndicator(LayerClient layerClient, Conversation conversation, String userID, LayerTypingIndicatorListener.TypingIndicator indicator) {
        switch (indicator) {
            case STARTED:
                // This user started typing, so add them to the typing list if they are not already on it.
                if (!typingUsers.contains(userID) && !userID.equals(layerClient.getAuthenticatedUserId()))
                    typingUsers.add(userID);
                break;

            case FINISHED:
                // This user isn't typing anymore, so remove them from the list.
                typingUsers.remove(userID);
                break;
        }

        if (typingUsers.size() == 0) {

            //No one is typing
            typingIndicator.setText("");
            typingIndicator.setVisibility(View.GONE);

        } else if (typingUsers.size() == 1) {
            String typeUser = LayerApplication.getUserNameById(typingUsers.get(0));
            if (typeUser == null)
                typeUser = typingUsers.get(0);
            //Name the one user that is typing (and make sure the text is grammatically correct)
            typingIndicator.setVisibility(View.VISIBLE);
            typingIndicator.setText(typeUser + " is typing");

        } else if (typingUsers.size() > 1) {

            //Name all the users that are typing (and make sure the text is grammatically correct)
            String users = "";
            for (int i = 0; i < typingUsers.size(); i++) {
                String typeUser = LayerApplication.getUserNameById(typingUsers.get(i));
                if (typeUser == null)
                    typeUser = typingUsers.get(i);
                users += typeUser;
                if (i < typingUsers.size() - 1)
                    users += ", ";
            }

            typingIndicator.setText(users + " are typing");
        }
    }
}
