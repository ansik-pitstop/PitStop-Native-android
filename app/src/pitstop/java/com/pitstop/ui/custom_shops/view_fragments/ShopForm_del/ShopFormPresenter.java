package com.pitstop.ui.custom_shops.view_fragments.ShopForm_del;

import android.app.Fragment;
import android.content.res.Resources;

import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddShopUseCase;
import com.pitstop.interactors.update.UpdateCarDealershipUseCase;
import com.pitstop.interactors.update.UpdateShopUseCase;
import com.pitstop.models.Address;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.settings.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matt on 2017-06-09.
 */

public class ShopFormPresenter {



    private ShopFormView shopForm;
    private CustomShopActivityCallback switcher1;
    private FragmentSwitcher switcher2;
    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;

    private String googlePlacesId;

    public ShopFormPresenter(CustomShopActivityCallback switcher1, FragmentSwitcher switcher2, UseCaseComponent component, MixpanelHelper mixpanelHelper){
        this.switcher1 = switcher1;
        this.switcher2 = switcher2;
        this.component = component;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(ShopFormView shopForm ){
        mixpanelHelper.trackViewAppeared("ShopForm");
        this.shopForm = shopForm;
    }

    public void unsubscribe() {
        this.shopForm = null;
    }

    public void clearFields(){
        if(shopForm == null){return;}
        googlePlacesId = "";
        shopForm.showName("");
        shopForm.showPhone("");
        shopForm.showEmail("");
        shopForm.showAddress("");
        shopForm.showCity("");
        shopForm.showProvince("");
        shopForm.showCountry("");
        shopForm.showPostal("");
    }

    public void submitShop(Boolean update){
         if(shopForm == null){return;}
        mixpanelHelper.trackButtonTapped("Submit","ShopForm");
        String shopName = shopForm.getName();
        String shopPhone = shopForm.getPhone();
        String shopEmail = shopForm.getEmail();
        String shopAddress = shopForm.getAddress();
        String shopCity = shopForm.getCity();
        String shopProvince =shopForm.getProvince();
        String shopCountry = shopForm.getCountry();
        String shopPostal = shopForm.getPostal();

        if(shopName.isEmpty()){
            shopForm.showReminder(((Fragment)shopForm).getString(R.string.enter_shop_name_message));
            return;
        }
        if(shopPhone.isEmpty() && shopEmail.isEmpty()){
            shopForm.showReminder(((Fragment)shopForm).getString(R.string.enter_shop_email_or_phone_message));
            return;
        }
        Dealership dealership = new Dealership();
        dealership.setName(shopName);
        dealership.setPhoneNumber(shopPhone);
        dealership.setEmail(shopEmail);
        if(!shopCity.isEmpty()){shopCity = ","+shopCity;}
        if(!shopProvince.isEmpty()){shopProvince = ","+shopProvince;}
        if(!shopPostal.isEmpty()){shopPostal = ","+shopPostal;}
        if(!shopCountry.isEmpty()){shopCountry = ","+shopCountry;}
        dealership.setAddress(shopAddress+shopCity+shopProvince+shopPostal+shopCountry);
        dealership.setGooglePlaceId(googlePlacesId);
        if(update){
          dealership.setId(shopForm.getDealership().getId());
            component.getUpdateShopUseCase().execute(dealership, EventSource.SOURCE_SETTINGS, new UpdateShopUseCase.Callback() {
                @Override
                public void onShopUpdated() {
                    if(shopForm != null){
                        switcher2.setViewMainSettings();
                    }
                }
                @Override
                public void onError(RequestError error) {
                    if(shopForm != null){
                        shopForm.toast(((Fragment)shopForm).getString(R.string.error_updating_shop_details_toast_massage));
                    }
                }
            });

        }else{
            component.getAddShopUseCase().execute(dealership, new AddShopUseCase.Callback() {
                @Override
                public void onShopAdded() {
                        component.getUpdateCarDealershipUseCase().execute(shopForm.getCar().getId(), dealership,EventSource.SOURCE_SETTINGS, new UpdateCarDealershipUseCase.Callback() {
                            @Override
                            public void onCarDealerUpdated() {
                                if(shopForm != null){
                                    switcher1.endCustomShops();
                                }
                            }

                            @Override
                            public void onError(RequestError error) {
                                if(shopForm != null){
                                    shopForm.toast(((Fragment)shopForm).getString(R.string.error_adding_shops_toast_massage));
                                }
                            }
                        });
                }
                @Override
                public void onError(RequestError error) {
                    if(shopForm != null){
                        shopForm.toast(((Fragment)shopForm).getString(R.string.error_adding_shops_toast_massage));
                    }
                }
            });
        }
    }

    public void fillFields(Dealership dealership){
        if(shopForm == null){return;}
        if(dealership == null){
            return;
        }
        googlePlacesId = dealership.getGooglePlaceId();
        Address address = new Address(dealership.getAddress());
        if(dealership.getName() != null){
            shopForm.showName(dealership.getName());
        }
        if(dealership.getPhone() !=null){
            shopForm.showPhone(dealership.getPhone());
        }
        if(dealership.getEmail() != null){
            shopForm.showEmail(dealership.getEmail());
        }
        if(address.getStreet() != null){
            shopForm.showAddress(address.getStreet());
        }
        if(address.getCity() != null){
            shopForm.showCity(address.getCity());
        }
        if(address.getProvince()!= null){
            shopForm.showProvince(address.getProvince());
        }
        if(address.getPostal() != null){
            shopForm.showPostal(address.getPostal());
        }
        if(address.getCountry() != null){
            shopForm.showCountry(address.getCountry());
        }
    }


}
