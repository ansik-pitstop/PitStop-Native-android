package com.pitstop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SplashScreen extends AppCompatActivity {
    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForID = "com.pitstop.login.id";
    final static String pfCodeForPassword = "com.pitstop.login.passwd";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    public static final String TAG = SplashScreen.class.getSimpleName();

    boolean signup  = false;

    final String loginCache = "PITSTOP_LOGIN_DATA0";

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
            Toast.makeText(getApplicationContext(), "Logging in" , Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Log.i(TAG, currentUser.getUsername());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
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

    public void goToMainScreen(View view) {
        Intent intent = new Intent(SplashScreen.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    public void signUp(final View view) {
        if (signup) {
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
                        Toast.makeText(getApplicationContext(), "Congrats, you have signed up!", Toast.LENGTH_SHORT).show();
                        login(view);
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed, please double check your information!", Toast.LENGTH_SHORT).show();
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
        ParseUser.logInInBackground(((TextView) findViewById(R.id.email)).getText().toString(), ((TextView) findViewById(R.id.password)).getText().toString(), new LogInCallback() {

            @Override
            public void done(ParseUser user, ParseException e) {
                if (e == null) {
                    Toast.makeText(getApplicationContext(), "Congrats, you have logged in!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SplashScreen.this, MainActivity.class);

                    SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(pfCodeForID, ((TextView) findViewById(R.id.email)).getText().toString());
                    editor.putString(pfCodeForPassword, ((TextView) findViewById(R.id.password)).getText().toString());
                    editor.putString(pfCodeForObjectID, ParseUser.getCurrentUser().getObjectId());
                    if(ParseUser.getCurrentUser().getParseObject("subscribedShopPointer")!=null) {
                        editor.putString(MainActivityFragment.pfCodeForShopObjectID,ParseUser.getCurrentUser().getParseObject("subscribedShopPointer").getObjectId());
                    }
                    editor.commit();
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Failed, please double check your information!", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
