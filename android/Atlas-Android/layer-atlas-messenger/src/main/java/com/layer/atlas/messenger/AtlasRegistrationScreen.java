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

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.sdk.LayerClient;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class AtlasRegistrationScreen extends Activity {
    private static final String TAG = AtlasRegistrationScreen.class.getSimpleName();
    private static final boolean debug = true;

    private volatile boolean inProgress = false;
    private EditText loginText;
    private EditText passwordText;
    private EditText confirmPasswordText;
    private EditText firstNameText;
    private EditText lastNameText;
    private EditText emailText;
    private LinearLayout expandLayout;
    private Button moreButton;
    private ProgressDialog dialog;
    TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (loginText.getText().toString().isEmpty()) {
                    loginText.setError("Login cannot be empty!");
                    loginText.requestFocus();
                } else if (isValidEmail() && isValidPassword())
                    registration();
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_registration);

        loginText = (EditText) findViewById(R.id.atlas_screen_registration_username);
        passwordText = (EditText) findViewById(R.id.atlas_screen_registration_password);
        confirmPasswordText = (EditText) findViewById(R.id.atlas_screen_registration_password_confirm);
        firstNameText = (EditText) findViewById(R.id.atlas_screen_registration_firstname);
        lastNameText = (EditText) findViewById(R.id.atlas_screen_registration_lastname);
        emailText = (EditText) findViewById(R.id.atlas_screen_registration_email);
        expandLayout = (LinearLayout) findViewById(R.id.atlas_screen_registration_expanded_layout);

        confirmPasswordText.setOnEditorActionListener(actionListener);
        emailText.setOnEditorActionListener(actionListener);

        loginText.requestFocus();

        moreButton = (Button) findViewById(R.id.atlas_screen_registration_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandLayout.getVisibility() == VISIBLE) {
                    expandLayout.setVisibility(GONE);
                    moreButton.setText("More...");
                } else {
                    moreButton.setText("Hide");
                    expandLayout.setVisibility(VISIBLE);
                }
            }
        });
    }

    private void updateValues() {
        // TODO: Do something with inProgress
    }

    private void registration() {
        final String login = loginText.getText().toString().trim();
        final String password = passwordText.getText().toString().trim();
        final String confirmPassword = confirmPasswordText.getText().toString().trim();
        String firstName = firstNameText.getText().toString().trim();
        String lastName = lastNameText.getText().toString().trim();
        String email = emailText.getText().toString().trim();
        if (login.isEmpty() || password.isEmpty() || confirmPassword.isEmpty())
            return;
        setInProgress(true);

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        ParseUser newUser = new ParseUser();

        newUser.setUsername(login);
        newUser.setPassword(password);

        if (firstName.isEmpty())
            firstName = login;
        newUser.put(getString(R.string.parse_firstname_key), firstName);

        if (!lastName.isEmpty())
            newUser.put(getString(R.string.parse_lastname_key), lastName);

        if (!email.isEmpty())
            newUser.put(getString(R.string.parse_email_key), email);

        newUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (dialog != null)
                    dialog.dismiss();
                if (e == null) {
                    Log.d(this.toString(), String.format("create new user %s", login));

                    final MessengerApp app = (MessengerApp) getApplication();
                    final LayerClient layerClient = app.getLayerClient();
                    layerClient.registerAuthenticationListener(new MyAuthenticationListener(AtlasRegistrationScreen.this));

                    layerClient.authenticate();
                    updateValues();
                } else {
                    Log.d("Error Registration", e.toString());
                    setInProgress(false);
                    loginText.setText("");
                    passwordText.setText("");
                    confirmPasswordText.setText("");
                    loginText.requestFocus();
                    if (e.getCode() == ParseException.USERNAME_TAKEN)
                        loginText.setError("Login Already used");
                    else
                        Toast.makeText(AtlasRegistrationScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private boolean isValidPassword() {
        String confirmPassword = confirmPasswordText.getText().toString();
        String password = passwordText.getText().toString();
        if (password.length() < 4) {
            passwordText.setText("");
            confirmPasswordText.setText("");
            passwordText.setError("Password must be at least 4 characters in length or more!");
            passwordText.requestFocus();
        } else if (!confirmPassword.equals(password)) {
            passwordText.setText("");
            confirmPasswordText.setText("");
            passwordText.setError("Password and its confirm does not match!");
            passwordText.requestFocus();
        } else
            return true;
        return false;
    }

    public boolean isValidEmail() {
        CharSequence email = emailText.getText();
        if (TextUtils.isEmpty(email)) {
            return true;
        } else {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailText.setText("");
                passwordText.setText("");
                confirmPasswordText.setText("");
                emailText.requestFocus();
                emailText.setError("Invalid email address!");
                return false;
            } else
                return true;
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null)
            dialog.dismiss();
        super.onDestroy();
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        if (!inProgress && this.dialog != null) {
            dialog.dismiss();
        }
    }
}
