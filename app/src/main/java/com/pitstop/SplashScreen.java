package com.pitstop;

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.SplashSlidePagerAdapter;

public class SplashScreen extends AppCompatActivity {

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForID = "com.pitstop.login.id";
    final static String pfCodeForPassword = "com.pitstop.login.passwd";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    public static final String TAG = SplashScreen.class.getSimpleName();

    boolean signup  = false;
    boolean backPressed = false;


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

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        String email = settings.getString(pfCodeForID, "NA");

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Log.i(TAG, "Current Parse user is null");
        }
        else {
            ParseApplication.mixpanelAPI.identify(currentUser.getObjectId());
            ParseApplication.mixpanelAPI.getPeople().identify(currentUser.getObjectId());
            ParseApplication.mixpanelAPI.getPeople().set("Phone Number",currentUser.get("phoneNumber"));
            ParseApplication.mixpanelAPI.getPeople().set("Name",currentUser.getUsername());
            ParseApplication.mixpanelAPI.getPeople().set("Email",currentUser.getEmail());
            Toast.makeText(getApplicationContext(), "Logging in" , Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.i(TAG, currentUser.getUsername());
        }


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new SplashSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position==3){
                    findViewById(R.id.radio_layout).setVisibility(View.GONE);
                    findViewById(R.id.button7).setVisibility(View.GONE);
                    findViewById(R.id.login).setVisibility(View.VISIBLE);
                    findViewById(R.id.name).setVisibility(View.GONE);
                    findViewById(R.id.phone).setVisibility(View.GONE);

                    ((EditText)findViewById(R.id.password)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                    findViewById(R.id.radio_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.button7).setVisibility(View.VISIBLE);
                    for(int i = 0; i<3; i++){
                        ((RadioButton)((LinearLayout)findViewById(R.id.radio_layout)).getChildAt(i)).setChecked(false);
                    }
                    ((RadioButton)((LinearLayout)findViewById(R.id.radio_layout)).getChildAt(position)).setChecked(true);
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
                findViewById(R.id.login).setVisibility(View.VISIBLE);
                findViewById(R.id.name).setVisibility(View.GONE);
                findViewById(R.id.phone).setVisibility(View.GONE);
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

    public void signUp(final View view) {
        if (signup) {
            showLoading();
            if(((TextView)findViewById(R.id.password)).getText().toString().length()<6){
                Snackbar.make(findViewById(R.id.splash_layout), "Password length must be greater than 6",Snackbar.LENGTH_SHORT).show();
                hideLoading();
                return;
            }
            if(((TextView)findViewById(R.id.phone)).getText().toString().length()!=10){
                Snackbar.make(findViewById(R.id.splash_layout), "Invalid phone number",Snackbar.LENGTH_SHORT).show();
                hideLoading();
                return;
            }
            ParseUser user = new ParseUser();
            user.setUsername(((TextView)findViewById(R.id.email)).getText().toString());
            user.setPassword(((TextView)findViewById(R.id.password)).getText().toString());
            user.setEmail(((TextView)findViewById(R.id.email)).getText().toString());

// other fields can be set just like with ParseObject
            user.put("name", ((TextView)findViewById(R.id.name)).getText().toString());
            user.put("phoneNumber", ((TextView)findViewById(R.id.phone)).getText().toString());
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
        }else{
            findViewById(R.id.name).setVisibility(View.VISIBLE);
            findViewById(R.id.phone).setVisibility(View.VISIBLE);
            findViewById(R.id.login).setVisibility(View.GONE);
            signup = !signup;
        }
    }

    public void login(View view) {
        showLoading();
        String username = ((TextView) findViewById(R.id.email)).getText().toString();
        String password = ((TextView) findViewById(R.id.password)).getText().toString();

        ParseUser.logInInBackground(username, password, new LogInCallback() {

            @Override
            public void done(ParseUser user, ParseException e) {
                if (e == null) {
                    Toast.makeText(SplashScreen.this, "Congrats, you have logged in!",
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SplashScreen.this, MainActivity.class);

                    SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(pfCodeForID, ((TextView) findViewById(R.id.email)).getText().toString());
                    editor.putString(pfCodeForPassword, ((TextView) findViewById(R.id.password)).getText().toString());
                    editor.putString(pfCodeForObjectID, ParseUser.getCurrentUser().getObjectId());
                    if(ParseUser.getCurrentUser().getParseObject("subscribedShopPointer")!=null) {
                        editor.putString(MainActivity.pfCodeForShopObjectID,ParseUser.getCurrentUser().getParseObject("subscribedShopPointer").getObjectId());
                    }
                    editor.commit();

                    //save user data
                    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                    installation.put("userId", ParseUser.getCurrentUser().getObjectId());
                    installation.saveInBackground();

                    ParseApplication.mixpanelAPI.identify(ParseUser.getCurrentUser().getObjectId());
                    ParseApplication.mixpanelAPI.getPeople().identify( ParseUser.getCurrentUser().getObjectId());
                    ParseApplication.mixpanelAPI.getPeople().set("Phone Number", ParseUser.getCurrentUser().get("phoneNumber"));
                    ParseApplication.mixpanelAPI.getPeople().set("Name", ParseUser.getCurrentUser().getUsername());
                    ParseApplication.mixpanelAPI.getPeople().set("Email", ((TextView) findViewById(R.id.email)).getText().toString());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    Toast.makeText(SplashScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                hideLoading();

            }
        });
    }

    public void goToLogin(View view) {
        mPager.setCurrentItem(3);
    }

    private void showLoading(){
        findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
    }

    private void hideLoading(){
        findViewById(R.id.loading_section).setVisibility(View.GONE);
    }
}
