package com.layer.quick_start_android.layer_utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.layer.quick_start_android.LayerApplication;
import com.layer.quick_start_android.activities.MainActivity;
import com.layer.quick_start_android.activities.MessengerActivity;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;

public class MyProgressListener implements LayerProgressListener {

    private Activity activity;
    private ImageView imageView;

    public MyProgressListener(ImageView imageView) {
        this.imageView = imageView;
    }

    public void onProgressStart(MessagePart part, Operation operation) {
        System.out.println("Message part started " + operation.toString());
        this.activity = LayerApplication.getCurrentActivity();
    }

    public void onProgressUpdate(MessagePart part, Operation operation, long bytes) {
        //You can calculate the percentage complete based on the size of the Message Part
        float pctComplete = bytes / part.getSize();

        //Use this to update any GUI elements associated with this Message Part
        System.out.println(operation.toString() + " Percent Complete: " + pctComplete);
    }

    public void onProgressComplete(MessagePart part, Operation operation) {
        if (part.getMimeType().contains("image")) {
            Bitmap bm = BitmapFactory.decodeByteArray(part.getData(), 0, (int) part.getSize());
            imageView.setImageBitmap(bm);
//            if (activity != null)
//                if (activity.getClass().equals(MessengerActivity.class))
//                    ((MessengerActivity) activity).drawConversation();
//                else if (activity.getClass().equals(MainActivity.class))
//                    ((MainActivity) activity).dataChange();
        }
        System.out.println("Message part finished " + operation.toString());
//        LayerApplication.layerClient.unregisterProgressListener(part,this);
    }

    public void onProgressError(MessagePart part, Operation operation, Throwable e) {
        System.out.println("Message part error " + operation.toString());
        System.out.println(e);
    }
}