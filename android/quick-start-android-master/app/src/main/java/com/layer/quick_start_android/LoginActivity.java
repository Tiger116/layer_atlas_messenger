package com.layer.quick_start_android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

public class LoginActivity extends ActionBarActivity {
    private EditText userNameText;
    private EditText passwordText;
    private final int requestRegistration = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LayerApplication.setCurrentActivity(this);

        userNameText = (EditText) findViewById(R.id.userName);
        passwordText = (EditText) findViewById(R.id.password);
        Button button = (Button) findViewById(R.id.btnSingIn);
        button.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            verify(userNameText.getText().toString(), passwordText.getText().toString());
        }
    };

    public void verify(final String username, final String password) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {

                    userNameText.setText("");
                    passwordText.setText("");
                    userNameText.requestFocus();
                    userNameText.setError("Wrong user name or password!");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
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
            case R.id.registration_menu:
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(intent, requestRegistration);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case requestRegistration:
                if (resultCode == RESULT_OK) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
                break;
        }
    }
}