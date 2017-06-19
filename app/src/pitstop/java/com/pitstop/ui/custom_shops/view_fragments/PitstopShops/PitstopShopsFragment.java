package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.FragmentSwitcherInterface;
import com.pitstop.ui.custom_shops.ShopAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by xirax on 2017-06-08.
 */

public class PitstopShopsFragment extends Fragment implements PitstopShopsInterface {


    private FragmentSwitcherInterface switcher;
    private PitstopShopsPresenter presenter;

    private Context context;
    private GlobalApplication application;

    private ShopAdapter shopAdapter;

    private RecyclerView shopList;


    @BindView(R.id.search_pitstop)
    SearchView searchView;




    @Override
    public void setSwitcher(FragmentSwitcherInterface switcher) {
        this.switcher = switcher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_pitstop_shops, container, false);
        ButterKnife.bind(this,view);

        shopList = (RecyclerView) view.findViewById(R.id.pitstop_shop_list);

        presenter = new PitstopShopsPresenter();
        presenter.subscribe(this,switcher);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(presenter);

        presenter.focusSearch();
        presenter.getShops();
        return view;
    }

    @Override
    public void setupList(List<Dealership> dealerships) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        shopAdapter = new ShopAdapter(dealerships,presenter);

        shopList.setAdapter(shopAdapter);
        shopList.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void focusSearch() {
        searchView.onActionViewExpanded();
    }
}
