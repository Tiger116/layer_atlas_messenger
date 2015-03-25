package com.layer.quick_start_android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MainActivity extends ActionBarActivity implements LayerChangeEventListener.BackgroundThread {

    //Layer connection and authentication callback listeners
    private MyConnectionListener connectionListener;
    private MyAuthenticationListener authenticationListener;


    public static final int requestCodeLogin = 0;
    public static final int requestCodeUsers = 1;

    private ProgressDialog dialog;
    private ArrayAdapter<String> adapter;
    private List<Conversation> conversations;
    private ArrayList<String> conversNames = new ArrayList<>();
    private static ArrayList<String> participants;

    private int count = 0;

    //onCreate is called on App Start
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (connectionListener == null)
            connectionListener = new MyConnectionListener(this);

        if (authenticationListener == null)
            authenticationListener = new MyAuthenticationListener(this);

        ListView lvMain = (ListView) findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, conversNames);
        layerClient.registerEventListener(this);
        conversations = new ArrayList<>();
        lvMain.setAdapter(adapter);
        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conversation = conversations.get(position);
                participants = new ArrayList<>(conversation.getParticipants());
                Intent intent = new Intent(MainActivity.this, MessengerActivity.class);
                intent.putExtra(getString(R.string.title_label), conversNames.get(position));
                intent.putExtra(getString(R.string.conversation_id_key), conversation.getId().toString());
                startActivity(intent);
            }
        });
        registerForContextMenu(lvMain);
    }

    private void dataChange() {
        conversations.clear();
        if (layerClient.isAuthenticated()) {
            conversations = layerClient.getConversations();
            if (!conversations.isEmpty()) {
                conversNames.clear();
                for (Conversation conversation : conversations) {
                    String s;
                    if (conversation.getMetadata().get(getString(R.string.title_label)) == null)
                        s = conversation.getParticipants().toString();
                    else
                        s = conversation.getMetadata().get(getString(R.string.title_label)).toString();
                    conversNames.add(s);
                }
                adapter.notifyDataSetChanged();
                if (dialog != null)
                    dialog.dismiss();
            } else {
                if (count < 4) {
                    Handler handler = new Handler();
                    handler.postDelayed(r, 1000);
                } else {
                    count = 0;
                    if (dialog != null)
                        dialog.dismiss();
                }       // Toast.makeText(MainActivity.this,"Error sync with server! Please try later", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Runnable r = new Runnable() {
        @Override
        public void run() {
            count++;
            dataChange();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //Connect to Layer and Authenticate a user
        loadLayerClient();
    }

    public static List<String> getCurrentParticipants() {
        if (participants != null) {
            return participants;
        }
        return new ArrayList<>();
    }

    public static void setParticipants(ArrayList<String> participants) {
        MainActivity.participants = participants;
    }

    public static void addParticipant(String participant) {
        MainActivity.participants.add(participant);
    }

    public static void removeParticipant(String participant) {
        MainActivity.participants.remove(participant);
    }

    //Checks to see if the SDK is connected to Layer and whether a user is authenticated
    //The respective callbacks are executed in MyConnectionListener and MyAuthenticationListener
    private void loadLayerClient() {

        if (layerClient != null) {
            layerClient.registerConnectionListener(connectionListener);
            layerClient.registerAuthenticationListener(authenticationListener);
            if (ParseUser.getCurrentUser() != null) {
                if (!layerClient.isAuthenticated()) layerClient.authenticate();
                else if (!layerClient.isConnected()) layerClient.connect();
                else onUserAuthenticated();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent, requestCodeLogin);
            }
        }
    }

    public void onAuthenticateStart() {
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    //Once the user has successfully authenticated, begin the conversationView
    public void onUserAuthenticated() {
        dataChange();
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
        conversNames.clear();
        adapter.notifyDataSetChanged();
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
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Conversation conversation = conversations.get(info.position);
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
                input.setText(conversNames.get(info.position));
                input.setSelection(input.getText().length());
                dialog.setView(input);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String title = input.getEditableText().toString();
                        if (title.isEmpty())
                            title = conversation.getParticipants().toString();
                        conversation.putMetadataAtKeyPath(getString(R.string.title_label), title);
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
                    onAuthenticateStart();
                    layerClient.authenticate();
                } else
                    this.finish();
                break;
            case requestCodeUsers:
                if (resultCode == RESULT_OK) {
                    String participantName = data.getExtras().getString(getString(R.string.participants));
                    if (!participantName.equals(layerClient.getAuthenticatedUserId())) {
                        participants = new ArrayList<>(Arrays.asList(layerClient.getAuthenticatedUserId(), participantName));
                        Intent intent = new Intent(MainActivity.this, MessengerActivity.class);
                        intent.putExtra(getString(R.string.title_label), getCurrentParticipants().toString());
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

    @Override
    public void onEventAsync(LayerChangeEvent layerChangeEvent) {
        Log.d(MainActivity.class.toString(), "Data changed");
        dataChange();
    }
}