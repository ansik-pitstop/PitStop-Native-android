package com.pitstop.bluetooth.bleDevice;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public interface AbstractDevice {

    // parameters
    boolean getVin();                               //215B, 212B, ELM327, RVD

    boolean getPids(String pids);                   //215B, 212B, ELM327, RVD
    boolean getSupportedPids();                     //215B, 212B, ELM327, RVD
    boolean setPidsToSend(String pids, int timeInterval);   //215B, 212B, ELM327, RVD
    boolean requestSnapshot();   //215B, 212B, ELM327, RVD

    // monitor
    boolean clearDtcs();        //215B, 212B, ELM327, RVD(maybe)
    boolean getDtcs(); // stored    //215B, 212B, ELM327, RVD
    boolean getPendingDtcs();   //215B, 212B, ELM327, RVD(maybe)

    boolean closeConnection();  //215B, 212B, ELM327, RVD
    boolean setCommunicatorState(int state);        //idk if we need this at all
    int getCommunicatorState();     //215B, 212B, ELM327, RVD
}
