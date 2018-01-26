package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-30.
 */

public class HeaderOnCommand extends ObdCommand {

    public HeaderOnCommand() {
        super("ATH 1",false,true,1);
    }

    @Override
    protected void performCalculations() {

    }

    @Override
    public String getFormattedResult() {
        return "";
    }

    @Override
    public String getCalculatedResult() {
        return "";
    }

    @Override
    public String getName() {
        return HeaderOnCommand.class.getSimpleName();
    }
}
