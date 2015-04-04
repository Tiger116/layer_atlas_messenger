package com.layer.quick_start_android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.parse.Parse;

import java.util.UUID;

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

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mCurrentActivity = null;
        // Enable Local Datastore
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "hE41H4TvIuyn1eiPMV8E7mSOFCxAM5sBnhv9b3D8", "XTcDzrh0b2E299VsdeP7YqzuzBkSk0dUIIW2w6Gx");

        UUID appID = UUID.fromString(Layer_App_ID);
        layerClient = LayerClient.newInstance(this, appID, GCM_Project_Number);
        LayerClient.setLogLevel(LayerClient.LogLevel.DETAILED);

        conversationView = new ConversationViewController(null);
    }

    public static Activity getCurrentActivity(){
        return mCurrentActivity;
    }

    public static void setCurrentActivity(Activity currentActivity){
        mCurrentActivity = currentActivity;
    }

    public static Context getContext() {
        return mContext;
    }
}
