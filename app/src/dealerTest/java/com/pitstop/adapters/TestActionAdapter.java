package com.pitstop.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.models.TestAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ben Wu on 2016-09-15.
 */
public class TestActionAdapter extends PagerAdapter {

    public static int MAX_ELEVATION_FACTOR = 8;

    private List<CardView> mViews;
    private float mBaseElevation;
    private Context context;

    private List<TestAction> testActions;

    public TestActionAdapter(Context context, List<TestAction> testActions) {
        this.context = context;
        this.testActions = testActions;
        mViews = new ArrayList<>();

        for (int i = 0 ; i < testActions.size() ; i++) {
            mViews.add(null);
        }
    }

    public float getBaseElevation() {
        return mBaseElevation;
    }

    public CardView getCardViewAt(int position) {
        return mViews.get(position);
    }

    @Override
    public int getCount() {
        return testActions.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(container.getContext()).inflate(R.layout.item_test_action, container, false);
        container.addView(view);
        CardView cardView = (CardView) view.findViewById(R.id.cardView);

        if (mBaseElevation == 0) {
            mBaseElevation = cardView.getCardElevation();
        }

        TextView title = (TextView) view.findViewById(R.id.cardTitle);
        TextView description = (TextView) view.findViewById(R.id.cardDescription);
        Button testButton = (Button) view.findViewById(R.id.testButton);

        final TestAction action = testActions.get(position);

        if(title != null) {
            title.setText(action.title);
        }
        if(description != null) {
            description.setText(action.description);
        }
        if(testButton != null) {
            testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doTest(action.type);
                }
            });
        }

        cardView.setMaxCardElevation(mBaseElevation * MAX_ELEVATION_FACTOR);
        mViews.set(position, cardView);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        mViews.set(position, null);
    }

    private void doTest(TestAction.Type type) {
        switch (type) {
            case CONNECT:
                Toast.makeText(context, "Connect", Toast.LENGTH_SHORT).show();
                break;
            case CHECK_TIME:
                Toast.makeText(context, "Check Time", Toast.LENGTH_SHORT).show();
                break;
            case PID:
                Toast.makeText(context, "Sensor Data", Toast.LENGTH_SHORT).show();
                break;
            case DTC:
                Toast.makeText(context, "Engine Codes", Toast.LENGTH_SHORT).show();
                break;
            case VIN:
                Toast.makeText(context, "VIN", Toast.LENGTH_SHORT).show();
                break;
            case RESET:
                Toast.makeText(context, "Reset", Toast.LENGTH_SHORT).show();
                break;
        }
    }

}
