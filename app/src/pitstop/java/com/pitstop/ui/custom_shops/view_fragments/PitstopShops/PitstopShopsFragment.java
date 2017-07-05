package com.pitstop.ui.custom_shops.view_fragments.PitstopShops;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.custom_shops.ShopAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-06-08.
 */

public class PitstopShopsFragment extends Fragment implements PitstopShopsInterface {


    private CustomShopActivityCallback switcher;
    private PitstopShopsPresenter presenter;

    private Context context;
    private GlobalApplication application;

    private Car car;

    private ShopAdapter shopAdapter;


    @BindView(R.id.pitstop_shop_list)
    RecyclerView shopList;

    @BindView(R.id.pitstopshops_progress)
    ProgressBar progressBar;


    @BindView(R.id.search_pitstop)
    SearchView searchView;




    @Override
    public void setSwitcher(CustomShopActivityCallback switcher) {
        this.switcher = switcher;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_pitstop_shops, container, false);
        ButterKnife.bind(this,view);


        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        presenter = new PitstopShopsPresenter(switcher,component);
        presenter.subscribe(this);



        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                presenter.filterShops(newText);
                return false;
            }
        });
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.focusSearch();
            }
        });
        presenter.getShops();
        return view;
    }

    @Override
    public void showConfirmation(Dealership dealership) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());//will probably need to move these to the activity
        alertDialogBuilder.setTitle("Set shop to "+dealership.getName());
        alertDialogBuilder
                .setMessage("Change the shop of this car")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        presenter.changeShop(dealership);
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();

                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT);
    }

    @Override
    public void loading(boolean show) {
        if(show){
            progressBar.setVisibility(View.VISIBLE);
        }else{
            progressBar.setVisibility(View.GONE);
        }
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
