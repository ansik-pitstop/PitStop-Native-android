package com.pitstop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(getActivity().getResources().getColor(R.color.highlight)));
        String[] cars = new String[]{
                "honda civics","nissan rogue","mitsubushi rogue"
        };
        for(final String s: cars) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final LinearLayout itemBox = (LinearLayout)inflater.inflate(R.layout.car_button, null);
            ((TextView) itemBox.findViewById(R.id.car_title)).setText(s);
            itemBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                    intent.putExtra("title", s);
                    startActivity(intent);
                }
            });
            itemBox.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.button_width), getResources().getDimensionPixelSize(R.dimen.button_height));
            params.rightMargin=getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).addView(itemBox, 0, params);
        }
    }
}
