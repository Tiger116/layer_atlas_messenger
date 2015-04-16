package com.layer.quick_start_android.layer_utils;

import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;

import static com.layer.quick_start_android.LayerApplication.reDrawUI;

public class MyProgressListener implements LayerProgressListener {

    public void onProgressStart(MessagePart part, Operation operation) {
        System.out.println("Message part started " + operation.toString());
    }

    public void onProgressUpdate(MessagePart part, Operation operation, long bytes) {
        //You can calculate the percentage complete based on the size of the Message Part
        float pctComplete = bytes / part.getSize();

        //Use this to update any GUI elements associated with this Message Part
        System.out.println(operation.toString() + " Percent Complete: " + pctComplete);
    }

    public void onProgressComplete(final MessagePart part, Operation operation) {
        reDrawUI();

        System.out.println("Message part finished " + operation.toString() + " " + part.getMimeType());
    }

    public void onProgressError(MessagePart part, Operation operation, Throwable e) {
        System.out.println("Message part error " + operation.toString());
        System.out.println(e);
    }
}