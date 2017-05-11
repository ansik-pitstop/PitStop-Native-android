package com.pitstop.view;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class TextFeedView extends AppCompatTextView {

    private StringBuilder mText;

    public TextFeedView(Context context) {
        this(context, null);
    }

    public TextFeedView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public TextFeedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mText = new StringBuilder();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        mText = new StringBuilder();
        mText.append(text);
    }

    public void addLine(String line) {
        mText.append("\n");
        mText.append(line);
        mText.append("\n");
        super.setText(mText.toString());
    }

    public void clearText() {
        setText("");
    }

}
