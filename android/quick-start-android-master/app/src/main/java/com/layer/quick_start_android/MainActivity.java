package com.layer.quick_start_android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MainActivity extends ActionBarActivity {       //} implements LayerChangeEventListener.MainThread, LayerChangeEventListener.BackgroundThread {

    //Layer connection and authentication callback listeners
    private MyConnectionListener connectionListener;
    private MyAuthenticationListener authenticationListener;

    public static final int requestCodeLogin = 0;
    public static final int requestCodeUsers = 1;

    private ProgressDialog dialog;
    private MyArrayAdapter myAdapter;
    private ArrayList<String> conversationList;
    private List<Conversation> conversations;
    private ListView lvMain;

    private int count = 0;

    //onCreate is called on App Start
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayerApplication.setCurrentActivity(this);

        if (connectionListener == null)
            connectionListener = new MyConnectionListener(this);

        if (authenticationListener == null)
            authenticationListener = new MyAuthenticationListener(this);

        lvMain = (ListView) findViewById(R.id.list_view);


        conversationList = new ArrayList<>();
        conversations = new ArrayList<>();
        myAdapter = new MyArrayAdapter(MainActivity.this, conversationList, conversations);
        lvMain.setAdapter(myAdapter);
        dataChange();
        //layerClient.registerEventListener(this);

        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, MessengerActivity.class);
                intent.putExtra(getString(R.string.conversation_id_key), conversations.get(position).getId().toString());
                startActivity(intent);
            }
        });
        registerForContextMenu(lvMain);
    }

    public void dataChange() {
        if (layerClient.isAuthenticated()) {
            Query query = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING))
                    .build();
//            List<Conversation> conversations;
            conversations = layerClient.executeQuery(query, Query.ResultType.OBJECTS);
//            List<Conversation> conversationList = layerClient.getConversations();
            if (!conversations.isEmpty()) {

//                conversationList = null;
//                conversationList = new ArrayList<>();
                conversationList.clear();
                for (Conversation conversation : conversations) {
                    if (conversation.getMetadata().get(getString(R.string.title_label)) != null)
                        conversationList.add(conversation.getMetadata().get(getString(R.string.title_label)).toString());
                }
//                myAdapter.clear();
//                myAdapter.addAll(conversationList);
//                myAdapter.setList(conversations);
//                myAdapter.notifyDataSetChanged();
//                myAdapter = null;
                myAdapter = new MyArrayAdapter(MainActivity.this, conversationList, conversations);
                lvMain.setAdapter(myAdapter);

                if (dialog != null)
                    dialog.cancel();
            } else {
                if (count < 6) {
                    Handler handler = new Handler();
                    handler.postDelayed(run, 500);
                } else {
                    if (dialog != null)
                        dialog.cancel();
                }
            }
        }
    }

    private Runnable run = new Runnable() {
        @Override
        public void run() {
            count++;
            dataChange();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        LayerApplication.setCurrentActivity(this);

        //if ((myAdapter!=null)&&(!myAdapter.observerRegistered))
        //    myAdapter.registerDataSetObserver(myAdapter.observer);

        //Connect to Layer and Authenticate a user
        loadLayerClient();
    }

    //Checks to see if the SDK is connected to Layer and whether a user is authenticated
    //The respective callbacks are executed in MyConnectionListener and MyAuthenticationListener


    @Override
    protected void onPause() {
        super.onPause();

        LayerApplication.setCurrentActivity(null);
    }

    private void loadLayerClient() {

        if (layerClient != null) {
            layerClient.registerConnectionListener(connectionListener);
            layerClient.registerAuthenticationListener(authenticationListener);
            if (ParseUser.getCurrentUser() != null) {
                if (!layerClient.isAuthenticated()) layerClient.authenticate();
                else if (!layerClient.isConnected()) layerClient.connect();
                else {
                    dataChange();
                }
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent, requestCodeLogin);
            }
        }
    }

    public void showProgressDialog() {
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.log_out_menu:
                logOut();
                return true;
            case R.id.action_add_dialog:
                Intent intent = new Intent(MainActivity.this, UsersActivity.class);
                startActivityForResult(intent, requestCodeUsers);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        ParseUser.logOut();
        layerClient.deauthenticate();
        dataChange();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(intent, requestCodeLogin);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
            case R.id.list_view:
                menu.setHeaderTitle(getString(R.string.options_label));
                menu.add("Rename");
                menu.add("Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String menuItemText = String.valueOf(item.getTitle());
        switch (menuItemText) {
            case "Delete":
                conversations.get(info.position).delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                dataChange();
                break;
            case "Rename":
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Rename");
                dialog.setMessage("Enter new dialog name:");

                final EditText input = new EditText(dialog.getContext());
                input.setSingleLine(true);
                input.setSelectAllOnFocus(true);
                input.setText(conversationList.get(info.position));
                input.setSelection(input.getText().length());
                dialog.setView(input);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String title = input.getEditableText().toString();
                        if (title.isEmpty())
                            title = conversations.get(info.position).getParticipants().toString();
                        conversations.get(info.position).putMetadataAtKeyPath(getString(R.string.title_label), title);
                        conversationList.set(info.position, title);
                        dataChange();
                    }
                });
                dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
                AlertDialog alertDialog = dialog.create();
                alertDialog.show();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case requestCodeLogin:
                if (resultCode == RESULT_OK) {
                    showProgressDialog();
                    layerClient.authenticate();
                } else
                    this.finish();
                break;
            case requestCodeUsers:
                if (resultCode == RESULT_OK) {
                    String participantName = data.getExtras().getString(getString(R.string.participants));
                    if (!participantName.equals(layerClient.getAuthenticatedUserId())) {
//                        Conversation conversation = layerClient.newConversation(layerClient.getAuthenticatedUserId(), participantName);
//                        MessagePart messagePart = layerClient.newMessagePart("text/plain", "Hi, how are you?".getBytes());
//
//// Creates and returns a new message object with the given conversation and array of message parts
//                        Message message = layerClient.newMessage(Arrays.asList(messagePart));
//
////Sends the specified message to the conversation
//                        conversation.send(message);
                        Intent intent = new Intent(MainActivity.this, MessengerActivity.class);
//                        String s = conversation.getId().toString();
//                        myAdapter.notifyDataSetChanged();
                        intent.putExtra(getString(R.string.participant_key), participantName);
                        startActivity(intent);
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

//    @Override
//    public void onEventAsync(LayerChangeEvent layerChangeEvent) {
//        if (this.hasWindowFocus()) {
//            dataChange();
//            Log.d(MainActivity.class.toString(), "Data changed");
//        }
//    }
//
//    @Override
//    public void onEventMainThread(LayerChangeEvent layerChangeEvent) {
//        if (this.hasWindowFocus()) {
//            dataChange();
//            Log.d(MainActivity.class.toString(), "Main Thread Data changed ");
//        }
//    }
}