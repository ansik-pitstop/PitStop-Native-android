package com.pitstop.bluetooth.OBDcommands;

import com.github.pires.obd.commands.ObdCommand;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ishan on 2017-12-20.
 */

public class EmmisionsPIDCommand extends ObdCommand {
    public static final String AVAILABLE = "Available";
    public static final String INCOMPLETE = "Incomplete";
    private String PID;


    public EmmisionsPIDCommand(){
        super("01 41");
    }

    @Override
    protected void performCalculations() {
        PID = rawData;




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
