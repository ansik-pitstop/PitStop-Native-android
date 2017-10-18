package com.pitstop.bluetooth

/**
 * Created by ishan on 2017-10-18.
 */
interface BluetoothWriter {

    fun writeRTCInterval(interval: Int): Boolean
    fun resetMemory(): Boolean
    fun toggleHistoricalData(toggle: Boolean): Boolean
    fun setChunkSize(chunkSize: Int): Boolean
    fun clearDTCs() : Boolean

}