package com.pitstop.bluetooth.OBDcommands;

import com.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class EmissionsPIDCommand extends ObdCommand {
    public static final String AVAILABLE = "Available";
    public static final String INCOMPLETE = "Incomplete";
    private String PID;

    public EmissionsPIDCommand(){
        super("01 41");
    }

    @Override
    protected void performCalculations() {
        if (isHeaders()){
            PID = rawData.substring(9,18);

        }else{
            PID = rawData.substring(6,14);
        }
    }

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

