package com.pitstop.bluetooth.bleDevice

import java.util.*

/**
 * Created by Karol Zdebel on 9/2/2018.
 */
interface CastelDevice: LowLevelDevice {
    fun getServiceUuid(): UUID       //215B, 212B
    fun getReadChar(): UUID          //215B, 212B
    fun getWriteChar(): UUID         //215B, 212B
    fun getBytes(payload: String): ByteArray    //215B, 212B
    fun parseData(data: ByteArray)        //215B, 212B
    fun getRtc(): Boolean                                //215B, 212B
    fun setRtc(rtcTime: Long): Boolean                    //215B, 212B
    fun getFreezeFrame(): Boolean
}