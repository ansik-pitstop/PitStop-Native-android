package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class EmissionsPIDCommand extends ObdCommand {
    public static final String AVAILABLE = "Available";
    public static final String INCOMPLETE = "Incomplete";
    private String PID;

    public EmissionsPIDCommand(boolean hasHeaders){
        super("01 41",hasHeaders,4);
    }

//    @Override
//    protected void performCalculations() {
//        if (hasHeaders()){
//            PID = rawData.substring(7,15); //Header is first 1.5 bytes, then 2 bytes for request code
//
//        }else{
//            PID = rawData.substring(4,12); //2 bytes for request code
//        }
//    }

    @Override
    public String getFormattedResult() {
        return PID;
    }

    @Override
    public String getCalculatedResult() {
        return PID;
    }

    @Override
    public String getName() {
        return "Emmissions PID";
    }
}

