package com.pitstop.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Karol Zdebel on 8/2/2017.
 */

public interface LoadingView {

    //Displays aindeterminate progress dialog with a particular message
    void showLoading(@NonNull String message);

    void showLoading(@NonNull int message);

    //Hides progress dialog and displays Toast message if one is specified
    void hideLoading(@Nullable String message);

    //Sets whether or not the progress dialog can be cancelled by some other action
    void setLoadingCancelable(boolean cancelable);
}
