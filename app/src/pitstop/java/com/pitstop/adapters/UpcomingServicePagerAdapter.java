package com.pitstop.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.pitstop.R;
import com.pitstop.models.service.UpcomingService;

import java.util.List;

/**
 * Created by ishan on 2017-10-12.
 */

public class UpcomingServicePagerAdapter extends PagerAdapter {
    private List<UpcomingService> services;
    private final LayoutInflater layoutInflater;
    private Context context;

    public UpcomingServicePagerAdapter(List<UpcomingService> serviceList,Context context){
        this.services = serviceList;
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        if (services == null)return 0;
        return services.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View rootView = layoutInflater.inflate(R.layout.activity_issue_details_item, container, false);
        setupView(rootView, services.get(position));
        container.addView(rootView);
        return rootView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    public void setupView(View rootView, UpcomingService service){
        String title = service.getAction() + " " + service.getItem();
        String description  = service.getDescription();
        int severity  = service.getPriority();

        /*int mileage = service.getMileage();*/
        ((TextView)rootView.findViewById(R.id.issue_title)).setText(title);
        if (description != null){
            rootView.findViewById(R.id.description_layout).setVisibility(View.VISIBLE);
            ((TextView)rootView.findViewById(R.id.description)).setText(description);
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
          case 3:
                severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_high_indicator));
              severityTV.setText(context.getResources().getString(R.string.severity_indicator_high));
             break;
            default:
                severityLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.severity_medium_indicator));
                severityTV.setText(context.getResources().getString(R.string.severity_indicator_medium));
                break;

        }
        rootView.findViewById(R.id.issue_pending_hint).setVisibility(View.GONE);
    }
}
