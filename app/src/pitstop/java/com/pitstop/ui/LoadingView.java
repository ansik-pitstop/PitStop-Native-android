package com.pitstop.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Karol Zdebel on 8/2/2017.
 */

public interface LoadingView {

    int PROGRESS_MAX = 100;
    int PROGRESS_MIN = 0;

    void showLoading(@NonNull String message, boolean indeterminate);
    void hideLoading(@Nullable String message);
    void setLoadingCancelable(boolean cancelable);
    void setLoadingProgress(int progress);
}
