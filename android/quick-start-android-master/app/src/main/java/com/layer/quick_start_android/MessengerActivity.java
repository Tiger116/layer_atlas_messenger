package com.layer.quick_start_android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.layer.quick_start_android.LayerApplication.layerClient;

public class MessengerActivity extends ActionBarActivity {
    private ConversationViewController conversationView;
    private MyAutoCompleteTextView usersView;
    private ArrayAdapter<String> myAutoCompleteAdapter;
    private ArrayList<String> availableUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        setTitle(getIntent().getExtras().getString(getString(R.string.title_label)));
        String conversationId = getIntent().getExtras().getString(getString(R.string.conversation_id_key));

        if (conversationView == null) {
            conversationView = new ConversationViewController(this, conversationId);
            if (layerClient != null) {
                layerClient.registerTypingIndicator(conversationView);
            }
        }
        usersView = (MyAutoCompleteTextView) findViewById(R.id.participants_text);
        availableUsers = getAvailableUsers();
        usersView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView c = (TextView) view;
                addUser(c.getText().toString());
                usersView.clearFocus();
                usersView.requestFocus();
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(usersView, InputMethodManager.SHOW_IMPLICIT);
                Log.d("Focus", "" + usersView.isFocused());
            }
        });
        myAutoCompleteAdapter = new ArrayAdapter<>(MessengerActivity.this, android.R.layout.simple_dropdown_item_1line, availableUsers);
        usersView.setTokenizer(new MyAutoCompleteTextView.CommaTokenizer());
        usersView.setAdapter(myAutoCompleteAdapter);
        usersView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> textUsers = new ArrayList<>(Arrays.asList(s.toString().split(", ")));
                if (textUsers.contains(""))
                    textUsers.removeAll(Collections.singleton(""));
                Log.d("Text change", String.format("%d, %d, %d", before, start, count));
                if (count == 0) {
                    if ((MainActivity.getCurrentParticipants().size() - 1) > textUsers.size()) {
                        for (String participant : MainActivity.getCurrentParticipants()) {
                            if (!textUsers.contains(participant) && !participant.equals(layerClient.getAuthenticatedUserId())) {
                                conversationView.getConversation().removeParticipants(Arrays.asList(participant));
                                availableUsers.add(participant);
                                myAutoCompleteAdapter.add(participant);
                                MainActivity.removeParticipant(participant);
                                if (conversationView.getConversation().getMetadata().get(getString(R.string.title_label)) == null)
                                    setTitle(String.format("[%s, %s", layerClient.getAuthenticatedUserId(), textUsers.toString().replace("[", "")));
                            }
                        }
                    }
                }
                usersView.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        for (String userName : MainActivity.getCurrentParticipants()) {
            if (!userName.equals(layerClient.getAuthenticatedUserId())) {
                addBubble(userName);
            }
        }
    }

    private void addUser(String userName) {
        conversationView.getConversation().addParticipants(Arrays.asList(userName));
        availableUsers.remove(userName);
        MainActivity.addParticipant(userName);
        myAutoCompleteAdapter.clear();
        myAutoCompleteAdapter.addAll(availableUsers);
        addBubble(userName);
    }

    private ArrayList<String> getAvailableUsers() {
        ArrayList<String> users = new ArrayList<>();
        List<ParseObject> results = UsersActivity.getParseUsers();
        if (results != null) {
            for (ParseObject obj : results) {
                if (!MainActivity.getCurrentParticipants().contains(obj.getString("username")))
                    users.add(obj.getString("username"));
            }
        }
        return users;
    }

    private void addBubble(String userName) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        TextView tv = createContactTextView(userName);
        BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(tv);

        bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());

        sb.append(userName);
        sb.setSpan(new ImageSpan(bd), 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(", ");

        usersView.setMovementMethod(LinkMovementMethod.getInstance());
        usersView.append(sb);
    }

    public TextView createContactTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.white));
        tv.setTextSize(14);
        tv.setBackgroundResource(R.drawable.btn);
        return tv;
    }

    public Object convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(getResources(), viewBmp);
    }

    //onResume is called on App Start and when the app is brought to the foreground
    protected void onResume() {
        super.onResume();

        //Every time the app is brought to the foreground, register the typing indicator
        if (layerClient != null && conversationView != null)
            layerClient.registerTypingIndicator(conversationView);
    }

    //onPause is called when the app is sent to the background
    protected void onPause() {
        super.onPause();

        //When the app is moved to the background, unregister the typing indicator
        if (layerClient != null && conversationView != null)
            layerClient.unregisterTypingIndicator(conversationView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_messenger, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_add_user:
                Intent intentAdd = new Intent(MessengerActivity.this, UsersActivity.class);
                startActivityForResult(intentAdd, MainActivity.requestCodeUsers);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.requestCodeUsers:
                if (resultCode == RESULT_OK) {
                    String participantName = data.getExtras().getString(getString(R.string.participants));
                    if (!usersView.getText().toString().contains(participantName)&&!usersView.getText().toString().contains(layerClient.getAuthenticatedUserId())) {
                        addUser(participantName);
                    }
                }
                break;
        }
    }
}
