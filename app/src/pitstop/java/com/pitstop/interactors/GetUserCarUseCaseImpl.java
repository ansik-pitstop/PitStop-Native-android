package com.pitstop.interactors;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.User;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class GetUserCarUseCaseImpl implements GetUserCarUseCase {

    private LocalCarAdapter localCarAdapter;
    private NetworkHelper networkHelper;
    private int userId;
    private Callback callback;

    public GetUserCarUseCaseImpl(LocalCarAdapter localCarAdapter, NetworkHelper networkHelper) {
        this.localCarAdapter = localCarAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(int userId, Callback callback) {
        this.callback = callback;
        this.userId = userId;
        new Thread(this).start();
    }

    @Override
    public void run() {
        CarRepository.getInstance(localCarAdapter,networkHelper).get(userId, new U

            @Override
            public void onGotuser(User user){
                callback.onCarRetrieved();
            }

            @Override
            public void onError(){

            }
        });
    }
}
