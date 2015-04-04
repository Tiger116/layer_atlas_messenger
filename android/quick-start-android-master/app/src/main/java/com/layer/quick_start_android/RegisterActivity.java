package com.layer.quick_start_android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.List;

import static android.view.View.*;


public class RegisterActivity extends ActionBarActivity {

    private TextView nameTextView;
    private TextView passwordTextView;
    private TextView confirmPasswordTextView;
    private TextView emailTextView;
    private Button button;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        LayerApplication.setCurrentActivity(this);

        nameTextView = (TextView) findViewById(R.id.log_in_name);
        passwordTextView = (TextView) findViewById(R.id.password);
        confirmPasswordTextView = (TextView) findViewById(R.id.password_confirm);
        emailTextView = (TextView) findViewById(R.id.email);
        button = (Button) findViewById(R.id.btn_register);
        button.setOnClickListener(onClickListener);
    }

    OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isValidEmail() && isValidPassword()) {
                registerNewUser(nameTextView.getText().toString(), passwordTextView.getText().toString(), emailTextView.getText().toString());
            }
        }
    };

    private void createProgressDialog() {
        dialog = new ProgressDialog(RegisterActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void registerNewUser(final String userName, final String password, final String email) {
        createProgressDialog();
        List<ParseObject> results = null;
        try {
            results = ParseQuery.getQuery("_User").whereContains(getString(R.string.username_parse_key), userName).find();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (results == null || results.isEmpty()) {
                ParseUser newUser = new ParseUser();
                newUser.setUsername(userName);
                newUser.setPassword(password);
                if (email != null)
                    newUser.setEmail(email);

                newUser.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(com.parse.ParseException e) {
                        if (dialog != null)
                            dialog.dismiss();
                        if (e == null) {
                            Log.i(this.toString(), "create new user");
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.d("Error Registration", e.toString());
                        }
                    }
                });
            } else {
                if (dialog != null)
                    dialog.dismiss();
                passwordTextView.setText("");
                confirmPasswordTextView.setText("");
                nameTextView.requestFocus();
                nameTextView.setError("This nickname is already used!");
            }
        }
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

    private boolean isValidPassword() {
        String confirmPassword = confirmPasswordTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        if (password.length() < 4) {
            passwordTextView.setText("");
            confirmPasswordTextView.setText("");
            passwordTextView.requestFocus();
            passwordTextView.setError("Password must be at least 4 characters in length or more!");
        } else if (!confirmPassword.equals(password)) {
            passwordTextView.setText("");
            confirmPasswordTextView.setText("");
            passwordTextView.requestFocus();
            passwordTextView.setError("Password and its confirm does not match!");
        } else
            return true;
        return false;
    }

    public boolean isValidEmail() {
        CharSequence email = emailTextView.getText();
        if (TextUtils.isEmpty(email)) {
            return true;
        } else {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailTextView.setText("");
                passwordTextView.setText("");
                confirmPasswordTextView.setText("");
                emailTextView.requestFocus();
                emailTextView.setError("Invalid email address!");
                return false;
            } else
                return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }
}
