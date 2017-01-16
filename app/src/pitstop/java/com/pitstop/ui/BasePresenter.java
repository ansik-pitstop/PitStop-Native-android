package com.pitstop.ui;

/**
 * The Presenter needs:
 *      A reference to the Model
 *      A reference to the View
 */
public interface BasePresenter {

    void bind(BaseView<? extends BasePresenter> view);

    void unbind();

}
