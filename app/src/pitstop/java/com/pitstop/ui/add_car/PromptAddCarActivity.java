package com.pitstop.ui.add_car;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.pitstop.R;

import static com.pitstop.ui.MainActivity.RC_ADD_CAR;

public class PromptAddCarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt_add_car);

        //Change toolbar
        getSupportActionBar().setTitle("Add First Car");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    public void startAddCarActivity(View view) {
        Intent intent = new Intent(PromptAddCarActivity.this, AddCarActivity.class);
        startActivityForResult(intent, RC_ADD_CAR);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
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
