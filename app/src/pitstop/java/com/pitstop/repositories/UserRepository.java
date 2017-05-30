package com.pitstop.repositories;

import com.pitstop.database.UserAdapter;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * User repository, use this class to modify, retrieve, and delete user data.
 * Updates data both remotely and locally.
 *
 * Created by Karol Zdebel on 5/29/2017.
 */

public class UserRepository {

    private static UserRepository INSTANCE;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;

    public static synchronized UserRepository getInstance(UserAdapter userAdapter
            , NetworkHelper networkHelper) {
        if (INSTANCE == null) {
            INSTANCE = new UserRepository(userAdapter, networkHelper);
        }
        return INSTANCE;
    }

    public UserRepository(UserAdapter userAdapter, NetworkHelper networkHelper){
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    public boolean insert(User model, RequestCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName(),model.getPhone(),null);
        return true;
    }

    public boolean update(User model, RequestCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName(),model.getPhone(),callback);
        networkHelper.setMainCar(model.getId(),model.getCurrentCar().getId(),null);
        return true;
    }

    public User get(int id, RequestCallback callback) {
        networkHelper.getUser(id,callback);
        return userAdapter.getUser();
    }

}
