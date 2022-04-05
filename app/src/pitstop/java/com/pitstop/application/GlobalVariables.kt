package com.pitstop.application

import android.content.Context
import androidx.preference.PreferenceManager


class GlobalVariables {
    companion object {
        private const val carIdKey = "carId"
        private const val userIdKey = "userId"

        fun getMainCarId(context: Context): Int? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val carId = preferences.getInt(carIdKey, -1)
            if (carId == -1) return null
            return carId
        }

        fun setMainCarId(context: Context, carId: Int?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            if (carId == null) {
                editor.remove(carIdKey)
            } else {
                editor.putInt(carIdKey, carId)
            }
            editor.commit()
        }

        fun getUserId(context: Context): Int? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val userId = preferences.getInt(userIdKey, -1)
            if (userId == -1) return null
            return userId
        }

        fun setUserId(context: Context, userId: Int) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putInt(userIdKey, userId)
            editor.commit()
        }
    }
}