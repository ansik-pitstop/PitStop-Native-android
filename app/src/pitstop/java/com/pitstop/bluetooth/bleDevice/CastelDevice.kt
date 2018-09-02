package com.pitstop.bluetooth.bleDevice

import java.util.*

/**
 * Created by Karol Zdebel on 9/2/2018.
 */
interface CastelDevice: LowLevelDevice {
    abstract fun getServiceUuid(): UUID       //215B, 212B
    abstract fun getReadChar(): UUID          //215B, 212B
    abstract fun getWriteChar(): UUID         //215B, 212B
    abstract fun getBytes(payload: String): ByteArray    //215B, 212B
    abstract fun parseData(data: ByteArray)        //215B, 212B

    abstract fun getRtc(): Boolean                                //215B, 212B
    abstract fun setRtc(rtcTime: Long): Boolean                    //215B, 212B

    abstract fun getFreezeFrame(): Boolean
}