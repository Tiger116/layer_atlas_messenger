/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;

/**
 * @author Oleg Orlov
 * @since 14 May 2015
 */
public class AtlasConversationsList extends FrameLayout implements LayerChangeEventListener.MainThread {

    private static final String TAG = AtlasConversationsList.class.getSimpleName();
    private static final boolean debug = false;

    private ListView conversationsList;
    private AtlasFilteredAdapter conversationsAdapter;
    private ConversationClickListener clickListener;
    private ConversationLongClickListener longClickListener;

    private AttributeSet attrs;
    private Context context;

    public AtlasConversationsList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.attrs = attrs;
        this.context = context;
    }

    public AtlasConversationsList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasConversationsList(Context context) {
        super(context);
        this.context = context;
    }

    public void init(final LayerClient layerClient, final Atlas.ParticipantProvider participantProvider) {
        if (layerClient == null) throw new IllegalArgumentException("LayerClient cannot be null");
        if (participantProvider == null)
            throw new IllegalArgumentException("ParticipantProvider cannot be null");

        // inflate children:
        LayoutInflater.from(getContext()).inflate(R.layout.atlas_conversations_list, this);

        this.conversationsList = (ListView) findViewById(R.id.atlas_conversations_view);
        conversationsAdapter = new AtlasFilteredAdapter(context, layerClient, participantProvider, attrs);
        this.conversationsList.setAdapter(conversationsAdapter);

        conversationsList.setOnItemClickListener(new

                                                         OnItemClickListener() {
                                                             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                                 Conversation conv = conversationsAdapter.get(position);
                                                                 if (clickListener != null)
                                                                     clickListener.onItemClick(conv);
                                                             }
                                                         }

        );
        conversationsList.setOnItemLongClickListener(new OnItemLongClickListener() {
                                                         public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                                                                        long id) {
                                                             Conversation conv = conversationsAdapter.get(position);
                                                             if (longClickListener != null)
                                                                 longClickListener.onItemLongClick(conv);
                                                             return true;
                                                         }
                                                     }

        );

        // clean everything if deathenticated (client will explode on .getConversation())
        // and rebuilt everything back after successful authentication
        layerClient.registerAuthenticationListener(new

                                                           LayerAuthenticationListener() {
                                                               public void onDeauthenticated(LayerClient client) {
                                                                   if (debug)
                                                                       Log.w(TAG, "onDeauthenticated() ");
                                                                   conversationsAdapter.updateValues();
                                                               }

                                                               public void onAuthenticated(LayerClient client, String userId) {
                                                                   conversationsAdapter.updateValues();
                                                               }

                                                               public void onAuthenticationError(LayerClient client, LayerException exception) {
                                                               }

                                                               public void onAuthenticationChallenge(LayerClient client, String nonce) {
                                                               }
                                                           });

        applyStyle();
        conversationsAdapter.updateValues();
    }

    private void applyStyle() {
        conversationsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        for (LayerChange change : event.getChanges()) {
            if (change.getObjectType() == LayerObject.Type.CONVERSATION
                    || change.getObjectType() == LayerObject.Type.MESSAGE) {
                conversationsAdapter.updateValues();
                return;
            }
        }
    }

    public ConversationClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(ConversationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public ConversationLongClickListener getLongClickListener() {
        return longClickListener;
    }

    public void setLongClickListener(ConversationLongClickListener conversationLongClickListener) {
        this.longClickListener = conversationLongClickListener;
    }

    public AtlasFilteredAdapter getAdapter() {
        return conversationsAdapter;
    }


    public interface ConversationClickListener {
        void onItemClick(Conversation conversation);
    }

    public interface ConversationLongClickListener {
        void onItemLongClick(Conversation conversation);
    }
}
