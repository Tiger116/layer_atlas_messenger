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
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.CompoundPredicate;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;

import java.util.List;

import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MyArrayAdapter extends ArrayAdapter {

    private Context context;
    private List<Conversation> conversations;
//    private ArrayList<String> conversNames;
//    private boolean observerRegistered;
//    private DataSetObserver observer;


    public MyArrayAdapter(Context context, List<Conversation> conversations) {//, ArrayList<String> names) {
        super(context, R.layout.conversations_item, conversations);
        this.conversations = conversations;
//        this.conversNames = names;
        this.context = context;
    }

    @Override
    public int getCount() {
        return conversations.size();
    }

    @Override
    public Object getItem(int position) {
        return conversations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return conversations.get(position).hashCode();
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
            Conversation conversation = conversations.get(position);
            if (conversation != null) {
                String title = "";
                if (conversation.getMetadata() != null)
                    if (conversation.getMetadata().get(context.getString(R.string.title_label)) != null)
                        title = conversation.getMetadata().get(context.getString(R.string.title_label)).toString();
                    else
                        title = conversation.getParticipants().toString();
                holder.conversationName.setText(title);

                Query query = Query.builder(Message.class)
                        .predicate(new CompoundPredicate(CompoundPredicate.Type.AND,
                                new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation),
                                new Predicate(Message.Property.IS_UNREAD, Predicate.Operator.EQUAL_TO, true)))
                        .sortDescriptor(new SortDescriptor(Message.Property.SENT_AT, SortDescriptor.Order.DESCENDING))
                        .build();
                List<Long> resultArray = layerClient.executeQuery(query, Query.ResultType.COUNT);
                int count = resultArray.get(0).intValue();
                if (count > 0)
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

    class ViewHolder {
        TextView conversationName;
        ImageView newMessageIcon;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
//        Log.d(MyArrayAdapter.class.toString(), "observer registered");
//        if (observer != null) {
//            this.observer = observer;
//            observerRegistered = true;
//        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
//        Log.d(MyArrayAdapter.class.toString(), "observer unregistered");
//        if (observer != null) {
//            if (observerRegistered)
//            observerRegistered = false;
//        }
    }
}
