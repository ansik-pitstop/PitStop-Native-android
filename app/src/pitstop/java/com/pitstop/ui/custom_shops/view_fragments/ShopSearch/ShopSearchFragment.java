package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.pitstop.R;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchFragment extends Fragment implements ShopSearchInterface {
    private ShopSearchPresenter presenter;
    private FragmentSwitcherInterface switcher;
    @BindView(R.id.search_bar)
    SearchView searchBar;

    @BindView(R.id.add_own_button)
    CardView addOwnButton;

    @Override
    public void setSwitcher(FragmentSwitcherInterface switcher) {
        this.switcher = switcher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_search, container, false);
        ButterKnife.bind(this,view);
        addOwnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setViewShopForm();
            }
        });
        presenter = new ShopSearchPresenter();
        presenter.subscribe(this,switcher);
        presenter.focusSearch();
        return view;
    }

    @Override
    public void focusSearch() {
        searchBar.onActionViewExpanded();
    }
}

