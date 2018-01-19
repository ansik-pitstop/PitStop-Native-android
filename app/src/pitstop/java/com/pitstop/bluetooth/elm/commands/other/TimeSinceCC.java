package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class TimeSinceCC extends ObdCommand {
    private int minutes = 0;


    public TimeSinceCC(boolean hasHeaders){
        super("01 4E",hasHeaders,2);
    }

    @Override
    protected void performCalculations() {
        minutes  = buffer.get(2) * 256 + buffer.get(3);

    }

    @Override
    public String getFormattedResult() {
        return String.valueOf(minutes) + " minutes";
    }

    @Override
    public String getCalculatedResult() {
        return String.valueOf(minutes);
    }

    @Override
    public String getName() {
        return "TimeSinceCC";
    }
}
