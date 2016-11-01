package com.pitstop.ui.mainFragments;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.pitstop.R;

/**
 * Created by yifan on 16/10/31.
 */

public class AnimatedDialogBuilder extends AlertDialog.Builder{

    public static final int ANIMATION_SLIDE_RIGHT_TO_LEFT = R.style.DialogAnimations_slide;
    public static final int ANIMATION_GROW = R.style.DialogAnimations_grow;

    private int animation = ANIMATION_SLIDE_RIGHT_TO_LEFT;
    private String title;

    public AnimatedDialogBuilder(Context context) {
        super(context);
    }

    public AnimatedDialogBuilder setAnimation(final int animation){
        this.animation = animation;
        return this;
    }

    @Override
    public AlertDialog.Builder setTitle(CharSequence title) {
        this.title = title.toString();
        return super.setTitle(title);
    }

    @Override
    public AlertDialog create() {
        if (title != null && !title.isEmpty()){
            View customTitle = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_title_primary_dark, null);
            ((TextView)customTitle.findViewById(R.id.custom_title_text)).setText(title);
            setCustomTitle(customTitle);
        }
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
