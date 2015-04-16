package com.layer.quick_start_android.layer_utils;

import android.util.Log;

import com.layer.quick_start_android.activities.MainActivity;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;


public class MyAuthenticationListener implements LayerAuthenticationListener {

    private MainActivity main_activity;

    public MyAuthenticationListener(MainActivity ma) {
        main_activity = ma;
    }

    //Called after layerClient.authenticate() executes
    //You will need to set up an Authentication Service to take a Layer App ID, User ID, and the
    //nonce to create a Identity Token to pass back to Layer
    public void onAuthenticationChallenge(final LayerClient client, final String nonce) {

        ParseUser user = ParseUser.getCurrentUser();

        String userID = null;
        if (user != null)
            userID = user.getObjectId();

        //Note: This Layer Authentication Service is for TESTING PURPOSES ONLY
        //When going into production, you will need to create your own web service
        //Check out https://developer.layer.com/docs/guides#authentication for guidance
        HashMap<String, Object> params = new HashMap<>();
        params.put("userID", userID);
        params.put("nonce", nonce);

        ParseCloud.callFunctionInBackground("generateToken", params, new FunctionCallback<String>() {
            @Override
            public void done(String o, ParseException e) {
                if (e == null) {
                    client.answerAuthenticationChallenge(o);
                } else {
                    Log.d(this.toString(), "Parse Cloud function failed to be called to generate token with error: " + e.getMessage());
                }
            }
        });
    }

    //Called when the user has successfully authenticated
    public void onAuthenticated(LayerClient client, String userId) {
        //Start the conversation view after a successful authentication
        System.out.println("Authentication successful");
        if (main_activity != null) {
            main_activity.dataChange();
            main_activity.setLayerDownloadParams();
        }
    }

    //Called when there was a problem authenticating
    //Common causes include a malformed identity token, missing parameters in the identity token, missing
    //or incorrect nonce
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {
        System.out.println("There was an error authenticating: " + e);
    }

    //Called after the user has been deauthenticated
    public void onDeauthenticated(LayerClient client) {
        System.out.println("User is deauthenticated");
    }
}