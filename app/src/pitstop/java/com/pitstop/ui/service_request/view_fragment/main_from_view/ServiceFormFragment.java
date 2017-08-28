package com.pitstop.ui.service_request.view_fragment.main_from_view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.service_request.RequestServiceCallback;
import com.pitstop.utils.MixpanelHelper;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matthew on 2017-07-11.
 */

public class ServiceFormFragment extends Fragment implements ServiceFormView{

    public static final String STATE_TENTATIVE = "tentative";
    public static final String STATE_REQUESTED = "requested";

    @BindView(R.id.service_scroll)
    ScrollView scrollView;

    @BindView(R.id.addition_comments)
    EditText additionalComments;

    @BindView(R.id.service_date_text)
    TextView dateText;

    @BindView(R.id.service_date_button)
    LinearLayout dateButton;

    @BindView(R.id.service_time_button)
    LinearLayout timeButton;

    @BindView(R.id.service_shop_name)
    TextView shopName;

    @BindView(R.id.service_shop_address)
    TextView shopAddress;

    @BindView(R.id.service_calender)
    MaterialCalendarView calendarView;

    @BindView(R.id.service_time_list)
    RecyclerView timeList;

    @BindView(R.id.service_time_list_holder)
    RelativeLayout timeListHolder;

    @BindView(R.id.service_list)
    RecyclerView serviceList;

    @BindView(R.id.service_list_holder)
    RelativeLayout serviceListHolder;

    @BindView(R.id.service_add_button)
    LinearLayout addButton;

    @BindView(R.id.service_time_text)
    TextView timeText;

    @BindView(R.id.service_chosen_list)
    RecyclerView serviceChosenList;

    @BindView(R.id.service_submit)
    Button submitButton;

    @BindView(R.id.service_time_loading)
    ProgressBar timeLoading;

    private ServiceFormPresenter presenter;

    private RequestServiceCallback callback;

    private Car dashCar;

    private TimeAdapter timeAdapter;

    private Context context;
    private GlobalApplication application;

    private IssueAdapter serviceAdapter;

    private IssueAdapter serviceChosenAdapter;

    public void setActivityCallback(RequestServiceCallback callback){
     this.callback = callback;
    }

    public void setCar(Car car){
        this.dashCar = car;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication)context;
        View view = inflater.inflate(R.layout.fragment_service_form, container, false);
        ButterKnife.bind(this,view);
        calendarView.setVisibility(View.GONE);
        timeListHolder.setVisibility(View.GONE);
        serviceListHolder.setVisibility(View.GONE);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        Calendar calendar = Calendar.getInstance();
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                presenter.dateSelected(date.getYear(),date.getMonth()+1,date.getDay(),calendarView);//month is 0 based
            }
        });

        MixpanelHelper mixpanelHelper = new MixpanelHelper(application);

        presenter = new ServiceFormPresenter(callback,component,mixpanelHelper,dashCar);

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.timeButtonClicked();
            }
        });
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.dateButtonClicked();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.addButtonClicked();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onSubmitClicked();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);
        Calendar calendar = Calendar.getInstance();
        calendarView.state().edit().setMinimumDate(calendar).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public String getComments() {
        return additionalComments.getText().toString();
    }

    @Override
    public void showReminder(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setTitle("Reminder");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void showLoading(boolean show) {
        if(show){
            timeLoading.setVisibility(View.VISIBLE);
        }else{
            timeLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }

    @Override
    public  List<CarIssue> getPresetList() {
        List<CarIssue> presetIssues = new ArrayList<>();
        presetIssues.add(new CarIssue.Builder()
                .setId(4)
                .setAction(context.getString(R.string.preset_issue_service_emergency))
                .setItem(context.getString(R.string.preset_issue_item_tow_truck))
                .setIssueType(CarIssue.TYPE_PRESET)
                .setDescription(context.getString(R.string.tow_truck_description))
                .setPriority(5).build());
        presetIssues.add(new CarIssue.Builder()
                .setId(1)
                .setAction(context.getString(R.string.preset_issue_service_emergency))
                .setItem(context.getString(R.string.preset_issue_item_flat_tire))
                .setIssueType(CarIssue.TYPE_PRESET)
                .setDescription(context.getString(R.string.flat_tire_description))
                .setPriority(5).build());
        presetIssues.add(new CarIssue.Builder()
                .setId(2)
                .setAction(context.getString(R.string.preset_issue_service_replace))
                .setItem(context.getString(R.string.preset_issue_item_engine_oil_filter))
                .setIssueType(CarIssue.TYPE_PRESET)
                .setDescription(context.getString(R.string.engine_oil_filter_description))
                .setPriority(3).build());
        presetIssues.add(new CarIssue.Builder()
                .setId(3)
                .setAction(context.getString(R.string.preset_issue_service_replace))
                .setItem(context.getString(R.string.preset_issue_item_wipers_fluids))
                .setIssueType(CarIssue.TYPE_PRESET)
                .setDescription(context.getString(R.string.wipers_fluids_description))
                .setPriority(2).build());
        presetIssues.add(new CarIssue.Builder()
                .setId(5)
                .setAction(context.getString(R.string.preset_issue_service_request))
                .setItem(context.getString(R.string.preset_issue_item_shuttle_service))
                .setIssueType(CarIssue.TYPE_PRESET)
                .setDescription(context.getString(R.string.shuttle_service_description))
                .setPriority(3).build());
        return presetIssues;
    }

    @Override
    public void setupTimeList(List<String> times) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        timeAdapter = new TimeAdapter(times,presenter);
        timeList.setAdapter(timeAdapter);
        timeList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void setupPresetIssues(List<CarIssue> issues) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        serviceAdapter = new IssueAdapter(issues,false,presenter,context);
        serviceList.setAdapter(serviceAdapter);
        serviceList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void setupSelectedIssues(List<CarIssue> issues) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        serviceChosenAdapter= new IssueAdapter(issues,true,presenter,context);
        serviceChosenList.setAdapter(serviceChosenAdapter);
        serviceChosenList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void showShop(String name,String address) {
        shopName.setText(name);
        shopAddress.setText(address);
    }

    @Override
    public void setCommentHint(String hint) {
        additionalComments.setHint(hint);
    }

    @Override
    public void hideTimeList() {
        timeListHolder.setVisibility(View.GONE);
    }

    @Override
    public void hideCalender() {
        calendarView.setVisibility(View.GONE);
    }

    @Override
    public void disableButton(boolean disable) {
            submitButton.setClickable(!disable);
    }

    @Override
    public void showCalender() {
        calendarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showDate(String date) {
        dateText.setText(date);
    }

    @Override
    public void showTime(String time) {
        timeText.setText(time);
    }

    @Override
    public void toggleTimeList() {
        if(timeListHolder.getVisibility()== View.GONE){
            timeListHolder.setVisibility(View.VISIBLE);
        }else{
            timeListHolder.setVisibility(View.GONE);
        }
    }

    @Override
    public void toggleCalender() {
        if(calendarView.getVisibility()== View.GONE){
            calendarView.setVisibility(View.VISIBLE);
        }else{
            calendarView.setVisibility(View.GONE);
        }
    }

    @Override
    public void toggleServiceList() {
        if(serviceListHolder.getVisibility() == View.GONE){
            serviceListHolder.setVisibility(View.VISIBLE);
        }else{
            serviceListHolder.setVisibility(View.GONE);
        }
    }
}

