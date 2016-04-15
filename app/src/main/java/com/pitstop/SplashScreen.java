package com.pitstop;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.SplashSlidePagerAdapter;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashScreen extends AppCompatActivity {

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForID = "com.pitstop.login.id";
    final static String pfCodeForPassword = "com.pitstop.login.passwd";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    public static String LOGIN_REFRESH = "login_refresh";

    public static final String TAG = SplashScreen.class.getSimpleName();

    ParseApplication application;

    boolean signup  = false;
    boolean backPressed = false;

    private ProgressDialog progressDialog;

    private EditText name;
    private EditText password;
    private EditText phoneNumber;
    private EditText email;

    private View splashLayout;
    private LinearLayout radioLayout;
    private Button loginButton, skipButton;


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
        setContentView(R.layout.activity_splash_screen);

        application = (ParseApplication) getApplicationContext();

        setUpUIReferences();

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        String email = settings.getString(pfCodeForID, "NA");

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Log.i(TAG, "Current Parse user is null");
        }
        else {
            ParseApplication.setUpMixPanel();

            showLoading("Logging in...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                        hideLoading();
                        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(LOGIN_REFRESH, true);
                        startActivity(intent);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            Log.i(TAG, currentUser.getUsername());
        }


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
                if(position==3){
                    radioLayout.setVisibility(View.GONE);
                    skipButton.setVisibility(View.GONE);
                    loginButton.setVisibility(View.VISIBLE);
                    name.setVisibility(View.GONE);
                    phoneNumber.setVisibility(View.GONE);

                    password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            boolean handled = false;
                            if (actionId == EditorInfo.IME_ACTION_SEND) {
                                if(signup){
                                    signUp(null);
                                }else{
                                    login(null);
                                }
                                handled = true;
                                View view = getCurrentFocus();
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                            return handled;
                        }
                    });
                }else{
                    radioLayout.setVisibility(View.VISIBLE);
                    skipButton.setVisibility(View.VISIBLE);
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
            // Otherwise, select the previous step.
            //TODO: Come up with an elegant solution
            if(signup && backPressed) {
                signup = !signup;
                backPressed = !backPressed;
                loginButton.setVisibility(View.VISIBLE);
                name.setVisibility(View.GONE);
                phoneNumber.setVisibility(View.GONE);
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

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        name = (EditText) findViewById(R.id.name);
        password = (EditText) findViewById(R.id.password);
        phoneNumber = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);

        splashLayout = findViewById(R.id.splash_layout);
        radioLayout = (LinearLayout) findViewById(R.id.radio_layout);
        loginButton = (Button) findViewById(R.id.login_btn);
        skipButton = (Button) findViewById(R.id.skip_btn);
    }

    public void signUp(final View view) {
        if (signup) {
            showLoading("Loading");
            if(password.getText().toString().length()<6){
                Snackbar.make(splashLayout, "Password length must be greater than 6",Snackbar.LENGTH_SHORT).show();
                hideLoading();
                return;
            }
            if(phoneNumber.getText().toString().length()!=10){
                Snackbar.make(splashLayout, "Invalid phone number",Snackbar.LENGTH_SHORT).show();
                hideLoading();
                return;
            }
            ParseUser user = new ParseUser();
            user.setUsername(email.getText().toString());
            user.setPassword(password.getText().toString());
            user.setEmail(email.getText().toString());

            // other fields can be set just like with ParseObject
            user.put("name", name.getText().toString());
            user.put("phoneNumber", phoneNumber.getText().toString());
            user.put("role", "customer");

            user.signUpInBackground(new SignUpCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(SplashScreen.this, "Congrats, you have signed up!",
                                Toast.LENGTH_SHORT).show();
                        login(view);
                    } else {
                        hideLoading();
                        Toast.makeText(SplashScreen.this,
                                "Failed, please double check your information!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            try {
                application.getMixpanelAPI().track("Button Clicked",
                        new JSONObject("{'Button':'Sign Up','View':'SplashActivity'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            name.setVisibility(View.VISIBLE);
            phoneNumber.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            signup = !signup;
        }
    }

    public void login(View view) {

        showLoading("Logging in...");
        final String usernameInput = email.getText().toString();
        final String passwordInput = password.getText().toString();

        ParseUser.logInInBackground(usernameInput, passwordInput, new LogInCallback() {

            @Override
            public void done(ParseUser user, ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(SplashScreen.this, MainActivity.class);

                    SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(pfCodeForID, usernameInput);
                    editor.putString(pfCodeForPassword, passwordInput);
                    editor.putString(pfCodeForObjectID, ParseUser.getCurrentUser().getObjectId());
                    if(ParseUser.getCurrentUser().getParseObject("subscribedShopPointer")!=null) {
                        editor.putString(MainActivity.pfCodeForShopObjectID,
                                ParseUser.getCurrentUser().getParseObject("subscribedShopPointer").getObjectId());
                    }
                    editor.apply();

                    //save user data
                    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                    installation.put("userId", ParseUser.getCurrentUser().getObjectId());
                    installation.saveInBackground();
                    ParseApplication.setUpMixPanel();
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(LOGIN_REFRESH, true);

                    hideLoading();
                    startActivity(intent);


                    try {
                        application.getMixpanelAPI().track("Button Clicked",
                                new JSONObject("{'Button':'Log In','View':'SplashActivity'}"));
                    } catch (JSONException e2) {
                        e2.printStackTrace();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(SplashScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void goToLogin(View view) {
        mPager.setCurrentItem(3);
    }

    private void showLoading(String text){
        progressDialog.setMessage(text);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading(){
        progressDialog.dismiss();
    }

    @Override
    protected void onPause() {
        application.getMixpanelAPI().flush();
        super.onPause();
    }
}
