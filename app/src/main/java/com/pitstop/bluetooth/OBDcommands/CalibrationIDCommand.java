package com.pitstop.bluetooth.OBDcommands;

import com.github.pires.obd.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class CalibrationIDCommand extends ObdCommand {

    public CalibrationIDCommand(){
        super("09 04");
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
        return CalibrationIDCommand.class.getSimpleName();
    }
}
