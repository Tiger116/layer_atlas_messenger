package com.layer.quick_start_android.layer_utils;

import android.util.Log;

import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerSyncListener;

import java.util.List;

public class MySyncListener implements LayerSyncListener {
    @Override
    public void onBeforeSync(LayerClient layerClient) {

    }

    @Override
    public void onAfterSync(LayerClient layerClient) {
        Log.d(this.toString(), "After Sync callback catch");
//        LayerApplication.reDrawUI();
    }

    @Override
    public void onSyncError(LayerClient layerClient, List<LayerException> layerExceptions) {

    }
}
