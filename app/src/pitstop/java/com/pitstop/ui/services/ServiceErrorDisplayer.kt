package com.pitstop.ui.services

/**
 * Created by Karol Zdebel on 2/9/2018.
 */
interface ServiceErrorDisplayer {
    fun displayServiceErrorDialog(message: String)
    fun displayServiceErrorDialog(code: Int)
}