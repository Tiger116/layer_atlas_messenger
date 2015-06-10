package com.layer.messenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;

public class RegisterActivity extends AppCompatActivity {

    private LinearLayout expandLayout;
    private TextView nameTextView;
    private TextView lastNameTextView;
    private TextView loginTextView;
    private TextView passwordTextView;
    private TextView confirmPasswordTextView;
    private TextView emailTextView;
    private Button moreButton;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        LayerApplication.setCurrentActivity(this);

        expandLayout = (LinearLayout) findViewById((R.id.expanded_layout));
        loginTextView = (TextView) findViewById(R.id.log_in_name);
        nameTextView = (TextView) findViewById(R.id.firstname);
        lastNameTextView = (TextView) findViewById(R.id.lastname);
        passwordTextView = (TextView) findViewById(R.id.password);
        confirmPasswordTextView = (TextView) findViewById(R.id.password_confirm);
        emailTextView = (TextView) findViewById(R.id.email);
        moreButton = (Button) findViewById(R.id.btn_more);
        moreButton.setOnClickListener(new OnClickListener() {
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
        Button confirmButton = (Button) findViewById(R.id.btn_register);
        confirmButton.setOnClickListener(onClickListener);
    }

    OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (loginTextView.getText().toString().isEmpty()) {
                loginTextView.setError("Nickname cannot be empty!");
                loginTextView.requestFocus();
            } else if (nameTextView.getText().toString().isEmpty()) {
                nameTextView.setError("Name cannot be empty!");
                nameTextView.requestFocus();
            } else if (isValidEmail() && isValidPassword()) {
                registerNewUser();
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

    public void registerNewUser() {
        String userName = loginTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        String email = emailTextView.getText().toString();
        String firstName = nameTextView.getText().toString();
        String lastName = lastNameTextView.getText().toString();
        createProgressDialog();
        List<ParseObject> results = null;
        try {
            results = ParseQuery.getQuery("_User").whereContains(getString(R.string.userName_label), userName).find();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (results == null || results.isEmpty()) {
                ParseUser newUser = new ParseUser();
                newUser.setUsername(userName);
                newUser.put(getString(R.string.firstname_parse_key), firstName);
                newUser.setPassword(password);
                if (!email.isEmpty())
                    newUser.setEmail(email);
                if (!lastName.isEmpty())
                    newUser.put(getString(R.string.lastname_parse_key), lastName);
                newUser.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(com.parse.ParseException e) {
                        if (dialog != null)
                            dialog.dismiss();
                        if (e == null) {
                            Log.d(this.toString(), "create new user");
                            LayerApplication.setParseUsers();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.d("Error Registration", e.toString());
                            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                if (dialog != null)
                    dialog.dismiss();
                passwordTextView.setText("");
                confirmPasswordTextView.setText("");
                loginTextView.requestFocus();
                loginTextView.setError("This nickname is already used!");
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
