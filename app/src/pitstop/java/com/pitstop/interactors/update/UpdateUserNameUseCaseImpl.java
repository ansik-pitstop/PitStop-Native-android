package com.pitstop.interactors.update;

import android.os.Handler;

import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matt on 2017-06-15.
 */

public class UpdateUserNameUseCaseImpl implements UpdateUserNameUseCase {
    private Handler handler;
    private UserRepository userRepository;
    private UpdateUserNameUseCase.Callback callback;
    private String name;


    public UpdateUserNameUseCaseImpl(UserRepository userRepository, Handler handler) {
        this.userRepository = userRepository;
        this.handler = handler;

    }

    @Override
    public void execute(String name, UpdateUserNameUseCase.Callback callback) {
        this.callback = callback;
        this.name = name;
        handler.post(this);
    }
    private User parseName(User user, String name){
        String f,l;
        String[] nameFL= name.split(" ");
        if( nameFL.length> 0){
            f = nameFL[0];
            if(nameFL.length>1){
                l = nameFL[1];
            }else{
                l ="";
            }
        }else{
            f = l = "";
        }
        user.setFirstName(f);
        user.setLastName(l);
        return user;
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                userRepository.update(parseName(user, name), new Repository.Callback<Object>() {
                    @Override
                    public void onSuccess(Object object) {
                        callback.onUserNameUpdated();
                    }
                    @Override
                    public void onError(RequestError error) {
                        callback.onError();

                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                callback.onError();
            }
        });

    }
}
