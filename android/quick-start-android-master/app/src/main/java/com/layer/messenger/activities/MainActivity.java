package com.layer.messenger.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.layer.messenger.LayerApplication;
import com.layer.messenger.MyArrayAdapter;
import com.layer.messenger.R;
import com.layer.messenger.layer_utils.MyAuthenticationListener;
import com.layer.messenger.layer_utils.MyConnectionListener;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.layer.messenger.LayerApplication.layerClient;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    //Layer connection and authentication callback listeners
    private MyConnectionListener connectionListener;
    private MyAuthenticationListener authenticationListener;

    public static final int requestCodeLogin = 0;
    public static final int requestCodeUsers = 1;

    private SwipeRefreshLayout mainLayout;

    private ProgressDialog dialog;
    private MyArrayAdapter myAdapter;
    //    private ArrayList<String> conversationList;
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

        mainLayout = (SwipeRefreshLayout) findViewById(R.id.main_layout);
        mainLayout.setOnRefreshListener(this);

        lvMain = (ListView) findViewById(R.id.list_view);

//        conversationList = new ArrayList<>();
        conversations = new ArrayList<>();
//        myAdapter = new MyArrayAdapter(MainActivity.this, conversations);
//        lvMain.setAdapter(myAdapter);
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
        Handler handler = new Handler();
        conversations.clear();
        if (layerClient.isAuthenticated()) {
            Query query = Query.builder(Conversation.class).sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING)).build();
            conversations = layerClient.executeQuery(query, Query.ResultType.OBJECTS);
            List<Conversation> toRemove = new ArrayList<>();
            if (!conversations.isEmpty()) {
                for (Conversation conversation : conversations) {
                    if (conversation.getParticipants().isEmpty() || conversation.getParticipants().size() == 1
                            || conversation.isDeleted()) {
                        toRemove.add(conversation);
                    }
                }
                if (!toRemove.isEmpty()) {
                    conversations.removeAll(toRemove);
                }
                myAdapter = new MyArrayAdapter(MainActivity.this, conversations);
                lvMain.setAdapter(myAdapter);

                if (dialog != null)
                    dialog.cancel();
            } else {
                if (count < 6) {
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
        Log.d("ON RESUME", "reload");
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
        File cacheDir = new File(getExternalCacheDir() + File.separator + layerClient.getAuthenticatedUserId());
        deleteDir(cacheDir);
        ParseUser.logOut();
        layerClient.deauthenticate();
        if (myAdapter != null)
            myAdapter.clear();
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
                if (conversations.get(info.position).getMetadata().get(getString(R.string.title_label)) != null) {
                    String title = conversations.get(info.position).getMetadata().get(getString(R.string.title_label)).toString();
                    input.setText(title);
                }
                input.setHint("Input dialog name");
//                input.setSelection(input.getText().length());

                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setGravity(Gravity.CENTER_HORIZONTAL);
                layout.setPadding(10, 0, 10, 0);
                layout.addView(input);

                dialog.setView(layout);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String title = input.getEditableText().toString();
                        if (!title.isEmpty())
                            conversations.get(info.position).putMetadataAtKeyPath(getString(R.string.title_label), title);
                        else
                            conversations.get(info.position).removeMetadataAtKeyPath(getString(R.string.title_label));
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
                    if (ParseUser.getCurrentUser() != null)
                        showProgressDialog();
                    layerClient.authenticate();
                } else
                    this.finish();
                break;
            case requestCodeUsers:
                if (resultCode == RESULT_OK) {
                    String participantId = data.getExtras().getString(getString(R.string.participant_key));
                    if (!participantId.equals(layerClient.getAuthenticatedUserId())) {
                        Intent intent = new Intent(MainActivity.this, MessengerActivity.class);
                        intent.putExtra(getString(R.string.participant_key), participantId);
                        startActivity(intent);
                    }
                }
                break;
        }
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onRefresh() {
        mainLayout.setRefreshing(true);
        dataChange();
        mainLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainLayout.setRefreshing(false);
            }
        }, 1000);
    }
}