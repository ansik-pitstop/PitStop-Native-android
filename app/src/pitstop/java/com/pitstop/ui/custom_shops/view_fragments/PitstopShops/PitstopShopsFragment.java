package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by xirax on 2017-06-08.
 */

public class PitstopShopsFragment extends Fragment implements PitstopShopsInterface {


    private FragmentSwitcherInterface switcher;
    private PitstopShopsPresenter presenter;


    @Override
    public void setSwitcher(FragmentSwitcherInterface switcher) {
        this.switcher = switcher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pitstop_shops, container, false);
        ButterKnife.bind(this,view);
        presenter = new PitstopShopsPresenter();
        presenter.subscribe(this,switcher);
        return view;
    }
}
