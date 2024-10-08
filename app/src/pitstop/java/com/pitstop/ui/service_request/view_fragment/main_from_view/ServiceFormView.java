package com.pitstop.ui.service_request.view_fragment.main_from_view;

import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.service_request.RequestServiceCallback;

import java.util.List;

/**
 * Created by Matthew on 2017-07-11.
 */

public interface ServiceFormView {
    void showShop(String name,String address);
    void toggleCalender();
    void showDate(String date);
    void hideCalender();
    void showCalender();
    void setupTimeList(List<String> times);
    void toggleTimeList();
    void showTime(String time);
    void hideTimeList();
    void setupPresetIssues(List<CarIssue> issues);
    List<CarIssue> getPresetList();
    void toggleServiceList();
    void setupSelectedIssues(List<CarIssue> issues);
    void showReminder(String message);
    String getComments();
    void toast(String message);
    void showLoading(boolean show);
    void setCommentHint(String hint);
    void disableButton(boolean disable);
    void showLoadingTime(boolean show);
    void finish();
    RequestServiceCallback getRequestServiceCallback();

}
