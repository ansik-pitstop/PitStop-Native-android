package com.pitstop.interactors;

import com.pitstop.database.UserAdapter;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import static android.R.attr.id;

/**
 * Created by Karol Zdebel on 5/30/2017.
 */

public class MarkServiceDoneUseCaseImpl implements MarkServiceDoneUseCase {

    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private Callback callback;
    private int issueId;
    private int userId;

    public MarkServiceDoneUseCaseImpl(UserAdapter userAdapter, NetworkHelper networkHelper){
        this.userAdapter = userAdapter;
        this.networkHelper = networkHelper;
    }

    @Override
    public void run() {

    }

    @Override
    public void execute(int userId, int issueId, Callback callback) {
        this.issueId = id;
        this.userId = userId;
        this.callback = callback;
    }
}
