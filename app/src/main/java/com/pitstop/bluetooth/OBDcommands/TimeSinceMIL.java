package com.pitstop.bluetooth.OBDcommands;

import com.github.pires.obd.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class TimeSinceMIL extends ObdCommand {
    private int minutes = 0;

    public TimeSinceMIL(){
        super("01 4D");
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
        return "TimeSinceMIL";
    }
}
