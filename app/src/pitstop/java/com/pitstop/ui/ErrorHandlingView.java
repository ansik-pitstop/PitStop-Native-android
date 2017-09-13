package com.pitstop.ui;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */

public interface ErrorHandlingView {
    void displayOfflineErrorDialog();
    void displayUnknownErrorDialog();
    void displayUnknownErrorView();
    void displayOfflineView();
    void displayOnlineView();
}
