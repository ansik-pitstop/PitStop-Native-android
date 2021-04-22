package com.pitstop.ui;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by David on 7/22/2016.
 */
public interface ILoadingActivity {
    void showLoading(@NonNull String message);
    void hideLoading(@Nullable String message);
}
