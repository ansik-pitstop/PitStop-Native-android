package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand_01_20;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 1/26/2018.
 */
public class AvailablePidsCommandTest {

    /*
    * Tests response from device for the available PID command
    * Headers are not included
    * Device output includes a 1 ECU response, with NO headers
     */
    @Test
    public void readResults_OneECUResponse() throws Exception {
        System.out.println("\nRunning test readResults_OneECUResponse()");

        //Dummy input
        String INPUT = "41 00 86 07 EF 80";

        //Expected output
        List<String> headers = new ArrayList<>();

        //Expected request codes
        String REQUEST_AVAILABLE_PID = "4100";
        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_AVAILABLE_PID);

        //Expected data
        String DATA_RESPONSE_AVAILABLE_PID_ECU8 = "8607EF80";
        List<String> data = new ArrayList<>();
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECU8);

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        ObdCommand obdCommand = new AvailablePidsCommand_01_20(false);

        obdCommand.readResult(deviceOuput);


        System.out.println("EXPECTED- headers: "+headers+", data: "+data+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", data: "+obdCommand.getData()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && data.equals(obdCommand.getData()));
    }

    /*
    * Tests response from device for the available PID command
    * Headers are not included
    * Device output includes a 3 ECU response, with NO headers
     */
    @Test
    public void readResults_ThreeECUResponse() throws Exception {
        System.out.println("\nRunning test readResults_OneECUResponse()");

        //Dummy input
        String INPUT = "41 00 86 07 EF 80 41 00 81 00 00 00 41 00 00 00 00 00";

        //Expected output
        List<String> headers = new ArrayList<>();

        //Expected request codes
        String REQUEST_AVAILABLE_PID = "4100";
        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_AVAILABLE_PID);
        requestCodes.add(REQUEST_AVAILABLE_PID);
        requestCodes.add(REQUEST_AVAILABLE_PID);

        //Expected data
        String DATA_RESPONSE_AVAILABLE_PID_ECU8 = "8607EF80";
        String DATA_RESPONSE_AVAILABLE_PID_ECU9 = "81000000";
        String DATA_RESPONSE_AVAILABLE_PID_ECUA = "00000000";
        List<String> data = new ArrayList<>();
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECU8);
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECU9);
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECUA);

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        ObdCommand obdCommand = new AvailablePidsCommand_01_20(false);

        obdCommand.readResult(deviceOuput);


        System.out.println("EXPECTED- headers: "+headers+", data: "+data+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", data: "+obdCommand.getData()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && data.equals(obdCommand.getData()));
    }
}