package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by DavidIsDum on 1/30/2016.
 */
public class CarHistoryActivity extends AppCompatActivity {
    private CustomAdapter customAdapter;
    private RecyclerView mRecyclerView;
    private CardView messageCard;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private Car dashboardCar;

    private static final String TAG = CarHistoryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_history);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        messageCard = (CardView) findViewById(R.id.message_card);
        // set up listview
        mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        dashboardCar = (Car) getIntent().getParcelableExtra("dashboardCar");

        CarIssue[] doneIssues = dashboardCar.getDoneIssues().toArray(new CarIssue[dashboardCar.getDoneIssues().size()]);

        Arrays.sort(doneIssues, new Comparator<CarIssue>() {
            @Override
            public int compare(CarIssue lhs, CarIssue rhs) {
                return getDateToCompare(rhs.getTimestamp()) - getDateToCompare(lhs.getTimestamp());
            }
        });

        ArrayList<CarIssue> doneIssuesList = new ArrayList<>(Arrays.asList(doneIssues));

        if(doneIssuesList.isEmpty()) {
            messageCard.setVisibility(View.VISIBLE);
        }

        customAdapter = new CustomAdapter(doneIssuesList);
        mRecyclerView.setAdapter(customAdapter);

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id ==  android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adapter for listview
     */
    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder>{

        ArrayList<CarIssue> doneIssues;

        public CustomAdapter(ArrayList<CarIssue> doneIssues){
            this.doneIssues = new ArrayList<>(doneIssues);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_details_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.desc.setText(doneIssues.get(position).getIssueDetail().getDescription());
            if(doneIssues.get(position).getTimestamp() == null || doneIssues.get(position).getTimestamp().equals("null")) {
                holder.date.setText("Done");
            } else {
                holder.date.setText(String.format("Done on %s", formatDate(doneIssues.get(position).getTimestamp())));
            }

            if(doneIssues.get(position).getIssueType().equals(CarIssue.RECALL)) {
                holder.title.setText(doneIssues.get(position).getIssueDetail().getItem());
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_red_600_24dp));

            } else if(doneIssues.get(position).getIssueType().equals(CarIssue.DTC)) {
                holder.title.setText(String.format("Engine issue: Code %s", doneIssues.get(position).getIssueDetail().getItem()));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.car_engine_red));

            } else if(doneIssues.get(position).getIssueType().equals(CarIssue.PENDING_DTC)) {
                holder.title.setText(String.format("Potential engine issue: Code %s", doneIssues.get(position).getIssueDetail().getItem()));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.car_engine_yellow));

            } else {
                holder.title.setText(doneIssues.get(position).getIssueDetail().getItem());
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }

        }

        @Override
        public int getItemCount() {
            return doneIssues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView desc;
            TextView date;
            ImageView imageView;
            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView)itemView.findViewById(R.id.title);
                desc = (TextView)itemView.findViewById(R.id.description);
                date = (TextView)itemView.findViewById(R.id.date);
                imageView = (ImageView) itemView.findViewById(R.id.image_icon);
            }
        }
    }

    private String formatDate(String rawDate) {
        String[] splittedDate = rawDate.split("-");
        String[] months = new String[] {"null", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};

        splittedDate[2] = splittedDate[2].substring(0, 2);

        return months[Integer.parseInt(splittedDate[1])] + ". " + splittedDate[2] + ", " + splittedDate[0];
    }

    private int getDateToCompare(String rawDate) {
        if(rawDate == null || rawDate.isEmpty() || rawDate.equals("null")) {
            return 0;
        }

        String[] splittedDate = rawDate.split("-");
        splittedDate[2] = splittedDate[2].substring(0, 2);

        return Integer.parseInt(splittedDate[2])
                + Integer.parseInt(splittedDate[1]) * 30
                + Integer.parseInt(splittedDate[0]) * 365;
    }

}
