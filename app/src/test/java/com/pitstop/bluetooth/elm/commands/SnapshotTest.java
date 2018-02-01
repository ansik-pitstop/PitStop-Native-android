package com.pitstop.bluetooth.elm.commands;

import com.pitstop.bluetooth.elm.commands.control.DistanceMILOnCommand;
import com.pitstop.bluetooth.elm.commands.control.DistanceSinceCCCommand;
import com.pitstop.bluetooth.elm.commands.fuel.FindFuelTypeCommand;
import com.pitstop.bluetooth.elm.commands.other.CalibrationIDCommand;
import com.pitstop.bluetooth.elm.commands.other.CalibrationVehicleNumberCommand;
import com.pitstop.bluetooth.elm.commands.other.EmissionsPIDCommand;
import com.pitstop.bluetooth.elm.commands.other.OBDStandardCommand;
import com.pitstop.bluetooth.elm.commands.other.TimeSinceCC;
import com.pitstop.bluetooth.elm.commands.other.TimeSinceMIL;
import com.pitstop.bluetooth.elm.commands.other.WarmupsSinceCC;
import com.pitstop.bluetooth.elm.exceptions.NoDataException;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Karol Zdebel on 1/31/2018.
 */
public class SnapshotTest {

    @Test
    public void testSnapshot(){

        //Input
        final String inEmissionsPid = "21 41 00 00 11 11";
        final String distanceMILOn = "21 21 AA AA";
        final String warmupsSinceCC = "21 30 BB";
        final String distanceSinceCC = "21 31 CC CC";
        final String timeSinceCC = "21 4E DD DD";
        final String timeSinceMIL = "21 4D EE EE";
        final String calibrationID = "0130:4904014A4D421:2A3336373631352:30350000000000";
        final String calibrationVehicleNumber = "29 06 FF";
        final String findFuelType = "21 51 01";
        final String obdStandard = "21 1C 02";
        Queue<String> deviceOutputQueue = new LinkedList<>();
        deviceOutputQueue.add(inEmissionsPid);
        deviceOutputQueue.add(distanceMILOn);
        deviceOutputQueue.add(warmupsSinceCC);
        deviceOutputQueue.add(distanceSinceCC);
        deviceOutputQueue.add(timeSinceCC);
        deviceOutputQueue.add(timeSinceMIL);
        deviceOutputQueue.add(calibrationID);
        deviceOutputQueue.add(calibrationVehicleNumber);
        deviceOutputQueue.add(findFuelType);
        deviceOutputQueue.add(obdStandard);

        //0130:4904014A4D421:2A3336373631352:30350000000000
        //Expected Output
        final String outEmissionsPid = "00001111";
        final String outDistanceMILOn = "AAAA";
        final String outWarmupsSinceCC = "BB";
        final String outDistanceSinceCC = "CCCC";
        final String outTimeSinceCC = "DDDD";
        final String outTimeSinceMIL = "EEEE";
        final String outCalibrationID = "JMB*36761505";
        final String outCalibrationVehicleNumber = "FF";
        final String outFindFuelType = "01";
        final String outObdStandard = "02";
        Queue<String> calculatedOutputQueue = new LinkedList<>();
        calculatedOutputQueue.add(outEmissionsPid);
        calculatedOutputQueue.add(outDistanceMILOn);
        calculatedOutputQueue.add(outWarmupsSinceCC);
        calculatedOutputQueue.add(outDistanceSinceCC);
        calculatedOutputQueue.add(outTimeSinceCC);
        calculatedOutputQueue.add(outTimeSinceMIL);
        calculatedOutputQueue.add(outCalibrationID);
        calculatedOutputQueue.add(outCalibrationVehicleNumber);
        calculatedOutputQueue.add(outFindFuelType);
        calculatedOutputQueue.add(outObdStandard);

        //Setup command queue
        boolean headersEnabled = false;
        Queue<ObdCommand> pidCommandQueue = new LinkedList<>();
        pidCommandQueue.add(new EmissionsPIDCommand(headersEnabled));
        pidCommandQueue.add(new DistanceMILOnCommand(headersEnabled));
        pidCommandQueue.add(new WarmupsSinceCC(headersEnabled));
        pidCommandQueue.add(new DistanceSinceCCCommand(headersEnabled));
        pidCommandQueue.add(new TimeSinceCC(headersEnabled));
        pidCommandQueue.add(new TimeSinceMIL(headersEnabled));
        pidCommandQueue.add(new CalibrationIDCommand(headersEnabled));
        pidCommandQueue.add(new CalibrationVehicleNumberCommand(headersEnabled));
        pidCommandQueue.add(new FindFuelTypeCommand(headersEnabled));
        pidCommandQueue.add(new OBDStandardCommand(headersEnabled));

        //Feed each command input, compare output
        while (!pidCommandQueue.isEmpty()){
            ObdCommand commandBeingTested = pidCommandQueue.remove();
            String out = deviceOutputQueue.remove();
            InputStream deviceOuput = null;
            try{
                deviceOuput = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8.name()));
                commandBeingTested.readResult(deviceOuput);

                String result = commandBeingTested.getData() == null? null: commandBeingTested.getData().get(0);
                String expected = calculatedOutputQueue.remove();
                System.out.println("EXPECTED- data: "+expected);
                System.out.println("RESULT- data: "+result);

                assertTrue(result.equals(expected));

            }catch(IOException e){
                e.printStackTrace();
            }

        }


    }

    @Test
    public void testSnapshot_NODATA(){

        //Input
        final String inEmissionsPid = "NODATA";
        final String distanceMILOn = "NODATA";
        final String warmupsSinceCC = "NODATA";
        final String distanceSinceCC = "NODATA";
        final String timeSinceCC = "NODATA";
        final String timeSinceMIL = "NODATA";
        final String calibrationID = "NODATA";
        final String calibrationVehicleNumber = "NODATA";
        final String findFuelType = "NODATA";
        final String obdStandard = "NODATA";
        Queue<String> deviceOutputQueue = new LinkedList<>();
        deviceOutputQueue.add(inEmissionsPid);
        deviceOutputQueue.add(distanceMILOn);
        deviceOutputQueue.add(warmupsSinceCC);
        deviceOutputQueue.add(distanceSinceCC);
        deviceOutputQueue.add(timeSinceCC);
        deviceOutputQueue.add(timeSinceMIL);
        deviceOutputQueue.add(calibrationID);
        deviceOutputQueue.add(calibrationVehicleNumber);
        deviceOutputQueue.add(findFuelType);
        deviceOutputQueue.add(obdStandard);

        //0130:4904014A4D421:2A3336373631352:30350000000000
        //Expected Output
        final String outEmissionsPid = "";
        final String outDistanceMILOn = "";
        final String outWarmupsSinceCC = "";
        final String outDistanceSinceCC = "";
        final String outTimeSinceCC = "";
        final String outTimeSinceMIL = "";
        final String outCalibrationID = "";
        final String outCalibrationVehicleNumber = "";
        final String outFindFuelType = "";
        final String outObdStandard = "";
        Queue<String> calculatedOutputQueue = new LinkedList<>();
        calculatedOutputQueue.add(outEmissionsPid);
        calculatedOutputQueue.add(outDistanceMILOn);
        calculatedOutputQueue.add(outWarmupsSinceCC);
        calculatedOutputQueue.add(outDistanceSinceCC);
        calculatedOutputQueue.add(outTimeSinceCC);
        calculatedOutputQueue.add(outTimeSinceMIL);
        calculatedOutputQueue.add(outCalibrationID);
        calculatedOutputQueue.add(outCalibrationVehicleNumber);
        calculatedOutputQueue.add(outFindFuelType);
        calculatedOutputQueue.add(outObdStandard);

        //Setup command queue
        boolean headersEnabled = false;
        Queue<ObdCommand> pidCommandQueue = new LinkedList<>();
        pidCommandQueue.add(new EmissionsPIDCommand(headersEnabled));
        pidCommandQueue.add(new DistanceMILOnCommand(headersEnabled));
        pidCommandQueue.add(new WarmupsSinceCC(headersEnabled));
        pidCommandQueue.add(new DistanceSinceCCCommand(headersEnabled));
        pidCommandQueue.add(new TimeSinceCC(headersEnabled));
        pidCommandQueue.add(new TimeSinceMIL(headersEnabled));
        pidCommandQueue.add(new CalibrationIDCommand(headersEnabled));
        pidCommandQueue.add(new CalibrationVehicleNumberCommand(headersEnabled));
        pidCommandQueue.add(new FindFuelTypeCommand(headersEnabled));
        pidCommandQueue.add(new OBDStandardCommand(headersEnabled));

        //Feed each command input, compare output
        while (!pidCommandQueue.isEmpty()){
            ObdCommand commandBeingTested = pidCommandQueue.remove();
            String out = deviceOutputQueue.remove();
            InputStream deviceOuput = null;
            try{
                String expected = "";
                String result = "";
                deviceOuput = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8.name()));
                try{
                    commandBeingTested.readResult(deviceOuput);
                    result = commandBeingTested.getData() == null? null: commandBeingTested.getData().get(0);
                    expected = calculatedOutputQueue.remove();
                }catch(NoDataException e){
                }


                System.out.println("EXPECTED- data: "+expected);
                System.out.println("RESULT- data: "+result);

                assertTrue(result.equals(expected));

            }catch(IOException e){
                e.printStackTrace();
            }

        }


    }
}