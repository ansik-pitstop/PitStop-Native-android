package com.pitstop.ui.service_request.view_fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.CarIssue;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AddCustomIssueDialog extends DialogFragment {

    private static final String TAG = AddCustomIssueDialog.class.getSimpleName();

    private MixpanelHelper mixpanelHelper;
    private Context context;
    private CustomIssueCallback callback;

    private List<CarIssue> pickedIssues;

    public static AddCustomIssueDialog newInstance(Context context, List<CarIssue> pickedIssues) {
        Bundle args = new Bundle();

        AddCustomIssueDialog fragment = new AddCustomIssueDialog();
        fragment.setArguments(args);
        fragment.setContext(context);
        fragment.setMixpanelHelper(new MixpanelHelper((GlobalApplication) context.getApplicationContext()));
        fragment.setPickedIssues(pickedIssues);
        return fragment;
    }

    public void setMixpanelHelper(MixpanelHelper mixpanelHelper) {
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setCallback(CustomIssueCallback callback){
        this.callback = callback;
    }

    public void setPickedIssues(List<CarIssue> pickedIssues) {
        this.pickedIssues = pickedIssues;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogList = LayoutInflater.from(context).inflate(R.layout.dialog_add_preset_issue_list, null);
        final RecyclerView list = (RecyclerView) dialogList.findViewById(R.id.dialog_add_preset_issue_recycler_view);
        final IssueAdapter adapter = new IssueAdapter(pickedIssues);
        list.setAdapter(adapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(linearLayoutManager);
        list.setHasFixedSize(true);

        final AlertDialog requestIssueDialog = new AnimatedDialogBuilder(context)
                .setTitle(context.getString(R.string.service_request_dialog_prompt_title))
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setView(dialogList)
                .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_PRESET_ISSUE_CONFIRM, MixpanelHelper.DASHBOARD_VIEW);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        List<CarIssue> pickedIssues = adapter.getPickedIssues();
                        Log.d(TAG, "Confirm pickedIssues: " + pickedIssues.size());
                        if (callback != null) callback.onCustomIssueSelected(pickedIssues);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .create();

        requestIssueDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_PRESET_ISSUE_CANCEL, MixpanelHelper.DASHBOARD_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        return requestIssueDialog;
    }


    public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

        private List<CarIssue> mPresetIssues;
        private List<CarIssue> mPickedIssues;

        private void populateContent() {
            mPresetIssues = new ArrayList<>();
            mPresetIssues.add(new CarIssue.Builder()
                    .setId(4)
                    .setAction(context.getString(R.string.preset_issue_service_emergency))
                    .setItem(context.getString(R.string.preset_issue_item_tow_truck))
                    .setIssueType(CarIssue.TYPE_PRESET)
                    .setDescription(context.getString(R.string.tow_truck_description))
                    .setPriority(5).build());
            mPresetIssues.add(new CarIssue.Builder()
                    .setId(1)
                    .setAction(context.getString(R.string.preset_issue_service_emergency))
                    .setItem(context.getString(R.string.preset_issue_item_flat_tire))
                    .setIssueType(CarIssue.TYPE_PRESET)
                    .setDescription(context.getString(R.string.flat_tire_description))
                    .setPriority(5).build());
            mPresetIssues.add(new CarIssue.Builder()
                    .setId(2)
                    .setAction(context.getString(R.string.preset_issue_service_replace))
                    .setItem(context.getString(R.string.preset_issue_item_engine_oil_filter))
                    .setIssueType(CarIssue.TYPE_PRESET)
                    .setDescription(context.getString(R.string.engine_oil_filter_description))
                    .setPriority(3).build());
            mPresetIssues.add(new CarIssue.Builder()
                    .setId(3)
                    .setAction(context.getString(R.string.preset_issue_service_replace))
                    .setItem(context.getString(R.string.preset_issue_item_wipers_fluids))
                    .setIssueType(CarIssue.TYPE_PRESET)
                    .setDescription(context.getString(R.string.wipers_fluids_description))
                    .setPriority(2).build());
            mPresetIssues.add(new CarIssue.Builder()
                    .setId(5)
                    .setAction(context.getString(R.string.preset_issue_service_request))
                    .setItem(context.getString(R.string.preset_issue_item_shuttle_service))
                    .setIssueType(CarIssue.TYPE_PRESET)
                    .setDescription(context.getString(R.string.shuttle_service_description))
                    .setPriority(3).build());
        }

        public IssueAdapter(List<CarIssue> pickedIssues) {
            populateContent();
            mPickedIssues = (pickedIssues != null ? pickedIssues : new ArrayList<CarIssue>());
        }

        @Override
        public IssueAdapter.IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_add_preset_issue_item, parent, false);
            return new IssueAdapter.IssueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final IssueAdapter.IssueViewHolder holder, final int position) {
            final CarIssue presetIssue = mPresetIssues.get(position);

            holder.description.setText(presetIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            holder.title.setText(String.format("%s %s", presetIssue.getAction(), presetIssue.getItem()));

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        showDetailDialog(presetIssue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        mixpanelHelper.trackButtonTapped("Detail: " + presetIssue.getAction() + " " + presetIssue.getItem(),
                                MixpanelHelper.DASHBOARD_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            for (int index = 0; index < mPickedIssues.size(); index++){
                if (presetIssue.getId() == mPickedIssues.get(index).getId()){
                    holder.checkBox.setChecked(true);
                }
            }

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mPickedIssues.add(presetIssue);
                    } else {
                        for (int index = 0; index < mPickedIssues.size(); index++){
                            if (presetIssue.getId() == mPickedIssues.get(index).getId()){
                                mPickedIssues.remove(index);
                            }
                        }
                    }

                    Log.d(TAG, "Picked size: " + mPickedIssues.size());

                    try {
                        String check = isChecked ? "Checked: " : "Unchecked: ";
                        mixpanelHelper.trackButtonTapped(check + presetIssue.getAction() + " " + presetIssue.getItem(),
                                MixpanelHelper.DASHBOARD_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            switch (presetIssue.getId()) {
                case 1:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_flat_tire_severe));
                    break;
                case 2:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_orange_48px));
                    break;
                case 3:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_yellow_48px));
                    break;
                case 4:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_tow_truck_severe));
                    break;
                case 5:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
                default:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
            }
        }



        @Override
        public int getItemCount() {
            return mPresetIssues.size();
        }

        public List<CarIssue> getPickedIssues() {
            return mPickedIssues;
        }

        public class IssueViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public CheckBox checkBox;
            public View container;

            public IssueViewHolder(View itemView) {
                super(itemView);
                checkBox = (CheckBox) itemView.findViewById(R.id.dialog_preset_issue_list_checkbox);
                title = (TextView) itemView.findViewById(R.id.title);
                description = (TextView) itemView.findViewById(R.id.description);
                imageView = (ImageView) itemView.findViewById(R.id.image_icon);
                container = itemView.findViewById(R.id.list_car_item);
            }
        }
    }


    /**
     * @param data Chosen preset issue
     */
    private void showDetailDialog(CarIssue data) {
        if (data == null) return;

        final View dialogDetail = LayoutInflater.from(context).inflate(R.layout.dialog_add_preset_issue_detail, null);
        final String title = data.getAction() + " " + data.getItem();
        final String description = data.getDescription();
        final int severity = data.getPriority();

        ((TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_title_text)).setText(title);
        ((TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_description)).setText(description);

        RelativeLayout rLayout = (RelativeLayout) dialogDetail.findViewById(R.id.dialog_preset_issue_severity_indicator_layout);
        TextView severityTextView = (TextView) dialogDetail.findViewById(R.id.dialog_preset_issue_severity_text);

        switch (severity) {
            case 1:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_low_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[0]);
                break;
            case 2:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_medium_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[1]);
                break;
            case 3:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_high_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[2]);
                break;
            default:
                rLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_critical_indicator));
                severityTextView.setText(context.getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }

        final AlertDialog d = new AnimatedDialogBuilder(context)
                .setTitle(context.getString(R.string.add_preset_issue_dialog_detail_title))
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setView(dialogDetail)
                .setPositiveButton("OK", null)
                .create();

        d.show();
    }

    public interface CustomIssueCallback{
        void onCustomIssueSelected(final List<CarIssue> selectedIssues);
    }

}
