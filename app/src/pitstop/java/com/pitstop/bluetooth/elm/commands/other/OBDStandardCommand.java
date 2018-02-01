package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-21.
 */

public class OBDStandardCommand extends ObdCommand {

    public OBDStandardCommand(boolean hasHeader){
        super("01 1C",hasHeader,1);
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
        return "OBDStandardCommand";
    }
}
