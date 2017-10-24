package com.pitstop.interactors.set

import com.pitstop.interactors.Interactor
import com.pitstop.models.Notification

/**
 * Created by Karol Zdebel on 10/24/2017.
 */
interface SetNotificationReadUseCase: Interactor {
    interface Callback{
        fun onMarkedAsRead()
    }

    fun execute(notifications: List<Notification>, read: Boolean, callback: Callback)
}