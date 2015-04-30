package com.layer.messenger.contacts_utils;

import android.graphics.Bitmap;
import android.widget.TextView;

import com.layer.messenger.contacts_utils.async_task_thread_pool.AsyncTaskEx;

public class ViewHolder {
    public CircularContactView friendProfileCircularContactView;
    TextView friendName, headerView;
    public AsyncTaskEx<Void, Void, Bitmap> updateTask;
}