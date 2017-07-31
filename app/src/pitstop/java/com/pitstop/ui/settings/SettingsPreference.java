package com.pitstop.ui.settings;

import android.content.Context;
import android.preference.Preference;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.ui.add_car.AddCarContract;

/**
 * Created by Matt on 2017-07-31.
 */

public class SettingsPreference extends Preference {
    private String title;
    private String info;
    private boolean checkMark;


    public SettingsPreference(Context context,String title, String info, boolean checkMark){
        super(context);
        this.setLayoutResource(R.layout.list_item_settings);
        this.title = title;
        this.info = info;
        this.checkMark = checkMark;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ((TextView)view.findViewById(R.id.setting_title)).setText(title);
        ((TextView)view.findViewById(R.id.setting_info)).setText(info);
        if(checkMark){
           ((ImageView)view.findViewById(R.id.setting_check)).setVisibility(View.VISIBLE);
        }
    }

}
