package com.pitstop.dependency;

import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.ui.service_request.ServiceRequestActivity;
import com.pitstop.ui.services.CurrentServicesFragment;
import com.pitstop.ui.services.HistoryServiceFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by Karol Zdebel on 6/5/2017.
 */

@Singleton
@Component(modules = UseCaseModule.class)
public interface UseCaseComponent {

    void injectUseCases(CurrentServicesFragment fragment);

    void injectUseCases(HistoryServiceFragment fragment);

    void injectUseCases(ServiceRequestActivity serviceRequestActivity);
}
