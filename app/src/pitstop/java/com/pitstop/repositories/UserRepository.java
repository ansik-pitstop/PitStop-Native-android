package com.pitstop.repositories;

import com.pitstop.database.UserAdapter;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 5/29/2017.
 */

public class UserRepository implements Repository<User> {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;

    public UserRepository(UserAdapter userAdapter, NetworkHelper networkHelper){
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public boolean insert(User model, RequestCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName(),model.getPhone(),null);
        return true;
    }

    @Override
    public boolean update(User model, RequestCallback callback) {
        userAdapter.storeUserData(model);
        networkHelper.updateUser(model.getId(),model.getFirstName(),model.getLastName(),model.getPhone(),callback);
        networkHelper.setMainCar(model.getId(),model.getCurrentCar().getId(),null);
        return true;
    }

    @Override
    public User get(int id, RequestCallback callback) {
        networkHelper.getUser(id,callback);
        return userAdapter.getUser();
    }

    @Override
    public boolean delete(User model, RequestCallback callback) {
        //Not possible yet
        return false;
    }
}
