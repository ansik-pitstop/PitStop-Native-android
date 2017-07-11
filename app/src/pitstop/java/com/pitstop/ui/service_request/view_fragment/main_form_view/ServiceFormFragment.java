package com.pitstop.ui.service_request.view_fragment.main_form_view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.pitstop.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matthew on 2017-07-11.
 */

public class ServiceFormFragment extends Fragment implements ServiceFormView{

    @BindView(R.id.addition_comments)
    EditText additionalComments;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_form, container, false);
        ButterKnife.bind(this,view);

        additionalComments.setFocusableInTouchMode(true);

        return view;
    }
}
