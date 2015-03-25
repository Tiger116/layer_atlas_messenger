package com.layer.quick_start_android;

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
    public static String GCM_Project_Number = "00000";

    //Global variables used to manage the Layer Client and the conversations in this app
    public static LayerClient layerClient;

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        // Enable Local Datastore
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "hE41H4TvIuyn1eiPMV8E7mSOFCxAM5sBnhv9b3D8", "XTcDzrh0b2E299VsdeP7YqzuzBkSk0dUIIW2w6Gx");

        UUID appID = UUID.fromString(Layer_App_ID);
        layerClient = LayerClient.newInstance(this, appID, GCM_Project_Number);
        LayerClient.setLogLevel(LayerClient.LogLevel.DETAILED);
    }
    public static Context getContext(){
        return mContext;
    }
}
