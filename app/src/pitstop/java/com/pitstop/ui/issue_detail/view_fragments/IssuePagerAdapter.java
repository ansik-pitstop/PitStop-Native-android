package com.pitstop.ui.issue_detail.view_fragments;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.utils.DateTimeFormatUtil;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;


public class IssuePagerAdapter extends PagerAdapter {

    private List<CarIssue> mIssues;
    private final LayoutInflater mLayoutInflater;
    private WeakReference<Context> contextReference;
//    private final Context mContext;

    public IssuePagerAdapter(Context context, List<CarIssue> issues) {
        contextReference= new WeakReference<>(context);
        mIssues         = issues;
//        mContext        = context;
        mLayoutInflater = LayoutInflater.from(contextReference.get());
    }

    public IssuePagerAdapter(Context context, Set<CarIssue> issueSet){
        contextReference= new WeakReference<>(context);
        mIssues         = new ArrayList<>();
//        mContext        = context;
        mLayoutInflater = LayoutInflater.from(contextReference.get());
        mIssues.addAll(issueSet);
    }

    @Override
    public int getCount() {
        if (mIssues == null) return 0;
        return mIssues.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View rootView = mLayoutInflater.inflate(R.layout.activity_issue_details_item, container, false);
        setupView(rootView, mIssues.get(position));
        container.addView(rootView);
        return rootView;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    private void setupView(View rootView, CarIssue carIssue){
        if (contextReference.get() == null) return;
        Context context = contextReference.get();

        String title        = carIssue.getAction() + " " + carIssue.getItem();
        String description  = carIssue.getDescription();
        String symptoms     = carIssue.getSymptoms();
        String causes       = carIssue.getCauses();
        int severity        = carIssue.getPriority();
        String finishAt     = carIssue.getDoneAt();

        ((TextView)rootView.findViewById(R.id.issue_title)).setText(title);

        if (finishAt != null && !finishAt.isEmpty()){
            try{
                Calendar calendar = DateTimeFormatUtil.formatISO8601ToCalendar(finishAt);
                rootView.findViewById(R.id.history_layout).setVisibility(View.VISIBLE);
                ((TextView)rootView.findViewById(R.id.history))
                        .setText(DateTimeFormatUtil.formatToReadableDate(calendar) + " " + DateTimeFormatUtil.formatToReadableTime(calendar));
            } catch (ParseException e){
                e.printStackTrace();
            }
        }

        if (description != null && !description.isEmpty()){
            rootView.findViewById(R.id.description_layout).setVisibility(View.VISIBLE);
            ((TextView)rootView.findViewById(R.id.description)).setText(description);
        }

        if (symptoms != null && !symptoms.isEmpty()){
            rootView.findViewById(R.id.symptoms_layout).setVisibility(View.VISIBLE);
            String[] symptomArr = symptoms.split("--");
            StringBuilder builder = new StringBuilder();
            for (String symptom: symptomArr){
                builder.append(symptom.trim()).append("\n");
            }
            builder.setLength(builder.length() - 1); // get rid of the last newline
            ((TextView)rootView.findViewById(R.id.symptoms)).setText(builder.toString());
        }

        if (causes != null && !causes.isEmpty()){
            rootView.findViewById(R.id.causes_layout).setVisibility(View.VISIBLE);
            String[] causeArr = causes.split("--");
            StringBuilder builder = new StringBuilder();
            for (String cause: causeArr){
                builder.append(cause.trim()).append("\n");
            }
            builder.setLength(builder.length() - 1); // get rid of the last newline
            ((TextView)rootView.findViewById(R.id.causes)).setText(builder.toString());
        }

        RelativeLayout severityLayout = (RelativeLayout)rootView.findViewById(R.id.severity_indicator_layout);
        TextView severityTV = (TextView)rootView.findViewById(R.id.severity_text);

        switch (severity) {
            case 1:
                severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_low_indicator));
                severityTV.setText(context.getResources().getString(R.string.severity_indicator_low));
                break;
            case 2:
                severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_medium_indicator));
                severityTV.setText(context.getResources().getString(R.string.severity_indicator_medium));
                break;
//            case 3:
//                severityLayout.setBackground(ContextCompat.getDrawable(mContext, R.drawable.severity_high_indicator));
//                severityTV.setText(mContext.getResources().getString(R.string.severity_indicator_high);
//                break;
            default:
                severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_critical_indicator));
                severityTV.setText(context.getResources().getString(R.string.severity_indicator_critical));
                break;
        }

        if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)){
            rootView.findViewById(R.id.issue_pending_hint).setVisibility(View.VISIBLE);
            severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_low_indicator));
            severityTV.setText(context.getResources().getStringArray(R.array.severity_indicators)[0]);
        } else {
            rootView.findViewById(R.id.issue_pending_hint).setVisibility(View.GONE);
        }

    }

}
