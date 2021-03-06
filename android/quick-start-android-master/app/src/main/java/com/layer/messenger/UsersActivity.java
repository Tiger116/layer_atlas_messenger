package com.layer.messenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import com.layer.messenger.ui_utils.Contact;
import com.layer.messenger.ui_utils.ContactsAdapter;
import com.layer.messenger.ui_utils.pinned_header_utils.PinnedHeaderListView;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class UsersActivity extends AppCompatActivity {
    private ContactsAdapter adapter;
    private ArrayList<Contact> users;
    private ArrayList<Contact> arr_sort;
    private EditText searchText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        LayerApplication.setCurrentActivity(this);

        final PinnedHeaderListView lvUsers = (PinnedHeaderListView) findViewById(R.id.list);
        users = new ArrayList<>(getUsers());
        Collections.sort(users, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {
                char lhsFirstLetter = TextUtils.isEmpty(lhs.getDisplayName()) ? ' ' : lhs.getDisplayName().charAt(0);
                char rhsFirstLetter = TextUtils.isEmpty(rhs.getDisplayName()) ? ' ' : rhs.getDisplayName().charAt(0);
                int firstLetterComparison = Character.toUpperCase(lhsFirstLetter) - Character.toUpperCase(rhsFirstLetter);
                if (firstLetterComparison == 0)
                    return lhs.getDisplayName().compareTo(rhs.getDisplayName());
                return firstLetterComparison;
            }
        });
        arr_sort = new ArrayList<>(users);
        adapter = new ContactsAdapter(arr_sort, UsersActivity.this);
        lvUsers.setAdapter(adapter);
        lvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setResult(Activity.RESULT_OK, getIntent().putExtra(getString(R.string.participant_key), adapter.getItem(position).getContactId()));
                finish();
            }
        });
        searchText = (EditText) findViewById(R.id.search_text);

        searchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                arr_sort.clear();
                for (Contact user : users) {
                    String name = user.getDisplayName();
                    String search = searchText.getText().toString();
                    if ((search.length() <= name.length()) &&
                            (search.equalsIgnoreCase((String) name.subSequence(0, search.length())))) {
                        Contact contact = new Contact();
                        contact.setContactId(user.getContactId());
                        contact.setDisplayName(name);
//                contact.photoId = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
                        arr_sort.add(contact);
                    }
                }
                adapter.setData(arr_sort);
                lvUsers.setAdapter(adapter);
            }
        });
        LayerApplication.setParseUsers();
    }

    private List<Contact> getUsers() {
        List<Contact> users = new ArrayList<>();
        List<ParseObject> results = LayerApplication.getParseUsers();
        if (results != null) {
            for (ParseObject obj : results) {
                if (!obj.getString(getString(R.string.userName_label)).equals(ParseUser.getCurrentUser().getUsername())) {
                    Contact contact = new Contact();
                    contact.setContactId(obj.getObjectId());
                    String firstName = obj.getString(getString(R.string.firstname_parse_key));
                    String lastName = obj.getString(getString(R.string.lastname_parse_key));
                    String displayName = "";
                    if (firstName != null) {
                        displayName += firstName;
                        if (lastName != null)
                            displayName += String.format(" %s", lastName);
                    } else
                        displayName = obj.getString(getString(R.string.userName_label));
                    contact.setDisplayName(displayName);
//                contact.photoId = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
                    users.add(contact);
                }
            }
        }
        return users;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LayerApplication.setCurrentActivity(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LayerApplication.setCurrentActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (ParseUser.getCurrentUser() != null) {
                    Intent intent = NavUtils.getParentActivityIntent(this);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    NavUtils.navigateUpTo(this, intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
