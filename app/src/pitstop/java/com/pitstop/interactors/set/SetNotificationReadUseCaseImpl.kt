package com.pitstop.interactors.set

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.models.Notification
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 10/24/2017.
 */
class SetNotificationReadUseCaseImpl(val useCaseHandler: Handler, val mainHandler: Handler): SetNotificationReadUseCase {

    private var notifications: List<Notification>? = null
    private var read: Boolean? = null
    private var callback: SetNotificationReadUseCase.Callback? = null
    private val tag = javaClass.simpleName

    override fun execute(notifications: List<Notification>, read: Boolean, callback: SetNotificationReadUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.notifications = notifications
        this.read = read
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        for (n in notifications.orEmpty())
            n.isRead = read
        Logger.getInstance()!!.logI(tag, "Use case finished", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onMarkedAsRead()})
    }
}