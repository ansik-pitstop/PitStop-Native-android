package com.pitstop.ui.custom_shops.view_fragments.ShopSearch;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.custom_shops.ShopAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by matt on 2017-06-08.
 */

public class ShopSearchFragment extends Fragment implements ShopSearchInterface {
    private ShopSearchPresenter presenter;
    private FragmentSwitcherInterface switcher;

    private Context context;
    private GlobalApplication application;

    private ShopAdapter shopAdapter;

    @BindView(R.id.my_shops_list)
    RecyclerView myShopsList;
    @BindView(R.id.pitstop_search_list)
    RecyclerView pitstopShops;
    @BindView(R.id.search_results_list)
    RecyclerView searchResults;


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
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_shop_search, container, false);
        ButterKnife.bind(this,view);
        addOwnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setViewShopForm(null);
            }
        });
        presenter = new ShopSearchPresenter();
        presenter.subscribe(this,switcher);
        presenter.focusSearch();
        presenter.getMyShops();
        return view;
    }

    @Override
    public void focusSearch() {
        searchBar.onActionViewExpanded();
    }

    @Override
    public void setUpMyShopsList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        myShopsList.setAdapter(shopAdapter);
        myShopsList.setLayoutManager(linearLayoutManager);

    }

    @Override
    public void setUpPitstopList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        pitstopShops.setAdapter(shopAdapter);
        pitstopShops.setLayoutManager(linearLayoutManager);

    }

    @Override
    public void setUpSearchList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);
        searchResults.setAdapter(shopAdapter);
        searchResults.setLayoutManager(linearLayoutManager);
    }
}

