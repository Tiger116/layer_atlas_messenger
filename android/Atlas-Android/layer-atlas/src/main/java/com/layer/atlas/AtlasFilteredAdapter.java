package com.layer.atlas;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AtlasFilteredAdapter extends BaseAdapter implements Filterable {
    // date
    private final DateFormat dateFormat;
    private final DateFormat timeFormat;

    ArrayList<Conversation> conversations = new ArrayList<Conversation>();
    ArrayList<Conversation> allConversations = new ArrayList<Conversation>();
    Context context;
    private LayerClient layerClient;
    private Atlas.ParticipantProvider participantProvider;
    //styles
    private int titleTextColor;
    private int titleTextStyle;
    private Typeface titleTextTypeface;
    private int titleUnreadTextColor;
    private int titleUnreadTextStyle;
    private Typeface titleUnreadTextTypeface;
    private int subtitleTextColor;
    private int subtitleTextStyle;
    private Typeface subtitleTextTypeface;
    private int subtitleUnreadTextColor;
    private int subtitleUnreadTextStyle;
    private Typeface subtitleUnreadTextTypeface;
    private int cellBackgroundColor;
    private int cellUnreadBackgroundColor;
    private int dateTextColor;
    private int avatarTextColor;
    private int avatarBackgroundColor;

    public AtlasFilteredAdapter(Context context, LayerClient layerClient, Atlas.ParticipantProvider participantProvider, AttributeSet attrs) {
        this.layerClient = layerClient;
        this.participantProvider = participantProvider;
        parseStyle(context, attrs);
        updateValues();
        this.dateFormat = android.text.format.DateFormat.getDateFormat(context);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        this.context = context;
    }

    public void updateValues() {

        allConversations.clear();                              // always clean, rebuild if authenticated
//        notifyDataSetChanged();

        if (layerClient.isAuthenticated()) {

            List<Conversation> convs = layerClient.getConversations();
            for (Conversation conv : convs) {
                // no participants means we are removed from conversation (disconnected conversation)
                if (conv.getParticipants().size() == 0) continue;
                // only ourselves in participant list is possible to happen, but there is nothing to do with it
                // behave like conversation is disconnected
                if (conv.getParticipants().size() == 1
                        && conv.getParticipants().contains(layerClient.getAuthenticatedUserId()))
                    continue;

                allConversations.add(conv);
            }

            // the bigger .time the highest in the list
            Collections.sort(allConversations, new Comparator<Conversation>() {
                public int compare(Conversation lhs, Conversation rhs) {
                    long leftSentAt = 0;
                    Message leftLastMessage = lhs.getLastMessage();
                    if (leftLastMessage != null && leftLastMessage.getSentAt() != null) {
                        leftSentAt = leftLastMessage.getSentAt().getTime();
                    }
                    long rightSentAt = 0;
                    Message rightLastMessage = rhs.getLastMessage();
                    if (rightLastMessage != null && rightLastMessage.getSentAt() != null) {
                        rightSentAt = rightLastMessage.getSentAt().getTime();
                    }
                    long result = rightSentAt - leftSentAt;
                    if (result == 0L) return 0;
                    return result < 0L ? -1 : 1;
                }
            });
        }
        conversations.clear();
        conversations.addAll(allConversations);
        notifyDataSetChanged();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                notifyDataSetChanged();
//            }
//        }, 1000);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_conversations_list_convert, parent, false);
        }

        Uri convId = conversations.get(position).getId();
        Conversation conv = layerClient.getConversation(convId);

        ArrayList<String> allButMe = new ArrayList<String>(conv.getParticipants());
        allButMe.remove(layerClient.getAuthenticatedUserId());

        TextView textTitle = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_participant);
        String conversationTitle = Atlas.getTitle(conv, participantProvider, layerClient.getAuthenticatedUserId());
        textTitle.setText(conversationTitle);

        // avatar icons...
        TextView textInitials = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single_text);
        View avatarSingle = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_single);
        View avatarMulti = convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi);
        if (allButMe.size() < 2) {
            String conterpartyUserId = allButMe.get(0);
            Atlas.Participant participant = participantProvider.getParticipant(conterpartyUserId);
            textInitials.setText(participant == null ? null : Atlas.getInitials(participant));
            textInitials.setTextColor(avatarTextColor);
            ((GradientDrawable) textInitials.getBackground()).setColor(avatarBackgroundColor);
            avatarSingle.setVisibility(View.VISIBLE);
            avatarMulti.setVisibility(View.GONE);
        } else {
            TextView textInitialsLeft = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_left);
            String leftUserId = allButMe.get(0);
            Atlas.Participant participant = participantProvider.getParticipant(leftUserId);
            textInitialsLeft.setText(participant == null ? null : Atlas.getInitials(participant));
            textInitialsLeft.setTextColor(avatarTextColor);
            ((GradientDrawable) textInitialsLeft.getBackground()).setColor(avatarBackgroundColor);

            TextView textInitialsRight = (TextView) convertView.findViewById(R.id.atlas_view_conversations_list_convert_avatar_multi_right);
            String rightUserId = allButMe.get(1);
            participant = participantProvider.getParticipant(rightUserId);
            textInitialsRight.setText(participant == null ? null : Atlas.getInitials(participant));
            textInitialsRight.setTextColor(avatarTextColor);
            ((GradientDrawable) textInitialsRight.getBackground()).setColor(avatarBackgroundColor);

            avatarSingle.setVisibility(View.GONE);
            avatarMulti.setVisibility(View.VISIBLE);
        }

        TextView textLastMessage = (TextView) convertView.findViewById(R.id.atlas_conversation_view_last_message);
        TextView timeView = (TextView) convertView.findViewById(R.id.atlas_conversation_view_convert_time);
        if (conv.getLastMessage() != null) {
            Message last = conv.getLastMessage();
            String lastMessageText = Atlas.Tools.toString(last);

            textLastMessage.setText(lastMessageText);

            Date sentAt = last.getSentAt();
            if (sentAt == null) timeView.setText("...");
            else timeView.setText(formatTime(sentAt));

            String userId = last.getSender().getUserId();                   // could be null for system messages
            String myId = layerClient.getAuthenticatedUserId();
            if ((userId != null) && !userId.equals(myId) && last.getRecipientStatus(myId) != Message.RecipientStatus.READ) {
                textTitle.setTextColor(titleUnreadTextColor);
                textTitle.setTypeface(titleUnreadTextTypeface, titleUnreadTextStyle);
                textLastMessage.setTypeface(subtitleUnreadTextTypeface, subtitleUnreadTextStyle);
                textLastMessage.setTextColor(subtitleUnreadTextColor);
                convertView.setBackgroundColor(cellUnreadBackgroundColor);
            } else {
                textTitle.setTextColor(titleTextColor);
                textTitle.setTypeface(titleTextTypeface, titleTextStyle);
                textLastMessage.setTypeface(subtitleTextTypeface, subtitleTextStyle);
                textLastMessage.setTextColor(subtitleTextColor);
                convertView.setBackgroundColor(cellBackgroundColor);
            }
        } else {
            textLastMessage.setText("");
            textTitle.setTextColor(titleTextColor);
            textTitle.setTypeface(titleTextTypeface, titleTextStyle);
            textLastMessage.setTypeface(subtitleTextTypeface, subtitleTextStyle);
            textLastMessage.setTextColor(subtitleTextColor);
            convertView.setBackgroundColor(cellBackgroundColor);
        }
        timeView.setTextColor(dateTextColor);
        return convertView;
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return conversations.get(position);
    }

    public int getCount() {
        return conversations.size();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults result = new FilterResults();
                ArrayList<Conversation> tempList = new ArrayList<Conversation>();
                if (constraint == null || constraint.length() == 0) {
                    if (!allConversations.isEmpty())
                        tempList.addAll(allConversations);
//                                    result.values = conversations;
//                                    result.count = conversations.size();
                } else {
                    for (Conversation conv : allConversations) {
                        if (Atlas.getTitle(conv, participantProvider, layerClient.getAuthenticatedUserId()).toLowerCase().contains(constraint.toString().toLowerCase())
                                || Atlas.Tools.toString(conv.getLastMessage()).contains(constraint.toString().toLowerCase())) {
                            tempList.add(conv);
                        }
                    }
                }
                result.values = tempList;
                result.count = tempList.size();
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                conversations.clear();
                conversations.addAll((ArrayList<Conversation>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    private void parseStyle(Context context, AttributeSet attrs) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationList, R.attr.AtlasConversationList, 0);
        this.titleTextColor = ta.getColor(R.styleable.AtlasConversationList_cellTitleTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.titleTextStyle = ta.getInt(R.styleable.AtlasConversationList_cellTitleTextStyle, Typeface.NORMAL);
        String titleTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_cellTitleTextTypeface);
        this.titleTextTypeface = titleTextTypefaceName != null ? Typeface.create(titleTextTypefaceName, titleTextStyle) : null;

        this.titleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationList_cellTitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.titleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationList_cellTitleUnreadTextStyle, Typeface.BOLD);
        String titleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_cellTitleUnreadTextTypeface);
        this.titleUnreadTextTypeface = titleUnreadTextTypefaceName != null ? Typeface.create(titleUnreadTextTypefaceName, titleUnreadTextStyle) : null;

        this.subtitleTextColor = ta.getColor(R.styleable.AtlasConversationList_cellSubtitleTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.subtitleTextStyle = ta.getInt(R.styleable.AtlasConversationList_cellSubtitleTextStyle, Typeface.NORMAL);
        String subtitleTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_cellSubtitleTextTypeface);
        this.subtitleTextTypeface = subtitleTextTypefaceName != null ? Typeface.create(subtitleTextTypefaceName, subtitleTextStyle) : null;

        this.subtitleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationList_cellSubtitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.subtitleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationList_cellSubtitleUnreadTextStyle, Typeface.NORMAL);
        String subtitleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationList_cellSubtitleUnreadTextTypeface);
        this.subtitleUnreadTextTypeface = subtitleUnreadTextTypefaceName != null ? Typeface.create(subtitleUnreadTextTypefaceName, subtitleUnreadTextStyle) : null;

        this.cellBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_cellBackgroundColor, Color.TRANSPARENT);
        this.cellUnreadBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_cellUnreadBackgroundColor, Color.TRANSPARENT);
        this.dateTextColor = ta.getColor(R.styleable.AtlasConversationList_dateTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.avatarTextColor = ta.getColor(R.styleable.AtlasConversationList_avatarTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.avatarBackgroundColor = ta.getColor(R.styleable.AtlasConversationList_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_shape_avatar_gray));
        ta.recycle();
    }

    public String formatTime(Date sentAt) {
        if (sentAt == null) sentAt = new Date();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterMidnight = todayMidnight - (24 * 60 * 60 * 1000); // 24h less

        String timeText = null;
        if (sentAt.getTime() > todayMidnight) {
            timeText = timeFormat.format(sentAt.getTime());
        } else if (sentAt.getTime() > yesterMidnight) {
            timeText = "Yesterday";
        } else {
            timeText = dateFormat.format(sentAt);
        }
        return timeText;
    }

    public Conversation get(int position) {
        return conversations.get(position);
    }
}

