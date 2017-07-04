package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.settings.FragmentSwitcher;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-06-09.
 */

public class ShopFormFragment extends Fragment implements ShopFormInterface {
    private Context context;
    private GlobalApplication application;

    private ShopFormPresenter presenter;
    private CustomShopActivityCallback switcher1;
    private FragmentSwitcher switcher2;

    private boolean update = false;

    @BindView(R.id.shop_name)
    EditText shopName;

    @BindView(R.id.phone_number)
    EditText phoneNumber;

    @BindView(R.id.enter_email)
    EditText email;

    @BindView(R.id.street_address)
    EditText streetAddress;

    @BindView(R.id.postal_code)
    EditText postalCode;

    @BindView(R.id.enter_city)
    EditText city;

    @BindView(R.id.enter_province)
    EditText province;// or state

    @BindView(R.id.enter_country)
    EditText country;

    @BindView(R.id.submit_shop_button)
    Button submitShop;

    private Dealership dealership;
    private Car car;

    public void setUpdate(boolean update) {
        this.update = update;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public Dealership getDealership() {
        return dealership;
    }

    @Override
    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public void setDealership(Dealership dealership) {
        this.dealership = dealership;
    }

    @Override
    public void setSwitcher(CustomShopActivityCallback switcher) {
        this.switcher1 = switcher;
    }

    @Override
    public void setSwitcher(FragmentSwitcher switcher) {
        this.switcher2 = switcher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity().getApplicationContext();
        application = (GlobalApplication) context;

        View view = inflater.inflate(R.layout.fragment_shop_form, container, false);
        ButterKnife.bind(this,view);
        presenter = new ShopFormPresenter();
        presenter.subscribe(this,switcher1,switcher2);
        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(presenter);

        submitShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.submitShop(update);
            }
        });
        if(dealership!= null){
            presenter.fillFields(dealership);
        }
        return view;
    }

    @Override
    public void showReminder(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setTitle("Field Missing");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void toast(String message) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT);
    }

    @Override
    public void showError() {
        Toast.makeText(context,"Unable to add shop please try again",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(dealership != null){
            presenter.fillFields(dealership);
        }else{
            presenter.clearFields();
        }
    }

    @Override
    public String getName() {
        return shopName.getText().toString();
    }

    @Override
    public String getPhone() {
        return phoneNumber.getText().toString();
    }

    @Override
    public String getEmail() {
        return email.getText().toString();
    }

    @Override
    public String getAddress() {
        return streetAddress.getText().toString();
    }

    @Override
    public String getCity() {
        return city.getText().toString();
    }

    @Override
    public String getProvince() {
        return province.getText().toString();
    }

    @Override
    public String getPostal() {
        return postalCode.getText().toString();
    }

    @Override
    public String getCountry() {
        return country.getText().toString();
    }

    @Override
    public void showName(String name) {
        this.shopName.setText(name);
    }

    @Override
    public void showPhone(String phone) {
        this.phoneNumber.setText(phone);
    }

    @Override
    public void showEmail(String email) {
        this.email.setText(email);
    }

    @Override
    public void showAddress(String address) {
        this.streetAddress.setText(address);
    }

    @Override
    public void showCity(String city) {
        this.city.setText(city);
    }

    @Override
    public void showProvince(String province) {
        this.province.setText(province);
    }

    @Override
    public void showPostal(String postal) {
        this.postalCode.setText(postal);
    }

    @Override
    public void showCountry(String country) {
        this.country.setText(country);
    }



}

