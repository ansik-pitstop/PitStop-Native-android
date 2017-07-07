package com.pitstop.ui.settings.shop_settings;

/**
 * Created by Matthew on 2017-06-26.
 */

public interface ShopSettingsView {
    void showDeleteWarning();
    void showCantDelete();
    void toast(String message);
}
