package com.pitstop.utils;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.View;

public class ViewUtils {

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(@NonNull View view, @IdRes int id) {
        return (T) view.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(@NonNull Activity activity, @IdRes int id) {
        return (T) activity.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(@NonNull Dialog dialog, @IdRes int id) {
        return (T) dialog.findViewById(id);
    }

    public static boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    public static void setGone(View view, boolean makeVisible) {
        view.setVisibility(makeVisible ? View.VISIBLE : View.GONE);
    }

    public static void setInvisible(View view, boolean makeVisible) {
        view.setVisibility(makeVisible ? View.VISIBLE : View.INVISIBLE);
    }

}
