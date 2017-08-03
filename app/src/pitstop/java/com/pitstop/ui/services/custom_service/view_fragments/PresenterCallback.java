package com.pitstop.ui.services.custom_service.view_fragments;

import com.pitstop.models.service.CustomIssueListItem;

/**
 * Created by Matt on 2017-07-26.
 */

public interface PresenterCallback {
    void onActionItemClicked(CustomIssueListItem item);
    void onPartNameItemClicked(CustomIssueListItem item);
    void onPriorityItemClicked(CustomIssueListItem item);
    void onActionOther();
    void onPartNameOther();
}
