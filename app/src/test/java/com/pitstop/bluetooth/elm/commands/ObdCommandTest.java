package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.control.VinCommand;
import com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand_01_20;

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
public class ObdCommandTest {

    //Headers
    private final String HEADER_MODULE8 = "7E8";
    private final String HEADER_MODULE9 = "7E9";
    private final String HEADER_MODULEA = "7EA";

    private final String REQUEST_AVAILABLE_PID = "4100";

    private final String DATA_RESPONSE_AVAILABLE_PID_ECU8 = "8607EF80";
    private final String DATA_RESPONSE_AVAILABLE_PID_ECU9 = "81000000";
    private final String DATA_RESPONSE_AVAILABLE_PID_ECUA = "00000000";

    @Test
    public void readRawData_AvailablePidsCommand() throws Exception {
        System.out.println("\nRunning test readRawData_AvailablePidsCommand_NoCheckByte()");

        //Dummy input
        String INPUT = "7E8 41 00 86 07 EF 80 7E9 41 00 81 00 00 00";

        //Expected output
        List<String> headers = new ArrayList<>();
        headers.add(HEADER_MODULE8);
        headers.add(HEADER_MODULE9);

        List<String> requestCodes = new ArrayList<>();
        requestCodes.add(REQUEST_AVAILABLE_PID);
        requestCodes.add(REQUEST_AVAILABLE_PID);

        List<String> data = new ArrayList<>();
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECU8);
        data.add(DATA_RESPONSE_AVAILABLE_PID_ECU9);

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        ObdCommand obdCommand = new AvailablePidsCommand_01_20(true);

        obdCommand.readRawData(deviceOuput);


        System.out.println("EXPECTED- headers: "+headers+", data: "+data+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", data: "+obdCommand.getData()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && data.equals(obdCommand.getData()));
    }

    @Test
    public void run_VinCommand() throws Exception {
        System.out.println("\nRunning test run_VinCommand()");

        String INPUT_1 = "0140:4902014A41331:325532465534412:55363038363032";
        String INPUT_2 = "09020140:4902014A41331:325532465534412:55363038363032";
        String EXPECTED_VIN = "JA32U2FU4AU608602";

        InputStream deviceOutput = new ByteArrayInputStream(INPUT_2.getBytes(StandardCharsets.UTF_8.name()));
        VinCommand obdCommand = new VinCommand(false);
        obdCommand.readResult(deviceOutput);

        System.out.println("EXPECTED- data: "+EXPECTED_VIN);
        System.out.println("RESULT-, data: "+obdCommand.getCalculatedResult());

        assertTrue(EXPECTED_VIN.equals(obdCommand.getCalculatedResult()));


    }

}