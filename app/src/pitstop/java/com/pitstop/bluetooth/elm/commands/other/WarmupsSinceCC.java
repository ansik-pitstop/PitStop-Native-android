package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class WarmupsSinceCC extends ObdCommand {

    public WarmupsSinceCC(boolean hasHeaders){
        super("01 30",hasHeaders,1);
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
        return "WarmupsSinceCC";
    }
}
