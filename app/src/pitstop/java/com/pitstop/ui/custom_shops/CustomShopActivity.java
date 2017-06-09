package com.pitstop.ui.custom_shops;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.ui.custom_shops.view_fragments.PitstopShops.PitstopShopsFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopForm.ShopFormFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopSearch.ShopSearchFragment;
import com.pitstop.ui.custom_shops.view_fragments.ShopType.ShopTypeFragment;

/**
 * Created by matt on 2017-06-07.
 */

public class CustomShopActivity extends AppCompatActivity implements CustomShopInterface,FragmentSwitcherInterface{
    private ShopTypeFragment shopTypeFragment;
    private ShopSearchFragment shopSearchFragment;
    private PitstopShopsFragment pitstopShopsFragment;
    private ShopFormFragment shopFormFragment;
    private CustomShopPresenter presenter;
    private FragmentManager fragmentManager;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_shop);

        fragmentManager = getFragmentManager();
        shopSearchFragment = new ShopSearchFragment();
        shopTypeFragment = new ShopTypeFragment();
        pitstopShopsFragment = new PitstopShopsFragment();
        shopFormFragment = new ShopFormFragment();
        shopSearchFragment.setSwitcher(this);
        shopTypeFragment.setSwitcher(this);
        pitstopShopsFragment.setSwitcher(this);
        shopFormFragment.setSwitcher(this);

        presenter = new CustomShopPresenter();
        presenter.subscribe(this,this);
        presenter.setViewCustomShop();
        presenter.setUpNavBar();
    }

    @Override
    public void setUpNavBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setViewShopType() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopTypeFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewSearchShop() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopSearchFragment);
        fragmentTransaction.addToBackStack("search_shop");
        fragmentTransaction.commit();
    }

    @Override
    public void setViewPitstopShops() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, pitstopShopsFragment);
        fragmentTransaction.addToBackStack("pitstop_shops");
        fragmentTransaction.commit();
    }

    @Override
    public void setViewShopForm() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.custom_shop_fragment_holder, shopFormFragment);
        fragmentTransaction.addToBackStack("shop_form");
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
