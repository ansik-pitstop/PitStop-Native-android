package com.pitstop.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Karol Zdebel on 8/2/2017.
 */

public interface LoadingView {
    void showLoading(@NonNull String message);
    void hideLoading(@Nullable String message);
    void setLoadingCancelable(boolean cancelable);
}
