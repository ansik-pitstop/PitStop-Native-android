package com.pitstop.repositories

/**
 * Created by Karol Zdebel on 11/1/2017.
 */
data class Response<out T>(val data: T?, val isLocal: Boolean)