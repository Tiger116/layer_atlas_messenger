package com.layer.atlas.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;

public class MyAuthenticationListener implements LayerAuthenticationListener {
    private final Context context;

    public MyAuthenticationListener(Context context) {
        this.context = context;
    }

    public void onAuthenticationChallenge(final LayerClient client,
                                          final String nonce) {
        Log.w(this.toString(), "onAuthenticationChallenge() nonce: " + nonce);
        new Thread(new Runnable() {
            public void run() {
                ParseUser user = ParseUser.getCurrentUser();

                String userID ="";
                if (user != null)
                    userID = user.getObjectId();

//                userID = "MQSYNxGvDn";

                HashMap<String, Object> params = new HashMap<>();
                params.put("userID", userID);
                params.put("nonce", nonce);

                ParseCloud.callFunctionInBackground("generateToken", params, new FunctionCallback<String>() {
                    @Override
                    public void done(String o, final ParseException e) {
                        if (e == null) {
                            client.answerAuthenticationChallenge(o);
                        } else {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    public void onAuthenticated(LayerClient client, String userId) {
        Log.w(this.toString(), "onAuthenticated() userID: " + userId);
        client.unregisterAuthenticationListener(this);
        ((Activity) context).setResult(Activity.RESULT_OK);
        ((Activity) context).finish();
    }

    public void onDeauthenticated(LayerClient client) {
        Log.e(this.toString(), "onDeauthenticated() ");
        Intent intent = new Intent(context, AtlasLogInScreen.class);
        context.startActivity(intent);
    }

    public void onAuthenticationError(LayerClient client, final LayerException exception) {
        Log.e(this.toString(), "onAuthenticationError() ", exception);
        client.unregisterAuthenticationListener(this);
        exception.printStackTrace();
    }
}
