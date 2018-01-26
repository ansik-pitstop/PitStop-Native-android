package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.other.EmissionsPIDCommand;
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
public class EmissionsPIDCommandTest {

    /*
    * Tests response from device for the emissions(2141) PID command
    * Headers are not included
    * Device output includes a 1 ECU response, with NO headers
     */
    @Test
    public void readResults_OneECUResponse() throws Exception {
        System.out.println("\nRunning test readResults_OneECUResponse()");

        //Dummy input
        String INPUT = "41 41 86 07 EF 80";

        //Expected output
        List<String> headers = new ArrayList<>();

        //Expected request codes
        String REQUEST_EMISSIONS_PID = "4141";
        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_EMISSIONS_PID);

        //Expected data
        String DATA_RESPONSE_EMISSIONS_PID = "8607EF80";
        List<String> data = new ArrayList<>();
        data.add(DATA_RESPONSE_EMISSIONS_PID);

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        ObdCommand obdCommand = new EmissionsPIDCommand(false);

        obdCommand.readResult(deviceOuput);

        System.out.println("EXPECTED- headers: "+headers+", data: "+data+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", data: "+obdCommand.getData()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && data.equals(obdCommand.getData()));
    }

    /*
    * Tests response from device for the Emissions PID command
    * Headers are not included
    * Device output includes a 2 ECU response, with NO headers
     */
    @Test
    public void readResults_TwoECUResponse() throws Exception {
        System.out.println("\nRunning test readResults_TwoECUResponse()");

        //Dummy input
        String INPUT = "41 41 AA 99 88 77 41 41 00 11 22 33";

        //Expected output
        List<String> headers = new ArrayList<>();

        //Expected request codes
        String REQUEST_EMISSIONS_PID = "4141";
        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_EMISSIONS_PID);
        requestCodes.add(REQUEST_EMISSIONS_PID);

        //Expected data
        String DATA_RESPONSE_EMISSIONS_PID_ECU8 = "AA998877";
        String DATA_RESPONSE_EMISSIONS_PID_ECU9 = "00112233";
        List<String> data = new ArrayList<>();
        data.add(DATA_RESPONSE_EMISSIONS_PID_ECU8);
        data.add(DATA_RESPONSE_EMISSIONS_PID_ECU9);

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