package com.pitstop.bluetooth.ELMCommands;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class CalibrationVehicleNumberCommand extends ObdCommand {

    public CalibrationVehicleNumberCommand(){
        super("09 06");
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
        return CalibrationVehicleNumberCommand.class.getSimpleName();
    }
}
