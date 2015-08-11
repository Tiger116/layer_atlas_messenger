package com.layer.atlas.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.layer.atlas.Atlas;
import com.layer.atlas.AtlasConversationsList;
import com.layer.atlas.AtlasConversationsList.ConversationClickListener;
import com.layer.atlas.AtlasConversationsList.ConversationLongClickListener;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.messaging.Conversation;
import com.parse.ParseUser;

import java.util.UUID;

public class AtlasConversationsScreen extends AppCompatActivity implements DrawerLayout.DrawerListener {
    private static final String TAG = AtlasConversationsScreen.class.getSimpleName();
    private static final boolean debug = true;
    private static final int REQUEST_CODE_LOGIN_SCREEN = 191;
    private static final int REQUEST_CODE_SETTINGS_SCREEN = 192;
    private MessengerApp app;
    private View headView;
    private final LayerConnectionListener connectionListener = new LayerConnectionListener() {
        @Override
        public void onConnectionConnected(LayerClient layerClient) {
            updateStatus();
        }

        @Override
        public void onConnectionDisconnected(LayerClient layerClient) {
            updateStatus();
        }

        @Override
        public void onConnectionError(LayerClient layerClient, LayerException e) {
            updateStatus();
        }
    };
    private AtlasConversationsList conversationsList;
    private ActionBar actionBar;
    private boolean isInitialized = false;
    private boolean forceLogout = false;
    private boolean showSplash = true;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversations);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.hide();
        }
        this.app = (MessengerApp) getApplication();

        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerLayout.setDrawerListener(this);
    }

    private synchronized void initializeViews() {
        if (app.getLayerClient() == null) {
            return;
        }

        if (!isInitialized) {
            this.conversationsList = (AtlasConversationsList) findViewById(R.id.atlas_screen_conversations_conversations_list);
            this.conversationsList.init(app.getLayerClient(), app.getParticipantProvider());
            conversationsList.setClickListener(new ConversationClickListener() {
                public void onItemClick(Conversation conversation) {
                    openChatScreen(conversation, false);
                }
            });
            conversationsList.setLongClickListener(new ConversationLongClickListener() {
                public void onItemLongClick(Conversation conversation) {
                    AtlasConversationSettingsScreen.conv = conversation;
                    Intent settingsIntent = new Intent(AtlasConversationsScreen.this, AtlasConversationSettingsScreen.class);
                    startActivity(settingsIntent);
                }
            });

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView != null) {
                setupDrawerContent(navigationView);
            }

            View btnNewConversation = findViewById(R.id.atlas_conversation_screen_new_conversation);
            btnNewConversation.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), AtlasMessagesScreen.class);
                    intent.putExtra(AtlasMessagesScreen.EXTRA_CONVERSATION_IS_NEW, true);
                    startActivity(intent);
                }
            });

            isInitialized = true;
        }
        app.getLayerClient().registerEventListener(conversationsList);
        updateValues();
    }

    private void updateValues() {
        conversationsList.getAdapter().updateValues();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN_SCREEN) {
            if (resultCode != RESULT_OK) {
                finish(); // no login - no app
            } else {
                this.forceLogout = false;
            }
            return;
        }
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            String qrCodeAppId = IntentIntegrator.parseActivityResult(requestCode, resultCode, data).getContents();
            if (!qrCodeAppId.startsWith("layer:///"))
                qrCodeAppId = "layer:///apps/staging/" + UUID.fromString(qrCodeAppId).toString();
            Log.w(TAG, "Captured App ID: " + qrCodeAppId);
            try {
                app.initLayerClient(qrCodeAppId);
                initializeViews();
            } catch (IllegalArgumentException e) {
                if (debug) Log.w(TAG, "Not a valid Layer QR code app ID: " + qrCodeAppId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (debug) Log.w(TAG, "onResume()");

        // Logging out?
        if (forceLogout) {
            forceLogout = false;
            Intent intent = new Intent(this, AtlasLogInScreen.class);
            startActivityForResult(intent, REQUEST_CODE_LOGIN_SCREEN);
            return;
        }

        // Initialize the LayerClient
        if ((app.getAppId() != null) && app.getLayerClient() == null) {
            app.initLayerClient(app.getAppId());
        }

        // Can we continue in this Activity?
        if ((app.getAppId() != null) && (app.getLayerClient() != null) && app.getLayerClient().isAuthenticated()) {
            findViewById(R.id.atlas_screen_login_splash).setVisibility(View.GONE);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (actionBar != null)
                actionBar.show();
            initializeViews();
            invalidateOptionsMenu();
            app.getLayerClient().registerConnectionListener(connectionListener);
            return;
        }

        // Must route.
        if (showSplash) {
            showSplash = false;
            new Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        public void run() {
                            route();
                        }
                    }, 3000);
        } else {
            findViewById(R.id.atlas_screen_login_splash).setVisibility(View.GONE);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (actionBar != null)
                actionBar.show();
            route();
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        headView = navigationView.inflateHeaderView(R.layout.atlas_nav_drawer_header);

        updateStatus();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.nav_about:
                        Intent intent = new Intent(AtlasConversationsScreen.this, AtlasSettingsScreen.class);
                        mDrawerLayout.closeDrawers();
                        startActivityForResult(intent, REQUEST_CODE_SETTINGS_SCREEN);
                        break;
                    case R.id.nav_log_out:
                        logout();
                        break;
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

    }

    private void updateStatus() {
        if (headView != null) {
            TextView statusText = (TextView) headView.findViewById(R.id.nav_header_status);
            TextView userNameText = (TextView) headView.findViewById(R.id.nav_header_username);
            TextView emailText = (TextView) headView.findViewById(R.id.nav_header_email);
            TextView avatarText = (TextView) headView.findViewById(R.id.nav_header_user_avatar_text);

            LayerClient client = app.getLayerClient();

            if (client != null) {
                statusText.setText((client.isConnected()) ? "Connected" : "Disconnected");
                Atlas.Participant currentUser = app.getParticipantProvider().getParticipant(client.getAuthenticatedUserId());
                if (currentUser != null) {
                    String fullName = Atlas.getFullName(currentUser);
                    userNameText.setText(fullName);

                    String initials = Atlas.getInitials(currentUser);
                    avatarText.setText(initials);

                    String email = currentUser.getEmail();

                    if (email != null && !email.isEmpty()) {
                        emailText.setText(email);
                        emailText.setVisibility(View.VISIBLE);
                    } else
                        emailText.setVisibility(View.GONE);
                }
            }
        }
    }

    private void route() {
        // Initialize a LayerClient with an App ID
        if (app.getAppId() == null) {
            // Launch QR code activity to capture App ID
            IntentIntegrator integrator = new IntentIntegrator(this)
                    .setCaptureActivity(AtlasQRCaptureScreen.class)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
                    .setPrompt(getResources().getString(R.string.atlas_screen_qr_prompt))
                    .setOrientationLocked(true);
            integrator.initiateScan();
            return;
        } else if (app.getLayerClient() == null) {
            // Use provided App ID to initialize new client
            app.initLayerClient(app.getAppId());
        }
        // Optionally launch the login screen
        if ((app.getLayerClient() != null) && (!app.getLayerClient().isAuthenticated() || forceLogout)) {
            forceLogout = false;
            Intent intent = new Intent(this, AtlasLogInScreen.class);
            if (mDrawerLayout != null)
                mDrawerLayout.closeDrawers();
            startActivityForResult(intent, REQUEST_CODE_LOGIN_SCREEN);
            return;
        }
        findViewById(R.id.atlas_screen_login_splash).setVisibility(View.GONE);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (actionBar != null)
            actionBar.show();
        initializeViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (app.getLayerClient() != null) {
            app.getLayerClient()
                    .unregisterEventListener(conversationsList)
                    .unregisterConnectionListener(connectionListener);
        }
    }

    public void openChatScreen(Conversation conv, boolean newConversation) {
        Context context = this;
        Intent intent = new Intent(context, AtlasMessagesScreen.class);
        intent.putExtra(AtlasMessagesScreen.EXTRA_CONVERSATION_URI, conv.getId().toString());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_screen_conversations, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (conversationsList != null && conversationsList.getAdapter() != null)
                    conversationsList.getAdapter().getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQuery("", false);
        searchView.clearFocus();
        searchMenuItem.collapseActionView();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout != null)
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    } else
                        mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (headView!=null)
            updateStatus();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    private void logout() {
        ParseUser.logOut();
        app.getLayerClient().deauthenticate();
        this.forceLogout = true;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        Intent intent = new Intent(this, AtlasLogInScreen.class);
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
        startActivityForResult(intent, REQUEST_CODE_LOGIN_SCREEN);
    }
}