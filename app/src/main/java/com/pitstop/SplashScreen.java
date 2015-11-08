package com.pitstop;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.PushService;
import com.parse.SignUpCallback;

public class SplashScreen extends AppCompatActivity {

    boolean signup  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appID), getString(R.string.parse_clientID));

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();

        installation.saveInBackground();
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


    public void signUp(View view) {
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
                        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
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
}
