package com.layer.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;

import com.layer.messenger.layer_utils.ConversationViewController;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.ubertesters.common.models.LockingMode;
import com.ubertesters.sdk.Ubertesters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LayerApplication extends Application implements LayerChangeEventListener {

    //Replace this with your App ID from the Layer Developer page.
    //Go http://developer.layer.com, click on "Dashboard" and select "Info"
    public static String Layer_App_ID = "07b40518-aaaa-11e4-bceb-a25d000000f4";

    //Replace this with your Project Number from http://console.developers.google.com
    public static String GCM_Project_Number = "520065212067";

    //Global variables used to manage the Layer Client and the conversations in this app
    public static LayerClient layerClient;
    public static ConversationViewController conversationView;
    private static Context mContext;
    private static Activity mCurrentActivity;
    private static List<ParseObject> parseUsers;
    private static Uri imageURI;

    public static Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mCurrentActivity = null;

        // Enable Local Datastore
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "hE41H4TvIuyn1eiPMV8E7mSOFCxAM5sBnhv9b3D8", "XTcDzrh0b2E299VsdeP7YqzuzBkSk0dUIIW2w6Gx");

        Ubertesters.initialize(this);
        Ubertesters.logger().info("logs");

//        UUID appID = UUID.fromString(Layer_App_ID);
        LayerClient.Options options = new LayerClient.Options();
        options.googleCloudMessagingSenderId(GCM_Project_Number);
        layerClient = LayerClient.newInstance(this, Layer_App_ID, options);
        LayerClient.enableLogging();
        layerClient.setAutoDownloadMimeTypes(Arrays.asList("image/jpeg+preview"));//, "image/jpeg"
        parseUsers = new ArrayList<>();
        setParseUsersSync();
//        setParseUsers();
        conversationView = new ConversationViewController(null);
    }

    public static void setCurrentActivity(Activity currentActivity) {
        mCurrentActivity = currentActivity;
    }

    public static Context getContext() {
        return mContext;
    }

    public static List<ParseObject> getParseUsers() {
        if (parseUsers == null)
            setParseUsersSync();
        return parseUsers;
    }

    public static void setParseUsers() {
//        parseUsers = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("_User");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    parseUsers.clear();
                    parseUsers.addAll(parseObjects);
                } else
                    e.printStackTrace();
            }
        });
    }

    private static void setParseUsersSync() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("_User");
        try {
            parseUsers = query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static String getUserNameById(String userId) {
        for (ParseObject object : parseUsers) {
            if (object.getObjectId().equals(userId)) {
                return ((ParseUser) object).getUsername();
            }
        }
        return null;
    }

    public static String getUserIdByName(String userName) {
        for (ParseObject object : parseUsers) {
            if (((ParseUser) object).getUsername().equals(userName)) {
                return object.getObjectId();
            }
        }
        return null;
    }

    public static Uri getImageURI() {
        return imageURI;
    }

    public static void setImageURI(Uri URI) {
        imageURI = URI;
    }
}
