package com.pitstop.ui.mainFragments;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import com.pitstop.R;

/**
 * Created by yifan on 16/10/31.
 */

public class AnimatedDialogBuilder extends AlertDialog.Builder{

    public static final int ANIMATION_SLIDE_RIGHT_TO_LEFT = R.style.DialogAnimations_slide;
    public static final int ANIMATION_GROW = R.style.DialogAnimations_grow;

    private int animation = ANIMATION_SLIDE_RIGHT_TO_LEFT;

    public AnimatedDialogBuilder(Context context) {
        super(context);
    }

    public AnimatedDialogBuilder(Context context, int theme) {
        super(context, theme);
    }

    public AnimatedDialogBuilder setAnimation(final int animation){
        this.animation = animation;
        return this;
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.getWindow().getAttributes().windowAnimations = animation;
        return dialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        dialog.getWindow().getAttributes().windowAnimations = animation;
        return dialog;
    }
}
