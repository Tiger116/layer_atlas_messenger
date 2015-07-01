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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.layer.sdk.LayerClient;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * @author Oleg Orlov
 * @since 24 Apr 2015
 */
public class AtlasLogInScreen extends Activity {
    private static final String TAG = AtlasLogInScreen.class.getSimpleName();
    private static final boolean debug = true;
    private static final int REGISTRATION_CODE_REQUEST = 1;

    private volatile boolean inProgress = false;
    private EditText loginText;
    private EditText passwordText;
    private Button registrationButton;
    private ProgressDialog dialog;
    TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                login();
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_login);

        loginText = (EditText) findViewById(R.id.atlas_screen_login_username);
        passwordText = (EditText) findViewById(R.id.atlas_screen_login_password);

        loginText.setOnEditorActionListener(actionListener);
        passwordText.setOnEditorActionListener(actionListener);

        loginText.requestFocus();

        registrationButton = (Button) findViewById(R.id.atlas_screen_login_button);
        registrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registrationIntent = new Intent(AtlasLogInScreen.this, AtlasRegistrationScreen.class);
                startActivityForResult(registrationIntent, REGISTRATION_CODE_REQUEST);
            }
        });
    }

    private void updateValues() {
//        if (dialog!= null)
//            dialog.dismiss();
        // TODO: Do something with inProgress
    }

    private void login() {
        final String userName = loginText.getText().toString().trim();
        final String password = passwordText.getText().toString().trim();
        if (password.isEmpty())
            passwordText.requestFocus();
        else if (userName.isEmpty())
            loginText.requestFocus();
        else {
            setInProgress(true);

            dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle("Loading");
            dialog.setMessage("Please wait...");
            dialog.setIndeterminate(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    ParseUser.logOut();
                }
            });
            dialog.show();

            ParseUser.logInInBackground(userName, password, new LogInCallback() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    if (e == null) {
                        final MessengerApp app = (MessengerApp) getApplication();
                        final LayerClient layerClient = app.getLayerClient();
                        layerClient.registerAuthenticationListener(new MyAuthenticationListener(AtlasLogInScreen.this));
                        layerClient.authenticate();
                        updateValues();
                    } else {
                        setInProgress(false);
                        loginText.setText("");
                        passwordText.setText("");
                        loginText.requestFocus();
                        loginText.setError(e.getLocalizedMessage());
                        if (dialog != null && dialog.isShowing())
                            dialog.cancel();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null)
            dialog.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTRATION_CODE_REQUEST:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
        }
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        if (!inProgress && this.dialog != null) {
            dialog.dismiss();
        }
    }
}
