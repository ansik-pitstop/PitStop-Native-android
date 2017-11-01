package com.pitstop.repositories

import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 11/1/2017.
 */
data class Response<out T>(val data: T, val error: RequestError?, val isLocal: Boolean)