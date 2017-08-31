package com.pitstop.ui.services.custom_service.view_fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.service.CustomIssueListItem;
import com.pitstop.ui.services.ServicesDatePickerDialog;
import com.pitstop.ui.services.custom_service.CustomServiceActivityCallback;
import com.pitstop.utils.MixpanelHelper;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-07-25.
 */

public class ServiceFormFragment extends Fragment implements ServiceFormView {
    @BindView(R.id.service_description_text)
    EditText descriptionText;
    @BindView(R.id.service_description)
    TextView serviceDescription;

    @BindView(R.id.service_action_button)
    RelativeLayout actionButton;
    @BindView(R.id.service_action_list)
    RecyclerView actionList;
    @BindView(R.id.service_action_text)
    EditText actionText;

    @BindView(R.id.service_part_name_button)
    RelativeLayout partNameButton;
    @BindView(R.id.service_part_name_list)
    RecyclerView partNameList;
    @BindView(R.id.service_part_name_text)
    EditText partNameText;

    @BindView(R.id.service_priority_button)
    RelativeLayout priorityButton;
    @BindView(R.id.service_priority_list)
    RecyclerView priorityList;
    @BindView(R.id.service_priority_text)
    EditText priorityText;

    @BindView(R.id.service_create_button)
    Button createButton;

    @BindView(R.id.service_scroll_view)
    ScrollView scrollView;

    private ServiceFormPresenter presenter;

    private Context context;
    private GlobalApplication application;

    private InputMethodManager imm;

    private CustomServiceListAdapter actionAdapter;
    private CustomServiceListAdapter partNameAdapter;
    private CustomServiceListAdapter priorityAdapter;

    private KeyListener actionTextListener;
    private KeyListener partNameTextListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_custom_services,container,false);
        ButterKnife.bind(this,view);
        actionTextListener = actionText.getKeyListener();
        partNameTextListener = partNameText.getKeyListener();
        actionList.setNestedScrollingEnabled(false);
        partNameList.setNestedScrollingEnabled(false);
        priorityList.setNestedScrollingEnabled(false);
        imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        MixpanelHelper mixpanelHelper = new MixpanelHelper(application);

        presenter = new ServiceFormPresenter(component, (CustomServiceActivityCallback)getActivity(),mixpanelHelper);
        priorityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onPriorityClicked();
            }
        });
        partNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onPartNameClicked();
            }
        });
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onActionClicked();
            }
        });
        descriptionText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() == 0){
                    serviceDescription.setText("Description");
                }else{
                    serviceDescription.setText("");
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onCreateButton();
            }
        });
        return view;
    }

    @Override
    public void toggleActionList() {
        if(actionList.getVisibility() == View.VISIBLE){
            actionList.setVisibility(View.GONE);
        }else{
            actionList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setActionList(List<CustomIssueListItem> items) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        actionAdapter = new CustomServiceListAdapter(items,presenter);
        actionList.setAdapter(actionAdapter);
        actionList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void togglePartNameList() {
        if(partNameList.getVisibility() == View.VISIBLE){
            partNameList.setVisibility(View.GONE);
        }else{
            partNameList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setParNameList(List<CustomIssueListItem> items) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        partNameAdapter = new CustomServiceListAdapter(items,presenter);
        partNameList.setAdapter(partNameAdapter);
        partNameList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void togglePriorityList() {
        if(priorityList.getVisibility() == View.VISIBLE){
            priorityList.setVisibility(View.GONE);
        }else{
            priorityList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setPriorityList(List<CustomIssueListItem> items) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        priorityAdapter = new CustomServiceListAdapter(items,presenter);
        priorityList.setAdapter(priorityAdapter);
        priorityList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void showActionText(CustomIssueListItem item) {
        actionText.setText(item.getText());
        actionText.setKeyListener(null);
        actionText.setTextColor(Color.parseColor(item.getCardColor()));
        actionText.setVisibility(View.VISIBLE);
    }

    @Override
    public void showPartNameText(CustomIssueListItem item) {
        partNameText.setText(item.getText());
        partNameText.setKeyListener(null);
        partNameText.setTextColor(Color.parseColor(item.getCardColor()));
        partNameText.setVisibility(View.VISIBLE);
    }

    @Override
    public void showPriorityText(CustomIssueListItem item) {
        priorityText.setText(item.getText());
        priorityText.setKeyListener(null);
        priorityText.setTextColor(Color.parseColor(item.getCardColor()));
        priorityText.setVisibility(View.VISIBLE);
    }

    @Override
    public void showPartNameText() {//this is the other option
        partNameText.setVisibility(View.VISIBLE);
        partNameText.setText("");
        partNameText.setKeyListener(partNameTextListener);
        partNameText.setTextColor(ContextCompat.getColor(getActivity(), R.color.label_text_dark));
        partNameText.requestFocus();
        imm.showSoftInput(partNameText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void showActionText() {//this is the other option
        actionText.setVisibility(View.VISIBLE);
        actionText.setText("");
        actionText.setKeyListener(actionTextListener);
        actionText.setTextColor(ContextCompat.getColor(getActivity(), R.color.label_text_dark));
        actionText.requestFocus();
        imm.showSoftInput(actionText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void disableCreateButton(boolean enabled) {
        createButton.setEnabled(enabled);
    }

    @Override
    public String getAction() {
        return actionText.getText().toString();
    }

    @Override
    public String getPriority() {
        return priorityText.getText().toString();
    }

    @Override
    public String getDescription() {
        return descriptionText.getText().toString();
    }

    @Override
    public String getPartName() {
        return partNameText.getText().toString();
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
    public void showDatePicker(CarIssue issue) {
        ServicesDatePickerDialog datePickerDialog = new ServicesDatePickerDialog(getActivity()
                , Calendar.getInstance(), (datePicker, i, i1, i2) -> {// year month day
                    presenter.datePicked(issue, i,i1,i2);
                });
        datePickerDialog.setTitle("Select when you completed this service.");
        datePickerDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }


}
