package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.control.VinCommand;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 1/26/2018.
 */
public class VinCommandTest {
    @Test
    public void readResults_VINShort() throws Exception {
        System.out.println("\nRunning test run_VinCommand()");

        String INPUT = "09020140:4902014A41331:325532465534412:55363038363032";
        String EXPECTED_VIN = "JA32U2FU4AU608602";

        InputStream deviceOutput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        VinCommand obdCommand = new VinCommand(false);
        obdCommand.readResult(deviceOutput);

        System.out.println("EXPECTED- data: "+EXPECTED_VIN);
        System.out.println("RESULT-, data: "+obdCommand.getCalculatedResult());

        assertTrue(EXPECTED_VIN.equals(obdCommand.getCalculatedResult()));
    }

    @Test
    public void readResults_VINLong() throws Exception {
        System.out.println("\nRunning test run_VinCommand()");

        String INPUT = "0140:4902014A41331:325532465534412:55363038363032";
        String EXPECTED_VIN = "JA32U2FU4AU608602";

        InputStream deviceOutput = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8.name()));
        VinCommand obdCommand = new VinCommand(false);
        obdCommand.readResult(deviceOutput);

        System.out.println("EXPECTED- data: "+EXPECTED_VIN);
        System.out.println("RESULT-, data: "+obdCommand.getCalculatedResult());

        assertTrue(EXPECTED_VIN.equals(obdCommand.getCalculatedResult()));
    }
}