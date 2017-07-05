package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.AddShopUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.interactors.UpdateShopUseCase;
import com.pitstop.models.Address;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.settings.FragmentSwitcher;

/**
 * Created by Matt on 2017-06-09.
 */

public class ShopFormPresenter {



    private ShopFormView shopForm;
    private CustomShopActivityCallback switcher1;
    private FragmentSwitcher switcher2;
    private UseCaseComponent component;

    public ShopFormPresenter(CustomShopActivityCallback switcher1, FragmentSwitcher switcher2, UseCaseComponent component){
        this.switcher1 = switcher1;
        this.switcher2 = switcher2;
        this.component = component;
    }

    public void subscribe(ShopFormView shopForm ){
        this.shopForm = shopForm;

    }

    public void clearFields(){
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
        String shopName = shopForm.getName();
        String shopPhone = shopForm.getPhone();
        String shopEmail = shopForm.getEmail();
        String shopAddress = shopForm.getAddress();
        String shopCity = shopForm.getCity();
        String shopProvince =shopForm.getProvince();
        String shopCountry = shopForm.getCountry();
        String shopPostal = shopForm.getPostal();

        if(shopName.isEmpty()){
            shopForm.showReminder("Please enter the shop name");
            return;
        }
        if(shopPhone.isEmpty() && shopEmail.isEmpty()){
            shopForm.showReminder("Please enter the shop phone or email");
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

        if(update){
          dealership.setId(shopForm.getDealership().getId());
            component.getUpdateShopUseCase().execute(dealership, new UpdateShopUseCase.Callback() {
                @Override
                public void onShopUpdated() {
                    switcher2.setViewMainSettings();
                }
                @Override
                public void onError() {
                    shopForm.toast("There was an error updating your shops details");
                }
            });

        }else{

            component.getAddShopUseCase().execute(dealership, new AddShopUseCase.Callback() {
                @Override
                public void onShopAdded() {
                    if(shopForm.getCar() != null){
                        component.getUpdateCarDealershipUseCase().execute(shopForm.getCar().getId(), dealership, new UpdateCarDealershipUseCase.Callback() {
                            @Override
                            public void onCarDealerUpdated() {
                                switcher1.endCustomShops();
                            }

                            @Override
                            public void onError() {
                                shopForm.toast("There was an error adding your shop");
                            }
                        });
                    }
                }
                @Override
                public void onError() {
                    shopForm.toast("There was an error adding your shop");
                }
            });
        }
    }

    public void fillFields(Dealership dealership){
        if(dealership == null){
            return;
        }
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
