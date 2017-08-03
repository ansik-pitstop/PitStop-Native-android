package com.pitstop.models.service;

import com.pitstop.R;

/**
 * Created by Matt on 2017-07-26.
 */

public class CustomIssueListItem {
    private String text;
    private String key;
    private String cardColor;
    private String textColor;

    public void setText(String text) {
        this.text = text;
    }

    public void setCardColor(String cardColor) {
        this.cardColor = cardColor;
    }

    public void setKey(String key) {
        this.key = key;
    }
    public void setTextColor(String textColor){
        this.textColor = textColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getCardColor() {
        return cardColor;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }
}
