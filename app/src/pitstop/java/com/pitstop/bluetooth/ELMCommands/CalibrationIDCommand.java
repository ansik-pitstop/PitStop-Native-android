package com.pitstop.bluetooth.ELMCommands;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class CalibrationIDCommand extends ObdCommand {

    public CalibrationIDCommand(boolean hasHeaders){
        super("09 04",hasHeaders,16);
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
