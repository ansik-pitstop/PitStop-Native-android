package com.pitstop.ui.add_car.ask_has_device;

import com.pitstop.ui.LoadingView;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface AskHasDeviceView extends LoadingView {
    void loadVinEntryView();
    void loadDeviceSearchView();
}
