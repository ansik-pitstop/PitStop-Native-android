package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 1/19/2018.
 */
public class PendingTroubleCodesCommandTest {

    //Headers
    private final String HEADER_MODULE8 = "7E8";
    private final String HEADER_MODULE9 = "7E9";
    private final String HEADER_MODULEA = "7EA";

    private final String REQUEST_DTC = "47";

    private final String DATA_RESPONSE_PENDING_DTC_ECU8 = "";
    private final String DATA_RESPONSE_AVAILABLE_PID_ECU9 = "";
    private final String DATA_RESPONSE_AVAILABLE_PID_ECUA = "00000000";

    @Test
    public void readRawData() throws Exception {
        System.out.println("\nRunning test readRawData");

        //Dummy input
        String INPUT = "47 00 47 00";

        //Expected output
        List<String> headers = new ArrayList<>();
//        headers.add(HEADER_MODULE8);
//        headers.add(HEADER_MODULE9);

        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_DTC);
        requestCodes.add(REQUEST_DTC);

        List<String> data = new ArrayList<>();

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        ObdCommand obdCommand = new PendingTroubleCodesCommand(false);

        obdCommand.readRawData(deviceOuput);


        System.out.println("EXPECTED- headers: "+headers+", data: "+data+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", data: "+obdCommand.getData()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && data.equals(obdCommand.getData()));
    }

}