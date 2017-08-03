package com.pitstop.ui.add_car;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.LoginActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;

public class PromptAddCarActivity extends AppCompatActivity {

    @BindView(R.id.logout_button)
    Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt_add_car);
        ButterKnife.bind(this);

        AppCompatActivity activity = this;

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GlobalApplication)getApplicationContext()).logOutUser();
                Intent intent = new Intent(activity, LoginActivity.class);
                startActivity(intent);
            }
        });
        //Change toolbar
        getSupportActionBar().setTitle("Add First Car");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    public void startAddCarActivity(View view) {
        Intent intent = new Intent(PromptAddCarActivity.this, AddCarActivity.class);
        startActivityForResult(intent, RC_ADD_CAR);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Pass results to MainActivity and finish();
        if (data != null) {
            setResult(resultCode, data);
            finish();
        }
    }

    //Go to home screen if back is pressed
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
