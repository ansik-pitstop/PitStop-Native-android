package com.pitstop.ui.add_car;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.LoginActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;

public class PromptAddCarActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.logout_button)
    Button logoutButton;

    @BindView(R.id.support_button)
    TextView supportButton;

    private UseCaseComponent useCaseComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt_add_car);
        ButterKnife.bind(this);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        AppCompatActivity activity = this;

        supportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversationActivity.show(getApplicationContext(),Intent.FLAG_ACTIVITY_NEW_TASK);
            }
        });
        supportButton.setPaintFlags(supportButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

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

    //Maybe the car was added on the back-end or logic error somewhere
    private void checkCarWasAdded(){
        Log.d(TAG,"checkCarWasAdded()");
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership) {
                finish();
            }

            @Override
            public void onNoCarSet() {
                //Everything is just right
            }

            @Override
            public void onError(RequestError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");
        //Check if car was added, after waiting a second for onActivityResult() to be processed
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCarWasAdded();
            }
        }, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG,"onActivityResult, data is null? "+(data == null));

        //Pass results to MainActivity and finish();
        if (data != null && requestCode == RC_ADD_CAR) {
            setResult(resultCode, data);
            finish();
        }
        //Double check whether car was somehow still added
        else{
            checkCarWasAdded();
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
