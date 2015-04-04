package com.layer.quick_start_android;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.layer.sdk.messaging.Conversation;

import java.util.ArrayList;
import java.util.List;

import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MyArrayAdapter extends ArrayAdapter {

    private Context context;
    private ArrayList<String> conversNames;
    private List<Conversation> conversations;
    private boolean observerRegistered;
    private DataSetObserver observer;


    public MyArrayAdapter(Context context, ArrayList<String> names, List<Conversation> conversations) {
        super(context, R.layout.conversations_item, names);
        this.conversations = conversations;
        this.conversNames = names;
        this.context = context;
    }

    @Override
    public int getCount() {
        return conversNames.size();
    }

    @Override
    public Object getItem(int position) {
        return conversNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return conversNames.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.conversations_item, parent, false);
            holder = new ViewHolder();
            holder.conversationName = (TextView) view.findViewById(R.id.conversationName);
            holder.newMessageIcon = (ImageView) view.findViewById(R.id.new_message_icon);
            holder.conversationName.setText(conversNames.get(position));
            Conversation conversation = conversations.get(position);
            if (conversation != null) {
                if (layerClient.getUnreadMessageCount(conversation) > 0)
                    holder.newMessageIcon.setVisibility(View.VISIBLE);
                else
                    holder.newMessageIcon.setVisibility(View.GONE);
            }
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
            view.setTag(holder);
        }
        return view;
    }

    public void setList(List<Conversation> data) {
//        this.conversNames = data;

        this.conversations.clear();
        this.conversations.addAll(data);

//        notifyDataSetChanged();
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                notifyDataSetChanged();
//            }
//        });
    }

    class ViewHolder {
        TextView conversationName;
        ImageView newMessageIcon;
    }

    @Override

    public void registerDataSetObserver(DataSetObserver observer) {
//        if (observer != null) {
//            this.observer = observer;
//            observerRegistered = true;
        super.registerDataSetObserver(observer);
        Log.d(MyArrayAdapter.class.toString(), "observer registered");
//        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
//        if (observer != null) {
//            if (observerRegistered)
        super.unregisterDataSetObserver(observer);

//            observerRegistered = false;
        Log.d(MyArrayAdapter.class.toString(), "observer unregistered");
//        }
    }
}
