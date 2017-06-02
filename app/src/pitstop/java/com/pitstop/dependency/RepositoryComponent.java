package com.pitstop.dependency;

import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

import dagger.Component;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@ApplicationScope
@Component(modules = RepositoryModule.class)
public interface RepositoryComponent {

    UserRepository getUserRepository();

    CarRepository getCarRepository();

    CarIssueRepository getCarIssueRepository();

}
