package com.pitstop.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.pitstop.ui.MainActivity;

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
        Button testButton = (Button) view.findViewById(R.id.testStatus);

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
            if(title != null) {
                if (position == 0) { // hardcoded button text
                    testButton.setText("Disconnect");
                } else if (position == testActions.size() - 1) {
                    testButton.setText("Reset");
                } else if (position == 1) {
                    testButton.setText("Start");
                }
            }
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
        Intent testBroadcast = new Intent(MainActivity.TEST_ACTION_ACTION);
        testBroadcast.putExtra(MainActivity.TEST_ACTION_TYPE, type);
        context.sendBroadcast(testBroadcast);
    }

    @Override
    public void notifyDataSetChanged() {
        mViews = new ArrayList<>();

        for (int i = 0 ; i < testActions.size() ; i++) {
            mViews.add(null);
        }
        super.notifyDataSetChanged();
    }

    public void updateItem(boolean success, int position) {
        CardView cardView = getCardViewAt(position);
        if(cardView != null) {
            Button statusIndicator = (Button) cardView.findViewById(R.id.testStatus);
            statusIndicator.setVisibility(View.VISIBLE);
            statusIndicator.setText(success ? "Success" : "Error");
            statusIndicator.setBackground(context.getResources()
                    .getDrawable(success ? R.drawable.shape_button_highlight : R.drawable.shape_button_red));
        }
    }
}
