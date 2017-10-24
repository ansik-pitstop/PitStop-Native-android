package com.pitstop.interactors.set

import android.os.Handler
import com.pitstop.models.Notification

/**
 * Created by Karol Zdebel on 10/24/2017.
 */
class SetNotificationReadUseCaseImpl(val useCaseHandler: Handler, val mainHandler: Handler): SetNotificationReadUseCase {

    var notification: Notification? = null
    var read: Boolean? = null
    var callback: SetNotificationReadUseCase.Callback? = null

    override fun execute(notification: Notification, read: Boolean, callback: SetNotificationReadUseCase.Callback) {
        this.notification = notification
        this.read = read
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        notification!!.isRead = read
        mainHandler.post({callback!!.onMarkedAsRead()})
    }
}