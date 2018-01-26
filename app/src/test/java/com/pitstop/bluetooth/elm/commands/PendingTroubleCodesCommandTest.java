package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.control.CodesCommand;
import com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;

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
public class PendingTroubleCodesCommandTest {
    @Test
    public void readResults_ManyTroubleCodesPresent() throws Exception {
        System.out.println("\nRunning test readResults_ManyTroubleCodesPresent()");

        //Dummy input
        String REQUEST_DTC = "47";
        String INPUT = "00C0:4305000100021:00030004400100";

        //Expected output
        List<String> headers = new ArrayList<>();

        List<String> requestCodes = new ArrayList<>();

        List<String> codes = new ArrayList<>();
        codes.add("P0001");
        codes.add("P0002");
        codes.add("P0003");
        codes.add("P0004");
        codes.add("C0001");

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        CodesCommand obdCommand = new PendingTroubleCodesCommand(ObdProtocols.ISO_15765_4_CAN,false);

        obdCommand.readResult(deviceOuput);

        System.out.println("EXPECTED- headers: "+headers+", codes: "+codes+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", codes: "+obdCommand.getCodes()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && codes.equals(obdCommand.getCodes()));
    }

    @Test
    public void readResults_TwoTroubleCodesPresent() throws Exception {
        System.out.println("\nRunning test readResults_TwoTroubleCodesPresent()");

        //Dummy input
        String REQUEST_DTC = "47";
        String INPUT = "430201000200";

        //Expected output
        List<String> headers = new ArrayList<>();

        List<String> requestCodes = new ArrayList<>();

        List<String> codes = new ArrayList<>();

        //Simulate environment INPUT
        InputStream deviceOuput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        CodesCommand obdCommand = new PendingTroubleCodesCommand(ObdProtocols.ISO_15765_4_CAN,false);

        obdCommand.readResult(deviceOuput);

        System.out.println("EXPECTED- headers: "+headers+", codes: "+codes+", requestCode: "+requestCodes);
        System.out.println("RESULT- headers: "+obdCommand.getHeaders()+", codes: "+obdCommand.getCodes()
                +", requestCode: "+obdCommand.getRequestCode());

        assertTrue(headers.equals(obdCommand.getHeaders())
                && requestCodes.equals(obdCommand.getRequestCode())
                && codes.equals(obdCommand.getCodes()));
    }
}