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
import android.os.Bundle;
import android.text.InputType;
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
    private MessengerApp app;
    private volatile boolean inProgress = false;
    private EditText loginText;
    private EditText passwordText;
    private EditText confirmPasswordText;
    private EditText firstNameText;
    private EditText lastNameText;
    private EditText emailText;
    private LinearLayout expandLayout;
    private Button moreButton;
    private MyDialogFragment dialog;
    TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (isValidLogin() && isValidEmail() && isValidPassword())
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
        emailText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

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

        dialog = new MyDialogFragment();

        app = (MessengerApp) getApplication();
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
                setInProgress(false);
                if (e == null) {
                    Log.d(this.toString(), String.format("create new user %s", login));

                    final MessengerApp app = (MessengerApp) getApplication();
                    final LayerClient layerClient = app.getLayerClient();
                    layerClient.registerAuthenticationListener(new MyAuthenticationListener(AtlasRegistrationScreen.this));

                    layerClient.authenticate();
                    updateValues();
                } else {
                    Log.d("Error Registration", e.toString());
                    if (e.getCode() == ParseException.USERNAME_TAKEN)
                        setError("Login Already used");
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
            setError("Password must be at least 4 characters in length or more!");
        } else if (!confirmPassword.equals(password)) {
            setError("Password and its confirm does not match!");
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

    private boolean isValidLogin() {
        String login = loginText.getText().toString();
        if (login.isEmpty()) {
            setError("Login cannot be empty!");
        } else if (login.matches(".*[^a-zA-Z0-9_-].*"))     //matches letters, numbers and _ or -
            setError("Login allow only letters and numbers with \"-\" and \"_\" !");
        else
            return true;
        return false;
    }

    private void setError(String error) {
        passwordText.setText("");
        confirmPasswordText.setText("");
        loginText.setText("");
        loginText.setError(error);
        loginText.requestFocus();
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        if (this.dialog != null) {
            if (inProgress)
                dialog.show(getFragmentManager(), TAG);
            else
                dialog.dismiss();
        }
    }
}
