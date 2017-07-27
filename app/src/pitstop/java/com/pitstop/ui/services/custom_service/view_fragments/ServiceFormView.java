package com.pitstop.ui.services.custom_service.view_fragments;

import com.pitstop.models.service.CustomIssueListItem;

import java.util.List;

/**
 * Created by Matt on 2017-07-25.
 */

public interface ServiceFormView {
    void setActionList(List<CustomIssueListItem> items);
    void setParNameList(List<CustomIssueListItem> items);
    void setPriorityList(List<CustomIssueListItem> items);
    void togglePriorityList();
    void toggleActionList();
    void togglePartNameList();
    void showActionText(CustomIssueListItem item);
    void showPartNameText(CustomIssueListItem item);
    void showPriorityText(CustomIssueListItem item);
    void showActionText();
    void showPartNameText();

}
