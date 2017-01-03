package com.pitstop.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.pitstop.ui.controls.ViewFlipperIndicator;
import com.pitstop.utils.MigrationService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.UiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    final static String pfName = "com.pitstop.login.name";
    public static String ACTIVITY_NAME = "splash_screen";

    public static String LOGIN_REFRESH = "login_refresh";

    public static final String TAG = LoginActivity.class.getSimpleName();
    public static final String MIXPANEL_TAG = "Mixpanel";
    public static final String FACEBOOK_TAG = "Facebook";

    public static final String FACEBOOK_PROVIDER = "facebook";
    public static final String FB_PROFILE_PERMISSION = "public_profile";
    public static final String FB_EMAIL_PERMISSION = "email";

    public static final float PERCENTAGE_OF_SCREEN_HEIGHT = 0.38f;
    public static final int INITIAL_LOGIN_ANIMATION_INTERVAL = 400;
    public static final int INITIAL_LOGIN_ANIMATION_DELAY = 300;

    public static final int FEATURE_HIGHLIGHTS_INDICATOR_RADIUS = 15;
    public static final int FEATURE_HIGHLIGHTS_INDICATOR_MARGIN = 20;
    public static final int FEATURE_HIGHLIGHTS_INTERVAL = 5000;

    public static final int SECTION_SLIDE_ANIMATION_INTERVAL = 300;


    GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    boolean signup = false;
    boolean facebookSignup = false;
    boolean mSliderSectionVisible = false;
    private ProgressDialog progressDialog;

    @BindView(R.id.firstNameLayout)
    TextInputLayout firstNameLayout;
    @BindView(R.id.firstName)
    TextInputEditText firstName;
    @BindView(R.id.lastNameLayout)
    TextInputLayout lastNameLayout;
    @BindView(R.id.lastName)
    TextInputEditText lastName;
    @BindView(R.id.passwordLayout)
    TextInputLayout passwordLayout;
    @BindView(R.id.password)
    TextInputEditText password;
    @BindView(R.id.phoneLayout)
    TextInputLayout phoneLayout;
    @BindView(R.id.phone)
    TextInputEditText phoneNumber;
    @BindView(R.id.emailLayout)
    TextInputLayout emailLayout;
    @BindView(R.id.email)
    TextInputEditText email;
    @BindView(R.id.fb_login_butt)
    Button mFbButton;
    @BindView(R.id.sign_log_switcher_button)
    Button mSwitcherButton;
    @BindView(R.id.login_btn)
    Button mLoginButton;
    @BindView(R.id.splash_layout)
    View splashLayout;
    @BindView(R.id.sign_up_skip)
    Button skipButton;
    @BindView(R.id.logo_imageview)
    ImageView mLogoImageView;
    @BindView(R.id.log_in_sign_up_container)
    LinearLayout mButtonContainer;

    // for facebook login
    public CallbackManager callbackManager;
    private LoginButton facebookLoginButton;

    private NetworkHelper networkHelper;

    @BindView(R.id.feature_highlights)
    protected ViewFlipperIndicator mFeatureHighlights;
    private GestureDetector mFeatureHighlightGestureDetector;
    @BindView(R.id.login_signup_fragment_container)
    protected FrameLayout mLoginContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MainActivity.TAG, "Calling on create");
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, getString(R.string.debug_toast_message) + BuildConfig.ENDPOINT_TYPE, Toast.LENGTH_LONG).show();
        }


        networkHelper = new NetworkHelper(getApplicationContext());

        application = (GlobalApplication) getApplicationContext();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        setUpFeatureHighlightCarousel();

        mixpanelHelper = new MixpanelHelper(application);

        try {
            mixpanelHelper.trackAppStatus(MixpanelHelper.APP_LAUNCHED);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            showLoading(getString(R.string.logging_in_message));
            loginParse(currentUser.getObjectId(), currentUser.getSessionToken());
        } else if (!application.isLoggedIn()
                || application.getAccessToken() == null || application.getRefreshToken() == null) {
            Log.i(TAG, "Not logged in");
        } else if (AccessToken.getCurrentAccessToken() != null) {
            startMainActivity(false);
        } else {
            showLoading(getString(R.string.logging_in_message));
            startMainActivity(false);
        }

        //For debug
        // Add code to print out the key hash
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.ansik.pitstop",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        //findViewById(R.id.log_in_sign_up_container).setVisibility(View.INVISIBLE);
        try {
            Log.d(MIXPANEL_TAG, "Login view appeared");
            mixpanelHelper.trackViewAppeared(MixpanelHelper.LOGIN_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        facebookLoginButton = (LoginButton) findViewById(R.id.fb_login);
        if (facebookLoginButton != null) {
            facebookLoginButton.setReadPermissions(FB_PROFILE_PERMISSION, FB_EMAIL_PERMISSION);
            facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    loginSocial(loginResult.getAccessToken().getToken(), FACEBOOK_PROVIDER);
                }

                @Override
                public void onCancel() {
                    Log.d(FACEBOOK_TAG, "cancel");
                }

                @Override
                public void onError(FacebookException error) {
                    Log.d(FACEBOOK_TAG, "error" + error.getMessage());
                }
            });
        }

        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    loginOrSignUp(null);

                    handled = true;
                    View view = getCurrentFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
                }
                return handled;
            }
        });

        ObjectAnimator logoAnimator = ObjectAnimator.ofFloat(mLogoImageView, View.TRANSLATION_Y,0, -UiUtils.getScreenHeight(this) * PERCENTAGE_OF_SCREEN_HEIGHT);
        logoAnimator.setStartDelay(INITIAL_LOGIN_ANIMATION_DELAY);
        logoAnimator.setDuration(INITIAL_LOGIN_ANIMATION_INTERVAL);
        logoAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                fadeInLoginViews();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        logoAnimator.start();

    }

    private void fadeInLoginViews() {
        ObjectAnimator highlightsAnimator =  ObjectAnimator.ofFloat(mFeatureHighlights, View.ALPHA, 0, 1);
        ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(mButtonContainer, View.ALPHA, 0,1);
        AnimatorSet fadeInViewAnimatorSet = new AnimatorSet();
        fadeInViewAnimatorSet.playTogether(highlightsAnimator, buttonAnimator);
        fadeInViewAnimatorSet.setDuration(INITIAL_LOGIN_ANIMATION_INTERVAL);
        fadeInViewAnimatorSet.start();
    }

    private void setUpFeatureHighlightCarousel() {
        FeatureHighlightGestureListener customGestureDetector = new FeatureHighlightGestureListener();
        mFeatureHighlightGestureDetector = new GestureDetector(this, customGestureDetector);
        mFeatureHighlights.setRadius(FEATURE_HIGHLIGHTS_INDICATOR_RADIUS);
        mFeatureHighlights.setMargin(FEATURE_HIGHLIGHTS_INDICATOR_MARGIN);
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.primary));
        mFeatureHighlights.setPaintCurrent(paint);
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.white));
        mFeatureHighlights.setPaintNormal(paint);
        mFeatureHighlights.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mFeatureHighlightGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
        mFeatureHighlights.setAutoStart(true);
        mFeatureHighlights.setFlipInterval(FEATURE_HIGHLIGHTS_INTERVAL);

    }

    @Override
    public void onBackPressed() {
        if (!mSliderSectionVisible) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            if (facebookSignup) {
                application.logOutUser();
            }
            // Otherwise, select the previous step.
            if (signup && mSliderSectionVisible && firstNameLayout.getVisibility() == View.VISIBLE) {
                firstNameLayout.setVisibility(View.GONE);
                lastNameLayout.setVisibility(View.GONE);
                phoneLayout.setVisibility(View.GONE);
                emailLayout.setVisibility(View.VISIBLE);
                passwordLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.sign_log_switcher_button).setVisibility(View.VISIBLE);
                findViewById(R.id.fb_login_butt).setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.login_btn)).setText(getString(R.string.sign_up_button));
            } else {
                slideOutLoginSignUpSection(false);
            }
        }

        // When the user tap back to move to previous screen
        try {
            String view = signup ? MixpanelHelper.REGISTER_VIEW : MixpanelHelper.LOGIN_VIEW;
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_BACK, view);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        application.getMixpanelAPI().flush();
        Log.i(MainActivity.TAG, "LoginActivity on pause");
        hideLoading();

        try {
            unregisterReceiver(migrationReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }

        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Invoked when the signUp/Login switcher button on the top right was tapped
     *
     * @param view The signup/login switcher button
     */
    public void signUpSwitcher(final View view) {
        if (signup) {
            showLoginSection();
        } else {
            showSignupSection();
        }
        slideOutLoginSignUpSection(true);
    }


    private void showSignupSection() {
        try {
            Log.d(MIXPANEL_TAG, "Register view appeared");
            mixpanelHelper.trackViewAppeared(MixpanelHelper.REGISTER_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mFbButton.setText(R.string.sign_up_fb);
        mLoginButton.setText(R.string.sign_up_button);
        mSwitcherButton.setText(R.string.log_in_button);
        signup = true;
    }

    private void showLoginSection() {
        try {
            Log.d(MIXPANEL_TAG, "Login view appeared");
            mixpanelHelper.trackViewAppeared(MixpanelHelper.LOGIN_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        firstNameLayout.setVisibility(View.GONE);
        lastNameLayout.setVisibility(View.GONE);
        phoneLayout.setVisibility(View.GONE);
        mFbButton.setText(R.string.log_in_fb);

        mLoginButton.setText(R.string.log_in_button);
        mSwitcherButton.setText(R.string.sign_up_button);
        signup = false;
    }

    /**
     * Invoked when the "Login with Facebook" button is tapped (The blue one)
     * @param view The Login/Signup with Facebook button in the splash screen
     */
    public void loginFacebook(View view) {
        try {
            if (signup) {
                Log.d(MIXPANEL_TAG, "Register with facebook");
                application.modifyMixpanelSettings("Registered With", "Facebook");
                mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_REGISTER_WITH_FACEBOOK, MixpanelHelper.REGISTER_VIEW);
            } else {
                Log.d(MIXPANEL_TAG, "Login with facebook");
                mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_LOGIN_WITH_FACEBOOK, MixpanelHelper.LOGIN_VIEW);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        if (facebookLoginButton != null) {
            facebookLoginButton.performClick();
        }
    }

    /**
     * Invoked when "SIGN UP" / "LOG IN" button is tapped on the splash_login page (The green one)
     *
     * @param view
     */
    public void loginOrSignUp(final View view) {
        if (signup) {
            //if signing up
            if (!NetworkHelper.isConnected(this)) {
                Toast.makeText(LoginActivity.this, R.string.internet_check_error, Toast.LENGTH_LONG).show();
                return;
            }

            if ((!email.getText().toString().equals(""))
                    && (!password.getText().toString().equals(""))
                    && firstNameLayout.getVisibility() != View.VISIBLE) {

                // The user tapped on the SIGNUP button after he entered his email and password
                try {
                    Log.d(MIXPANEL_TAG, "Register button tapped");
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.REGISTER_BUTTON_TAPPED, MixpanelHelper.REGISTER_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                finalizeProfile();
                return;
            } else if (firstName.getVisibility() != View.VISIBLE && !facebookSignup) {
                Snackbar.make(splashLayout, R.string.empty_email_pass_error, Snackbar.LENGTH_SHORT).show();
                return;
            }

            showLoading(getString(R.string.loading));
            if (Utils.isEmpty(firstName.getText().toString()) || Utils.isEmpty(lastName.getText().toString())) {
                Toast.makeText(LoginActivity.this, R.string.empty_name_error, Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }
            if (password.getText().toString().length() < 6 && !facebookSignup) {
                Toast.makeText(LoginActivity.this, R.string.password_length_error, Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }
            if (phoneNumber.getText().toString().length() != 10 && phoneNumber.getText().toString().length() != 11) {
                Toast.makeText(LoginActivity.this, R.string.invalid_phone_error, Toast.LENGTH_LONG).show();
                hideLoading();
                return;
            }

            // At this point, the user tapped the "FINALIZE PROFILE" button after entering his information
            try {
                Log.d(MIXPANEL_TAG, "Confirm information countinue");
                mixpanelHelper.trackButtonTapped(MixpanelHelper.CONFIRM_INFORMATION_CONTINUE, MixpanelHelper.CONFIRM_INFORMATION_VIEW);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // creating json to post
            if (!facebookSignup) {
                // If the user is signing up with the email
                JSONObject json = new JSONObject();
                try {
                    json.put("firstName", firstName.getText().toString());
                    json.put("lastName", lastName.getText().toString());
                    json.put("email", email.getText().toString().replace(" ", "").toLowerCase());
                    json.put("username", email.getText().toString().replace(" ", "").toLowerCase());
                    json.put("phone", phoneNumber.getText().toString());
                    json.put("password", password.getText().toString());
                    json.put("isSocial", false);
                    json.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                networkHelper.signUpAsync(json, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            Log.d("SIGNUP", "SignUp login");

                            // Track REGISTER_WITH_EMAIL
                            application.modifyMixpanelSettings("Registered With", "Email");
                            try {
                                Log.d(MIXPANEL_TAG, "Register with email");
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_REGISTER_WITH_EMAIL, MixpanelHelper.REGISTER_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            login(email.getText().toString().toLowerCase(), password.getText().toString());
                        } else {
                            Log.e(TAG, "Sign up error: " + requestError.getMessage());
                            Toast.makeText(LoginActivity.this, requestError.getMessage(), Toast.LENGTH_SHORT).show();
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
                                if (requestError == null) {
                                    application.setCurrentUser(user);
                                    application.setUpMixPanel();
                                    goToMainActivity(true);
                                    try {
                                        Log.d(MIXPANEL_TAG, "Register facebook");
                                        mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_REGISTER_WITH_FACEBOOK, MixpanelHelper.REGISTER_VIEW);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        } else {
            // Login
            try {
                Log.d(MIXPANEL_TAG, "Login with email");
                mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_LOGIN_WITH_EMAIL, MixpanelHelper.LOGIN_VIEW);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }

            if (!NetworkHelper.isConnected(this)) {
                Snackbar.make(findViewById(R.id.splash_layout), R.string.internet_check_error, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.retry_button, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                signUpSwitcher(null);
                            }
                        })
                        .show();
                return;
            }

            showLoading(getString(R.string.logging_in_message));
            final String usernameInput = email.getText().toString().toLowerCase();
            final String passwordInput = password.getText().toString();

            login(usernameInput, passwordInput);
        }
    }

    /**
     * <p>If the user is signing up (in whatever way), we need them to enter their name and phone number</p>
     * <p>
     * <p>This method changes the visibility of related views, allow the user to put in their info</p>
     */
    private void finalizeProfile() {
        firstNameLayout.setVisibility(View.VISIBLE);
        lastNameLayout.setVisibility(View.VISIBLE);
        phoneLayout.setVisibility(View.VISIBLE);
        emailLayout.setVisibility(View.GONE);
        passwordLayout.setVisibility(View.GONE);
        findViewById(R.id.sign_log_switcher_button).setVisibility(View.GONE);
        findViewById(R.id.fb_login_butt).setVisibility(View.GONE);
        ((Button) findViewById(R.id.login_btn)).setText(R.string.finalize_button);

        // Confirm your information view shows up
        // Prompt the user for the name and phone number
        try {
            Log.d(MIXPANEL_TAG, "Confirm information view appeared");
            mixpanelHelper.trackViewAppeared(MixpanelHelper.CONFIRM_INFORMATION_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loginSocial(final String fbAccessToken, final String provider) {
        showLoading(getString(R.string.logging_in_message));

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
                        if (user.getPhone() == null || user.getPhone().equals("null")) {
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
                    Snackbar.make(findViewById(R.id.splash_layout), R.string.invalid_credentials_error, Snackbar.LENGTH_SHORT)
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
            if (success) {
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
                if (requestError == null) { // start migration
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        User user = User.jsonToUserObject(response);
                        String accessToken = jsonObject.getString("accessToken");
                        String refreshToken = jsonObject.getString("refreshToken");

                        application.setCurrentUser(user);

                        if (jsonObject.has("user") && jsonObject.getJSONObject("user").has("migration")
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
                } else if (requestError.getMessage().contains("is already used") && application.getAccessToken() != null
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
                if (requestError == null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        User user = User.jsonToUserObject(response);
                        String accessToken = jsonObject.getString("accessToken");
                        String refreshToken = jsonObject.getString("refreshToken");

                        if (jsonObject.has("user") && jsonObject.getJSONObject("user").has("migration")
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
                    Toast.makeText(LoginActivity.this, requestError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void goToMainActivity(boolean refresh) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(LOGIN_REFRESH, refresh);
        intent.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);

        startActivity(intent);
    }

    public void login(View view) {
        try {
            Log.d(MIXPANEL_TAG, "Login with email");
            mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_LOGIN_WITH_EMAIL, MixpanelHelper.LOGIN_VIEW);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }

        if (!NetworkHelper.isConnected(this)) {
            Toast.makeText(LoginActivity.this, R.string.internet_check_error, Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(getString(R.string.logging_in_message));
        final String usernameInput = email.getText().toString().toLowerCase();
        final String passwordInput = password.getText().toString();

        login(usernameInput, passwordInput);
    }

    private void migrationFailedDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
        dialog.setMessage(R.string.update_fail_error);
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseUser.logOut();
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton(R.string.try_again_button, new DialogInterface.OnClickListener() {
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
        showLoading(getString(R.string.updating_app_message));

        registerReceiver(migrationReceiver, new IntentFilter(MigrationService.MIGRATION_BROADCAST));

        application.setTokens(accessToken, refreshToken);

        Intent migrationIntent = new Intent(LoginActivity.this, MigrationService.class);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_ID, userId);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_REFRESH, refreshToken);
        migrationIntent.putExtra(MigrationService.USER_MIGRATION_ACCESS, accessToken);
        startService(migrationIntent);
    }

    private void startMainActivity(boolean update) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(LOGIN_REFRESH, update);
        intent.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
        startActivity(intent);
    }

    /**
     * <h1>Notice</h1>
     * This onclick method has two different buttons refer to it
     *
     * @param view - The "LOG IN" or the "SIGN UP" button in the boarding view
     */
    public void goToLogin(View view) {
        slideInLoginSignUpSection();
        if (view.getId() == R.id.log_in_skip)
            showLoginSection();
        else
            showSignupSection();
    }

    private void slideInLoginSignUpSection() {
        //Slide it out immediately
        mLoginContainer.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mLoginContainer, View.TRANSLATION_Y, 0, UiUtils.getScreenHeight(this)).start();
        ObjectAnimator.ofFloat(mLoginContainer, View.ALPHA, 1, 0).start();
        //Slide and Fade in smoothly
        ObjectAnimator yTranslationAnimator = ObjectAnimator.ofFloat(mLoginContainer, View.TRANSLATION_Y, UiUtils.getScreenHeight(this), 0);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mLoginContainer, View.ALPHA, 0, 1);
        AnimatorSet slideFadeInAnimatorSet = new AnimatorSet();
        slideFadeInAnimatorSet.playTogether(yTranslationAnimator, alphaAnimator);
        slideFadeInAnimatorSet.setDuration(SECTION_SLIDE_ANIMATION_INTERVAL);
        slideFadeInAnimatorSet.start();
        mSliderSectionVisible = true;
    }

    private void slideOutLoginSignUpSection(final boolean switchingSection) {
        ObjectAnimator yTranslationAnimator = ObjectAnimator.ofFloat(mLoginContainer, View.TRANSLATION_Y, 0, UiUtils.getScreenHeight(this));
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mLoginContainer, View.ALPHA, 1, 0);
        AnimatorSet slideFadeInAnimatorSet = new AnimatorSet();
        slideFadeInAnimatorSet.playTogether(yTranslationAnimator, alphaAnimator);
        slideFadeInAnimatorSet.setDuration(SECTION_SLIDE_ANIMATION_INTERVAL);
        slideFadeInAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mLoginContainer.setVisibility(View.GONE);
                mSliderSectionVisible = false;
                if (switchingSection)
                    slideInLoginSignUpSection();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        slideFadeInAnimatorSet.start();
    }

    private void showLoading(String text) {
        Log.i(TAG, "Show loading: " + text);
        if (isFinishing())
            return;

        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading() {
        Log.i(MainActivity.TAG, "hiding loading");
        progressDialog.dismiss();
    }

    /**
     * Invoked when the user tap on the "Forgot Password" button
     *
     * @param view The "Forgot Password" button
     */
    public void forgotPassword(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        final EditText emailField = new EditText(this);
        emailField.setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setHint(R.string.email_hint);
        dialog.setView(emailField);
        dialog.setTitle(R.string.forgot_password_dialog_title);
        dialog.setMessage(R.string.forgot_password_dialog_message);

        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = emailField.getText().toString();
                networkHelper.resetPassword(email, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            Toast.makeText(LoginActivity.this, getString(R.string.email_sent_message, email),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, requestError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                try {
                    String view = signup ? MixpanelHelper.REGISTER_VIEW : MixpanelHelper.LOGIN_VIEW;
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.FORGOT_PASSWORD_CONFIRM, view);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                try {
                    String view = signup ? MixpanelHelper.REGISTER_VIEW : MixpanelHelper.LOGIN_VIEW;
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.FORGOT_PASSWORD_CANCEL, view);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        dialog.show();

        //Track FORGOT_PASSWORD
        try {
            String mixpanelView = signup ? MixpanelHelper.REGISTER_VIEW : MixpanelHelper.LOGIN_VIEW;
            mixpanelHelper.trackButtonTapped(MixpanelHelper.LOGIN_FORGOT_PASSWORD, mixpanelView);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    class FeatureHighlightGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            // Swipe left (next)
            if (e1.getX() > e2.getX()) {
                mFeatureHighlights.setInAnimation(LoginActivity.this, R.anim.view_left_in);
                mFeatureHighlights.setOutAnimation(LoginActivity.this, R.anim.view_left_out);
                mFeatureHighlights.showNext();
                return true;
            }

            // Swipe right (previous)
            if (e1.getX() < e2.getX()) {
                mFeatureHighlights.setInAnimation(LoginActivity.this, R.anim.view_right_in);
                mFeatureHighlights.setOutAnimation(LoginActivity.this, R.anim.view_right_out);

                mFeatureHighlights.showPrevious();
                return true;
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

}
