package com.layer.quick_start_android;

import android.net.Uri;
import android.util.Log;

import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;

import java.io.File;
import java.util.List;

import static com.layer.quick_start_android.LayerApplication.getContext;
import static com.layer.quick_start_android.LayerApplication.layerClient;

/**
 * Handles the conversation between the pre-defined participants (Device, Emulator) and displays
 * messages in the GUI.
 */
public class ConversationViewController implements LayerChangeEventListener.MainThread, LayerChangeEventListener.BackgroundThread {

    //Current conversation
    private Conversation activeConversation;

    private String conversationId;

    public ConversationViewController(String parameter) {
        this.conversationId = parameter;
//        this.participant = participant;
        //When conversations/messages change, capture them
        layerClient.registerEventListener(this);

        //Change the layout
//        ma.setContentView(R.layout.activity_messenger);


        //If there is an active conversation between the Device, Simulator, and Dashboard (web client), cache it
        activeConversation = getConversation();

    }

    //Checks to see if there is already a conversation between the device and emulator
    public Conversation getConversation() {

        if (activeConversation == null) {
            if (layerClient.isAuthenticated()) {
                setConversation(conversationId);
            }
        }
        //Returns the active conversation (which is null by default)
        return activeConversation;
    }

    public void setConversation(String parameter) {
        if (parameter != null) {
            this.conversationId = parameter;
            Uri uri = Uri.parse(parameter);
            if (uri.isAbsolute())
                activeConversation = layerClient.getConversation(uri);
            else {
                activeConversation = layerClient.newConversation(parameter);
//                activeConversation.putMetadataAtKeyPath(getContext().getString(R.string.title_label), Arrays.asList(layerClient.getAuthenticatedUserId(), parameter).toString());
            }
        }
    }

    //================================================================================
    // LayerChangeEventListener methods
    //================================================================================
    @Override
    public void onEventMainThread(LayerChangeEvent event) {
        Log.d("Conversation", "Main Thread");

        //You can choose to handle changes to conversations or messages however you'd like:
        List<LayerChange> changes = event.getChanges();
        for (int i = 0; i < changes.size(); i++) {
            LayerChange change = changes.get(i);
            if (change.getObjectType() == LayerObject.Type.CONVERSATION) {

                Conversation conversation = (Conversation) change.getObject();
                Log.d("Conversation", conversation.getId() + " attribute " + change.getAttributeName() + " was changed from " + change.getOldValue() + " to " + change.getNewValue());

                switch (change.getChangeType()) {
                    case INSERT:
                        Log.d("Conversation", "INSERT");
                        break;

                    case UPDATE:
                        Log.d("Conversation", "UPDATE");
                        break;

                    case DELETE:
                        File directory = new File(getContext().getExternalCacheDir() + File.separator + layerClient.getAuthenticatedUserId() + File.separator + activeConversation.getId().getLastPathSegment());
                        if (directory.isDirectory()) {
                            String[] children = directory.list();
                            for (String child : children) {
                                new File(directory, child).delete();
                            }
                        }
                        directory.delete();
                        conversationId = null;
                        activeConversation = null;

                        Log.d("Conversation", "DELETE");
                        break;
                }

            } else if (change.getObjectType() == LayerObject.Type.MESSAGE) {

                Message message = (Message) change.getObject();
                Log.d("Message", message.getId() + " attribute " + change.getAttributeName() + " was changed from " + change.getOldValue() + " to " + change.getNewValue());

                switch (change.getChangeType()) {
                    case INSERT:
                        break;

                    case UPDATE:
                        break;

                    case DELETE:
                        break;
                }
            }
        }
        //If we don't have an active conversation, grab the oldest one
        if (activeConversation == null)
            activeConversation = getConversation();

        LayerApplication.reDrawUI();
    }

    @Override
    public void onEventAsync(LayerChangeEvent layerChangeEvent) {
        Log.d("Conversation", "Async Thread");
    }
}
