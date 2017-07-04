package com.pitstop.ui.custom_shops.view_fragments.ShopForm;

import com.pitstop.interactors.AddShopUseCase;
import com.pitstop.interactors.UpdateCarDealershipUseCase;
import com.pitstop.interactors.UpdateShopUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.ui.custom_shops.CustomShopActivityCallback;
import com.pitstop.ui.settings.FragmentSwitcher;

import javax.inject.Inject;

/**
 * Created by Matt on 2017-06-09.
 */

public class ShopFormPresenter {

    @Inject
    AddShopUseCase addShopUseCase;

    @Inject
    UpdateCarDealershipUseCase updateCarDealershipUseCase;

    @Inject
    UpdateShopUseCase updateShopUseCase;

    private ShopFormInterface shopForm;
    private CustomShopActivityCallback switcher1;
    private FragmentSwitcher switcher2;
    public void subscribe(ShopFormInterface shopForm, CustomShopActivityCallback switcher1, FragmentSwitcher switcher2){
        this.shopForm = shopForm;
        this.switcher1 = switcher1;
        this.switcher2 = switcher2;

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
            updateShopUseCase.execute(dealership, new UpdateShopUseCase.Callback() {
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

            addShopUseCase.execute(dealership, new AddShopUseCase.Callback() {
                @Override
                public void onShopAdded() {
                    if(shopForm.getCar() != null){
                        updateCarDealershipUseCase.execute(shopForm.getCar().getId(), dealership, new UpdateCarDealershipUseCase.Callback() {
                            @Override
                            public void onCarDealerUpdated() {
                                switcher1.endActivity();
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
    private class Address{
        private String street;
        private String city;
        private String province;
        private String country;
        private String postal;
        Address(String address){
            String[] comaBreak = address.split(",");
            if(comaBreak.length>0){
                street = comaBreak[0];
            }
            if(comaBreak.length>1){
                city = comaBreak[1];
            }
            if(comaBreak.length>2){
                province = comaBreak[2];
            }
            if(comaBreak.length>3){
                postal = comaBreak[3];
            }
            if(comaBreak.length>4){
                country = comaBreak[4];
            }
        }

        public String getStreet() {
            return street;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }

        public String getPostal() {
            return postal;
        }

        public String getProvince() {
            return province;
        }
    }

}
