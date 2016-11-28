package com.pitstop.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by David on 7/22/2016.
 */
public interface ILoadingActivity {
    void showLoading(@NonNull String message);
    void hideLoading(@Nullable String message);
}
