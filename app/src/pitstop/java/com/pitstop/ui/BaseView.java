package com.pitstop.ui;

/**
 * Created by yifan on 16/11/21.
 */

public interface BaseView<T extends BasePresenter> {
    void setPresenter(T presenter);
}
