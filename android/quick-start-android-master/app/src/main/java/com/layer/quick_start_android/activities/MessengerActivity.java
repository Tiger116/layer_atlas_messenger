package com.layer.quick_start_android.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.layer.quick_start_android.ConversationViewController;
import com.layer.quick_start_android.ImageParams;
import com.layer.quick_start_android.LayerApplication;
import com.layer.quick_start_android.MessageView;
import com.layer.quick_start_android.MyAutoCompleteTextView;
import com.layer.quick_start_android.R;
import com.layer.quick_start_android.layer_utils.MyProgressListener;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static com.layer.quick_start_android.LayerApplication.conversationView;
import static com.layer.quick_start_android.LayerApplication.getUserIdByName;
import static com.layer.quick_start_android.LayerApplication.getUserNameById;
import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MessengerActivity extends ActionBarActivity implements View.OnClickListener, TextWatcher, LayerTypingIndicatorListener {
    private static final int REQUEST_LOAD_IMAGE = 2;

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
        attach_button = (ImageButton) findViewById(R.id.attach_photo);
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
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_LOAD_IMAGE);
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
                    if (userName != null)
                        userNames.add(userName);
                    else
                        userNames.add(userId);
                }
                title = userNames.toString();
            }
            setTitle(title);

            //Clear the GUI first and empty the list of stored messages
            conversationLayout.removeAllViews();
            allMessages = new Hashtable<>();

            //Grab all the messages from the conversation and add them to the GUI
            List<Message> allMsgs = layerClient.getMessages(conversationView.getConversation());
            for (int i = 0; i < allMsgs.size(); i++) {
                addMessageToView(allMsgs.get(i));
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
//        if (ma.hasWindowFocus())
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
            usersView.requestFocus();
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
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
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
            case REQUEST_LOAD_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri selectedImage = data.getData();
                    Bitmap bitmap;
                    ByteArrayOutputStream outputStream;
                    byte[] imageArray;
                    int quality = 100;
                    try {
                        ContentResolver cr = getBaseContext().getContentResolver();
                        InputStream inputStream = cr.openInputStream(selectedImage);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        outputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                        imageArray = outputStream.toByteArray();

                        if (imageArray != null) {
                            MessagePart jpeg = layerClient.newMessagePart("image/jpeg", imageArray);
                            ImageParams params = new ImageParams(bitmap);
                            Gson gson = new Gson();
                            String json = gson.toJson(params);
                            MessagePart jsonPart = layerClient.newMessagePart("application/json+imageSize", json.getBytes());

                            Log.d("LENGTH", String.format("%d, %d", imageArray.length, layerClient.getAutoDownloadSizeThreshold()));
                            
                            while (imageArray.length > layerClient.getAutoDownloadSizeThreshold() && quality > 1) {
                                quality /= 2;
                                outputStream.reset();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                                imageArray = outputStream.toByteArray();
                            }
                            MessagePart jpegPreview = layerClient.newMessagePart("image/jpeg+preview", imageArray);
                            Message message = layerClient.newMessage(jpeg, jpegPreview, jsonPart);
                            conversationView.getConversation().send(message);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

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