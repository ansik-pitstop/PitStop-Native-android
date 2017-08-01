package com.pitstop.ui.add_car_old.view_fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.ui.add_car_old.AddCarPresenter;

/**
 * Created by David on 7/20/2016.
 *
 *
 */
public class AddCar2NoDongleFragment extends Fragment {

    private static final String TAG = AddCar2NoDongleFragment.class.getSimpleName();
    private ViewGroup rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_add_car_2_no_dongle, container, false);
        final Button searchButton= (Button) rootView.findViewById(R.id.add_vehicle);
        final Button scanButton= (Button) rootView.findViewById(R.id.scan_vin);
        final TextView scanButtonText= (TextView) rootView.findViewById(R.id.scan_vin_alternate_text);

        final EditText VINField = (EditText) rootView.findViewById(R.id.VIN);
        VINField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Editable vin = VINField.getText();

                String whitespaceRemoved = String.valueOf(vin);
                whitespaceRemoved = whitespaceRemoved.replace(" ", "").replace("\t", "")
                        .replace("\r", "").replace("\n", "");
                if (String.valueOf(vin).equals(whitespaceRemoved)) {
                    if (AddCarPresenter.isValidVin(vin.toString())) {
                        Log.i(TAG,"AfterTextChanged -- valid vin");
                        searchButton.setEnabled(true);
                        scanButton.setVisibility(View.GONE);
                        scanButtonText.setVisibility(View.GONE);
                        searchButton.setBackground(getResources().getDrawable(R.drawable.color_button_rectangle_highlight));
                    } else {
                        Log.i(TAG,"AfterTextChanged -- Vin not valid");
                        scanButton.setVisibility(View.VISIBLE);
                        scanButtonText.setVisibility(View.VISIBLE);
                        searchButton.setBackground(getResources().getDrawable(R.drawable.color_button_rectangle_grey));
                        searchButton.setEnabled(false);
                    }
                } else {
                    Log.i(TAG, "whitespace in VIN input removed. Original input: " + vin);
                    VINField.setText(whitespaceRemoved);
                }
            }
        });
        VINField.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            searchButton.performClick();
                            //When user taps Enter on the keyboard after typing their mileage and/or VIN in order to add car
                            return true;

                        case KeyEvent.KEYCODE_BACK:
                            //When user taps the screen to dismiss the keyboard
                            break;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        return rootView;
    }
}
