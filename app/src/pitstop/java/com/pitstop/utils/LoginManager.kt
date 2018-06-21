package com.pitstop.utils

import com.pitstop.models.User

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
interface LoginManager {
    fun loginUser(accessToken: String, refreshToken: String, user: User)
    fun isLoggedIn(): Boolean
}