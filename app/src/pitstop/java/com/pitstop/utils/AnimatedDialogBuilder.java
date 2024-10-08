package com.pitstop.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.pitstop.R;

/**
 * Dialog with blue custom title (default) and animations.
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
    public AnimatedDialogBuilder setTitle(CharSequence title) {
        super.setTitle(title);
        this.title = title.toString();
        return this;
    }

    @Override
    public AnimatedDialogBuilder setView(View view) {
        super.setView(view);
        return this;
    }

    @Override
    public AlertDialog create() {
        if (title != null && !title.isEmpty()){
            View customTitle = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_title_primary_dark, null);
            ((TextView)customTitle.findViewById(R.id.custom_title_text)).setText(title);
            setCustomTitle(customTitle);
        }
        AlertDialog dialog = super.create();
        try{
            dialog.getWindow().setWindowAnimations(animation);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return dialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        try{
            dialog.getWindow().setWindowAnimations(animation);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return dialog;
    }
}
