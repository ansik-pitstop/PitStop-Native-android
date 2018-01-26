package com.pitstop.bluetooth

import com.pitstop.bluetooth.elm.enums.ObdProtocols

/**
 * Created by ishan on 2017-10-18.
 */
interface BluetoothWriter {

    fun writeRTCInterval(interval: Int): Boolean
    fun resetMemory(): Boolean
    fun clearDTCs() : Boolean
    fun setChunkSize(size: Int): Boolean
    fun requestSelectProtocol(p: ObdProtocols): Boolean


}