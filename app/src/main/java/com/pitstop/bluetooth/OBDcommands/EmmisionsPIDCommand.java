package com.pitstop.bluetooth.OBDcommands;

import com.github.pires.obd.commands.ObdCommand;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ishan on 2017-12-20.
 */

public class EmmisionsPIDCommand extends ObdCommand {

    public EmmisionsPIDCommand(){
        super("01 41");
    }

    @Override
    protected void performCalculations() {

    }

    @Override
    public String getFormattedResult() {
        return rawData;
    }

    @Override
    public String getCalculatedResult() {
        return rawData;
    }

    @Override
    public String getName() {
        return "Emmissions PID";
    }
}
