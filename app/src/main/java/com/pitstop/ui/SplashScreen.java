package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.util.Utils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MigrationService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.adapters.SplashSlidePagerAdapter;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashScreen extends AppCompatActivity {

    final static String pfName = "com.pitstop.login.name";
    public static String ACTIVITY_NAME = "splash_screen";

    public static String LOGIN_REFRESH = "login_refresh";

    public static final String TAG = SplashScreen.class.getSimpleName();

    GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    boolean signup  = false;
    boolean backPressed = false;
    boolean facebookSignup = false;

    private ProgressDialog progressDialog;

    private EditText firstName;
    private EditText lastName;
    private EditText password;
    private EditText phoneNumber;
    private EditText email;

    private View splashLayout;
    private LinearLayout radioLayout;
    private Button loginButton, skipButton;

    // for facebook login
    public CallbackManager callbackManager;
    private LoginButton facebookLoginButton;

    private NetworkHelper networkHelper;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MainActivity.TAG, "Calling on create");
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_splash_screen);

        if(BuildConfig.DEBUG) {
            Toast.makeText(this, "This is a debug build - " + BuildConfig.ENDPOINT_TYPE, Toast.LENGTH_LONG).show();
        }

        networkHelper = new NetworkHelper(getApplicationContext());

        application = (GlobalApplication) getApplicationContext();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        setUpUIReferences();

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new SplashSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setUpUIReferences();
            }

            @Override
            public void onPageSelected(int position) {
                setUpUIReferences();
                if(position==2){
                    findViewById(R.id.log_in_sign_up_container).setVisibility(View.INVISIBLE);
                    try {
                        mixpanelHelper.trackViewAppeared("Login");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    facebookLoginButton = (LoginButton) findViewById(R.id.fb_login);
                    if(facebookLoginButton != null) {
                        facebookLoginButton.setReadPermissions("public_profile", "email");
                        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(LoginResult loginResult) {
                                loginSocial(loginResult.getAccessToken().getToken(), "facebook");
                            }
                            @Override
                            public void onCancel() {
                                Log.d("Facebook", "cancel");
                            }
                            @Override
                            public void onError(FacebookException error) {
                                Log.d("Facebook", "error" + error.getMessage());
                            }
                        });
                    }

                    radioLayout.setVisibility(View.GONE);
//                    skipButton.setVisibility(View.GONE);
                    loginButton.setVisibility(View.VISIBLE);
                    firstName.setVisibility(View.GONE);
                    lastName.setVisibility(View.GONE);
                    phoneNumber.setVisibility(View.GONE);

                    password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            boolean handled = false;
                            if (actionId == EditorInfo.IME_ACTION_SEND) {
                                loginOrSignUp(null);

                                handled = true;
                                View view = getCurrentFocus();
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
                            }
                            return handled;
                        }
                    });
                }else{
                    findViewById(R.id.log_in_sign_up_container).setVisibility(View.VISIBLE);
                    radioLayout.setVisibility(View.VISIBLE);
//                    skipButton.setVisibility(View.VISIBLE);
                    for(int i = 0; i<3; i++){
                        ((RadioButton)radioLayout.getChildAt(i)).setChecked(false);
                    }
                    ((RadioButton)radioLayout.getChildAt(position)).setChecked(true);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mPager.setAdapter(mPagerAdapter);

        mixpanelHelper = new MixpanelHelper(application);

        try {
            mixpanelHelper.trackAppStatus(MixpanelHelper.APP_LAUNCHED);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ParseUser currentUser = ParseUser.getCurrentUser();
        if(currentUser != null) {
            showLoading("Logging in");
            loginParse(currentUser.getObjectId(), currentUser.getSessionToken());
        } else if (!application.isLoggedIn()
                || application.getAccessToken() == null || application.getRefreshToken() == null) {
            Log.i(TAG, "Not logged in");
        } else if(AccessToken.getCurrentAccessToken() != null) {
            startMainActivity(false);
        } else {
            showLoading("Logging in...");

            startMainActivity(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
    }


    @Override
    public void onBackPressed() {
        backPressed = true;
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            if(facebookSignup) {
                application.logOutUser();
            }
            // Otherwise, select the previous step.
            if(signup && mPager.getCurrentItem()==2&&firstName.getVisibility()== View.VISIBLE) {
                firstName.setVisibility(View.GONE);
                lastName.setVisibility(View.GONE);
                phoneNumber.setVisibility(View.GONE);
                email.setVisibility(View.VISIBLE);
                password.setVisibility(View.VISIBLE);
                findViewById(R.id.sign_log_switcher_button).setVisibility(View.VISIBLE);
//                findViewById(R.id.login_or).setVisibility(View.GONE);
                findViewById(R.id.fb_login_butt).setVisibility(View.VISIBLE);
                ((Button)findViewById(R.id.login_btn)).setText("SIGN UP");
            } else {
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpUIReferences() {


        firstName = (EditText) findViewById(R.id.firstName);
        lastName = (EditText) findViewById(R.id.lastName);
        password = (EditText) findViewById(R.id.password);
        phoneNumber = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);

        splashLayout = findViewById(R.id.splash_layout);
        radioLayout = (LinearLayout) findViewById(R.id.radio_layout);
        loginButton = (Button) findViewById(R.id.login_btn);
        skipButton = (Button) findViewById(R.id.sign_up_skip);
    }

    public void signUpSwitcher(final View view) {
        if (signup) {
            firstName.setVisibility(View.GONE);
            lastName.setVisibility(View.GONE);
            phoneNumber.setVisibility(View.GONE);
            ((Button)findViewById(R.id.fb_login_butt)).setText("Log in with facebook");
            ((Button)findViewById(R.id.login_btn)).setText("Log In");
            ((Button)findViewById(R.id.sign_log_switcher_button)).setText("SIGN UP");
            signup = !signup;
        }else{
            try {
                mixpanelHelper.trackButtonTapped("Register", TAG);
                mixpanelHelper.trackViewAppeared("Register");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ((Button)findViewById(R.id.fb_login_butt)).setText("Sign up with facebook");
            ((Button)findViewById(R.id.login_btn)).setText("Sign Up");
            ((Button)findViewById(R.id.sign_log_switcher_button)).setText("LOG IN");
            signup = !signup;
        }
    }

    public void loginFacebook(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Login with Facebook", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(facebookLoginButton != null) {
            facebookLoginButton.performClick();
        }
    }

    public void loginOrSignUp(final View view) {
        if (signup) {//if signing up
            try {
                mixpanelHelper.trackButtonTapped("Register", TAG);
                mixpanelHelper.trackViewAppeared("Register");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(!NetworkHelper.isConnected(this)) {
                Toast.makeText(SplashScreen.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                return;
            }
            if((!email.getText().toString().equals(""))
                    &&(!password.getText().toString().equals(""))
                    &&firstName.getVisibility()!= View.VISIBLE){
                finalizeProfile();
                return;
            }else if(firstName.getVisibility()!= View.VISIBLE && !facebookSignup){
                Snackbar.make(splashLayout, "Email or Password Field are not filled",Snackbar.LENGTH_SHORT).show();
                return;
            }

            showLoading("Loading");
            if(Utils.isEmpty(firstName.getText().toString()) || Utils.isEmpty(lastName.getText().toString())) {
                Toast.makeText(SplashScreen.this, "First and last name are required", Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }
            if(password.getText().toString().length()<6 && !facebookSignup){
                Toast.makeText(SplashScreen.this, "Password length must be greater than 6", Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }
            if(phoneNumber.getText().toString().length()!=10 && phoneNumber.getText().toString().length()!=11){
                Toast.makeText(SplashScreen.this, "Invalid phone number", Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }
            // creating json to post
            if(!facebookSignup) {
                JSONObject json = new JSONObject();
                try { // TODO: put names and phone number from second screen
                    json.put("firstName", firstName.getText().toString());
                    json.put("lastName", lastName.getText().toString());
                    json.put("email", email.getText().toString().replace(" ", ""));
                    json.put("username", email.getText().toString().replace(" ", ""));
                    json.put("phone", phoneNumber.getText().toString());
                    json.put("password", password.getText().toString());
                    json.put("isSocial", false);
                    json.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                    return;
                }
                networkHelper.signUpAsync(json, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            login(email.getText().toString(), password.getText().toString());
                        } else {
                            Log.e(TAG, "Sign up error: " + requestError.getMessage());
                            Toast.makeText(SplashScreen.this, "This email is already in use", Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    }
                });
            } else {
                final User user = application.getCurrentUser();
                user.setFirstName(firstName.getText().toString());
                user.setLastName(lastName.getText().toString());
                user.setPhone(phoneNumber.getText().toString());
                networkHelper.updateUser(application.getCurrentUserId(), firstName.getText().toString(),
                        lastName.getText().toString(), phoneNumber.getText().toString(),
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(requestError == null) {
                                    application.setCurrentUser(user);
                                    application.setUpMixPanel();
                                    goToMainActivity(true);
                                } else {
                                    Toast.makeText(SplashScreen.this, "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }else{
            try {
                mixpanelHelper.trackButtonTapped("Login with Email", TAG);

                try {
                    mixpanelHelper.trackButtonTapped("Login with Email", TAG);
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }

                if(!NetworkHelper.isConnected(this)) {
                    Snackbar.make(findViewById(R.id.splash_layout), "Please check your internet connection", Snackbar.LENGTH_SHORT)
                            .setAction("Retry", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    signUpSwitcher(null);
                                }
                            })
                            .show();
                    return;
                }

                showLoading("Logging in...");
                final String usernameInput = email.getText().toString().toLowerCase();
                final String passwordInput = password.getText().toString();

                login(usernameInput, passwordInput);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
    }

    private void finalizeProfile() {
        mPager.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
        firstName.setVisibility(View.VISIBLE);
        lastName.setVisibility(View.VISIBLE);
        phoneNumber.setVisibility(View.VISIBLE);
        email.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        findViewById(R.id.sign_log_switcher_button).setVisibility(View.GONE);
        findViewById(R.id.fb_login_butt).setVisibility(View.GONE);
        ((Button)findViewById(R.id.login_btn)).setText("FINALIZE PROFILE");
    }

    private void loginSocial(final String fbAccessToken, final String provider) {
        showLoading("Logging in");

        networkHelper.loginSocial(fbAccessToken, provider, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                if (requestError == null) {
                    facebookSignup = true;
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        User user = User.jsonToUserObject(response);
                        String accessToken = jsonObject.getString("accessToken");
                        String refreshToken = jsonObject.getString("refreshToken");
                        application.logInUser(accessToken, refreshToken, user);
                        if(user.getPhone() == null || user.getPhone().equals("null")) {
                            signup = true;
                            hideLoading();
                            finalizeProfile();
                            firstName.setText(user.getFirstName());
                            lastName.setText(user.getLastName());
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    application.setUpMixPanel();

                    goToMainActivity(true);
                } else {
                    Log.e(TAG, "Login: " + requestError.getError() + ": " + requestError.getMessage());
                    Snackbar.make(findViewById(R.id.splash_layout), "Invalid username/password", Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private BroadcastReceiver migrationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(MigrationService.USER_MIGRATION_SUCCESS, false);
            Log.d(TAG, "migration result received: " + success);
            unregisterReceiver(this);
            if(success) {
                startMainActivity(true);
            } else {
                hideLoading();
                migrationFailedDialog();
            }

        }
    };

    private void loginParse(final String userId, final String sessionId) {

        networkHelper.loginLegacy(userId, sessionId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                if(requestError == null) { // start migration
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        User user = User.jsonToUserObject(response);
                        String accessToken = jsonObject.getString("accessToken");
                        String refreshToken = jsonObject.getString("refreshToken");

                        application.setCurrentUser(user);

                        if(jsonObject.has("user") && jsonObject.getJSONObject("user").has("migration")
                            && jsonObject.getJSONObject("user").getJSONObject("migration").getBoolean("isMigrationDone")) {
                            application.logInUser(accessToken, refreshToken, user);
                            startMainActivity(true);
                        } else {
                            startMigration(accessToken, refreshToken, user.getId());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    application.setUpMixPanel();
                    goToMainActivity(true);
                } else if(requestError.getMessage().contains("is already used") && application.getAccessToken() != null
                        && application.getRefreshToken() != null && application.getCurrentUserId() != -1) { // retry migration because first time failed
                    migrationFailedDialog();
                }
            }
        });
    }

    private void login(final String username, final String password) {
        networkHelper.loginAsync(username, password, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                if(requestError == null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        User user = User.jsonToUserObject(response);
                        String accessToken = jsonObject.getString("accessToken");
                        String refreshToken = jsonObject.getString("refreshToken");

                        if(jsonObject.has("user") && jsonObject.getJSONObject("user").has("migration")
                                && jsonObject.getJSONObject("user").getJSONObject("migration").getBoolean("isMigrationDone")) {
                            application.logInUser(accessToken, refreshToken, user);
                            startMainActivity(true);
                        } else {
                            startMigration(accessToken, refreshToken, user.getId());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    application.setUpMixPanel();
                    goToMainActivity(true);
                } else {
                    Log.e(TAG, "Login: " + requestError.getError() + ": " + requestError.getMessage());
                    Toast.makeText(SplashScreen.this, requestError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void goToMainActivity(boolean refresh) {
        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(LOGIN_REFRESH, refresh);
        intent.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
        startActivity(intent);
    }
    public void login(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Login with Email", TAG);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }

        if(!NetworkHelper.isConnected(this)) {
            Toast.makeText(SplashScreen.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading("Logging in...");
        final String usernameInput = email.getText().toString().toLowerCase();
        final String passwordInput = password.getText().toString();

        login(usernameInput, passwordInput);
    }

    private void migrationFailedDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(SplashScreen.this);
        dialog.setMessage("Update failed. Please contact us at info@getpitstop.io.");
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseUser.logOut();
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton("Try again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                application.setUpMixPanel();
                startMigration(application.getAccessToken(), application.getRefreshToken(), application.getCurrentUserId());
            }
        });
        dialog.show();
    }

    private void startMigration(String accessToken, String refreshToken, int userId) {
        showLoading("We are updating the app.  This may take a minute.  " +
                "Feel free to leave the app during this time.");

        registerReceiver(migrationReceiver, new IntentFilter(MigrationService.MIGRATION_BROADCAST));

        application.setTokens(accessToken, refreshToken);

        Intent migrationIntent = new Intent(SplashScreen.this, MigrationService.class);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_ID, userId);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_REFRESH, refreshToken);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_ACCESS, accessToken);
        startService(migrationIntent);
    }

    private void startMainActivity(boolean update) {
        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(LOGIN_REFRESH, update);
        intent.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
        startActivity(intent);
    }

    public void goToLogin(View view) {
        mPager.setCurrentItem(2);
        if(((Button)view).getText().equals("Sign Up")&&!signup){
            signUpSwitcher(view);
        }else if (((Button)view).getText().equals("Log In")&&signup){
            signUpSwitcher(view);
        }
    }

    private void showLoading(String text){
        Log.i(TAG, "Show loading: " + text);
        if(isFinishing())
            return;

        progressDialog.setMessage(text);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading(){
        Log.i(MainActivity.TAG, "hiding loading");
        progressDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        application.getMixpanelAPI().flush();
        Log.i(MainActivity.TAG, "SplashScreen on pause");
        hideLoading();

        try {
            unregisterReceiver(migrationReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(MainActivity.TAG, "SplashScreen onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void forgotPassword(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        final EditText emailField = new EditText(this);
        emailField.setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setHint("Email");

        dialog.setView(emailField);
        dialog.setTitle("Reset Password");
        dialog.setMessage("Please enter your email address");

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = emailField.getText().toString();
                networkHelper.resetPassword(email, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            Toast.makeText(SplashScreen.this, String.format("An email has been sent to %s with further instructions. ", email) +
                                    "It may take up to a few minutes to arrive.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SplashScreen.this, requestError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
