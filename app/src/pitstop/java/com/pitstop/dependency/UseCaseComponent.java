package com.pitstop.dependency;

import com.pitstop.ui.services.CurrentServicesFragment;

import dagger.Component;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@ApplicationScope
@Component(modules = UseCaseModule.class)
public interface UseCaseComponent {

    void injectUseCases(CurrentServicesFragment fragment);

}
