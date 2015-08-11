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
package com.layer.atlas.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.Atlas;
import com.layer.atlas.Atlas.FilteringComparator;
import com.layer.atlas.messenger.provider.Participant;
import com.layer.atlas.messenger.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

import java.util.Arrays;
import java.util.HashSet;

public class AtlasConversationSettingsScreen extends AppCompatActivity {
    private static final String TAG = AtlasConversationSettingsScreen.class.getSimpleName();
    private static final boolean debug = true;

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 999;

    public static Conversation conv;
    private ParticipantProvider participantProvider;
    private String userId;
    private ViewGroup namesList;
    private View btnLeaveGroup;
    private View btnDelete;
    private EditText textGroupName;
    private OnClickListener contactClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Participant participant = (Participant) v.getTag();
            int size = conv.getParticipants().size();
            if (conv.getParticipants().size() > 2) {
                Toast.makeText(v.getContext(), "Removing " + Atlas.getFullName(participant), Toast.LENGTH_SHORT).show();
                conv.removeParticipants(participant.getId());
                updateValues();
            } else
                Toast.makeText(v.getContext(), Atlas.getFullName(participant) + " not removed, because it's last participant", Toast.LENGTH_SHORT).show();
        }
    };

    private OnClickListener leaveClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), String.format("Leave from  \"%s\" conversation", Atlas.getTitle(conv, participantProvider, userId)), Toast.LENGTH_LONG).show();
            conv.removeParticipants(userId);
            conv.removeParticipants(userId);
            finish();
        }
    };

    private OnClickListener deleteClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), String.format("Delete \"%s\" conversation", Atlas.getTitle(conv, participantProvider, userId)), Toast.LENGTH_LONG).show();
            conv.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_conversation_settings);

        participantProvider = ((MessengerApp) getApplication()).getParticipantProvider();
        userId = ((MessengerApp) getApplication()).getLayerClient().getAuthenticatedUserId();

        btnDelete = findViewById(R.id.atlas_screen_conversation_settings_delete);
        btnDelete.setOnClickListener(deleteClickListener);

        btnLeaveGroup = findViewById(R.id.atlas_screen_conversation_settings_leave_group);
        btnLeaveGroup.setOnClickListener(leaveClickListener);

        textGroupName = (EditText) findViewById(R.id.atlas_screen_conversation_settings_groupname_text);

        View btnAddParticipant = findViewById(R.id.atlas_screen_conversation_settings_add_participant);
        btnAddParticipant.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(AtlasConversationSettingsScreen.this, AtlasParticipantPickersScreen.class);
                final String[] skipUserIds = conv.getParticipants().toArray(new String[0]);
                intent.putExtra(AtlasParticipantPickersScreen.EXTRA_KEY_USERIDS_SKIP, skipUserIds);
                startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
            }
        });

        this.namesList = (ViewGroup) findViewById(R.id.atlas_screen_conversation_settings_participants_list);

    }

    private void updateValues() {

        MessengerApp app101 = (MessengerApp) getApplication();
        String conversationTitle;
        if (conv != null) {
            conversationTitle = Atlas.getTitle(conv);
            if (conversationTitle != null && !conversationTitle.isEmpty()) {
                setTitle(String.format("\"%s\" settings", conversationTitle));
                textGroupName.setText(conversationTitle.trim());
                textGroupName.clearFocus();
            }
        }

        // refresh names screen
        namesList.removeAllViews();

        HashSet<String> participantSet = new HashSet<String>(conv.getParticipants());
        participantSet.remove(app101.getLayerClient().getAuthenticatedUserId());
        Atlas.Participant[] participants = new Atlas.Participant[participantSet.size()];
//        if (participants.length == 0) {
//        }
        int i = 0;
        for (String userId : participantSet) {
            Participant participant = app101.getParticipantProvider().get(userId);
            participants[i++] = participant;
        }
        Arrays.sort(participants, new FilteringComparator(""));

        for (int iContact = 0; iContact < participants.length; iContact++) {
            View convert = getLayoutInflater().inflate(R.layout.atlas_screen_conversation_settings_participant_convert, namesList, false);

            TextView avaText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_ava);
            avaText.setText(Atlas.getInitials(participants[iContact]));
            TextView nameText = (TextView) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_name);
            nameText.setText(Atlas.getFullName(participants[iContact]));

            ImageButton removeButton = (ImageButton) convert.findViewById(R.id.atlas_screen_conversation_settings_convert_remove_button);

            removeButton.setTag(participants[iContact]);
            removeButton.setOnClickListener(contactClickListener);

            namesList.addView(convert);
        }
        namesList.setVisibility(View.VISIBLE);

        if (participantSet.size() == 1) { // one-on-one
            btnLeaveGroup.setVisibility(View.GONE);
        } else {                        // multi
            btnLeaveGroup.setVisibility(View.VISIBLE);
        }

        if (conv.isDeleted()) {
            btnDelete.setVisibility(View.GONE);
            btnLeaveGroup.setVisibility(View.GONE);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT && resultCode == RESULT_OK) {
            String[] addedParticipants = data.getStringArrayExtra(AtlasParticipantPickersScreen.EXTRA_KEY_USERIDS_SELECTED);
            conv.addParticipants(addedParticipants);
            updateValues();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateValues();
    }

    protected void onPause() {
        super.onPause();
        if (!conv.isDeleted())
            Atlas.setTitle(conv, textGroupName.getText().toString().trim());
    }

    @Override
    protected void onDestroy() {
        HashSet<String> participantSet = new HashSet<String>(conv.getParticipants());
        if (participantSet.size() < 2) {
            Toast.makeText(this, "Delete conversation without participants", Toast.LENGTH_LONG).show();
            conv.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
            finish();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }
}
