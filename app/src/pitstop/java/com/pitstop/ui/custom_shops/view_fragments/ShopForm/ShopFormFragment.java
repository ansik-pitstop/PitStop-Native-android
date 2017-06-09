package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

/**
 * Created by xirax on 2017-06-09.
 */

public class ShopFormFragment extends Fragment implements ShopFormInterface {
    private ShopFormPresenter presenter;
    private FragmentSwitcherInterface switcher;

    @Override
    public void setSwitcher(FragmentSwitcherInterface switcher) {
        this.switcher = switcher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_form, container, false);
        presenter = new ShopFormPresenter();
        presenter.subscribe(this,switcher);

        return view;
    }
}
